package com.github.g4memas0n.services.config;

import com.github.g4memas0n.services.Services;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Settings class that represent the configuration file of this plugin.
 *
 * @author G4meMason
 * @since Release 1.0.0
 */
public final class Settings {

    private final Services instance;
    private final YamlConfiguration storage;

    private Set<DamageCause> blacklist;
    private Set<Environment> environments;
    private Set<Material> items;
    private Set<Material> disabled;
    private Set<PotionEffectType> effects;
    private Set<UUID> worlds;

    private boolean action;
    private boolean buckets;
    private boolean debug;
    private boolean durability;
    private boolean environment;
    private boolean item;
    private boolean world;

    private int maximum;
    private int warmup;
    private int grace;

    public Settings(@NotNull final Services instance) {
        this.instance = instance;
        this.storage = new YamlConfiguration();
    }

    public void load() {
        final File config = new File(this.instance.getDataFolder(), "config.yml");

        try {
            this.storage.load(config);

            this.instance.getLogger().info("Loaded configuration file: " + config.getName());
        } catch (FileNotFoundException ex) {
            this.instance.getLogger().warning("Unable to find configuration file: " + config.getName() + " (Saving default configuration...)");
            this.instance.saveResource(config.getName(), true);
            this.instance.getLogger().info("Saved default configuration from template: " + config.getName());

            this.load();
            return;
        } catch (InvalidConfigurationException ex) {
            this.instance.getLogger().warning("Unable to load broken configuration file: " + config.getName() + " (Renaming it and saving default configuration...)");

            final File broken = new File(config.getParent(), config.getName().replaceAll("(?i)(yml)$", "broken.$1"));

            if (broken.exists() && broken.delete()) {
                this.instance.getLogger().info("Deleted old broken configuration file: " + broken.getName());
            }

            if (config.renameTo(broken)) {
                this.instance.getLogger().info("Renamed broken configuration file to: " + broken.getName());
            }

            this.instance.saveResource(config.getName(), true);
            this.instance.getLogger().info("Saved default configuration from template: " + config.getName());

            this.load();
            return;
        } catch (IOException ex) {
            this.instance.getLogger().warning("Unable to load configuration file: " + config.getName() + " (Loading default configuration...)");

            /*
             * Removing each key manual to clear existing configuration, as loading a blank config does not work here
             * for any reason.
             */
            this.storage.getKeys(false).forEach(key -> this.storage.set(key, null));

            this.instance.getLogger().info("Loaded default configuration from template: " + config.getName());
        }

        this.blacklist = this._getDamageBlacklist();
        this.environments = this._getServiceEnvironments();
        this.items = this._getServiceItems();
        this.disabled = this._getDisabledDrops();
        this.effects = this._getDisabledEffects();
        this.worlds = this._getServiceWorlds();

        this.action = this._getNotifyActionBar();
        this.buckets = this._getUnlimitedBuckets();
        this.durability = this._getUnlimitedDurability();
        this.environment = this._getPermissionPerEnvironment();
        this.item = this._getPermissionPerItem();
        this.world = this._getPermissionPerWorld();
        this.debug = this._getDebug();

        this.maximum = this._getDamageMaximum();
        this.warmup = this._getWarmupPeriod();
        this.grace = this._getGracePeriod();
    }

    @SuppressWarnings("unused")
    public void save() {
        /*
         * Disabled, because it is not intended to save the config file, as this breaks the comments.
         */
    }

    private @NotNull Set<DamageCause> _getDamageBlacklist() {
        final Set<DamageCause> blacklist = EnumSet.noneOf(DamageCause.class);

        for (final String name : this.storage.getStringList("damage.blacklist")) {
            try {
                blacklist.add(DamageCause.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                this.instance.getLogger().warning("Detected invalid damage cause: Cause '" + name + "' does not exist.");
            }
        }

        return Collections.unmodifiableSet(blacklist);
    }

    public boolean isDamageBlacklist(@NotNull final DamageCause cause) {
        return this.blacklist.contains(cause);
    }

    private int _getDamageMaximum() {
        final int damage = this.storage.getInt("damage.maximum", 0);

        if (damage < 0) {
            this.instance.getLogger().warning("Detected invalid damage maximum: Maximum is negative.");

            return 0;
        }

        return damage;
    }

    public boolean isDamageMaximum(final double damage) {
        return this.maximum > 0 && this.maximum < damage;
    }

    private boolean _getDebug() {
        return this.storage.getBoolean("debug", false);
    }

    public boolean isDebug() {
        return this.debug;
    }

    private @NotNull Locale _getLocale() {
        final String locale = this.storage.getString("locale");

        if (locale != null && !locale.isEmpty()) {
            final Matcher match = Pattern.compile("^([a-zA-Z]{2,8})([_-]([a-zA-Z]{2}|\\d{3}))?$").matcher(locale);

            if (match.matches()) {
                return match.group(3) == null ? new Locale(match.group(1)) : new Locale(match.group(1), match.group(3));
            }

            this.instance.getLogger().warning("Detected invalid locale: Locale does not match regex.");
        }

        return Locale.ENGLISH;
    }

    public @NotNull Locale getLocale() {
        return this._getLocale();
    }

    private @NotNull Set<Material> _getDisabledDrops() {
        final Set<Material> materials = EnumSet.noneOf(Material.class);

        for (final String name : this.storage.getStringList("feature.disabled-drops")) {
            final NamespacedKey key = NamespacedKey.fromString(name.toLowerCase());

            if (key == null) {
                this.instance.getLogger().warning("Detected malformed disabled-drop item: Key '" + name + "' is invalid.");
                continue;
            }

            final Material material = Registry.MATERIAL.get(key);

            if (material == null) {
                this.instance.getLogger().warning("Detected invalid disabled-drop item: Material '" + name + "' does not exist.");
                continue;
            }

            if (!this.items.contains(material)) {
                this.instance.getLogger().warning("Detected invalid disabled-drop item: Material '" + name + "' is not a service item.");
                continue;
            }

            materials.add(material);
        }

        return Collections.unmodifiableSet(materials);
    }

    public boolean isDisabledDrops() {
        return !this.disabled.isEmpty();
    }

    public boolean isDisabledDrop(@NotNull final Material material) {
        return this.disabled.contains(material);
    }

    private @NotNull Set<PotionEffectType> _getDisabledEffects() {
        final Set<PotionEffectType> effects = new HashSet<>();

        for (final String name : this.storage.getStringList("feature.disabled-effects")) {
            final PotionEffectType effect = PotionEffectType.getByName(name.toUpperCase());

            if (effect == null) {
                this.instance.getLogger().warning("Detected invalid potion effect: Potion Effect '" + name + "' does not exist.");
                continue;
            }

            effects.add(effect);
        }

        return Collections.unmodifiableSet(effects);
    }

    public boolean isDisabledEffects() {
        return !this.effects.isEmpty();
    }

    public boolean isDisabledEffect(@NotNull final PotionEffectType effect) {
        return this.effects.contains(effect);
    }

    private boolean _getUnlimitedBuckets() {
        return this.storage.getBoolean("feature.unlimited-buckets", false);
    }

    public boolean isUnlimitedBuckets() {
        return this.buckets;
    }

    private boolean _getUnlimitedDurability() {
        return this.storage.getBoolean("feature.unlimited-durability", false);
    }

    public boolean isUnlimitedDurability() {
        return this.durability;
    }

    private boolean _getNotifyActionBar() {
        return this.storage.getBoolean("notify.action-bar", true);
    }

    public boolean isNotifyActionBar() {
        return this.action;
    }

    private int _getWarmupPeriod() {
        final int period = this.storage.getInt("period.warmup", 3);

        if (period < 0 || period > 10) {
            this.instance.getLogger().warning("Detected invalid warmup period: Period is out of range.");

            return 3;
        }

        return period;
    }

    public int getWarmupPeriod() {
        return this.warmup;
    }

    public boolean isWarmupPeriod() {
        return this.warmup > 0;
    }

    private int _getGracePeriod() {
        final int period = this.storage.getInt("period.grace", 1);

        if (period < 0 || period > 10) {
            this.instance.getLogger().warning("Detected invalid grace period: Period is out of range.");

            return 1;
        }

        return period;
    }

    public int getGracePeriod() {
        return this.grace;
    }

    public boolean isGracePeriod() {
        return this.grace > 0;
    }

    private boolean _getPermissionPerEnvironment() {
        return this.storage.getBoolean("permission.per-environment", true);
    }

    public boolean isPermissionPerEnvironment() {
        return this.environment;
    }

    private boolean _getPermissionPerItem() {
        return this.storage.getBoolean("permission.per-item", false);
    }

    public boolean isPermissionPerItem() {
        return this.item;
    }

    private boolean _getPermissionPerWorld() {
        return this.storage.getBoolean("permission.per-world", false);
    }

    public boolean isPermissionPerWorld() {
        return this.world;
    }

    private @NotNull Set<Environment> _getServiceEnvironments() {
        final Set<Environment> environments = EnumSet.noneOf(Environment.class);

        for (final String name : this.storage.getStringList("service.environments")) {
            try {
                environments.add(Environment.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                this.instance.getLogger().warning("Detected invalid service environment: Environment '" + name + "' does not exist.");
            }
        }

        return Collections.unmodifiableSet(environments);
    }

    public boolean isServiceEnvironment(@NotNull final Environment environment) {
        if (this.environments.isEmpty()) {
            return true;
        }

        return this.environments.contains(environment);
    }

    public boolean isServiceGameMode(@NotNull final GameMode mode) {
        // Note: service-mode for other game-modes as survival make no sense.
        return mode == GameMode.SURVIVAL;
    }

    private @NotNull Set<Material> _getServiceItems() {
        final Permission wildcard = this.instance.getServer().getPluginManager().getPermission("services.item.*");
        final Set<Material> materials = EnumSet.noneOf(Material.class);

        for (final String name : this.storage.getStringList("service.items")) {
            final NamespacedKey key = NamespacedKey.fromString(name.toLowerCase());

            if (key == null) {
                this.instance.getLogger().warning("Detected malformed service item: Key '" + name + "' is invalid.");
                continue;
            }

            final Material material = Registry.MATERIAL.get(key);

            if (material == null) {
                this.instance.getLogger().warning("Detected invalid service item: Material '" + key + "' does not exist.");
                continue;
            }

            if (!material.isItem()) {
                this.instance.getLogger().warning("Detected invalid service item: Material '" + key + "' is not an obtainable item.");
                continue;
            }

            if (material.isEdible() || Registry.ENTITY_TYPE.get(key) != null || key.getKey().contains("bottle")) {
                this.instance.getLogger().warning("Detected invalid service item: Material '" + key + "' is not an allowed item.");
                continue;
            }

            if (wildcard != null) {
                wildcard.getChildren().put("services.item." + material.getKey().getKey(), true);
            }

            materials.add(material);
        }

        if (materials.isEmpty()) {
            this.instance.getLogger().warning("Detected missing or only invalid service items: Using default items...");

            materials.addAll(Arrays.asList(Material.BEDROCK, Material.WOODEN_AXE));
        }

        return Collections.unmodifiableSet(materials);
    }

    public boolean isServiceItem(@NotNull final ItemStack... stacks) {
        for (final ItemStack stack : stacks) {
            if (this.isServiceItem(stack.getType())) {
                return true;
            }
        }

        return false;
    }

    public boolean isServiceItem(@NotNull final Material item) {
        if (!item.isItem()) {
            throw new IllegalArgumentException("Material is not an obtainable item");
        }

        return this.items.contains(item);
    }

    private @NotNull Set<UUID> _getServiceWorlds() {
        final Permission wildcard = this.instance.getServer().getPluginManager().getPermission("services.world.*");
        final Set<UUID> worlds = new HashSet<>();

        for (final String name : this.storage.getStringList("service.worlds")) {
            final World world = this.instance.getServer().getWorld(name);

            if (world == null) {
                this.instance.getLogger().warning("Detected invalid service world: World '" + name + "' does not exist.");
                continue;
            }

            if (wildcard != null) {
                wildcard.getChildren().put("services.world." + world.getName().toLowerCase(), true);
            }

            worlds.add(world.getUID());
        }

        return Collections.unmodifiableSet(worlds);
    }

    public boolean isServiceWorld(@NotNull final World world) {
        if (this.worlds.isEmpty()) {
            return true;
        }

        return this.worlds.contains(world.getUID());
    }
}

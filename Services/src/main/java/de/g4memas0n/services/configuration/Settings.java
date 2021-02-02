package de.g4memas0n.services.configuration;

import de.g4memas0n.services.Services;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.permissions.Permission;
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

    private static final String CONFIG = "config.yml";

    private final Services instance;
    private final YamlConfiguration storage;

    private Set<DamageCause> blacklist;
    private Set<Environment> environments;
    private Set<Material> items;
    private Set<UUID> worlds;

    private boolean environment;
    private boolean item;
    private boolean world;
    private boolean buckets;
    private boolean durability;
    private boolean action;
    private boolean debug;

    private int maximum;
    private int warmup;
    private int grace;

    public Settings(@NotNull final Services instance) {
        this.instance = instance;
        this.storage = new YamlConfiguration();
    }

    public void load() {
        final File config = new File(this.instance.getDataFolder(), CONFIG);

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
        this.worlds = this._getServiceWorlds();

        this.environment = this._getPermissionPerEnvironment();
        this.item = this._getPermissionPerItem();
        this.world = this._getPermissionPerWorld();
        this.buckets = this._getUnlimitedBuckets();
        this.durability = this._getUnlimitedDurability();
        this.action = this._getNotifyActionBar();
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

    protected @NotNull Set<DamageCause> _getDamageBlacklist() {
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

    protected int _getDamageMaximum() {
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

    protected boolean _getDebug() {
        return this.storage.getBoolean("debug", false);
    }

    public boolean isDebug() {
        return this.debug;
    }

    protected @NotNull Locale _getLocale() {
        final String locale = this.storage.getString("locale");

        if (locale != null && !locale.isEmpty()) {
            final Matcher match = Pattern.compile("^([a-zA-Z]{2,8})([_-]([a-zA-Z]{2}|[0-9]{3}))?$").matcher(locale);

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

    protected boolean _getUnlimitedBuckets() {
        return this.storage.getBoolean("features.unlimited-buckets", false);
    }

    public boolean isUnlimitedBuckets() {
        return this.buckets;
    }

    protected boolean _getUnlimitedDurability() {
        return this.storage.getBoolean("features.unlimited-durability", false);
    }

    public boolean isUnlimitedDurability() {
        return this.durability;
    }

    protected int _getWarmupPeriod() {
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

    protected int _getGracePeriod() {
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

    protected boolean _getPermissionPerEnvironment() {
        return this.storage.getBoolean("permission.per-environment", true);
    }

    public boolean isPermissionPerEnvironment() {
        return this.environment;
    }

    protected boolean _getPermissionPerItem() {
        return this.storage.getBoolean("permission.per-item", false);
    }

    public boolean isPermissionPerItem() {
        return this.item;
    }

    protected boolean _getPermissionPerWorld() {
        return this.storage.getBoolean("permission.per-world", false);
    }

    public boolean isPermissionPerWorld() {
        return this.world;
    }

    protected @NotNull Set<Environment> _getServiceEnvironments() {
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

    protected @NotNull Set<Material> _getServiceItems() {
        final Permission wildcard = this.instance.getDescription().getPermissions().get(1);
        final Set<Material> materials = EnumSet.noneOf(Material.class);

        for (final String name : this.storage.getStringList("service.items")) {
            final Material material = Material.matchMaterial(name, false);

            if (material == null) {
                this.instance.getLogger().warning("Detected invalid service item: Material '" + name + "' does not exist.");
                continue;
            }

            if (!material.isItem()) {
                this.instance.getLogger().warning("Detected invalid service item: Material '" + name + "' is not an obtainable item.");
                continue;
            }

            if (material.isEdible() || EntityType.fromName(material.getKey().getKey()) != null) {
                this.instance.getLogger().warning("Detected invalid service item: Material '" + name + "' is not an allowed item.");
                continue;
            }

            wildcard.getChildren().put(this.instance.getPermission(material), true);
            materials.add(material);
        }

        if (materials.isEmpty()) {
            this.instance.getLogger().warning("Detected missing or only invalid service items: Using default items...");

            materials.addAll(Arrays.asList(Material.BEDROCK, Material.WOODEN_AXE));
        }

        return Collections.unmodifiableSet(materials);
    }

    public boolean isServiceItem(@NotNull final Material item) {
        if (!item.isItem()) {
            throw new IllegalArgumentException("Material is not an obtainable item");
        }

        return this.items.contains(item);
    }

    protected @NotNull Set<UUID> _getServiceWorlds() {
        final Permission wildcard = this.instance.getDescription().getPermissions().get(2);
        final Set<UUID> worlds = new HashSet<>();

        for (final String name : this.storage.getStringList("service.worlds")) {
            final World world = this.instance.getServer().getWorld(name);

            if (world == null) {
                this.instance.getLogger().warning("Detected invalid service world: World '" + name + "' does not exist.");
                continue;
            }

            wildcard.getChildren().put(this.instance.getPermission(world), true);
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

    protected boolean _getNotifyActionBar() {
        return this.storage.getBoolean("notify.action-bar", true);
    }

    public boolean isNotifyActionBar() {
        return this.action;
    }
}

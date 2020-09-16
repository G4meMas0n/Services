package de.g4memas0n.services.storage.configuration;

import de.g4memas0n.services.Services;
import de.g4memas0n.services.storage.YamlStorageFile;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The Settings class that represent the configuration file of this plugin.
 *
 * @author G4meMason
 * @since Release 1.0.0
 */
public final class Settings {

    private static final String FILE_CONFIG = "config.yml";
    private static final String FILE_CONFIG_BROKEN = "config.broken.yml";

    private final Services instance;
    private final YamlStorageFile storage;

    // Service-Condition-Settings:
    private Set<Environment> environments;
    private Set<GameMode> modes;
    private Set<Material> items;
    private Set<String> worlds;

    // Permission-Settings:
    private boolean environment;
    private boolean item;
    private boolean world;

    // Feature-Settings:
    private boolean buckets;
    private boolean durability;

    // Period-Settings:
    private int warmup;
    private int grace;

    // Notify-Settings:
    private boolean action;

    public Settings(@NotNull final Services instance) {
        this.instance = instance;
        this.storage = new YamlStorageFile(new File(instance.getDataFolder(), FILE_CONFIG));
    }

    @SuppressWarnings("unused")
    public void delete() {
        try {
            this.storage.delete();

            this.instance.getLogger().debug("Deleted configuration file: " + this.storage.getFile().getName());
        } catch (IOException ex) {
            this.instance.getLogger().warning(String.format("Unable to delete configuration file '%s': %s",
                    this.storage.getFile().getName(), ex.getMessage()));
        }
    }

    public void load() {
        try {
            this.storage.load();

            this.instance.getLogger().debug("Loaded configuration file: " + this.storage.getFile().getName());
        } catch (FileNotFoundException ex) {
            this.instance.getLogger().warning(String.format("Unable to find configuration file '%s'. "
                    + "Saving default configuration...", this.storage.getFile().getName()));

            this.instance.saveResource(FILE_CONFIG, true);
            this.instance.getLogger().info(String.format("Saved default configuration from template: %s", FILE_CONFIG));
            this.load();
        } catch (InvalidConfigurationException ex) {
            this.instance.getLogger().severe(String.format("Unable to load configuration file '%s', because it is broken. "
                    + "Renaming it and saving default configuration...", this.storage.getFile().getName()));

            final File broken = new File(this.instance.getDataFolder(), FILE_CONFIG_BROKEN);

            if (broken.exists() && broken.delete()) {
                this.instance.getLogger().debug("Deleted old broken configuration file: " + broken.getName());
            }

            if (this.storage.getFile().renameTo(broken)) {
                this.instance.getLogger().info(String.format("Renamed broken configuration file '%s' to: %s",
                        this.storage.getFile().getName(), broken.getName()));
            }

            this.instance.saveResource(FILE_CONFIG, true);
            this.instance.getLogger().info(String.format("Saved default configuration from template: %s", FILE_CONFIG));
            this.load();
        } catch (IOException ex) {
            this.instance.getLogger().warning(String.format("Unable to load configuration file '%s'. "
                    + "Loading default configuration...", this.storage.getFile().getName()));

            this.storage.clear();
        }

        this.environments = this._getServiceEnvironments();
        this.modes = this._getServiceGameModes();
        this.items = this._getServiceItems();
        this.worlds = this._getServiceWorlds();

        this.environment = this._getPermissionPerEnvironment();
        this.item = this._getPermissionPerItem();
        this.world = this._getPermissionPerWorld();

        this.buckets = this._getUnlimitedBuckets();
        this.durability = this._getUnlimitedDurability();

        this.warmup = this._getWarmupPeriod();
        this.grace = this._getGracePeriod();

        this.action = this._getNotifyActionBar();
    }

    @SuppressWarnings("unused")
    public void save() {
        /*
        Disabled, because it is not intended to save the config file, as this breaks the comments.
        try {
            this.storage.save();
            this.instance.getLogger().debug("Saved configuration file: " + this.storage.getFile().getName());
        } catch (IOException ex) {
            this.instance.getLogger().warning(String.format("Unable to save configuration file '%s': %s",
                    this.storage.getFile().getName(), ex.getMessage()));
        }
         */
    }

    // Plugin-Settings Methods:
    protected boolean _getDebug() {
        return this.storage.getBoolean("debug", false);
    }

    public boolean isDebug() {
        return this._getDebug();
    }

    protected @NotNull Locale _getLocale() {
        final Locale locale = this.storage.getLocale("locale");

        if (locale == null) {
            return Locale.ENGLISH;
        }

        return locale;
    }

    public @NotNull Locale getLocale() {
        return this._getLocale();
    }

    // Feature-Settings Methods:
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

    // Period-Settings Methods:
    protected int _getWarmupPeriod() {
        final int period = this.storage.getInt("period.warmup", 3);

        if (period < 0 || period > 10) {
            this.instance.getLogger().warning(String.format("Detected invalid warmup period in configuration file '%s': "
                    + "Period is out of range.", this.storage.getFile().getName()));

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
            this.instance.getLogger().warning(String.format("Detected invalid grace period in configuration file '%s': "
                    + "Period is out of range.", this.storage.getFile().getName()));

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

    // Permission-Settings Methods:
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

    // Service-Condition-Settings Methods:
    protected @NotNull @Unmodifiable Set<Environment> _getServiceEnvironments() {
        final Set<Environment> environments = new HashSet<>();

        for (final String name : this.storage.getStringList("service.environments")) {
            try {
                environments.add(Environment.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                this.instance.getLogger().warning(String.format("Detected invalid environment in configuration file '%s': "
                        + "Environment '%s' does not exist.", this.storage.getFile().getName(), name));
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

    protected @NotNull @Unmodifiable Set<GameMode> _getServiceGameModes() {
        // Check if hidden configuration option is set.
        if (this.storage.contains("service.game-modes")) {
            final Set<GameMode> modes = new HashSet<>();

            for (final String name : this.storage.getStringList("service.game-modes")) {
                try {
                    modes.add(GameMode.valueOf(name.toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    this.instance.getLogger().warning(String.format("Detected invalid game-mode in configuration file '%s': "
                            + "Game-Mode '%s' does not exist.", this.storage.getFile().getName(), name));
                }
            }

            return Collections.unmodifiableSet(modes);
        }

        return Collections.singleton(GameMode.SURVIVAL);
    }

    public boolean isServiceGameMode(@NotNull final GameMode mode) {
        if (this.modes.isEmpty()) {
            return true;
        }

        return this.modes.contains(mode);
    }

    protected @NotNull @Unmodifiable Set<Material> _getServiceItems() {
        final Set<Material> materials = new HashSet<>();

        for (final String name : this.storage.getStringList("service.items")) {
            final Material material = Material.matchMaterial(name, false);

            if (material == null) {
                this.instance.getLogger().warning(String.format("Detected invalid item in configuration file '%s': "
                        + "Material '%s' does not exist.", this.storage.getFile().getName(), name));
                continue;
            }

            if (!material.isItem()) {
                this.instance.getLogger().warning(String.format("Detected invalid item in configuration file '%s': "
                        + "Material '%s' is not an obtainable item.", this.storage.getFile().getName(), name));
                continue;
            }

            if (material.isEdible()) {
                this.instance.getLogger().warning(String.format("Detected invalid item in configuration file '%s': "
                        + "Material '%s' is not an allowed item.", this.storage.getFile().getName(), name));
                continue;
            }

            // Checks if the current material is an entity type. Throws IllegalArgumentException if not.
            try {
                EntityType.valueOf(material.getKey().getKey().toUpperCase());

                this.instance.getLogger().warning(String.format("Detected invalid item in configuration file '%s': "
                        + "Material '%s' is not an allowed item.", this.storage.getFile().getName(), name));
            } catch (IllegalArgumentException ex) {
                materials.add(material);
            }
        }

        if (materials.isEmpty()) {
            this.instance.getLogger().warning(String.format("Detected missing or only invalid items in configuration file '%s': "
                    + "Using default items...", this.storage.getFile().getName()));

            materials.addAll(Arrays.asList(Material.BEDROCK, Material.WOODEN_AXE));
        }

        return Collections.unmodifiableSet(materials);
    }

    public boolean isServiceItem(@NotNull final Material item) {
        if (!item.isItem()) {
            throw new IllegalArgumentException();
        }

        return this.items.contains(item);
    }

    protected @NotNull @Unmodifiable Set<String> _getServiceWorlds() {
        final Set<String> worlds = new HashSet<>();

        for (final String name : this.storage.getStringList("service.worlds")) {
            final World world = this.instance.getServer().getWorld(name);

            if (world == null) {
                this.instance.getLogger().warning(String.format("Detected invalid world in configuration file '%s': "
                        + "World '%s' does not exist.", this.storage.getFile().getName(), name));
                continue;
            }

            worlds.add(world.getName());
        }

        return Collections.unmodifiableSet(worlds);
    }

    public boolean isServiceWorld(@NotNull final World world) {
        if (this.worlds.isEmpty()) {
            return true;
        }

        return this.worlds.contains(world.getName());
    }

    protected boolean _getNotifyActionBar() {
        return this.storage.getBoolean("notify.action-bar", false);
    }

    public boolean isNotifyActionBar() {
        return this.action;
    }
}

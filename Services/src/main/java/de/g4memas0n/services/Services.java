package de.g4memas0n.services;

import de.g4memas0n.services.command.BasicPluginCommand;
import de.g4memas0n.services.command.ServicesCommand;
import de.g4memas0n.services.listener.BasicListener;
import de.g4memas0n.services.listener.ConditionListener;
import de.g4memas0n.services.listener.ServiceListener;
import de.g4memas0n.services.storage.configuration.Settings;
import de.g4memas0n.services.util.Permission;
import de.g4memas0n.services.util.logging.BasicLogger;
import de.g4memas0n.services.util.messaging.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * The Services main class.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public final class Services extends JavaPlugin {

    private final Set<BasicPluginCommand> commands;
    private final Set<BasicListener> listeners;

    private final BasicLogger logger;

    private ServiceManager manager;
    private Settings settings;
    private Messages messages;

    private boolean loaded;
    private boolean enabled;

    public Services() {
        this.commands = new HashSet<>(2, 1);
        this.listeners = new HashSet<>(3, 1);

        this.logger = new BasicLogger(super.getLogger(), "Plugin", "Services");
    }

    public @NotNull ServiceManager getServiceManager() {
        return this.manager;
    }

    public @NotNull Settings getSettings() {
        return this.settings;
    }

    @SuppressWarnings("unused")
    public @NotNull Messages getMessages() {
        return this.messages;
    }

    @Override
    public void onLoad() {
        if (this.loaded) {
            this.getLogger().severe("Tried to load plugin twice. Plugin is already loaded.");
            return;
        }

        this.settings = new Settings(this);
        this.settings.load();

        this.messages = new Messages(this.getDataFolder(), this.logger);
        this.messages.setLocale(this.settings.getLocale());

        this.manager = new ServiceManager(this);

        this.loaded = true;
    }

    @Override
    public void onEnable() {
        if (this.enabled) {
            this.getLogger().severe("Tried to enable plugin twice. Plugin is already enabled.");
            return;
        }

        if (!this.loaded) {
            this.getLogger().warning("Plugin was not loaded. Loading it...");
            this.onLoad();
        }

        if (this.commands.isEmpty()) {
            this.commands.add(new ServicesCommand());
        }

        if (this.listeners.isEmpty()) {
            this.listeners.add(new ConditionListener());
            this.listeners.add(new ServiceListener());
        }

        this.getLogger().debug("Register all plugin commands and listeners...");

        this.listeners.forEach(listener -> listener.register(this));

        this.getLogger().debug("All plugin commands and listeners has been registered.");

        for (final Player player : this.getServer().getOnlinePlayers()) {
            this.handleConditionCheck(player);
        }

        this.enabled = true;
    }

    @Override
    public void onDisable() {
        if (!this.enabled) {
            this.getLogger().severe("Tried to disable plugin twice. Plugin is already disabled.");
            return;
        }

        for (final Player player : this.getServer().getOnlinePlayers()) {
            if (this.manager.removeFromCondition(player)) {
                if (this.manager.removeFromWarmup(player) || this.manager.removeFromService(player)) {
                    this.manager.removeFromGrace(player);

                    player.sendMessage(this.messages.translate("serviceDisable"));
                }

                this.manager.removeFromGrace(player);
            }
        }

        this.getLogger().debug("Unregister all plugin commands and listeners...");

        this.listeners.forEach(BasicListener::unregister);

        this.getLogger().debug("All plugin commands and listeners has been unregistered.");

        this.messages.disable();

        this.manager = null;
        this.settings = null;
        this.messages = null;

        this.enabled = false;
        this.loaded = false;
    }

    @Override
    public @NotNull BasicLogger getLogger() {
        return this.logger;
    }

    @Override
    public void reloadConfig() {
        this.settings.load();

        this.logger.setDebug(this.settings.isDebug());

        this.messages.setLocale(this.settings.getLocale());

        // Update for all plugin commands the no-permission message.
        for (final BasicPluginCommand command : this.commands) {
            command.getCommand().setPermissionMessage(this.messages.translate("noPermission"));
        }

        // Check for all online players the condition and the service.
        for (final Player player : this.getServer().getOnlinePlayers()) {
            this.handleConditionCheck(player);
        }
    }

    @Override
    public void saveConfig() {
        // Disabled, because it is not intended to save the config file, as this breaks the comments.
    }

    public @NotNull BukkitTask runTask(@NotNull final Runnable task) {
        return this.getServer().getScheduler().runTask(this, task);
    }

    public @NotNull BukkitTask scheduleTask(@NotNull final Runnable task, final long delay) {
        return this.getServer().getScheduler().runTaskLater(this, task, delay);
    }

    public void handleConditionCheck(@NotNull final Player target) {
        if (!target.hasPermission(Permission.SERVICE.getNode())) {
            if (this.manager.removeFromCondition(target)) {
                this.logger.debug(String.format("Player '%s' is no longer in condition, as he is no longer permitted to use services.", target.getName()));

                if (this.manager.removeFromWarmup(target) || this.manager.removeFromService(target)) {
                    this.manager.removeFromGrace(target);

                    target.sendMessage(this.messages.translate("serviceDenied"));
                }
            }

            return;
        }

        // Check if current game-mode of the player is a service game-mode.
        if (this.settings.isServiceGameMode(target.getGameMode())) {
            // If true, check if player is in a service world.
            if (this.settings.isServiceWorld(target.getWorld())) {
                // Filter world if per world permission is enabled.
                if (this.settings.isPermissionPerWorld()) {
                    if (!target.hasPermission(Permission.WORLD.getChildren(target.getWorld().getName()))) {
                        if (this.manager.removeFromCondition(target)) {
                            this.logger.debug(String.format("Service player '%s' no longer in condition.", target.getName()));

                            if (this.manager.removeFromWarmup(target) || this.manager.removeFromService(target)) {
                                this.manager.removeFromGrace(target);

                                target.sendMessage(this.messages.format("worldDenied", target.getWorld().getName()));
                            }
                        }

                        return;
                    }
                }

                // If true, check if player is in a service environment.
                if (this.settings.isServiceEnvironment(target.getWorld().getEnvironment())) {
                    // Filter environment if per environment permission is enabled.
                    if (this.settings.isPermissionPerEnvironment()) {
                        if (!target.hasPermission(Permission.ENVIRONMENT.getChildren(target.getWorld().getEnvironment().name().toLowerCase()))) {
                            // Check if player gets removed from condition.
                            if (this.manager.removeFromCondition(target)) {
                                this.logger.debug(String.format("Service player '%s' no longer in condition.", target.getName()));

                                // If true, check if player gets removed from warmup or services.
                                if (this.manager.removeFromWarmup(target) || this.manager.removeFromService(target)) {
                                    this.manager.removeFromGrace(target);

                                    final String name = target.getWorld().getEnvironment().name().charAt(0)
                                            + target.getWorld().getEnvironment().name().substring(1).toLowerCase();

                                    target.sendMessage(this.messages.format("environmentDenied", name));
                                }
                            }

                            return;
                        }
                    }

                    // If true, check if player gets added to condition.
                    if (this.manager.addToCondition(target)) {
                        this.logger.debug(String.format("Service player '%s' is now in condition.", target.getName()));

                        this.handleServiceCheck(target, target.getInventory().getItemInMainHand().getType());
                    }

                    return;
                }
                // Check if player gets removed from condition.
                if (this.manager.removeFromCondition(target)) {
                    this.logger.debug(String.format("Service player '%s' no longer in condition.", target.getName()));

                    // If true, check if player gets removed from warmup or services.
                    if (this.manager.removeFromWarmup(target) || this.manager.removeFromService(target)) {
                        this.manager.removeFromGrace(target);

                        final String name = target.getWorld().getEnvironment().name().charAt(0)
                                + target.getWorld().getEnvironment().name().substring(1).toLowerCase();

                        target.sendMessage(this.messages.format("noServiceEnvironment", name));
                    }
                }

                return;
            }

            // Check if player gets removed from condition.
            if (this.manager.removeFromCondition(target)) {
                this.logger.debug(String.format("Service player '%s' no longer in condition.", target.getName()));

                // If true, check if player gets removed from warmup or services.
                if (this.manager.removeFromWarmup(target) || this.manager.removeFromService(target)) {
                    this.manager.removeFromGrace(target);

                    target.sendMessage(this.messages.format("noServiceWorld", target.getWorld().getName()));
                }
            }

            return;
        }

        // Check if player gets removed from condition.
        if (this.manager.removeFromCondition(target)) {
            this.logger.debug(String.format("Service player '%s' no longer in condition.", target.getName()));

            // If true, check if player gets removed from warmup or services.
            if (this.manager.removeFromWarmup(target) || this.manager.removeFromService(target)) {
                final String mode = target.getGameMode().name().charAt(0) + target.getGameMode().name().substring(1).toLowerCase();

                target.sendMessage(this.messages.format("noServiceGameMode", mode));
            }
        }
    }

    public void handleServiceCheck(@NotNull final Player target, @Nullable final Material item) {
        if (!this.manager.isInCondition(target.getUniqueId())) {
            return;
        }

        // Check if new item slot contains a item and if it is a service item.
        if (item != null && this.settings.isServiceItem(item)) {
            // Check if permission per item is enabled.
            if (this.settings.isPermissionPerItem()) {
                // If true, check if player has permission for this service item.
                if (!target.hasPermission(Permission.ITEM.getChildren(item.getKey().getKey()))) {
                    // If not, check if player gets removed from warmup.
                    if (this.manager.removeFromWarmup(target)) {
                        target.sendMessage(this.messages.translate("warmupAbort"));
                    }

                    // Check if player is currently in service.
                    if (this.manager.isInService(target.getUniqueId())) {
                        // If true, check if the grace period exist.
                        if (this.settings.isGracePeriod()) {
                            // If true, check if player gets added to grace.
                            if (this.manager.addToGrace(target, this.settings.getGracePeriod())) {
                                target.sendMessage(this.messages.format("graceStart", this.settings.getGracePeriod()));
                            }
                        } else {
                            // If false, check if player gets removed from service.
                            if (this.manager.removeFromService(target)) {
                                target.sendMessage(this.messages.translate("serviceDisable"));
                            }
                        }
                    }

                    return;
                }
            }

            // Check if player gets removed from grace.
            if (this.manager.removeFromGrace(target)) {
                return; // Maybe add information message about grace abort.
            }

            // Check if a warmup period exist.
            if (this.settings.isWarmupPeriod()) {
                // Check if player gets added to warmup.
                if (this.manager.addToWarmup(target, this.settings.getWarmupPeriod())) {
                    target.sendMessage(this.messages.format("warmupStart", this.settings.getWarmupPeriod()));
                }
            } else {
                // Check if player gets added to service.
                if (this.manager.addToService(target)) {
                    target.sendMessage(this.messages.translate("serviceEnabled"));
                }
            }

            return;
        }

        // Check if player gets removed from warmup.
        if (this.manager.removeFromWarmup(target)) {
            target.sendMessage(this.messages.translate("warmupAbort"));
        }

        // Check if player is currently in service.
        if (this.manager.isInService(target.getUniqueId())) {
            // If true, check if the grace period exist.
            if (this.settings.isGracePeriod()) {
                // If true, check if player gets added to grace.
                if (this.manager.addToGrace(target, this.settings.getGracePeriod())) {
                    target.sendMessage(this.messages.format("graceStart", this.settings.getGracePeriod()));
                }
            } else {
                // If false, check if player gets removed from service.
                if (this.manager.removeFromService(target)) {
                    target.sendMessage(this.messages.translate("serviceDisable"));
                }
            }
        }
    }
}

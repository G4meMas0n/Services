package de.g4memas0n.services;

import de.g4memas0n.services.command.BasicPluginCommand;
import de.g4memas0n.services.command.ServicesCommand;
import de.g4memas0n.services.listener.BasicListener;
import de.g4memas0n.services.listener.ConditionListener;
import de.g4memas0n.services.listener.FeatureListener;
import de.g4memas0n.services.listener.ServiceListener;
import de.g4memas0n.services.storage.configuration.Settings;
import de.g4memas0n.services.util.Permission;
import de.g4memas0n.services.util.logging.BasicLogger;
import de.g4memas0n.services.util.messaging.Messages;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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

    private final Set<BasicListener> listeners;

    private final BasicPluginCommand command;
    private final BasicLogger logger;

    private ServiceManager manager;
    private Settings settings;
    private Messages messages;

    private boolean loaded;
    private boolean enabled;

    public Services() {
        this.listeners = new HashSet<>(4, 1);
        this.command = new ServicesCommand();

        this.logger = new BasicLogger(super.getLogger(), "Plugin", this.getName());
    }

    public @NotNull ServiceManager getServiceManager() {
        return this.manager;
    }

    public @NotNull Settings getSettings() {
        return this.settings;
    }

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

        this.logger.setDebug(this.settings.isDebug());

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

        this.messages.enable();

        this.getLogger().debug("Register plugin command and listeners...");

        this.command.register(this);

        if (this.listeners.isEmpty()) {
            this.listeners.add(new ConditionListener());
            this.listeners.add(new FeatureListener());
            this.listeners.add(new ServiceListener());
        }

        this.listeners.forEach(listener -> listener.register(this));

        this.getLogger().debug("Plugin command and listeners has been registered.");

        // Check if players are online and if then check the players condition and service.
        if (!this.getServer().getOnlinePlayers().isEmpty()) {
            this.getLogger().debug("Checking service conditions for all online players...");

            this.getServer().getOnlinePlayers().forEach(this::handleConditionCheck);

            this.getLogger().debug("Service conditions for all online players has been checked.");
        }

        this.enabled = true;
    }

    @Override
    public void onDisable() {
        if (!this.enabled) {
            this.getLogger().severe("Tried to disable plugin twice. Plugin is already disabled.");
            return;
        }

        // Check if players are online and if then remove the players from condition and service.
        if (!this.getServer().getOnlinePlayers().isEmpty()) {
            this.getLogger().debug("Removing service conditions for all online players...");

            for (final Player player : this.getServer().getOnlinePlayers()) {
                if (this.handleConditionRemove(player)) {
                    player.sendMessage(this.messages.translate("serviceDisable"));
                }
            }

            this.getLogger().debug("Service conditions for all online players has been removed.");
        }

        this.getLogger().debug("Unregister plugin command and listeners...");

        this.command.unregister();
        this.listeners.forEach(BasicListener::unregister);

        this.getLogger().debug("Plugin command and listeners has been unregistered.");

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
        this.command.getCommand().setPermissionMessage(this.messages.translate("noPermission"));

        // Check for all online players the condition and the service.
        this.getServer().getOnlinePlayers().forEach(this::handleConditionCheck);
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

    /**
     * Checks if the given player is in condition to use services. The player is in condition, if it is in the correct
     * game-mode, correct world and correct environment.
     *
     * @param target the player that should be checked for condition.
     */
    public void handleConditionCheck(@NotNull final Player target) {
        if (!target.hasPermission(Permission.SERVICE.getNode())) {
            if (this.handleConditionRemove(target)) {
                this.sendMessage(target, this.messages.translate("serviceDenied"));
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
                        // Check if player gets removed from condition and from warmup or service.
                        if (this.handleConditionRemove(target)) {
                            this.sendMessage(target, this.messages.format("worldDenied", target.getWorld().getName()));
                        }

                        return;
                    }
                }

                // If true, check if player is in a service environment.
                if (this.settings.isServiceEnvironment(target.getWorld().getEnvironment())) {
                    // Filter environment if per environment permission is enabled.
                    if (this.settings.isPermissionPerEnvironment()) {
                        if (!target.hasPermission(Permission.ENVIRONMENT.getChildren(target.getWorld().getEnvironment().name().toLowerCase()))) {
                            // Check if player gets removed from condition and from warmup or service.
                            if (this.handleConditionRemove(target)) {
                                this.sendMessage(target, this.messages.format("environmentDenied", target.getWorld().getEnvironment().name().charAt(0)
                                        + target.getWorld().getEnvironment().name().substring(1).toLowerCase()));
                            }

                            return;
                        }
                    }

                    // If true, check if player gets added to condition.
                    if (this.manager.addToCondition(target)) {
                        this.logger.debug(String.format("Service player '%s' is now in condition for service.", target.getName()));

                        this.handleServiceCheck(target, target.getInventory().getItemInMainHand());
                    }

                    return;
                }

                // Check if player gets removed from condition and from warmup or service.
                if (this.handleConditionRemove(target)) {
                    this.sendMessage(target, this.messages.format("noServiceEnvironment", target.getWorld().getEnvironment().name().charAt(0)
                            + target.getWorld().getEnvironment().name().substring(1).toLowerCase()));
                }

                return;
            }

            // Check if player gets removed from condition and from warmup or service.
            if (this.handleConditionRemove(target)) {
                this.sendMessage(target, this.messages.format("noServiceWorld", target.getWorld().getName()));
            }

            return;
        }

        // Check if player gets removed from condition and from warmup or service.
        if (this.handleConditionRemove(target)) {
            this.sendMessage(target, this.messages.format("noServiceGameMode", target.getGameMode().name().charAt(0)
                    + target.getGameMode().name().substring(1).toLowerCase()));
        }
    }

    /**
     * Checks if the given player gets removed from condition and returns whether the given player gets removed from
     * warmup or from service.
     *
     * @param target the player that should be removed from condition, warmup and service.
     * @return true when the player was removed from warmup or service, false otherwise.
     */
    public boolean handleConditionRemove(@NotNull final Player target) {
        // Check if player gets removed from condition.
        if (this.manager.removeFromCondition(target)) {
            this.logger.debug(String.format("Service player '%s' no longer in condition for service.", target.getName()));

            // If true, check if player gets removed from warmup or services.
            if (this.manager.removeFromWarmup(target)) {
                this.logger.debug(String.format("Service player '%s' is no longer in warmup timer.", target.getName()));

                return true;
            }

            if (this.manager.removeFromService(target)) {
                if (this.manager.removeFromGrace(target)) {
                    this.logger.debug(String.format("Service player '%s' is no longer in service mode and grace timer.", target.getName()));
                } else {
                    this.logger.debug(String.format("Service player '%s' is no longer in service mode.", target.getName()));
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the given item is a service item and adds/removes the given player to/from services.
     *
     * @param target the player that should be checked.
     * @param item the main hand item stack of the player.
     */
    public void handleServiceCheck(@NotNull final Player target, @Nullable final ItemStack item) {
        if (!this.manager.isInCondition(target.getUniqueId())) {
            return;
        }

        // Check if new item slot contains a item and if it is a service item.
        if (item != null && this.settings.isServiceItem(item.getType())) {
            // Check if permission per item is enabled.
            if (this.settings.isPermissionPerItem()) {
                // If true, check if player has permission for this service item.
                if (!target.hasPermission(Permission.ITEM.getChildren(item.getType().getKey().getKey()))) {
                    // If not, check if player gets removed from warmup.
                    if (this.manager.removeFromWarmup(target)) {
                        this.logger.debug(String.format("Service player '%s' is no longer in warmup timer.", target.getName()));

                        this.sendMessage(target, this.messages.translate("warmupAbort"));
                    }

                    // Check if player is currently in service.
                    if (this.manager.isInService(target.getUniqueId())) {
                        // If true, check if the grace period exist.
                        if (this.settings.isGracePeriod()) {
                            // If true, check if player gets added to grace.
                            if (this.manager.addToGrace(target, this.settings.getGracePeriod())) {
                                this.logger.debug(String.format("Service player '%s' is now in grace timer.", target.getName()));

                                this.sendMessage(target, this.messages.format("graceStart", this.settings.getGracePeriod()));
                            }
                        } else {
                            // If false, check if player gets removed from service.
                            if (this.manager.removeFromService(target)) {
                                this.logger.debug(String.format("Service player '%s' is no longer in service mode.", target.getName()));

                                this.sendMessage(target, this.messages.translate("serviceDisable"));
                            }
                        }
                    }

                    return;
                }
            }

            // Check if player gets removed from grace.
            if (this.manager.removeFromGrace(target)) {
                this.logger.debug(String.format("Service player '%s' is no longer in grace timer.", target.getName()));

                this.sendMessage(target, this.messages.translate("graceAbort"));
            }

            // Check if a warmup period exist.
            if (this.settings.isWarmupPeriod()) {
                // Check if player gets added to warmup.
                if (this.manager.addToWarmup(target, this.settings.getWarmupPeriod())) {
                    this.logger.debug(String.format("Service player '%s' is now in warmup timer.", target.getName()));

                    this.sendMessage(target, this.messages.format("warmupStart", this.settings.getWarmupPeriod()));
                }
            } else {
                // Check if player gets added to service.
                if (this.manager.addToService(target)) {
                    this.logger.debug(String.format("Service player '%s' is now in service mode.", target.getName()));

                    this.sendMessage(target, this.messages.translate("serviceEnable"));
                }
            }

            return;
        }

        // Check if player gets removed from warmup.
        if (this.manager.removeFromWarmup(target)) {
            this.logger.debug(String.format("Service player '%s' is no longer in warmup timer.", target.getName()));

            this.sendMessage(target, this.messages.translate("warmupAbort"));
        }

        // Check if player is currently in service.
        if (this.manager.isInService(target.getUniqueId())) {
            // If true, check if the grace period exist.
            if (this.settings.isGracePeriod()) {
                // If true, check if player gets added to grace.
                if (this.manager.addToGrace(target, this.settings.getGracePeriod())) {
                    this.logger.debug(String.format("Service player '%s' is now in grace timer.", target.getName()));

                    this.sendMessage(target, this.messages.format("graceStart", this.settings.getGracePeriod()));
                }
            } else {
                // If false, check if player gets removed from service.
                if (this.manager.removeFromService(target)) {
                    this.logger.debug(String.format("Service player '%s' is no longer in service mode.", target.getName()));

                    this.sendMessage(target, this.messages.translate("serviceDisable"));
                }
            }
        }
    }

    /**
     * Checks if the given player is online and the given message is not empty before sending the message to the player.
     *
     * @param player the player to send the message to.
     * @param message the message that should be sent to the player.
     */
    public void sendMessage(@NotNull final Player player,
                            @NotNull final String message) {
        if (player.isOnline() && !message.isEmpty()) {
            player.sendMessage(message);
        }
    }
}

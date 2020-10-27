package de.g4memas0n.services;

import de.g4memas0n.services.command.ServicesCommand;
import de.g4memas0n.services.listener.BasicListener;
import de.g4memas0n.services.listener.ConditionListener;
import de.g4memas0n.services.listener.FeatureListener;
import de.g4memas0n.services.listener.ServiceListener;
import de.g4memas0n.services.configuration.Settings;
import de.g4memas0n.services.util.Permission;
import de.g4memas0n.services.util.Messages;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.World.Environment;
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
    private final ServicesCommand command;

    private ServiceManager manager;
    private Settings settings;
    private Messages messages;

    private boolean loaded;
    private boolean enabled;

    public Services() {
        this.listeners = new HashSet<>(4, 1);
        this.command = new ServicesCommand();
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

        this.messages = new Messages(this.getDataFolder(), this.getLogger());
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

        if (this.settings.isDebug()) {
            this.getLogger().info("Register plugin command and listeners...");
        }

        this.command.register(this);

        if (this.listeners.isEmpty()) {
            this.listeners.add(new ConditionListener());
            this.listeners.add(new FeatureListener());
            this.listeners.add(new ServiceListener());
        }

        this.listeners.forEach(listener -> listener.register(this));

        if (this.settings.isDebug()) {
            this.getLogger().info("Plugin command and listeners has been registered.");
        }

        // Check if players are online and if then check the players condition and service.
        if (!this.getServer().getOnlinePlayers().isEmpty()) {
            this.getLogger().info("Checking service conditions for all online players...");

            this.getServer().getOnlinePlayers().forEach(this::handleConditionCheck);

            this.getLogger().info("Service conditions for all online players has been checked.");
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
            this.getLogger().info("Removing service conditions for all online players...");

            this.getServer().getOnlinePlayers().forEach(this::handleConditionRemove);

            this.getLogger().info("Service conditions for all online players has been removed.");
        }

        if (this.settings.isDebug()) {
            this.getLogger().info("Unregister plugin command and listeners...");
        }

        this.command.unregister();
        this.listeners.forEach(BasicListener::unregister);

        if (this.settings.isDebug()) {
            this.getLogger().info("Plugin command and listeners has been unregistered.");
        }

        this.messages.disable();

        this.manager = null;
        this.settings = null;
        this.messages = null;

        this.enabled = false;
        this.loaded = false;
    }

    @Override
    public void reloadConfig() {
        this.settings.load();
        this.messages.setLocale(this.settings.getLocale());
        this.command.getCommand().setPermissionMessage(this.messages.translate("noPermission"));

        // Check if players are online and if then check the players condition and service.
        if (!this.getServer().getOnlinePlayers().isEmpty()) {
            this.getLogger().info("Checking service conditions for all online players...");

            this.getServer().getOnlinePlayers().forEach(this::handleConditionCheck);

            this.getLogger().info("Service conditions for all online players has been checked.");
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

    /**
     * Checks if the given player is in condition to use services. The player is in condition, if it is in the correct
     * game-mode, correct world and correct environment.
     *
     * @param target the player that should be checked for condition.
     */
    public void handleConditionCheck(@NotNull final Player target) {
        if (!target.hasPermission(Permission.SERVICE.getNode())) {
            if (this.settings.isDebug()) {
                this.getLogger().info(String.format("Player '%s' has no permission for service mode.", target.getName()));
            }

            if (this.handleConditionRemove(target)) {
                target.sendMessage(this.messages.translate("serviceDenied"));
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
                        if (this.settings.isDebug()) {
                            this.getLogger().info(String.format("Player '%s' has no permission for service world '%s'.",
                                    target.getName(), target.getWorld().getName()));
                        }

                        // Check if player gets removed from condition and from warmup or service.
                        if (this.handleConditionRemove(target)) {
                            target.sendMessage(this.messages.format("worldDenied", target.getWorld().getName()));
                        }

                        return;
                    }
                }

                final Environment environment = target.getWorld().getEnvironment();

                // If true, check if player is in a service environment.
                if (this.settings.isServiceEnvironment(environment)) {
                    // Filter environment if per environment permission is enabled.
                    if (this.settings.isPermissionPerEnvironment()) {
                        // Check if player has no permission for this service environment.
                        if (!target.hasPermission(Permission.ENVIRONMENT.getChildren(environment.name()))) {
                            if (this.manager.isInCondition(target.getUniqueId()) && this.settings.isDebug()) {
                                this.getLogger().info(String.format("Player '%s' has no permission for service environment '%s'.",
                                        target.getName(), environment.name()));
                            }

                            // Check if player gets removed from condition and from warmup or service.
                            if (this.handleConditionRemove(target)) {
                                final String name = environment.name().charAt(0) + environment.name().substring(1).toLowerCase();

                                target.sendMessage(this.messages.format("environmentDenied", name));
                            }

                            return;
                        }
                    }

                    if (this.settings.isDebug()) {
                        this.getLogger().info(String.format("Player '%s' is now in service world '%s' with environment '%s'.",
                                target.getName(), target.getWorld().getName(), environment.name()));
                    }

                    // If true, check if player gets added to condition.
                    if (this.manager.addToCondition(target)) {
                        this.handleServiceCheck(target, target.getInventory().getItemInMainHand());
                    }

                    return;
                }

                if (this.settings.isDebug() && this.manager.isInCondition(target.getUniqueId())) {
                    this.getLogger().info(String.format("Player '%s' is now in non-service environment '%s'.",
                            target.getName(), target.getWorld().getName()));
                }

                // Check if player gets removed from condition and from warmup or service.
                if (this.handleConditionRemove(target)) {
                    final String name = environment.name().charAt(0) + environment.name().substring(1).toLowerCase();

                    target.sendMessage(this.messages.format("noServiceEnvironment", name));
                }

                return;
            }

            if (this.settings.isDebug() && this.manager.isInCondition(target.getUniqueId())) {
                this.getLogger().info(String.format("Player '%s' is now in non-service world '%s'.",
                        target.getName(), target.getWorld().getName()));
            }

            // Check if player gets removed from condition and from warmup or service.
            if (this.handleConditionRemove(target)) {
                target.sendMessage(this.messages.format("noServiceWorld", target.getWorld().getName()));
            }

            return;
        }

        if (this.settings.isDebug() && this.manager.isInCondition(target.getUniqueId())) {
            this.getLogger().info(String.format("Player '%s' is now in non-service game-mode '%s'.",
                    target.getName(), target.getGameMode().name()));
        }

        // Check if player gets removed from condition and from warmup or service.
        if (this.handleConditionRemove(target)) {
            final String name = target.getGameMode().name().charAt(0) + target.getGameMode().name().substring(1).toLowerCase();

            target.sendMessage(this.messages.format("noServiceGameMode", name));
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
            // If true, check if player gets removed from warmup or services.
            if (this.manager.removeFromWarmup(target)) {
                this.notify(target, this.messages.translate("warmupAbort"));

                return true;
            }

            if (this.manager.removeFromService(target)) {
                this.manager.removeFromGrace(target);
                this.notify(target, this.messages.translate("serviceDisable"));

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
                    if (this.settings.isDebug()) {
                        this.getLogger().info(String.format("Player '%s' has no permission for service item '%s'.",
                                target.getName(), item.getType().getKey().getKey()));
                    }

                    this.handleServiceRemove(target);
                    return;
                }
            }

            if (this.settings.isDebug()) {
                this.getLogger().info(String.format("Player '%s' is now using service item '%s'.",
                        target.getName(), item.getType().getKey().getKey()));
            }

            // Check if player gets removed from grace.
            if (this.manager.removeFromGrace(target)) {
                this.notify(target, this.messages.translate("graceAbort"));
            }

            // Check if a warmup period exist.
            if (this.settings.isWarmupPeriod()) {
                // Check if player gets added to warmup.
                if (this.manager.addToWarmup(target, this.settings.getWarmupPeriod())) {
                    this.notify(target, this.messages.format("warmupStart", this.settings.getWarmupPeriod()));
                }
            } else {
                // Check if player gets added to service.
                if (this.manager.addToService(target)) {
                    this.notify(target, this.messages.translate("serviceEnable"));
                }
            }

            return;
        }

        if (this.settings.isDebug() && this.manager.isInService(target.getUniqueId())) {
            this.getLogger().info(String.format("Player '%s' is now using non-service item '%s'.", target.getName(),
                    item != null ? item.getType().getKey().getKey() : Material.AIR.getKey().getKey()));
        }

        this.handleServiceRemove(target);
    }

    /**
     * Checks if the given player gets removed from warmup or from service and notifies it.
     *
     * @param target the player that should be removed from warmup and service.
     */
    public void handleServiceRemove(@NotNull final Player target) {
        // Check if player gets removed from warmup.
        if (this.manager.removeFromWarmup(target)) {
            this.notify(target, this.messages.translate("warmupAbort"));

            return;
        }

        // Check if player is currently in service.
        if (this.manager.isInService(target.getUniqueId())) {
            // If true, check if the grace period exist.
            if (this.settings.isGracePeriod()) {
                // If true, check if player gets added to grace.
                if (this.manager.addToGrace(target, this.settings.getGracePeriod())) {
                    this.notify(target, this.messages.format("graceStart", this.settings.getGracePeriod()));
                }
            } else {
                // If false, check if player gets removed from service.
                if (this.manager.removeFromService(target)) {
                    this.notify(target, this.messages.translate("serviceDisable"));
                }
            }
        }
    }

    /**
     * Checks if the given player is online and the given message is not empty before notify the player.
     *
     * <i><b>Note:</b> The Notification will be send as title or chat message depending of the titles active option.</i>
     *
     * @param player the player to send the message to.
     * @param message the message that should be sent to the player.
     */
    public void notify(@NotNull final Player player, @NotNull final String message) {
        if (player.isOnline() && !message.isEmpty()) {
            if (this.settings.isNotifyActionBar()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
                return;
            }

            player.sendMessage(message);
        }
    }
}

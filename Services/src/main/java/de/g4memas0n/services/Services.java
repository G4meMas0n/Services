package de.g4memas0n.services;

import de.g4memas0n.services.command.ServicesCommand;
import de.g4memas0n.services.listener.BasicListener;
import de.g4memas0n.services.listener.ConditionListener;
import de.g4memas0n.services.listener.FeatureListener;
import de.g4memas0n.services.listener.ServiceListener;
import de.g4memas0n.services.configuration.Settings;
import de.g4memas0n.services.util.Messages;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static de.g4memas0n.services.util.Messages.tl;

/**
 * The Services main class.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public final class Services extends JavaPlugin {

    private final Set<BasicListener> listeners;
    private final ServicesCommand command;

    private Map<UUID, BukkitTask> schedules;

    private ServiceManager manager;
    private Settings settings;
    private Messages messages;

    private boolean loaded;
    private boolean enabled;

    public Services() {
        this.listeners = new HashSet<>(4, 1);
        this.command = new ServicesCommand();
    }

    public @NotNull ServiceManager getManager() {
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

        this.schedules = new HashMap<>();
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
            this.getServer().getOnlinePlayers().forEach(this::runConditionCheck);
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

        if (this.settings.isDebug()) {
            this.getLogger().info("Unregister plugin command and listeners...");
        }

        this.command.unregister();
        this.listeners.forEach(BasicListener::unregister);

        if (this.settings.isDebug()) {
            this.getLogger().info("Plugin command and listeners has been unregistered.");
        }

        this.messages.disable();

        this.schedules = null;
        this.settings = null;
        this.messages = null;
        this.manager = null;

        this.enabled = false;
        this.loaded = false;
    }

    @Override
    public void reloadConfig() {
        this.settings.load();
        this.messages.setLocale(this.settings.getLocale());
        this.command.getCommand().setPermissionMessage(tl("noPermission"));

        // Check if players are online and if then check the players condition and service.
        if (!this.getServer().getOnlinePlayers().isEmpty()) {
            this.getLogger().info("Checking service conditions for all online players...");
            this.getServer().getOnlinePlayers().forEach(this::runConditionCheck);
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
     * Checks if the given player is in condition to use services and runs the service check if it is.
     *
     * <p>A player is in condition if it satisfy following requirements:<br>
     * - The player has the permission {@code services.service} to use service mode.<br>
     * - The players current {@link org.bukkit.GameMode game-mode} equals a registered service game-mode.<br>
     * - The players current {@link World world} equals a registered service world.<br>
     * - The {@link Environment environment} of the players current world equals a registered service environment.</p>
     *
     * @param player the player that should be checked for condition.
     */
    public void runConditionCheck(@NotNull final Player player) {
        // Return if player is not permitted to use service mode.
        if (!player.hasPermission("services.service")) {
            if (this.settings.isDebug()) {
                this.getLogger().info("Player '" + player.getName() + "' is not permitted for service mode.");
            }

            // Send deny message if player was previously in service.
            if (this.manager.removeCondition(player) && this.runServiceRemove(player)) {
                player.sendMessage(tl("serviceDenied"));
            }

            return;
        }

        // Check if current game-mode of the player is a service game-mode.
        if (this.settings.isServiceGameMode(player.getGameMode())) {
            final World world = player.getWorld();

            // Check if player is in a service world.
            if (this.settings.isServiceWorld(world)) {
                // If true, check if permission per world is enabled.
                if (this.settings.isPermissionPerWorld()) {
                    final String permission = "services.world." + world.getName().toLowerCase();

                    // Return if player is not permitted for this service world.
                    if (!player.hasPermission(permission)) {
                        if (this.settings.isDebug()) {
                            this.getLogger().info("Player '" + player.getName() + "' is missing permission '" + permission + "' for service world: " + world.getName());
                        }

                        // Check if player gets removed from condition and service.
                        if (this.manager.removeCondition(player) && this.runServiceRemove(player)) {
                            player.sendMessage(tl("worldDenied", world.getName()));
                        }

                        return;
                    }
                }

                final Environment environment = world.getEnvironment();

                // Check if player is in a service environment.
                if (this.settings.isServiceEnvironment(environment)) {
                    // If true, check if permission per environment is enabled.
                    if (this.settings.isPermissionPerEnvironment()) {
                        final String permission = "services.environment." + environment.name().toLowerCase();

                        // Return if player is not permitted for this service environment.
                        if (!player.hasPermission(permission)) {
                            if (this.settings.isDebug()) {
                                this.getLogger().info("Player '" + player.getName() + "' is missing permission '" + permission + "' for service environment: " + environment.name());
                            }

                            // Check if player gets removed from condition and service.
                            if (this.manager.removeCondition(player) && this.runServiceRemove(player)) {
                                final String name = environment.name().charAt(0) + environment.name().substring(1).toLowerCase();

                                player.sendMessage(tl("environmentDenied", name));
                            }

                            return;
                        }
                    }

                    if (this.settings.isDebug()) {
                        this.getLogger().info("Player '" + player.getName() + "' is in service world: " + world.getName() + " (environment: " + environment.name() + ")");
                    }

                    // Check if player gets added to condition
                    if (this.manager.addCondition(player)) {
                        this.runServiceCheck(player);
                    }

                    return;
                }

                if (this.settings.isDebug() && this.manager.isCondition(player)) {
                    this.getLogger().info("Player '" + player.getName() + "' is in non-service environment: " + environment.name());
                }

                // Check if player gets removed from condition and service.
                if (this.manager.removeCondition(player) && this.runServiceRemove(player)) {
                    final String name = environment.name().charAt(0) + environment.name().substring(1).toLowerCase();

                    player.sendMessage(tl("noServiceEnvironment", name));
                }

                return;
            }

            if (this.settings.isDebug() && this.manager.isCondition(player)) {
                this.getLogger().info("Player '" + player.getName() + "' is in non-service world: " + world.getName());
            }

            // Check if player gets removed from condition and service.
            if (this.manager.removeCondition(player) && this.runServiceRemove(player)) {
                player.sendMessage(tl("noServiceWorld", world.getName()));
            }

            return;
        }

        if (this.settings.isDebug() && this.manager.isCondition(player)) {
            this.getLogger().info("Player '" + player.getName() + "' is in non-service game-mode: " + player.getGameMode().name());
        }

        // Check if player gets removed from condition and service.
        if (this.manager.removeCondition(player) && this.runServiceRemove(player)) {
            final String name = player.getGameMode().name().charAt(0) + player.getGameMode().name().substring(1).toLowerCase();

            player.sendMessage(tl("noServiceGameMode", name));
        }
    }

    public void scheduleServiceCheck(@NotNull final Player player) {
        // Check if the service check is already scheduled.
        if (this.schedules.containsKey(player.getUniqueId())) {
            // Cancel the previous scheduled service check.
            this.schedules.remove(player.getUniqueId()).cancel();
        }

        this.schedules.put(player.getUniqueId(), this.runTask(() -> this.runServiceCheck(player)));
    }

    /**
     * Checks if the main hand item of the given player is a service item and adds/removes the given player to/from services.
     *
     * @param player the player that should be checked.
     */
    public void runServiceCheck(@NotNull final Player player) {
        // Check if the service check was scheduled previously.
        if (this.schedules.containsKey(player.getUniqueId())) {
            // Return if the scheduled check was cancelled before execution.
            if (this.schedules.remove(player.getUniqueId()).isCancelled()) {
                return;
            }
        }

        this.runServiceCheck(player, player.getInventory().getItemInMainHand());
    }

    /**
     * Checks if the given item is a service item and adds/removes the given player to/from services.
     *
     * @param player the player that should be checked.
     * @param item the main hand item stack of the player.
     */
    public void runServiceCheck(@NotNull final Player player, @Nullable final ItemStack item) {
        // Return directly if player is not in condition.
        if (!this.manager.isCondition(player)) {
            return;
        }

        // Check if new item slot contains a item and if it is a service item.
        if (item != null && this.settings.isServiceItem(item.getType())) {
            // If true, check if permission per item is enabled.
            if (this.settings.isPermissionPerItem()) {
                final String permission = "services.item." + item.getType().getKey().getKey();

                // Return if player is not permitted for this service item.
                if (!player.hasPermission(permission)) {
                    if (this.settings.isDebug()) {
                        this.getLogger().info("Player '" + player.getName() + "' is missing permission '" + permission + "' for service item: " + item.getType().getKey());
                    }

                    this.runServiceRemove(player);
                    return;
                }
            }

            if (this.settings.isDebug()) {
                this.getLogger().info("Player '" + player.getName() + "' is using service item: " + item.getType().getKey());
            }

            this.runServiceAdd(player);
            return;
        }

        if (this.settings.isDebug() && this.manager.isService(player)) {
            this.getLogger().info("Player '" + player.getName() + "' is using non-service item: " + (item != null ? item.getType().getKey() : Material.AIR.getKey()));
        }

        this.runServiceRemove(player);
    }

    public boolean runServiceAdd(@NotNull final Player player) {
        // Check if player gets removed from grace.
        if (this.manager.removeGrace(player)) {
            this.notify(player, tl("graceAbort"));

            return true;
        }

        // Check if player is not already in service.
        if (!this.manager.isService(player)) {
            // Check if a warmup period exist.
            if (this.settings.isWarmupPeriod()) {
                // Check if player gets added to warmup.
                if (this.manager.addWarmup(player, this.settings.getWarmupPeriod(), tl("serviceEnable"))) {
                    this.notify(player, tl("warmupStart", this.settings.getWarmupPeriod()));

                    return true;
                }
            } else {
                // Check if player gets added to service.
                if (this.manager.addService(player)) {
                    this.notify(player, tl("serviceEnable"));

                    return true;
                }
            }
        }

        return false;
    }

    public boolean runServiceRemove(@NotNull final Player player) {
        // Check if player gets removed from warmup.
        if (this.manager.removeWarmup(player)) {
            this.notify(player, tl("warmupAbort"));

            return true;
        }

        // Check if player is currently in service.
        if (this.manager.isService(player)) {
            // Check if a grace period exist.
            if (this.settings.isGracePeriod()) {
                // Check if player gets added to grace.
                if (this.manager.addGrace(player, this.settings.getGracePeriod(), tl("serviceDisable"))) {
                    this.notify(player, tl("graceStart", this.settings.getGracePeriod()));

                    return true;
                }
            } else {
                // Check if player gets removed from service.
                if (this.manager.removeService(player)) {
                    this.notify(player, tl("serviceDisable"));

                    return true;
                }
            }
        }

        return false;
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
        // Return if the player is offline or the message is empty.
        if (!player.isOnline() || message.isEmpty()) {
            return;
        }

        if (this.settings.isNotifyActionBar()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
            return;
        }

        player.sendMessage(message);
    }
}

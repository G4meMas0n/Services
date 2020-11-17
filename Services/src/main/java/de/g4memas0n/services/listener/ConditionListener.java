package de.g4memas0n.services.listener;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import static de.g4memas0n.services.util.Messages.tl;

/**
 * The Condition Listener, listening for events to check for service conditions.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public final class ConditionListener extends BasicListener {

    public ConditionListener() { }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(@NotNull final PlayerJoinEvent event) {
        this.getInstance().runConditionCheck(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull final PlayerQuitEvent event) {
        // Check if player gets removed from condition.
        if (this.getManager().removeCondition(event.getPlayer())) {
            this.getInstance().runServiceRemove(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorldEvent(@NotNull final PlayerChangedWorldEvent event) {
        // Check if player is currently in condition.
        if (this.getManager().isCondition(event.getPlayer())) {
            final Player player = event.getPlayer();
            final World world = player.getWorld();

            // If true, check if the players new world is still a service world.
            if (this.getSettings().isServiceWorld(world)) {
                // If true, check if permission per world is enabled.
                if (this.getSettings().isPermissionPerWorld()) {
                    final String permission = "services.world." + world.getName().toLowerCase();

                    // Return if player is not permitted for this service world.
                    if (!player.hasPermission(permission)) {
                        if (this.getSettings().isDebug()) {
                            this.getLogger().info(String.format("Player '%s' is missing permission '%s' for service world: %s", player.getName(), permission, world.getName()));
                        }

                        // Check if player gets removed from condition and service.
                        if (this.getManager().removeCondition(player) && this.getInstance().runServiceRemove(player)) {
                            player.sendMessage(tl("worldDenied", world.getName()));
                        }

                        return;
                    }
                }

                final Environment environment = world.getEnvironment();

                // Check if the players new world environment is still a service environment.
                if (this.getSettings().isServiceEnvironment(environment)) {
                    // If true, check if permission per environment is enabled.
                    if (this.getSettings().isPermissionPerEnvironment()) {
                        final String permission = "services.environment." + environment.name().toLowerCase();

                        // Return if player is not permitted for this service environment.
                        if (!player.hasPermission(permission)) {
                            if (this.getSettings().isDebug()) {
                                this.getLogger().info(String.format("Player '%s' is missing permission '%s' for service environment: %s", player.getName(), permission, environment.name()));
                            }

                            // Check if player gets removed from condition and service.
                            if (this.getManager().removeCondition(player) && this.getInstance().runServiceRemove(player)) {
                                final String name = environment.name().charAt(0) + environment.name().substring(1).toLowerCase();

                                player.sendMessage(tl("environmentDenied", name));
                            }

                            return;
                        }
                    }

                    if (this.getSettings().isDebug()) {
                        this.getLogger().info(String.format("Player '%s' is in service world: %s (environment: %s)", player.getName(), world.getName(), environment.name()));
                    }

                    return; // Return, as player is still in condition.
                }

                if (this.getSettings().isDebug()) {
                    this.getLogger().info(String.format("Player '%s' is in non-service environment: %s", player.getName(), environment.name()));
                }

                // Check if player gets removed from condition and service.
                if (this.getManager().removeCondition(player) && this.getInstance().runServiceRemove(player)) {
                    final String name = environment.name().charAt(0) + environment.name().substring(1).toLowerCase();

                    player.sendMessage(tl("noServiceEnvironment", name));
                }

                return;
            }

            if (this.getSettings().isDebug()) {
                this.getLogger().info(String.format("Player '%s' is in non-service world: %s", player.getName(), world.getName()));
            }

            // Check if player gets removed from condition and service.
            if (this.getManager().removeCondition(player) && this.getInstance().runServiceRemove(player)) {
                player.sendMessage(tl("noServiceWorld", world.getName()));
            }

            return;
        }

        this.getInstance().runConditionCheck(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerGameModeChangeEvent(@NotNull final PlayerGameModeChangeEvent event) {
        // Check if player is currently in condition.
        if (this.getManager().isCondition(event.getPlayer())) {
            final Player player = event.getPlayer();
            final GameMode mode = event.getNewGameMode();

            // If true, check if the players new game-mode is also a service game-mode.
            if (this.getSettings().isServiceGameMode(mode)) {
                if (this.getSettings().isDebug()) {
                    this.getLogger().info(String.format("Player '%s' is in service game-mode: %s", player, mode.name()));
                }

                return; // Return as player is still in condition.
            }

            if (this.getSettings().isDebug()) {
                this.getLogger().info(String.format("Player '%s' is in non-service game-mode: %s", player.getName(), mode.name()));
            }

            // Check if player gets removed from condition and service.
            if (this.getManager().removeCondition(player) && this.getInstance().runServiceRemove(player)) {
                final String name = mode.name().charAt(0) + mode.name().substring(1).toLowerCase();

                player.sendMessage(tl("noServiceGameMode", name));
            }

            return;
        }

        // Schedule task to next server tick, to check the correct game-mode.
        this.getInstance().runTask(() -> this.getInstance().runConditionCheck(event.getPlayer()));
    }
}

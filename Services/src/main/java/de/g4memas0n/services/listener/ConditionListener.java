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
        if (this.getManager().removeCondition(event.getPlayer())) {
            this.getManager().removeService(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorldEvent(@NotNull final PlayerChangedWorldEvent event) {
        // Only perform removing checks when player is in condition:
        if (this.getManager().isCondition(event.getPlayer())) {
            final Player player = event.getPlayer();
            final World world = player.getWorld();

            if (this.getSettings().isServiceWorld(world)) {
                // Check for world permission and remove player from condition if it is not permitted:
                if (this.getSettings().isPermissionPerWorld() && !player.hasPermission(this.getInstance().getPermission(world))) {
                    if (this.getSettings().isDebug()) {
                        this.getLogger().info("Player '" + player.getName() + "' is missing permission for service world: " + world.getName());
                    }

                    if (this.getManager().removeCondition(player) && this.getManager().removeService(player)) {
                        player.sendMessage(tl("worldDenied", world.getName()));
                    }

                    return;
                }

                final Environment environment = world.getEnvironment();

                if (this.getSettings().isServiceEnvironment(environment)) {
                    // Check for environment permission and remove player from condition if it is not permitted:
                    if (this.getSettings().isPermissionPerEnvironment() && !player.hasPermission(this.getInstance().getPermission(environment))) {
                        if (this.getSettings().isDebug()) {
                            this.getLogger().info("Player '" + player.getName() + "' is missing permission for service environment: " + environment.name());
                        }

                        if (this.getManager().removeCondition(player) && this.getManager().removeService(player)) {
                            final String name = environment.name().charAt(0) + environment.name().substring(1).toLowerCase();

                            player.sendMessage(tl("environmentDenied", name));
                        }

                        return;
                    }

                    if (this.getSettings().isDebug()) {
                        this.getLogger().info("Player '" + player.getName() + "' is now in service world: " + world.getName() + "(environment: " + environment.name() + ")");
                    }

                    return; // Player is still in a service world and service environment.
                }

                // Player is not in a service environment, remove it from condition:
                if (this.getManager().removeCondition(player)) {
                    if (this.getSettings().isDebug()) {
                        this.getLogger().info("Player '" + player.getName() + "' is now in non-service environment: " + environment.name());
                    }

                    if (this.getManager().removeService(player)) {
                        final String name = environment.name().charAt(0) + environment.name().substring(1).toLowerCase();

                        player.sendMessage(tl("noServiceEnvironment", name));
                    }
                }

                return;
            }

            // Player is not in a service world, remove it from condition:
            if (this.getManager().removeCondition(player)) {
                if (this.getSettings().isDebug()) {
                    this.getLogger().info("Player '" + player.getName() + "' is now in non-service world: " + world.getName());
                }

                if (this.getManager().removeService(player)) {
                    player.sendMessage(tl("noServiceWorld", world.getName()));
                }
            }

            return;
        }

        // Run complete condition check:
        this.getInstance().runConditionCheck(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerGameModeChangeEvent(@NotNull final PlayerGameModeChangeEvent event) {
        // Only perform removing checks when player is in condition:
        if (this.getManager().isCondition(event.getPlayer())) {
            final Player player = event.getPlayer();
            final GameMode mode = event.getNewGameMode();

            if (this.getSettings().isServiceGameMode(mode)) {
                if (this.getSettings().isDebug()) {
                    this.getLogger().info("Player '" + player.getName() + "' is now in service game-mode: " + mode.name());
                }

                return; // Player is still in a service game-mode.
            }

            // Player is not in a service game-mode, remove it from condition:
            if (this.getManager().removeCondition(player)) {
                if (this.getSettings().isDebug()) {
                    this.getLogger().info("Player '" + player.getName() + "' is now in non-service game-mode: " + mode.name());
                }

                if (this.getManager().removeService(player)) {
                    final String name = mode.name().charAt(0) + mode.name().substring(1).toLowerCase();

                    player.sendMessage(tl("noServiceGameMode", name));
                }
            }

            return;
        }

        // Schedule complete condition check to next server tick:
        this.getInstance().scheduleConditionCheck(event.getPlayer());
    }
}

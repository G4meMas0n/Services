package de.g4memas0n.services.listener;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.World.Environment;
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
        this.getInstance().handleConditionCheck(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull final PlayerQuitEvent event) {
        this.getInstance().handleConditionRemove(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorldEvent(@NotNull final PlayerChangedWorldEvent event) {
        // Check if player is currently in condition.
        if (this.getManager().isInCondition(event.getPlayer().getUniqueId())) {
            final World world = event.getPlayer().getWorld();

            // If true, check if the players new world is still a service world.
            if (this.getSettings().isServiceWorld(world)) {
                // Filter world if per world permission is enabled.
                if (this.getSettings().isPermissionPerWorld()) {
                    final String permission = "services.world." + world.getName().toLowerCase();

                    // Check if player has no permission for the new service world.
                    if (!event.getPlayer().hasPermission(permission)) {
                        if (this.getSettings().isDebug()) {
                            this.getLogger().info(String.format("Player '%s' is missing permission '%s' for service world: %s", event.getPlayer().getName(), permission, world.getName()));
                        }

                        // Check if player gets removed from condition and from warmup or service.
                        if (this.getInstance().handleConditionRemove(event.getPlayer())) {
                            event.getPlayer().sendMessage(tl("worldDenied", world.getName()));
                        }

                        return;
                    }
                }

                final Environment environment = world.getEnvironment();

                // If true, check if the players new world environment is still a service environment.
                if (this.getSettings().isServiceEnvironment(environment)) {
                    // Filter environment is per environment permission is enabled.
                    if (this.getSettings().isPermissionPerEnvironment()) {
                        final String permission = "services.environment." + environment.name().toLowerCase();

                        // Check if player has no permission for the new service environment.
                        if (!event.getPlayer().hasPermission(permission)) {
                            if (this.getSettings().isDebug()) {
                                this.getLogger().info(String.format("Player '%s' is missing permission '%s' for service environment: %s", event.getPlayer().getName(), permission, environment.name()));
                            }

                            // Check if player gets removed from condition and from warmup or service.
                            if (this.getInstance().handleConditionRemove(event.getPlayer())) {
                                final String name = environment.name().charAt(0) + environment.name().substring(1).toLowerCase();

                                event.getPlayer().sendMessage(tl("environmentDenied", name));
                            }

                            return;
                        }
                    }

                    if (this.getSettings().isDebug()) {
                        this.getLogger().info(String.format("Player '%s' is now in service world: %s (environment: %s)", event.getPlayer().getName(), world.getName(), environment.name()));
                    }

                    return; // Player is still in condition.
                }

                if (this.getSettings().isDebug()) {
                    this.getLogger().info(String.format("Player '%s' is now in non-service environment: %s", event.getPlayer().getName(), environment.name()));
                }

                // Check if player gets removed from condition and from warmup or service.
                if (this.getInstance().handleConditionRemove(event.getPlayer())) {
                    final String name = environment.name().charAt(0) + environment.name().substring(1).toLowerCase();

                    event.getPlayer().sendMessage(tl("noServiceEnvironment", name));
                }

                return;
            }

            if (this.getSettings().isDebug()) {
                this.getLogger().info(String.format("Player '%s' is now in non-service world: %s", event.getPlayer().getName(), world.getName()));
            }

            // Check if player gets removed from condition and from warmup or service.
            if (this.getInstance().handleConditionRemove(event.getPlayer())) {
                event.getPlayer().sendMessage(tl("noServiceWorld", world.getName()));
            }

            return;
        }

        this.getInstance().handleConditionCheck(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerGameModeChangeEvent(@NotNull final PlayerGameModeChangeEvent event) {
        // Check if player is currently in condition.
        if (this.getManager().isInCondition(event.getPlayer().getUniqueId())) {
            final GameMode mode = event.getNewGameMode();

            // If true, check if the players new game-mode is also a service game-mode.
            if (this.getSettings().isServiceGameMode(mode)) {
                if (this.getSettings().isDebug()) {
                    this.getLogger().info(String.format("Player '%s' is now in service game-mode: %s", event.getPlayer().getName(), mode.name()));
                }

                return; // Player is still in condition.
            }

            if (this.getSettings().isDebug()) {
                this.getLogger().info(String.format("Player '%s' is now in non-service game-mode: %s", event.getPlayer().getName(), mode.name()));
            }

            // Check if player gets removed from condition and from warmup or service.
            if (this.getInstance().handleConditionRemove(event.getPlayer())) {
                final String name = mode.name().charAt(0) + mode.name().substring(1).toLowerCase();

                event.getPlayer().sendMessage(tl("noServiceGameMode", name));
            }

            return;
        }

        // Schedule task to next server tick, to check the correct game-mode.
        this.getInstance().runTask(() -> this.getInstance().handleConditionCheck(event.getPlayer()));
    }
}

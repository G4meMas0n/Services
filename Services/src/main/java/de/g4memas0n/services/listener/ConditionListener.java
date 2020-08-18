package de.g4memas0n.services.listener;

import de.g4memas0n.services.util.Permission;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import static de.g4memas0n.services.util.messaging.Messages.tl;

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
        // Check if player gets removed from condition for services.
        if (this.getInstance().getServiceManager().removeFromCondition(event.getPlayer())) {
            this.getInstance().getLogger().debug("Service player '%s' is no longer in condition.");

            // If true, remove it from warmup, grace and service if it is in.
            this.getInstance().getServiceManager().removeFromWarmup(event.getPlayer());
            this.getInstance().getServiceManager().removeFromService(event.getPlayer());
            this.getInstance().getServiceManager().removeFromGrace(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorldEvent(@NotNull final PlayerChangedWorldEvent event) {
        if (this.getInstance().getServiceManager().isInCondition(event.getPlayer().getUniqueId())) {
            final World world = event.getPlayer().getWorld();

            // If true, check if players world is still a service world.
            if (this.getInstance().getSettings().isServiceWorld(world)) {
                // Filter world if per world permission is enabled.
                if (this.getInstance().getSettings().isPermissionPerWorld()) {
                    if (!event.getPlayer().hasPermission(Permission.WORLD.getChildren(world.getName()))) {
                        // Check if player gets removed from warmup or service.
                        if (this.getInstance().getServiceManager().removeFromWarmup(event.getPlayer())
                                || this.getInstance().getServiceManager().removeFromService(event.getPlayer())) {
                            this.getInstance().getServiceManager().removeFromGrace(event.getPlayer());

                            event.getPlayer().sendMessage(tl("worldDenied", world.getName()));
                        }

                        if (this.getInstance().getServiceManager().removeFromCondition(event.getPlayer())) {
                            this.getInstance().getLogger().debug("Service player '%s' is no longer in condition.");
                        }

                        return;
                    }
                }

                final Environment environment = world.getEnvironment();

                // If true, check if players environment is still a service environment.
                if (this.getInstance().getSettings().isServiceEnvironment(environment)) {
                    // Filter environment is per environment permission is enabled.
                    if (this.getInstance().getSettings().isPermissionPerEnvironment()) {
                        if (!event.getPlayer().hasPermission(Permission.ENVIRONMENT.getChildren(environment.name().toLowerCase()))) {
                            // Check if player gets removed from warmup or service.
                            if (this.getInstance().getServiceManager().removeFromWarmup(event.getPlayer())
                                    || this.getInstance().getServiceManager().removeFromService(event.getPlayer())) {
                                this.getInstance().getServiceManager().removeFromGrace(event.getPlayer());

                                event.getPlayer().sendMessage(tl("environmentDenied", environment.name().charAt(0)
                                        + environment.name().substring(1).toLowerCase()));
                            }

                            if (this.getInstance().getServiceManager().removeFromCondition(event.getPlayer())) {
                                this.getInstance().getLogger().debug("Service player '%s' is no longer in condition.");
                            }

                            return;
                        }
                    }

                    return; // Player is still in condition.
                }

                if (this.getInstance().getServiceManager().removeFromWarmup(event.getPlayer())
                        || this.getInstance().getServiceManager().removeFromService(event.getPlayer())) {
                    this.getInstance().getServiceManager().removeFromGrace(event.getPlayer());

                    event.getPlayer().sendMessage(tl("noServiceEnvironment", environment.name().charAt(0)
                            + environment.name().substring(1).toLowerCase()));
                }

                if (this.getInstance().getServiceManager().removeFromCondition(event.getPlayer())) {
                    this.getInstance().getLogger().debug("Service player '%s' is no longer in condition.");
                }

                return;
            }

            if (this.getInstance().getServiceManager().removeFromWarmup(event.getPlayer())
                    || this.getInstance().getServiceManager().removeFromService(event.getPlayer())) {
                this.getInstance().getServiceManager().removeFromGrace(event.getPlayer());

                event.getPlayer().sendMessage(tl("noServiceWorld", world.getName()));
            }

            if (this.getInstance().getServiceManager().removeFromCondition(event.getPlayer())) {
                this.getInstance().getLogger().debug("Service player '%s' is no longer in condition.");
            }

            return;
        }

        this.getInstance().handleConditionCheck(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerGameModeChangeEvent(@NotNull final PlayerGameModeChangeEvent event) {
        if (this.getInstance().getServiceManager().isInCondition(event.getPlayer().getUniqueId())) {
            // If true, check if players new game-mode is also a service game-mode.
            if (this.getInstance().getSettings().isServiceGameMode(event.getNewGameMode())) {
                return; // Player is still in condition.
            }

            // If not, remove player from warmup or service if it is in.
            if (this.getInstance().getServiceManager().removeFromWarmup(event.getPlayer())
                    || this.getInstance().getServiceManager().removeFromService(event.getPlayer())) {
                this.getInstance().getServiceManager().removeFromGrace(event.getPlayer());

                event.getPlayer().sendMessage(tl("noServiceGameMode", event.getNewGameMode().name().charAt(0)
                        + event.getNewGameMode().name().substring(1).toLowerCase()));
            }

            if (this.getInstance().getServiceManager().removeFromCondition(event.getPlayer())) {
                this.getInstance().getLogger().debug("Service player '%s' is no longer in condition.");
            }

            return;
        }

        this.getInstance().handleConditionCheck(event.getPlayer());
    }
}

package de.g4memas0n.services;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service Manager, that manages all players in warmup, service, grace and also in condition.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public final class ServiceManager {

    private final Map<UUID, BukkitTask> warmups;
    private final Map<UUID, BukkitTask> graces;
    private final Set<UUID> services;
    private final Set<UUID> conditions;

    private final Services instance;

    public ServiceManager(@NotNull final Services instance) {
        this.warmups = new HashMap<>();
        this.graces = new HashMap<>();
        this.services = new HashSet<>();
        this.conditions = new HashSet<>();

        this.instance = instance;
    }

    // Condition collection methods:
    public boolean addToCondition(@NotNull final Player player) {
        if (this.conditions.add(player.getUniqueId())) {
            if (this.instance.getSettings().isDebug()) {
                this.instance.getLogger().info(String.format("Player '%s' is now in condition for service.", player.getName()));
            }

            return true;
        }

        return false;
    }

    public boolean removeFromCondition(@NotNull final Player player) {
        if (this.conditions.remove(player.getUniqueId())) {
            if (this.instance.getSettings().isDebug()) {
                this.instance.getLogger().info(String.format("Player '%s' is no longer in condition for service.", player.getName()));
            }
        }

        return this.conditions.remove(player.getUniqueId());
    }

    public boolean isInCondition(@NotNull final UUID uniqueId) {
        return this.conditions.contains(uniqueId);
    }

    // Warmup collections methods:
    public boolean addToWarmup(@NotNull final Player player, final long period) {
        if (this.warmups.containsKey(player.getUniqueId()) || this.services.contains(player.getUniqueId())) {
            return false;
        }

        if (this.instance.getSettings().isWarmupPeriod()) {
            this.warmups.put(player.getUniqueId(), this.instance.scheduleTask(() -> {
                if (this.warmups.containsKey(player.getUniqueId())) {
                    this.warmups.remove(player.getUniqueId());

                    if (this.instance.getSettings().isDebug()) {
                        this.instance.getLogger().info(String.format("Player '%s' is no longer in warmup: ENABLED", player.getName()));
                    }

                    if (this.services.add(player.getUniqueId())) {
                        if (this.instance.getSettings().isDebug()) {
                            this.instance.getLogger().info(String.format("Player '%s' is now in service mode.", player.getName()));
                        }

                        this.instance.notify(player, this.instance.getMessages().translate("serviceEnable"));
                    }
                }
            }, period * 20));

            if (this.instance.getSettings().isDebug()) {
                this.instance.getLogger().info(String.format("Player '%s' is now in warmup.", player.getName()));
            }

            return true;
        }

        return false;
    }

    public boolean removeFromWarmup(@NotNull final Player player) {
        if (this.warmups.containsKey(player.getUniqueId())) {
            this.warmups.remove(player.getUniqueId()).cancel();

            if (this.instance.getSettings().isDebug()) {
                this.instance.getLogger().info(String.format("Player '%s' is no longer in warmup: ABORTED", player.getName()));
            }

            return true;
        }

        return false;
    }

    public boolean isInWarmup(@NotNull final UUID uniqueId) {
        return this.warmups.containsKey(uniqueId);
    }

    // Service collection methods:
    public boolean addToService(@NotNull final Player player) {
        if (this.services.add(player.getUniqueId())) {
            if (this.instance.getSettings().isDebug()) {
                this.instance.getLogger().info(String.format("Player '%s' is now in service mode.", player.getName()));
            }

            return true;
        }

        return false;
    }

    public boolean removeFromService(@NotNull final Player player) {
        if (this.services.remove(player.getUniqueId())) {
            if (this.instance.getSettings().isDebug()) {
                this.instance.getLogger().info(String.format("Player '%s' is no longer in service mode.", player.getName()));
            }

            return true;
        }

        return false;
    }

    public boolean isInService(@NotNull final UUID uniqueId) {
        return this.services.contains(uniqueId);
    }

    // Grace collection methods:
    public boolean addToGrace(@NotNull final Player player, final long period) {
        if (this.warmups.containsKey(player.getUniqueId()) || this.graces.containsKey(player.getUniqueId())) {
            return false;
        }

        if (this.instance.getSettings().isGracePeriod()) {
            this.graces.put(player.getUniqueId(), this.instance.scheduleTask(() -> {
                if (this.graces.containsKey(player.getUniqueId())) {
                    this.graces.remove(player.getUniqueId());

                    if (this.instance.getSettings().isDebug()) {
                        this.instance.getLogger().info(String.format("Player '%s' is no longer in grace: DISABLED", player.getName()));
                    }

                    if (this.services.remove(player.getUniqueId())) {
                        if (this.instance.getSettings().isDebug()) {
                            this.instance.getLogger().info(String.format("Player '%s' is no longer in service mode.", player.getName()));
                        }

                        this.instance.notify(player, this.instance.getMessages().translate("serviceDisable"));
                    }
                }
            }, period * 20));

            if (this.instance.getSettings().isDebug()) {
                this.instance.getLogger().info(String.format("Player '%s' is now in grace.", player.getName()));
            }

            return true;
        }

        return false;
    }

    public boolean removeFromGrace(@NotNull final Player player) {
        if (this.graces.containsKey(player.getUniqueId())) {
            this.graces.remove(player.getUniqueId()).cancel();

            if (this.instance.getSettings().isDebug()) {
                this.instance.getLogger().info(String.format("Player '%s' is no longer in grace: ABORTED", player.getName()));
            }

            return true;
        }

        return false;
    }

    public boolean isInGrace(@NotNull final UUID uniqueId) {
        return this.graces.containsKey(uniqueId);
    }
}

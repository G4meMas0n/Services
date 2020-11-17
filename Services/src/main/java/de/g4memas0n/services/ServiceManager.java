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

    private final Set<UUID> conditions;
    private final Set<UUID> services;

    private final Services instance;

    public ServiceManager(@NotNull final Services instance) {
        this.warmups = new HashMap<>();
        this.graces = new HashMap<>();

        this.conditions = new HashSet<>();
        this.services = new HashSet<>();

        this.instance = instance;
    }

    // Condition related methods:

    public boolean addCondition(@NotNull final Player player) {
        if (this.conditions.add(player.getUniqueId())) {
            if (this.instance.getSettings().isDebug()) {
                this.instance.getLogger().info(String.format("Player '%s' is now in condition for service.", player.getName()));
            }

            return true;
        }

        return false;
    }

    public boolean removeCondition(@NotNull final Player player) {
        if (this.conditions.remove(player.getUniqueId())) {
            if (this.instance.getSettings().isDebug()) {
                this.instance.getLogger().info(String.format("Player '%s' is no longer in condition for service.", player.getName()));
            }
        }

        return this.conditions.remove(player.getUniqueId());
    }

    public boolean isCondition(@NotNull final Player player) {
        return this.conditions.contains(player.getUniqueId());
    }

    // Warmup related methods:

    public boolean addWarmup(@NotNull final Player player, final long period,
                             @NotNull final String notification) {
        if (this.warmups.containsKey(player.getUniqueId()) || this.services.contains(player.getUniqueId())) {
            return false;
        }

        if (this.instance.getSettings().isWarmupPeriod()) {
            this.warmups.put(player.getUniqueId(), this.instance.scheduleTask(() -> {
                if (this.warmups.containsKey(player.getUniqueId())) {
                    this.warmups.remove(player.getUniqueId());

                    if (this.services.add(player.getUniqueId())) {
                        if (this.instance.getSettings().isDebug()) {
                            this.instance.getLogger().info(String.format("Player '%s' is now in service mode.", player.getName()));
                        }

                        this.instance.notify(player, notification);
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

    public boolean removeWarmup(@NotNull final Player player) {
        if (this.warmups.containsKey(player.getUniqueId())) {
            this.warmups.remove(player.getUniqueId()).cancel();

            if (this.instance.getSettings().isDebug()) {
                this.instance.getLogger().info(String.format("Player '%s' is no longer in warmup.", player.getName()));
            }

            return true;
        }

        return false;
    }

    public boolean isWarmup(@NotNull final Player player) {
        return this.warmups.containsKey(player.getUniqueId());
    }

    // Service related methods:

    public boolean addService(@NotNull final Player player) {
        if (this.services.add(player.getUniqueId())) {
            if (this.instance.getSettings().isDebug()) {
                this.instance.getLogger().info(String.format("Player '%s' is now in service mode.", player.getName()));
            }

            return true;
        }

        return false;
    }

    public boolean removeService(@NotNull final Player player) {
        if (this.services.remove(player.getUniqueId())) {
            if (this.instance.getSettings().isDebug()) {
                this.instance.getLogger().info(String.format("Player '%s' is no longer in service mode.", player.getName()));
            }

            return true;
        }

        return false;
    }

    public boolean isService(@NotNull final Player player) {
        return this.services.contains(player.getUniqueId());
    }

    // Grace related methods:
    public boolean addGrace(@NotNull final Player player, final long period,
                            @NotNull final String notification) {
        if (this.graces.containsKey(player.getUniqueId()) || !this.services.contains(player.getUniqueId())) {
            return false;
        }

        if (this.instance.getSettings().isGracePeriod()) {
            this.graces.put(player.getUniqueId(), this.instance.scheduleTask(() -> {
                if (this.graces.containsKey(player.getUniqueId())) {
                    this.graces.remove(player.getUniqueId());

                    if (this.services.remove(player.getUniqueId())) {
                        if (this.instance.getSettings().isDebug()) {
                            this.instance.getLogger().info(String.format("Player '%s' is no longer in service mode.", player.getName()));
                        }

                        this.instance.notify(player, notification);
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

    public boolean removeGrace(@NotNull final Player player) {
        if (this.graces.containsKey(player.getUniqueId())) {
            this.graces.remove(player.getUniqueId()).cancel();

            if (this.instance.getSettings().isDebug()) {
                this.instance.getLogger().info(String.format("Player '%s' is no longer in grace.", player.getName()));
            }

            return true;
        }

        return false;
    }

    public boolean isGrace(@NotNull final Player player) {
        return this.graces.containsKey(player.getUniqueId());
    }
}

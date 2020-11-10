package de.g4memas0n.services.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Service Listener, listening for events to check for service enabling and disabling.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public final class ServiceListener extends BasicListener {

    private final Map<UUID, BukkitTask> schedules;

    public ServiceListener() {
        this.schedules = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(@NotNull final BlockPlaceEvent event) {
        // Check if player is in condition.
        if (this.getManager().isInCondition(event.getPlayer().getUniqueId())) {
            // Return if block was not placed from the main hand.
            if (event.getHand() != EquipmentSlot.HAND) {
                return;
            }

            // Check if the block to place is a service item.
            if (this.getSettings().isServiceItem(event.getItemInHand().getType())) {
                // If true, check if player is already in grace.
                if (this.getManager().isInGrace(event.getPlayer().getUniqueId())) {
                    return; // If true, no check is required.
                }

                this.scheduleServiceCheck(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(@NotNull final InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        // Check if player is in condition.
        if (this.getManager().isInCondition(event.getPlayer().getUniqueId())) {
            final ItemStack item = event.getPlayer().getInventory().getItemInMainHand();

            // If true, execute service check.
            this.getInstance().handleServiceCheck((Player) event.getPlayer(), item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketFill(@NotNull final PlayerBucketFillEvent event) {
        // Check if player is in condition.
        if (this.getManager().isInCondition(event.getPlayer().getUniqueId())) {
            // If true, check if the used bucket is a service item.
            if (this.getSettings().isServiceItem(event.getBucket())) {
                // If true, check if player is already in grace.
                if (this.getManager().isInGrace(event.getPlayer().getUniqueId())) {
                    return; // If true, no check is required.
                }

                this.scheduleServiceCheck(event.getPlayer());
                return;
            }

            // Return if no resulting item exist after the event.
            if (event.getItemStack() == null) {
                return;
            }

            // If exist, check if resulting item is a service item.
            if (this.getSettings().isServiceItem(event.getItemStack().getType())) {
                // If true, check if player is already in warmup or service.
                if (this.getManager().isInWarmup(event.getPlayer().getUniqueId())
                        || this.getManager().isInService(event.getPlayer().getUniqueId())) {
                    return; // If true, no check is required.
                }

                this.scheduleServiceCheck(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketEmpty(@NotNull final PlayerBucketEmptyEvent event) {
        // Check if player is in condition.
        if (this.getManager().isInCondition(event.getPlayer().getUniqueId())) {
            // If true, check if the used bucket is a service item.
            if (this.getSettings().isServiceItem(event.getBucket())) {
                // If true, check if player is already in grace.
                if (this.getManager().isInGrace(event.getPlayer().getUniqueId())) {
                    return; // If true, no check is required.
                }

                this.scheduleServiceCheck(event.getPlayer());
                return;
            }

            // Return if no resulting item exist after the event.
            if (event.getItemStack() == null) {
                return;
            }

            // If exist, check if resulting item is a service item.
            if (this.getSettings().isServiceItem(event.getItemStack().getType())) {
                // If true, check if player is already in warmup or service.
                if (this.getManager().isInWarmup(event.getPlayer().getUniqueId())
                        || this.getManager().isInService(event.getPlayer().getUniqueId())) {
                    return; // If true, no check is required.
                }

                this.scheduleServiceCheck(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(@NotNull final EntityDamageEvent event) {
        if (!((event.getEntity() instanceof Player))) {
            return;
        }

        // Check if player is in service.
        if (this.getManager().isInService(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(@NotNull final PlayerDropItemEvent event) {
        // Check if player is in condition.
        if (this.getManager().isInCondition(event.getPlayer().getUniqueId())) {
            // If true, check if item to drop is a service item.
            if (this.getSettings().isServiceItem(event.getItemDrop().getItemStack().getType())) {
                // If true, check if player is already in grace.
                if (this.getManager().isInGrace(event.getPlayer().getUniqueId())) {
                    return; // If true, no check is required.
                }

                this.scheduleServiceCheck(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerItemBreak(@NotNull final PlayerItemBreakEvent event) {
        // Check if player is in condition.
        if (this.getManager().isInCondition(event.getPlayer().getUniqueId())) {
            // If true, check if broken item is a service item.
            if (this.getSettings().isServiceItem(event.getBrokenItem().getType())) {
                // If true, check if player is already in grace.
                if (this.getManager().isInGrace(event.getPlayer().getUniqueId())) {
                    return; // If true, no check is required.
                }

                this.scheduleServiceCheck(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemHeld(@NotNull final PlayerItemHeldEvent event) {
        // Check if player is in condition.
        if (this.getManager().isInCondition(event.getPlayer().getUniqueId())) {
            final ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());

            // If true, execute service check.
            this.getInstance().handleServiceCheck(event.getPlayer(), item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPickupItem(@NotNull final EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        // Check if player is in condition.
        if (this.getManager().isInCondition(event.getEntity().getUniqueId())) {
            // If true, check if item to pickup is a service item.
            if (this.getSettings().isServiceItem(event.getItem().getItemStack().getType())) {
                // If true, check if player is already in warmup or service.
                if (this.getManager().isInWarmup(event.getEntity().getUniqueId())
                        || this.getManager().isInService(event.getEntity().getUniqueId())) {
                    return; // If true, no check is required.
                }

                this.scheduleServiceCheck((Player) event.getEntity());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerSwapHandItem(@NotNull final PlayerSwapHandItemsEvent event) {
        // Check if player is in condition.
        if (this.getManager().isInCondition(event.getPlayer().getUniqueId())) {
            // If true, execute service check.
            this.getInstance().handleServiceCheck(event.getPlayer(), event.getMainHandItem());
        }
    }

    public void handleServiceCheck(@NotNull final Player target) {
        // Check if the check routine is still scheduled.
        if (this.schedules.containsKey(target.getUniqueId())) {
            // If true, remove the scheduled check routine and return if it was cancelled.
            if (this.schedules.remove(target.getUniqueId()).isCancelled()) {
                return;
            }
        }

        if (target.isOnline()) {
            this.getInstance().handleServiceCheck(target, target.getInventory().getItemInMainHand());
        }
    }

    public void scheduleServiceCheck(@NotNull final Player target) {
        // Check if a new Event got fired before the check routine got called.
        if (this.schedules.containsKey(target.getUniqueId())) {
            // If true, cancel the scheduled check routine and schedule a new one.
            this.schedules.remove(target.getUniqueId()).cancel();
        }

        this.schedules.put(target.getUniqueId(), this.getInstance().runTask(() -> this.handleServiceCheck(target)));
    }
}

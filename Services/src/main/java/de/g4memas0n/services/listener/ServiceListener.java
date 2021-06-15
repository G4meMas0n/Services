package de.g4memas0n.services.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent.ChangeReason;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The Service Listener, listening for events to check for service enabling and disabling.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public final class ServiceListener extends BasicListener {

    public ServiceListener() { }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerArmorStand(@NotNull final PlayerArmorStandManipulateEvent event) {
        // Note: this event fires before the items gets changed.
        // Only schedule check when player is in condition:
        if (this.getManager().isCondition(event.getPlayer())) {
            this.instance.scheduleServiceCheck(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBlockPlace(@NotNull final BlockPlaceEvent event) {
        // Note: this event fires before the placed block gets removed.
        // Only perform check when player is in condition:
        if (this.getManager().isCondition(event.getPlayer())) {
            if (event.getHand() != EquipmentSlot.HAND) {
                return;
            }

            if (this.getSettings().isServiceItem(event.getItemInHand().getType())) {
                if (this.getManager().isGrace(event.getPlayer())) {
                    return;
                }

                // Player has placed a service item, schedule a check:
                this.instance.scheduleServiceCheck(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketEmpty(@NotNull final PlayerBucketEmptyEvent event) {
        // Note: this event fires before the bucket gets emptied.
        this.onPlayerBucketChange(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketFill(@NotNull final PlayerBucketFillEvent event) {
        // Note: this event fires before the bucket gets filled.
        this.onPlayerBucketChange(event);
    }

    public void onPlayerBucketChange(@NotNull final PlayerBucketEvent event) {
        // Only perform check when player is in condition:
        if (this.getManager().isCondition(event.getPlayer())) {
            if (this.getSettings().isServiceItem(event.getBucket())) {
                if (this.getManager().isGrace(event.getPlayer())) {
                    return;
                }

                // Player has filled a service bucket, schedule a check:
                this.instance.scheduleServiceCheck(event.getPlayer());
                return;
            }

            if (event.getItemStack() == null) {
                return;
            }

            // Check for resulting bucket after the event:
            if (this.getSettings().isServiceItem(event.getItemStack().getType())) {
                if (this.getManager().isWarmup(event.getPlayer()) || this.getManager().isService(event.getPlayer())) {
                    return;
                }

                // Resulting bucket is a service bucket, schedule a check:
                this.instance.scheduleServiceCheck(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCauldronChange(@NotNull final CauldronLevelChangeEvent event) {
        if (event.getEntity() == null || !(event.getEntity() instanceof Player)) {
            return;
        }

        // Note: this event fires before the bucket gets emptied/filled.
        // Only perform check when player is in condition:
        if (this.getManager().isCondition((Player) event.getEntity())) {
            if (event.getReason() != ChangeReason.BUCKET_EMPTY && event.getReason() != ChangeReason.BUCKET_FILL) {
                return;
            }

            // Player has emptied/filled a cauldron with an item, schedule a check:
            this.instance.scheduleServiceCheck((Player) event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInventoryClose(@NotNull final InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        // Only perform check when player is in condition:
        if (this.getManager().isCondition((Player) event.getPlayer())) {
            this.instance.runServiceCheck((Player) event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(@NotNull final EntityDamageEvent event) {
        if (!((event.getEntity() instanceof Player))) {
            return;
        }

        // Only cancel event when player is in service:
        if (this.getManager().isService((Player) event.getEntity())) {
            if (this.getSettings().isDamageBlacklist(event.getCause()) || this.getSettings().isDamageMaximum(event.getDamage())) {
                if (!event.getEntity().hasPermission("services.bypass.restriction")) {
                    return;
                }
            }

            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerItemBreak(@NotNull final PlayerItemBreakEvent event) {
        // Note: this event fires before the item gets removed.
        // Only perform check when player is in condition:
        if (this.getManager().isCondition(event.getPlayer())) {
            if (this.getSettings().isServiceItem(event.getBrokenItem().getType())) {
                if (this.getManager().isGrace(event.getPlayer())) {
                    return;
                }

                // Player has break a service item, schedule a check:
                this.instance.scheduleServiceCheck(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemDrop(@NotNull final PlayerDropItemEvent event) {
        // Note: this event fires before the item gets dropped.
        // Only perform check when player is in condition:
        if (this.getManager().isCondition(event.getPlayer())) {
            if (this.getSettings().isServiceItem(event.getItemDrop().getItemStack().getType())) {
                if (this.getManager().isGrace(event.getPlayer())) {
                    return;
                }

                // Player has dropped a service item, schedule a check:
                this.instance.scheduleServiceCheck(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemHeld(@NotNull final PlayerItemHeldEvent event) {
        // Only perform check when player is in condition:
        if (this.getManager().isCondition(event.getPlayer())) {
            final ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());

            // Player has changed the held item, perform a check:
            this.instance.runServiceCheck(event.getPlayer(), item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemPickup(@NotNull final EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        // Note: this event fires before the item gets picked up.
        // Only perform check when player is in condition:
        if (this.getManager().isCondition((Player) event.getEntity())) {
            if (this.getSettings().isServiceItem(event.getItem().getItemStack().getType())) {
                if (this.getManager().isWarmup((Player) event.getEntity()) || this.getManager().isService((Player) event.getEntity())) {
                    return;
                }

                // Player has picked up a service item, schedule a check:
                this.instance.scheduleServiceCheck((Player) event.getEntity());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemSwapHand(@NotNull final PlayerSwapHandItemsEvent event) {
        // Only perform check when player is in condition:
        if (this.getManager().isCondition(event.getPlayer())) {
            // Player has swapped the held item, perform a check:
            this.instance.runServiceCheck(event.getPlayer(), event.getMainHandItem());
        }
    }
}

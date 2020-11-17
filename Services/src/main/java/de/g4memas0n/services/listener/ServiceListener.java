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
        // Check if player is in condition.
        if (this.getManager().isCondition(event.getPlayer())) {
            // Schedule check, as this event fires before the items gets changed.
            this.getInstance().scheduleServiceCheck(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBlockPlace(@NotNull final BlockPlaceEvent event) {
        // Check if player is in condition.
        if (this.getManager().isCondition(event.getPlayer())) {
            // Return if block was not placed from the main hand.
            if (event.getHand() != EquipmentSlot.HAND) {
                return;
            }

            // Check if the block to place is a service item.
            if (this.getSettings().isServiceItem(event.getItemInHand().getType())) {
                // If true, check if player is already in grace.
                if (this.getManager().isGrace(event.getPlayer())) {
                    return; // If true, no check is required.
                }

                // Schedule check, as this event fires before the placed block gets removed.
                this.getInstance().scheduleServiceCheck(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketEmpty(@NotNull final PlayerBucketEmptyEvent event) {
        // Check if player is in condition.
        if (this.getManager().isCondition(event.getPlayer())) {
            // If true, check if the used bucket is a service item.
            if (this.getSettings().isServiceItem(event.getBucket())) {
                // If true, check if player is already in grace.
                if (this.getManager().isGrace(event.getPlayer())) {
                    return; // If true, no check is required.
                }

                // Schedule check, as this event fires before the bucket gets emptied.
                this.getInstance().scheduleServiceCheck(event.getPlayer());
                return;
            }

            // Return if no resulting item exist after the event.
            if (event.getItemStack() == null) {
                return;
            }

            // If exist, check if resulting item is a service item.
            if (this.getSettings().isServiceItem(event.getItemStack().getType())) {
                // If true, check if player is already in warmup or service.
                if (this.getManager().isWarmup(event.getPlayer()) || this.getManager().isService(event.getPlayer())) {
                    return; // If true, no check is required.
                }

                // Schedule check, as this event fires before the resulting item gets set.
                this.getInstance().scheduleServiceCheck(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketFill(@NotNull final PlayerBucketFillEvent event) {
        // Check if player is in condition.
        if (this.getManager().isCondition(event.getPlayer())) {
            // If true, check if the used bucket is a service item.
            if (this.getSettings().isServiceItem(event.getBucket())) {
                // If true, check if player is already in grace.
                if (this.getManager().isGrace(event.getPlayer())) {
                    return; // If true, no check is required.
                }

                // Schedule check, as this event fires before the bucket gets filled.
                this.getInstance().scheduleServiceCheck(event.getPlayer());
                return;
            }

            // Return if no resulting item exist after the event.
            if (event.getItemStack() == null) {
                return;
            }

            // If exist, check if resulting item is a service item.
            if (this.getSettings().isServiceItem(event.getItemStack().getType())) {
                // If true, check if player is already in warmup or service.
                if (this.getManager().isWarmup(event.getPlayer()) || this.getManager().isService(event.getPlayer())) {
                    return; // If true, no check is required.
                }

                // Schedule check, as this event fires before the resulting item gets set.
                this.getInstance().scheduleServiceCheck(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCauldronChange(@NotNull final CauldronLevelChangeEvent event) {
        if (event.getEntity() == null || !(event.getEntity() instanceof Player)) {
            return;
        }

        // Check if player is in condition.
        if (this.getManager().isCondition((Player) event.getEntity())) {
            // Return if change reason is not emptying or filling a bucket.
            if (event.getReason() != ChangeReason.BUCKET_EMPTY && event.getReason() != ChangeReason.BUCKET_FILL) {
                return;
            }

            // Schedule check, as this event fires before the bucket gets emptied/filled.
            this.getInstance().scheduleServiceCheck((Player) event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInventoryClose(@NotNull final InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        // Check if player is in condition.
        if (this.getManager().isCondition((Player) event.getPlayer())) {
            // If true, execute service check.
            this.getInstance().runServiceCheck((Player) event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(@NotNull final EntityDamageEvent event) {
        if (!((event.getEntity() instanceof Player))) {
            return;
        }

        // Check if player is in service.
        if (this.getManager().isService((Player) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerItemBreak(@NotNull final PlayerItemBreakEvent event) {
        // Check if player is in condition.
        if (this.getManager().isCondition(event.getPlayer())) {
            // If true, check if broken item is a service item.
            if (this.getSettings().isServiceItem(event.getBrokenItem().getType())) {
                // If true, check if player is already in grace.
                if (this.getManager().isGrace(event.getPlayer())) {
                    return; // If true, no check is required.
                }

                // Schedule check, as this event fires before the item gets removed.
                this.getInstance().scheduleServiceCheck(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemDrop(@NotNull final PlayerDropItemEvent event) {
        // Check if player is in condition.
        if (this.getManager().isCondition(event.getPlayer())) {
            // If true, check if item to drop is a service item.
            if (this.getSettings().isServiceItem(event.getItemDrop().getItemStack().getType())) {
                // If true, check if player is already in grace.
                if (this.getManager().isGrace(event.getPlayer())) {
                    return; // If true, no check is required.
                }

                // Schedule check, as this event fires before the item gets removed.
                this.getInstance().scheduleServiceCheck(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemHeld(@NotNull final PlayerItemHeldEvent event) {
        // Check if player is in condition.
        if (this.getManager().isCondition(event.getPlayer())) {
            final ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());

            // If true, execute service check on new held item.
            this.getInstance().runServiceCheck(event.getPlayer(), item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemPickup(@NotNull final EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        // Check if player is in condition.
        if (this.getManager().isCondition((Player) event.getEntity())) {
            // If true, check if item to pickup is a service item.
            if (this.getSettings().isServiceItem(event.getItem().getItemStack().getType())) {
                // If true, check if player is already in warmup or service.
                if (this.getManager().isWarmup((Player) event.getEntity()) || this.getManager().isService((Player) event.getEntity())) {
                    return; // If true, no check is required.
                }

                // Schedule check, as this event fires before the item gets added.
                this.getInstance().scheduleServiceCheck((Player) event.getEntity());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemSwapHand(@NotNull final PlayerSwapHandItemsEvent event) {
        // Check if player is in condition.
        if (this.getManager().isCondition(event.getPlayer())) {
            // If true, execute service check on new main hand item.
            this.getInstance().runServiceCheck(event.getPlayer(), event.getMainHandItem());
        }
    }
}

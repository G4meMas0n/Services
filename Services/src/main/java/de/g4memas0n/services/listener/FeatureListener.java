package de.g4memas0n.services.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The Feature Listener, listening for events related for additional plugin features.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public final class FeatureListener extends BasicListener {

    // Event Listener for the unlimited buckets configuration feature.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerBucketEmpty(@NotNull final PlayerBucketEmptyEvent event) {
        // Check if unlimited service buckets is enabled.
        if (this.getInstance().getSettings().isUnlimitedBuckets()) {
            // If true, check if filled bucket is a service item.
            if (this.getInstance().getSettings().isServiceItem(event.getBucket())) {
                // If true, check if player is in service. (If he is in service, he must be in condition)
                if (this.getInstance().getServiceManager().isInService(event.getPlayer().getUniqueId())) {
                    // If true, check if the main hand contains the bucket.
                    if (event.getPlayer().getInventory().getItemInMainHand().getType().equals(event.getBucket())) {
                        // If true, set resulting item to the filled bucket.
                        if (event.getItemStack() != null) {
                            event.setItemStack(new ItemStack(event.getBucket(), event.getItemStack().getAmount()));
                        } else {
                            event.setItemStack(new ItemStack(event.getBucket()));
                        }

                        if (this.getInstance().getSettings().isDebug()) {
                            this.getInstance().getLogger().info(String.format("Player '%s' used unlimited buckets on his bucket: %s",
                                    event.getPlayer().getName(), event.getBucket().getKey()));
                        }

                        // Set resulting item on next tick, because this event ignores the resulting item-stack.
                        this.getInstance().runTask(() -> event.getPlayer().getInventory().setItemInMainHand(event.getItemStack()));
                    }
                }
            }
        }
    }

    // Event Listener for the unlimited durability configuration feature.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerItemDamage(@NotNull final PlayerItemDamageEvent event) {
        // Check if unlimited service durability is enabled.
        if (this.getInstance().getSettings().isUnlimitedDurability()) {
            // If true, check if damaged tool is a service item.
            if (this.getInstance().getSettings().isServiceItem(event.getItem().getType())) {
                // If true, check if player is in service. (If he is in service, he must be in condition)
                if (this.getInstance().getServiceManager().isInService(event.getPlayer().getUniqueId())) {
                    // If true, cancel event as the tool should not be damaged.
                    event.setCancelled(true);

                    if (this.getInstance().getSettings().isDebug()) {
                        this.getInstance().getLogger().info(String.format("Player '%s' used unlimited durability on his tool: %s",
                                event.getPlayer().getName(), event.getItem().getType().getKey()));
                    }
                }
            }
        }
    }
}

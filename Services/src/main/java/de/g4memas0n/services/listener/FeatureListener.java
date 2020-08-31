package de.g4memas0n.services.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The Feature Listener, listening for events related for additional plugin features.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public final class FeatureListener extends BasicListener {

    // Event Listener for the hidden unlimited bucket configuration feature.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerBucketUnlimited(@NotNull final PlayerBucketEmptyEvent event) {
        // Check if unlimited service buckets is enabled.
        if (this.getInstance().getSettings().isUnlimitedBucket()) {
            // Check if filled bucket is a service item.
            if (this.getInstance().getSettings().isServiceItem(event.getBucket())) {
                // If true, check if player is in condition.
                if (this.getInstance().getServiceManager().isInCondition(event.getPlayer().getUniqueId())) {
                    // If true, check if player is also in service.
                    if (this.getInstance().getServiceManager().isInService(event.getPlayer().getUniqueId())) {
                        // If true, check if the main hand contains the bucket.
                        if (event.getPlayer().getInventory().getItemInMainHand().getType().equals(event.getBucket())) {
                            // If true, set resulting item to the filled bucket.
                            if (event.getItemStack() != null) {
                                event.setItemStack(new ItemStack(event.getBucket(), event.getItemStack().getAmount()));
                            } else {
                                event.setItemStack(new ItemStack(event.getBucket()));
                            }

                            // Set resulting item on next tick, because this event ignores the resulting item-stack.
                            this.getInstance().runTask(() -> event.getPlayer().getInventory().setItemInMainHand(event.getItemStack()));
                        }
                    }
                }
            }
        }
    }
}
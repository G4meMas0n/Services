package com.github.g4memas0n.services.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import java.util.Iterator;

/**
 * The Feature Listener, listening for events related for additional plugin features.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public final class FeatureListener extends BasicListener {

    public FeatureListener() { }

    /*
     * Event Listener for the disabled drops configuration feature.
     */

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(@NotNull final PlayerDeathEvent event) {
        // Only perform the checks when disabled drops is enabled.
        if (this.getSettings().isDisabledDrops()) {
            final Player player = event.getEntity();

            // Only filter items when player is allowed to use service:
            if (player.hasPermission("services.service")) {
                for (final Iterator<ItemStack> iterator = event.getDrops().iterator(); iterator.hasNext();) {
                    final Material material = iterator.next().getType();

                    if (this.getSettings().isDisabledDrop(material)) {
                        if (this.getSettings().isPermissionPerItem()) {
                            if (!player.hasPermission("services.item." + material.getKey().getKey())) {
                                continue;
                            }
                        }

                        iterator.remove();
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerItemDrop(@NotNull final PlayerDropItemEvent event) {
        // Only perform the checks when disabled drops is enabled.
        if (this.getSettings().isDisabledDrops()) {
            final Player player = event.getPlayer();

            // Only block item drop when player is allowed to use service:
            if (player.hasPermission("services.service")) {
                final Material material = event.getItemDrop().getItemStack().getType();

                if (this.getSettings().isDisabledDrop(material)) {
                    if (this.getSettings().isPermissionPerItem()) {
                        if (!player.hasPermission("services.item." + material.getKey().getKey())) {
                            return;
                        }
                    }

                    event.setCancelled(true);

                    if (this.getSettings().isDebug()) {
                        this.getLogger().info("Cancelled drop of service item '" + material + "' for player: " + player.getName());
                    }
                }
            }
        }
    }

    /*
     * Event Listener for the disabled effects configuration feature.
     */

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPotionEffect(@NotNull final EntityPotionEffectEvent event) {
        if (!((event.getEntity() instanceof Player))) {
            return;
        }

        // Only perform the checks when disabled effects is enabled.
        if (this.getSettings().isDisabledEffects()) {
            if (event.getAction() == Action.CLEARED || event.getAction() == Action.REMOVED) {
                return;
            }

            if (this.getSettings().isDisabledEffect(event.getModifiedType())) {
                final Player player = (Player) event.getEntity();

                // Only cancel effect when player is in service:
                if (this.getManager().isService(player)) {
                    event.setCancelled(true);

                    if (this.getSettings().isDebug()) {
                        this.getLogger().info("Cancelled added/changed potion effect '" + event.getModifiedType().getName() + "' for service player:" + player.getName());
                    }
                }
            }
        }
    }

    /*
     * Event Listener for the unlimited buckets feature.
     */

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerBucketEmpty(@NotNull final PlayerBucketEmptyEvent event) {
        // Only perform the checks when unlimited buckets are enabled.
        if (this.getSettings().isUnlimitedBuckets()) {
            if (this.getSettings().isServiceItem(event.getBucket())) {
                final Player player = event.getPlayer();

                // Only perform feature when player is in service:
                if (this.getManager().isService(player)) {
                    if (player.getInventory().getItemInMainHand().getType().equals(event.getBucket())) {
                        if (event.getItemStack() != null) {
                            event.setItemStack(new ItemStack(event.getBucket(), event.getItemStack().getAmount()));
                        } else {
                            event.setItemStack(new ItemStack(event.getBucket()));
                        }

                        if (this.getSettings().isDebug()) {
                            this.getLogger().info("Filled service bucket '" + event.getBucket() + "' of service player: " + player.getName());
                        }

                        // Note: this event ignores the resulting item-stack.
                        this.instance.runTask(() -> player.getInventory().setItemInMainHand(event.getItemStack()));
                    }
                }
            }
        }
    }

    /*
     * Event Listener for the unlimited durability configuration feature.
     */

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerItemDamage(@NotNull final PlayerItemDamageEvent event) {
        // Only perform the checks when unlimited durability is enabled.
        if (this.getSettings().isUnlimitedDurability()) {
            if (this.getSettings().isServiceItem(event.getItem().getType())) {
                final Player player = event.getPlayer();

                // Only perform feature when player is in service:
                if (this.getManager().isService(player)) {
                    if (player.getInventory().getItemInMainHand().equals(event.getItem())) {
                        event.setCancelled(true);

                        if (this.getSettings().isDebug()) {
                            this.getLogger().info("Repaired service tool '" + event.getItem().getType() + "' of service player: " + player.getName());
                        }
                    }
                }
            }
        }
    }
}

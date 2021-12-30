/*
 * Services - A Spigot plugin that provides moderators god mode to fulfill service tasks.
 * Copyright (C) 2021 G4meMas0n
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.g4memas0n.services;

import com.github.g4memas0n.services.command.ServicesCommand;
import com.github.g4memas0n.services.listener.ConditionListener;
import com.github.g4memas0n.services.listener.FeatureListener;
import com.github.g4memas0n.services.listener.ServiceListener;
import com.github.g4memas0n.services.config.Settings;
import com.github.g4memas0n.services.util.Messages;
import com.github.g4memas0n.services.util.Registrable;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.g4memas0n.services.util.Messages.tl;
import static com.github.g4memas0n.services.util.Messages.tlEnum;

/**
 * The Services main class.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public final class Services extends JavaPlugin {

    private final List<Registrable<Services>> registries;

    private Map<UUID, BukkitTask> schedules;

    private ServiceManager manager;
    private Settings settings;
    private Messages messages;

    private boolean loaded;
    private boolean enabled;

    public Services() {
        this.registries = new ArrayList<>(4);
    }

    public @NotNull ServiceManager getManager() {
        return this.manager;
    }

    public @NotNull Settings getSettings() {
        return this.settings;
    }

    public @NotNull Messages getMessages() {
        return this.messages;
    }

    @Override
    public void onLoad() {
        if (this.loaded) {
            this.getLogger().severe("Tried to load plugin twice. Plugin is already loaded.");
            return;
        }

        this.settings = new Settings(this);
        this.settings.load();
        this.messages = new Messages(this.getDataFolder(), this.getLogger());
        this.messages.setLocale(this.settings.getLocale());
        this.manager = new ServiceManager(this);
        this.loaded = true;
    }

    @Override
    public void onEnable() {
        if (this.enabled) {
            this.getLogger().severe("Tried to enable plugin twice. Plugin is already enabled.");
            return;
        }

        if (!this.loaded) {
            this.getLogger().warning("Plugin was not loaded. Loading it now...");
            this.onLoad();
        }

        this.schedules = new HashMap<>();
        this.messages.enable();

        if (this.settings.isDebug()) {
            this.getLogger().info("Register plugin command and listeners...");
        }

        if (this.registries.isEmpty()) {
            this.registries.add(new ServicesCommand());
            this.registries.add(new ConditionListener());
            this.registries.add(new FeatureListener());
            this.registries.add(new ServiceListener());
        }

        this.registries.forEach(registry -> registry.register(this));

        if (this.settings.isDebug()) {
            this.getLogger().info("Plugin command and listeners has been registered.");
        }

        // Perform condition check for all online players:
        if (!this.getServer().getOnlinePlayers().isEmpty()) {
            this.getLogger().info("Check service conditions for all online players...");
            this.getServer().getOnlinePlayers().forEach(this::runConditionCheck);
            this.getLogger().info("Service conditions for all online players has been checked.");
        }

        this.enabled = true;
    }

    @Override
    public void onDisable() {
        if (!this.enabled) {
            this.getLogger().severe("Tried to disable plugin twice. Plugin is already disabled.");
            return;
        }

        this.getServer().getScheduler().cancelTasks(this);

        if (this.settings.isDebug()) {
            this.getLogger().info("Unregister plugin command and listeners...");
        }

        this.registries.forEach(Registrable::unregister);

        if (this.settings.isDebug()) {
            this.getLogger().info("Plugin command and listeners has been unregistered.");
        }

        this.messages.disable();
        this.schedules = null;
        this.settings = null;
        this.messages = null;
        this.manager = null;
        this.enabled = false;
        this.loaded = false;
    }

    @Override
    public void reloadConfig() {
        this.settings.load();
        this.messages.setLocale(this.settings.getLocale());
        this.registries.forEach(Registrable::reload);

        // Perform condition check for all online players:
        if (!this.getServer().getOnlinePlayers().isEmpty()) {
            this.getLogger().info("Check service conditions for all online players...");
            this.getServer().getOnlinePlayers().forEach(this::runConditionCheck);
            this.getLogger().info("Service conditions for all online players has been checked.");
        }
    }

    @Override
    public void saveConfig() {
        /*
         * Disabled, because it is not intended to save the config file, as this breaks the comments.
         */
    }

    public @NotNull BukkitTask runTask(@NotNull final Runnable task) {
        return this.getServer().getScheduler().runTask(this, task);
    }

    public @NotNull BukkitTask scheduleTask(@NotNull final Runnable task, final long delay) {
        return this.getServer().getScheduler().runTaskLater(this, task, delay);
    }

    /**
     * Performs the condition check for the given player.
     *
     * <p>A player must fulfill following requirements to be in condition:<br>
     * - The player has permission {@code services.service}<br>
     * - The player is in a registered service {@link org.bukkit.GameMode game-mode}<br>
     * - The player is in a registered service {@link World world}<br>
     * - The player is in a registered service {@link Environment environment}</p>
     *
     * @param player the player to check for condition.
     */
    public void runConditionCheck(@NotNull final Player player) {
        this.schedules.remove(player.getUniqueId());

        // Check for service permission and remove player from condition if it is not permitted.
        if (!player.hasPermission("services.service")) {
            if (this.settings.isDebug()) {
                this.getLogger().info("Player '" + player.getName() + "' is missing permission for service mode.");
            }

            if (this.manager.removeCondition(player) && this.manager.removeService(player)) {
                player.sendMessage(tl("service.denied"));
            }

            return;
        }

        if (this.settings.isServiceGameMode(player.getGameMode())) {
            final World world = player.getWorld();

            if (this.settings.isServiceWorld(world)) {
                // Check for world permission and remove player from condition if it is not permitted:
                if (this.settings.isPermissionPerWorld() && !player.hasPermission("services.world." + world.getName().toLowerCase())) {
                    if (this.settings.isDebug()) {
                        this.getLogger().info("Player '" + player.getName() + "' is missing permission for service world: " + world.getName());
                    }

                    if (this.manager.removeCondition(player) && this.manager.removeService(player)) {
                        player.sendMessage(tl("service.denied.world", world.getName()));
                    }

                    return;
                }

                final Environment environment = world.getEnvironment();

                if (this.settings.isServiceEnvironment(environment)) {
                    // Check for environment permission and remove player from condition if it is not permitted:
                    if (this.settings.isPermissionPerEnvironment() && !player.hasPermission("services.environment." + environment.name().toLowerCase())) {
                        if (this.settings.isDebug()) {
                            this.getLogger().info("Player '" + player.getName() + "' is missing permission for service environment: " + environment.name());
                        }

                        if (this.manager.removeCondition(player) && this.manager.removeService(player)) {
                            player.sendMessage(tlEnum("service.denied.environment", environment));
                        }

                        return;
                    }

                    // Player is in a service game-mode, world and environment, add it to condition:
                    if (this.manager.addCondition(player)) {
                        if (this.settings.isDebug()) {
                            this.getLogger().info("Player '" + player.getName() + "' is now in service world: " + world.getName() + " (environment: " + environment.name() + ")");
                        }

                        this.runServiceCheck(player);
                    }

                    return;
                }

                // Player is not in a service environment, remove it from condition:
                if (this.manager.removeCondition(player)) {
                    if (this.settings.isDebug()) {
                        this.getLogger().info("Player '" + player.getName() + "' is now in non-service environment: " + environment.name());
                    }

                    if (this.manager.removeService(player)) {
                        player.sendMessage(tlEnum("service.disabled.environment", environment));
                    }
                }

                return;
            }

            // Player is not in a service world, remove it from condition:
            if (this.manager.removeCondition(player)) {
                if (this.settings.isDebug()) {
                    this.getLogger().info("Player '" + player.getName() + "' is now in non-service world: " + world.getName());
                }

                if (this.manager.removeService(player)) {
                    player.sendMessage(tl("service.disabled.world", world.getName()));
                }
            }

            return;
        }

        // Player is not in a service game-mode, remove it from condition:
        if (this.manager.removeCondition(player)) {
            if (this.settings.isDebug()) {
                this.getLogger().info("Player '" + player.getName() + "' is now in non-service game-mode: " + player.getGameMode().name());
            }

            if (this.manager.removeService(player)) {
                player.sendMessage(tlEnum("service.disabled.game-mode", player.getGameMode()));
            }
        }
    }

    /**
     * Schedules the condition check for the given player.
     *
     * @param player the player to check for condition.
     * @see Services#runConditionCheck(Player)
     */
    public void scheduleConditionCheck(@NotNull final Player player) {
        // Cancel previous scheduled task, if existed:
        if (this.schedules.containsKey(player.getUniqueId())) {
            this.schedules.remove(player.getUniqueId()).cancel();
        }

        this.schedules.put(player.getUniqueId(), this.runTask(() -> this.runConditionCheck(player)));
    }

    /**
     * Performs the service check for the given player with the item in the main hand.
     *
     * @param player the player to check for service.
     * @see Services#runServiceCheck(Player, ItemStack)
     */
    public void runServiceCheck(@NotNull final Player player) {
        this.runServiceCheck(player, player.getInventory().getItemInMainHand());
    }

    /**
     * Performs the service check for the given player.
     *
     * <p>A player must fulfill following requirements to be in service:<br>
     * - The player is in {@link Services#runConditionCheck(Player) condition} for service<br>
     * - The player holds a registered service {@link Material item} in the main hand<br>
     *
     * @param player the player to check for service.
     * @param item the held item to check for.
     * @see Services#runConditionCheck(Player)
     */
    public void runServiceCheck(@NotNull final Player player, @Nullable final ItemStack item) {
        this.schedules.remove(player.getUniqueId());

        // Only perform check when player is in condition:
        if (this.manager.isCondition(player)) {
            if (item != null && this.settings.isServiceItem(item.getType())) {
                // Check for permission and remove player from service if it is not permitted:
                if (this.settings.isPermissionPerItem() && !player.hasPermission("services.item." + item.getType().getKey().getKey())) {
                    if (this.settings.isDebug()) {
                        this.getLogger().info("Player '" + player.getName() + "' is missing permission for service item: " + item.getType().getKey());
                    }

                    if (this.settings.isGracePeriod() && !player.hasPermission("services.bypass.grace")) {
                        this.manager.addGrace(player, this.settings.getGracePeriod());
                        return;
                    }

                    this.manager.removeService(player);
                    return;
                }

                if (this.settings.isDebug()) {
                    this.getLogger().info("Player '" + player.getName() + "' is now using service item: " + item.getType().getKey());
                }

                // Player is using a service item, add it to service:
                if (this.settings.isWarmupPeriod() && !player.hasPermission("services.bypass.warmup")) {
                    this.manager.addWarmup(player, this.settings.getWarmupPeriod());
                    return;
                }

                this.manager.addService(player);
                return;
            }

            // Player is not using any service items, remove it from service:
            if (this.manager.isWarmup(player) || this.manager.isService(player)) {
                if (this.settings.isDebug() && !this.manager.isGrace(player)) {
                    final Material material = item != null ? item.getType() : Material.AIR;

                    this.getLogger().info("Player '" + player.getName() + "' is now using non-service item: " + material.getKey());
                }

                if (this.settings.isGracePeriod()  && !player.hasPermission("services.bypass.grace")) {
                    this.manager.addGrace(player, this.settings.getGracePeriod());
                    return;
                }

                this.manager.removeService(player);
            }
        }
    }

    /**
     * Schedules the service check for the given player with the item in the main hand.
     *
     * @param player the player to check for service.
     * @see Services#runServiceCheck(Player, ItemStack)
     */
    public void scheduleServiceCheck(@NotNull final Player player) {
        // Cancel previous scheduled task, if existed:
        if (this.schedules.containsKey(player.getUniqueId())) {
            this.schedules.remove(player.getUniqueId()).cancel();
        }

        this.schedules.put(player.getUniqueId(), this.runTask(() -> this.runServiceCheck(player)));
    }
}

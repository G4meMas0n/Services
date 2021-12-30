package com.github.g4memas0n.services.listener;

import com.github.g4memas0n.services.ServiceManager;
import com.github.g4memas0n.services.Services;
import com.github.g4memas0n.services.config.Settings;
import com.github.g4memas0n.services.util.Registrable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import java.util.logging.Logger;

/**
 * Abstract Representation of an event listener.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public abstract class BasicListener extends Registrable<Services> implements Listener {

    protected BasicListener() { }

    public final boolean register(@NotNull final Services instance) {
        if (super.register(instance)) {
            this.instance.getServer().getPluginManager().registerEvents(this, instance);

            if (this.instance.getSettings().isDebug()) {
                this.instance.getLogger().info("Registered listener: " + this);
            }

            return true;
        }

        return false;
    }

    public final boolean unregister() {
        if (super.unregister()) {
            HandlerList.unregisterAll(this);

            if (this.instance.getSettings().isDebug()) {
                this.instance.getLogger().info("Unregistered listener: " + this);
            }

            this.instance = null;
            return true;
        }

        return false;
    }

    public final @NotNull ServiceManager getManager() {
        return this.instance.getManager();
    }

    public final @NotNull Settings getSettings() {
        return this.instance.getSettings();
    }

    public final @NotNull Logger getLogger() {
        return this.instance.getLogger();
    }

    @Override
    public final @NotNull String toString() {
        return this.getClass().getSimpleName();
    }
}

package de.g4memas0n.services.listener;

import de.g4memas0n.services.ServiceManager;
import de.g4memas0n.services.Services;
import de.g4memas0n.services.config.Settings;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import java.util.logging.Logger;

/**
 * Abstract Representation of a event listener.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public abstract class BasicListener implements Listener {

    /**
     * This reference will never be null for all implementing listeners, as they will only be called when they are
     * registered by {@link #register(Services)}.
     */
    protected Services instance;

    protected BasicListener() { }

    public final void register(@NotNull final Services instance) {
        if (this.instance == null) {
            this.instance = instance;
            this.instance.getServer().getPluginManager().registerEvents(this, instance);

            if (this.instance.getSettings().isDebug()) {
                this.instance.getLogger().info("Registered listener: " + this);
            }
        }
    }

    public final void unregister() {
        if (this.instance != null) {
            HandlerList.unregisterAll(this);

            if (this.instance.getSettings().isDebug()) {
                this.instance.getLogger().info("Unregistered listener: " + this);
            }

            this.instance = null;
        }
    }

    public final @NotNull Services getInstance() {
        if (this.instance == null) {
            throw new IllegalStateException("Unregistered listener '" + this + "' tried to get the plugin instance");
        }

        return this.instance;
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
    public final String toString() {
        return this.getClass().getSimpleName();
    }
}

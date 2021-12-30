package com.github.g4memas0n.services.util;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract Registrable Class that represents a class that must be registered in bukkit/spigot,
 * like command executors or listeners.
 * @param <T> The main class of the plugin.
 *
 * @author G4meMas0n
 * @since Release 2.0.0
 */
public abstract class Registrable<T extends JavaPlugin> {

    /**
     * This reference will never be null for all implementing registrable, as they will only be called when they are
     * registered by {@link #register(T)}.
     */
    protected T instance;

    public boolean register(@NotNull final T instance) {
        if (this.instance == null) {
            this.instance = instance;
            return true;
        }

        return true;
    }

    public boolean unregister() {
        return this.instance != null;
    }

    public void reload() { }
}

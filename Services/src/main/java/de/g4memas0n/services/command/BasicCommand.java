package de.g4memas0n.services.command;

import de.g4memas0n.services.Services;
import de.g4memas0n.services.util.Messages;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * Abstract Command Representation that represents all non bukkit/spigot commands.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public abstract class BasicCommand {

    protected final String name;
    protected final int minArgs;
    protected final int maxArgs;

    /**
     * This reference will never be null for all implementing commands, as they will only be called when they are
     * registered by {@link #register(Services)}.
     */
    protected Services instance;

    private String permission;

    protected BasicCommand(@NotNull final String name, final int minArgs, final int maxArgs) {
        this.name = name;
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
        this.permission = "";
    }

    public boolean register(@NotNull final Services instance) {
        if (this.instance == null) {
            this.instance = instance;

            if (this.instance.getSettings().isDebug()) {
                this.instance.getLogger().info("Registered command: " + this);
            }

            return true;
        }

        return false;
    }

    public boolean unregister() {
        if (this.instance != null) {
            if (this.instance.getSettings().isDebug()) {
                this.instance.getLogger().info("Unregistered command: " + this);
            }

            this.instance = null;
            return true;
        }

        return false;
    }

    public final @NotNull Services getInstance() {
        if (this.instance == null) {
            throw new IllegalStateException("Unregistered command '" + this.name + "' tried to get the plugin instance");
        }

        return this.instance;
    }

    public final @NotNull String getName() {
        return this.name;
    }

    @SuppressWarnings("unused")
    public final int getMinArgs() {
        return this.minArgs;
    }

    @SuppressWarnings("unused")
    public final int getMaxArgs() {
        return this.maxArgs;
    }

    public final boolean argsInRange(final int arguments) {
        return this.maxArgs > 0
                ? arguments >= this.minArgs && arguments <= this.maxArgs
                : arguments >= this.minArgs;
    }

    public final @NotNull String getDescription() {
        return Messages.tl(this.name.concat("CommandDescription"));
    }

    public final @NotNull String getUsage() {
        return Messages.tl(this.name.concat("CommandUsage"));
    }

    public @NotNull String getPermission() {
        return this.permission;
    }

    public void setPermission(@NotNull final String permission) {
        this.permission = permission;
    }

    /**
     * Executes the command for the given sender, returning its success.
     *
     * <p>If false is returned, then the help of the command will be sent to the sender.</p>
     *
     * @param sender the source who executed the command.
     * @param arguments the passed arguments of the sender.
     * @return true if the command execution was valid, false otherwise.
     */
    public abstract boolean execute(@NotNull final CommandSender sender,
                                    @NotNull final String[] arguments);

    /**
     * Requests a list of possible completions for a command argument.
     *
     * @param sender the source who tab-completed the command.
     * @param arguments the passed arguments of the sender, including the final partial argument to be completed.
     * @return a list of possible completions for the final arguments.
     */
    public abstract @NotNull List<String> tabComplete(@NotNull final CommandSender sender,
                                                      @NotNull final String[] arguments);

    @Override
    public final @NotNull String toString() {
        return this.getClass().getSimpleName()
                + "{name=" + this.name
                + ";min-args=" + this.minArgs
                + ";max-args=" + this.maxArgs
                + ";permission=" + (this.permission != null ? this.permission : "")
                + "}";
    }

    @Override
    public final boolean equals(@Nullable final Object object) {
        if (object != null) {
            if (object == this) {
                return true;
            }

            if (object instanceof BasicCommand) {
                final BasicCommand other = (BasicCommand) object;

                return this.name.equals(other.name)
                        && this.minArgs == other.minArgs
                        && this.maxArgs == other.maxArgs;
            }
        }

        return false;
    }

    @Override
    public final int hashCode() {
        final int prime = 69;
        int result = 2;

        result = prime * result + this.name.hashCode();
        result = prime * result + Integer.hashCode(this.minArgs);
        result = prime * result + Integer.hashCode(this.maxArgs);

        return result;
    }
}

package de.g4memas0n.services.command;

import de.g4memas0n.services.Services;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Collections;
import java.util.List;

import static de.g4memas0n.services.util.messaging.Messages.tl;

/**
 * Abstract Command Representation that represents all non bukkit/spigot commands.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public abstract class BasicCommand {

    private final String name;
    private final int minArgs;
    private final int maxArgs;

    private Services instance;

    private List<String> aliases;
    private String permission;

    protected BasicCommand(@NotNull final String name,
                           final int minArgs,
                           final int maxArgs) {
        this.name = name;
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
        this.permission = "";
    }

    public boolean register(@NotNull final Services instance) {
        if (this.instance != null) {
            return false;
        }

        this.instance = instance;
        return true;
    }

    public boolean unregister() {
        if (this.instance == null) {
            return false;
        }

        this.instance = null;
        return true;
    }

    public final @NotNull Services getInstance() {
        if (this.instance == null) {
            throw new IllegalStateException(String.format("Unregistered command '%s' tried to get the plugin instance",
                    this.getName()));
        }

        return this.instance;
    }

    public final @NotNull String getName() {
        return this.name;
    }

    public final int getMinArgs() {
        return this.minArgs;
    }

    public final int getMaxArgs() {
        return this.maxArgs;
    }

    public final boolean argsInRange(final int arguments) {
        return this.maxArgs > 0
                ? arguments >= this.minArgs && arguments <= this.maxArgs
                : arguments >= this.minArgs;
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

    public @NotNull List<String> getAliases() {
        if (this.aliases == null) {
            return Collections.emptyList();
        }

        return this.aliases;
    }

    public void setAliases(@NotNull final List<String> aliases) {
        if (aliases.equals(this.aliases)) {
            return;
        }

        this.aliases = Collections.unmodifiableList(aliases);
    }

    public @NotNull String getPermission() {
        return this.permission;
    }

    public void setPermission(@NotNull final String permission) {
        if (permission.equals(this.permission)) {
            return;
        }

        this.permission = permission;
    }

    public final @NotNull String getDescription() {
        return tl(this.name.concat("CommandDescription"));
    }

    public final @NotNull String getUsage() {
        return tl(this.name.concat("CommandUsage"));
    }

    @Override
    public final @NotNull String toString() {
        final StringBuilder builder = new StringBuilder(this.getClass().getSimpleName());

        builder.append("{name=");
        builder.append(this.name);
        builder.append(";min-args=");
        builder.append(this.minArgs);
        builder.append(";max-args=");
        builder.append(this.maxArgs);

        if (!this.getAliases().isEmpty()) {
            builder.append(";aliases=");
            builder.append(String.join(",", this.getAliases()));
        }

        if (!this.getPermission().isEmpty()) {
            builder.append(";permission=");
            builder.append(this.getPermission());
        }

        return builder.append("}").toString();
    }

    @Override
    public final boolean equals(@Nullable final Object object) {
        if (object == null) {
            return false;
        }

        if (object == this) {
            return true;
        }

        if (object instanceof BasicCommand) {
            final BasicCommand other = (BasicCommand) object;

            return this.name.equals(other.name)
                    && this.minArgs == other.minArgs
                    && this.maxArgs == other.maxArgs;
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

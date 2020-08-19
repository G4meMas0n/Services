package de.g4memas0n.services.command;

import de.g4memas0n.services.Services;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import java.util.Collections;
import java.util.List;

import static de.g4memas0n.services.util.messaging.Messages.tl;

/**
 * Abstract Plugin Command Representation that represent commands that are registered to bukkit/spigot.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public abstract class BasicPluginCommand extends BasicCommand implements TabExecutor {

    private PluginCommand command;

    protected BasicPluginCommand(@NotNull final String name,
                                 final int minArgs,
                                 final int maxArgs) {
        super(name, minArgs, maxArgs);
    }

    public final @NotNull PluginCommand getCommand() {
        if (this.command == null) {
            throw new IllegalStateException(String.format("Unregistered command '%s' tried to get the plugin command",
                    this.getName()));
        }

        return this.command;
    }

    @Override
    public boolean register(@NotNull final Services instance) {
        if (this.command != null) {
            return false;
        }

        this.command = instance.getCommand(this.getName());

        if (this.command == null) {
            instance.getLogger().warning("Failed to register command " + this.getName()
                    + "! Is it registered to bukkit/spigot?");
            return false;
        }

        if (super.register(instance)) {
            this.command.setExecutor(this);
            this.command.setTabCompleter(this);
            this.command.setPermissionMessage(tl("noPermission"));
            return true;
        }

        this.command = null;
        return false;
    }

    @Override
    public boolean unregister() {
        if (this.command == null) {
            return false;
        }

        if (super.unregister()) {
            this.command = null;
            return true;
        }

        return false;
    }

    @Override
    public final boolean onCommand(@NotNull final CommandSender sender,
                                   @NotNull final Command command,
                                   @NotNull final String alias,
                                   @NotNull final String[] arguments) {
        if (this.command == null) {
            this.getInstance().getLogger().severe(String.format("Unregistered plugin command '%s' was executed.", this.getName()));
            return true;
        }

        if (sender instanceof BlockCommandSender) {
            this.getInstance().getLogger().severe(String.format("Plugin command '%s' was executed by unallowed sender.", this.getName()));
            return true;
        }

        if (sender.hasPermission(this.getPermission())) {
            if (this.execute(sender, arguments)) {
                return true;
            }

            // Invalid command usage. Send syntax help:
            sender.sendMessage(this.getDescription());
            sender.sendMessage(this.getUsage());
            return true;
        }

        sender.sendMessage(tl("noPermission"));
        return true;
    }

    @Override
    public final @NotNull List<String> onTabComplete(@NotNull final CommandSender sender,
                                                     @NotNull final Command command,
                                                     @NotNull final String alias,
                                                     @NotNull final String[] arguments) {
        if (this.command == null) {
            this.getInstance().getLogger().severe(String.format("Unregistered plugin command '%s' was tab completed", this.getName()));

            return Collections.emptyList();
        }

        if (sender instanceof BlockCommandSender) {
            this.getInstance().getLogger().severe(String.format("Plugin command '%s' was tab completed by unallowed sender.", this.getName()));

            return Collections.emptyList();
        }

        if (sender.hasPermission(this.getPermission())) {
            return this.tabComplete(sender, arguments);
        }

        return Collections.emptyList();
    }
}
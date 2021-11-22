package com.github.g4memas0n.services.command;

import com.github.g4memas0n.services.Services;
import com.github.g4memas0n.services.util.Messages;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.g4memas0n.services.util.Messages.tl;

/**
 * The main services command that delegates to all sub-commands.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public final class ServicesCommand extends BasicCommand implements TabExecutor {

    private final Map<String, BasicCommand> commands = new HashMap<>(3, 1);

    private PluginCommand command;

    public ServicesCommand() {
        super("services", 1, -1);

        this.addCommand(new ReloadCommand());
        this.addCommand(new VersionCommand());
        this.setPermission("services.use");
    }

    public @NotNull String getPermission() {
        if (this.command.getPermission() != null) {
            return this.command.getPermission();
        }

        return super.getPermission();
    }

    public @NotNull PluginCommand getCommand() {
        if (this.command == null) {
            throw new IllegalStateException("Unregistered command '" + this.name + "' tried to get the plugin command");
        }

        return this.command;
    }

    public void addCommand(@NotNull final BasicCommand command) {
        if (this.commands.containsKey(command.getName())) {
            return;
        }

        this.commands.put(command.getName(), command);
    }

    @Override
    public boolean register(@NotNull final Services instance) {
        this.command = instance.getCommand(this.name);

        if (this.command == null) {
            instance.getLogger().warning("Failed to register command " + this.name + "! Is it registered to bukkit/spigot?");
            return false;
        }

        if (super.register(instance)) {
            this.command.setExecutor(this);
            this.command.setTabCompleter(this);
            this.command.setPermissionMessage(tl("command.denied"));
            this.commands.values().forEach(command -> command.register(instance));
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
            this.commands.values().forEach(BasicCommand::unregister);
            this.command.setTabCompleter(null);
            this.command.setExecutor(null);
            this.command = null;
            return true;
        }

        return false;
    }

    @Override
    public boolean execute(@NotNull final CommandSender sender,
                           @NotNull final String[] arguments) {
        if (this.argsInRange(arguments.length)) {
            final BasicCommand delegate = this.commands.get(arguments[0].toLowerCase());

            if (delegate == null) {
                sender.sendMessage(Messages.tlErr("command.unknown", arguments[0]));
                return true;
            }

            if (sender.hasPermission(delegate.getPermission())) {
                if (delegate.execute(sender, arguments.length <= this.minArgs ? new String[0]
                        : Arrays.copyOfRange(arguments, this.minArgs, arguments.length))) {
                    return true;
                }

                // Invalid command usage. Send syntax help:
                sender.sendMessage(delegate.getDescription());
                sender.sendMessage(delegate.getUsage());
                return true;
            }

            sender.sendMessage(tl("command.denied"));
            return true;
        }

        return false;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull final CommandSender sender,
                                             @NotNull final String[] arguments) {
        if (arguments.length == this.minArgs) {
            final List<String> completion = new ArrayList<>();

            for (final BasicCommand delegate : this.commands.values()) {
                if (sender.hasPermission(delegate.getPermission())) {
                    if (StringUtil.startsWithIgnoreCase(delegate.getName(), arguments[0])) {
                        completion.add(delegate.getName());
                    }
                }
            }

            Collections.sort(completion);

            return completion;
        }

        if (arguments.length > this.minArgs) {
            final BasicCommand delegate = this.commands.get(arguments[0].toLowerCase());

            if (delegate != null) {
                if (sender.hasPermission(delegate.getPermission())) {
                    return delegate.tabComplete(sender, Arrays.copyOfRange(arguments, this.minArgs, arguments.length));
                }
            }
        }

        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command,
                             @NotNull final String alias, @NotNull final String[] arguments) {
        if (sender instanceof BlockCommandSender) {
            this.instance.getLogger().severe("Command '" + this.name + "' was executed by an illegal sender.");
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

        sender.sendMessage(tl("command.denied"));
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command command,
                                               @NotNull final String alias, @NotNull final String[] arguments) {
        if (sender instanceof BlockCommandSender) {
            this.instance.getLogger().severe("Command '" + this.name + "' was tab-completed by an illegal sender.");

            return Collections.emptyList();
        }

        if (sender.hasPermission(this.getPermission())) {
            return this.tabComplete(sender, arguments);
        }

        return Collections.emptyList();
    }
}

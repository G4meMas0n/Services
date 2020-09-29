package de.g4memas0n.services.command;

import de.g4memas0n.services.Services;
import de.g4memas0n.services.util.Permission;
import de.g4memas0n.services.util.messaging.Messages;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.g4memas0n.services.util.messaging.Messages.tl;
 
/**
 * The main services command that delegates to all sub-commands.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public final class ServicesCommand extends BasicCommand implements TabExecutor {

    private static final int DELEGATE = 0;
    private static final int ARGUMENTS = 1;

    private final Map<String, BasicCommand> commands = new HashMap<>();

    private PluginCommand command;

    public ServicesCommand() {
        super("services", 1, -1);

        this.addCommand(new ReloadCommand());
        this.addCommand(new VersionCommand());

        this.setPermission(Permission.USE.getNode());
    }

    public final @NotNull PluginCommand getCommand() {
        if (this.command == null) {
            throw new IllegalStateException(String.format("Unregistered command '%s' tried to get the plugin command",
                    this.getName()));
        }

        return this.command;
    }

    public final @Nullable BasicCommand getCommand(@NotNull final String name) {
        return this.commands.get(name.toLowerCase());
    }

    public final void addCommand(@NotNull final BasicCommand command) {
        if (this.commands.containsKey(command.getName())) {
            return;
        }

        this.commands.put(command.getName(), command);
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
            this.command = null;
            this.commands.values().forEach(BasicCommand::unregister);
            return true;
        }

        return false;
    }

    @Override
    public boolean execute(@NotNull final CommandSender sender,
                           @NotNull final String[] arguments) {
        if (this.argsInRange(arguments.length)) {
            final BasicCommand delegate = this.getCommand(arguments[DELEGATE]);

            if (delegate == null) {
                sender.sendMessage(Messages.tlErr("commandNotFound", arguments[DELEGATE]));
                return true;
            }

            if (sender.hasPermission(delegate.getPermission())) {
                if (delegate.execute(sender, arguments.length <= ARGUMENTS ? new String[0]
                        : Arrays.copyOfRange(arguments, ARGUMENTS, arguments.length))) {
                    return true;
                }

                // Invalid command usage. Send syntax help:
                sender.sendMessage(delegate.getDescription());
                sender.sendMessage(delegate.getUsage());
                return true;
            }

            sender.sendMessage(Messages.tl("noPermission"));
            return true;
        }

        return false;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull final CommandSender sender,
                                             @NotNull final String[] arguments) {
        if (arguments.length == DELEGATE + 1) {
            final List<String> completion = new ArrayList<>();

            for (final BasicCommand delegate : this.commands.values()) {
                if (!sender.hasPermission(delegate.getPermission())) {
                    continue;
                }

                if (StringUtil.startsWithIgnoreCase(delegate.getName(), arguments[DELEGATE])) {
                    completion.add(delegate.getName());
                }
            }

            Collections.sort(completion);

            return completion;
        }

        if (arguments.length > ARGUMENTS) {
            final BasicCommand delegate = this.getCommand(arguments[DELEGATE]);

            if (delegate == null) {
                return Collections.emptyList();
            }

            if (sender.hasPermission(delegate.getPermission())) {
                return delegate.tabComplete(sender, Arrays.copyOfRange(arguments, ARGUMENTS, arguments.length));
            }
        }

        return Collections.emptyList();
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

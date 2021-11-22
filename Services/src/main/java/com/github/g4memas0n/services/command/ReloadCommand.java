package com.github.g4memas0n.services.command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import java.util.Collections;
import java.util.List;

import static com.github.g4memas0n.services.util.Messages.tl;

/**
 * The reload command that allows to reload this plugin.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public final class ReloadCommand extends BasicCommand {

    public ReloadCommand() {
        super("reload", 0, 0);

        this.setPermission("services.reload");
    }

    @Override
    public boolean execute(@NotNull final CommandSender sender,
                           @NotNull final String[] arguments) {
        if (this.argsInRange(arguments.length)) {
            this.instance.reloadConfig();

            sender.sendMessage(tl("command.reload.plugin", this.instance.getName()));
            return true;
        }

        return false;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull final CommandSender sender,
                                             @NotNull final String[] arguments) {
        return Collections.emptyList();
    }
}

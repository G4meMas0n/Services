package de.g4memas0n.services.command;

import de.g4memas0n.services.util.Permission;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import java.util.Collections;
import java.util.List;

import static de.g4memas0n.services.util.Messages.tl;

/**
 * The reload command that allows to reload this plugin.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public final class ReloadCommand extends BasicCommand {

    public ReloadCommand() {
        super("reload", 0, 0);

        this.setPermission(Permission.RELOAD.getNode());
    }

    @Override
    public boolean execute(@NotNull final CommandSender sender,
                           @NotNull final String[] arguments) {
        if (this.argsInRange(arguments.length)) {
            this.getInstance().reloadConfig();

            sender.sendMessage(tl("reloadPlugin", this.getInstance().getName()));
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

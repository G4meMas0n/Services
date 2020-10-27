package de.g4memas0n.services.command;

import de.g4memas0n.services.util.Permission;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import java.util.Collections;
import java.util.List;

import static de.g4memas0n.services.util.Messages.tl;

/**
 * The version command that allows to show the version of this plugin.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public final class VersionCommand extends BasicCommand {

    public VersionCommand() {
        super("version", 0, 0);

        this.setPermission(Permission.VERSION.getNode());
    }

    @Override
    public boolean execute(@NotNull final CommandSender sender,
                           @NotNull final String[] arguments) {
        if (this.argsInRange(arguments.length)) {
            // Show plugin name and version.
            sender.sendMessage(tl("versionInfo", this.getInstance().getName(),
                    this.getInstance().getDescription().getVersion()));

            // Show server name, version and build.
            sender.sendMessage(tl("versionServer", this.getInstance().getServer().getName(),
                    this.getInstance().getServer().getBukkitVersion(),
                    this.getInstance().getServer().getVersion()));

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

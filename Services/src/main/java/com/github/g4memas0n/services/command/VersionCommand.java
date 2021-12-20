package com.github.g4memas0n.services.command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import java.util.Collections;
import java.util.List;

import static com.github.g4memas0n.services.util.Messages.tl;

/**
 * The version command that allows to show the version of this plugin.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public final class VersionCommand extends BasicCommand {

    public VersionCommand() {
        super("version", 0, 0);

        this.setPermission("services.manage.version");
    }

    @Override
    public boolean execute(@NotNull final CommandSender sender,
                           @NotNull final String[] arguments) {
        if (this.argsInRange(arguments.length)) {
            // Show plugin name and version.
            sender.sendMessage(tl("command.version.info", this.instance.getName(),
                    this.instance.getDescription().getVersion()));

            // Show server name, version and build.
            sender.sendMessage(tl("command.version.server", this.instance.getServer().getName(),
                    this.instance.getServer().getBukkitVersion(),
                    this.instance.getServer().getVersion()));
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

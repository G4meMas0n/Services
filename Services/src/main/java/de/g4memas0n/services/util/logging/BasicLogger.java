package de.g4memas0n.services.util.logging;

import org.jetbrains.annotations.NotNull;
import java.util.function.Supplier;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * The BasicLogger class that is a modified {@link java.util.logging.Logger} that offers a additional debug feature.
 * Also it provides the ability to prepend all logging calls with a specified prefix.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public class BasicLogger extends Logger {

    private final String prefix;

    private boolean debug;

    @SuppressWarnings("unused")
    public BasicLogger(@NotNull final Logger parent, @NotNull final String name) {
        super(parent.getName() + "_" + name, null);

        this.prefix = "";
        this.debug = false;

        this.setParent(parent);
    }

    public BasicLogger(@NotNull final Logger parent, @NotNull final String name, @NotNull final String prefix) {
        super(parent.getName() + "_" + name, null);

        this.prefix = "[" + prefix + "] ";
        this.debug = false;

        this.setParent(parent);
    }

    @SuppressWarnings("unused")
    public boolean isDebug() {
        return this.debug;
    }

    public void setDebug(final boolean debug) {
        if (this.debug == debug) {
            return;
        }

        this.debug = debug;
    }

    @Override
    public void log(@NotNull final LogRecord record) {
        record.setMessage(this.prefix + record.getMessage());

        super.log(record);
    }

    public void debug(@NotNull final String msg) {
        if (this.debug) {
            this.info(msg);
        }
    }

    @SuppressWarnings("unused")
    public void debug(@NotNull final Supplier<String> supplier) {
        if (this.debug) {
            this.info(supplier);
        }
    }
}

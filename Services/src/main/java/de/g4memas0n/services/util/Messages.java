package de.g4memas0n.services.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Message class, that is used to receive all localized messages of this plugin.
 *
 * @author G4meMas0n
 * @since Release 1.0.0
 */
public final class Messages {

    private static final String BUNDLE_BASE = "resources/messages";

    private static Messages instance;

    private final Logger logger;
    private final File directory;

    private final ResourceBundle defaultBundle;
    private ResourceBundle localBundle;
    private ResourceBundle customBundle;

    public Messages(@NotNull final File directory, @NotNull final Logger logger) {
        this.directory = directory;
        this.logger = logger;

        this.defaultBundle = ResourceBundle.getBundle(BUNDLE_BASE);
        this.localBundle = this.defaultBundle;
        this.customBundle = null;
    }

    public void enable() {
        instance = this;
    }

    public void disable() {
        instance = null;
    }

    public @NotNull Locale getLocale() {
        return this.customBundle != null ? this.customBundle.getLocale() : this.localBundle.getLocale();
    }

    public void setLocale(@NotNull final Locale locale) {
        ResourceBundle.clearCache();

        try {
            this.localBundle = ResourceBundle.getBundle(BUNDLE_BASE, locale, new CustomPropertiesControl());

            if (this.localBundle.getLocale().equals(locale)) {
                this.logger.info("Loaded resource bundle for language: " + locale);
            } else {
                this.logger.warning("Unable to find resource bundle for language: " + locale);
                this.logger.info("Loaded fallback resource bundle for language: " + this.localBundle.getLocale());
            }
        } catch (MissingResourceException ex) {
            this.localBundle = this.defaultBundle;
            this.logger.warning("Unable to find resource bundle. Using default bundle.");
        }

        try {
            this.customBundle = ResourceBundle.getBundle(BUNDLE_BASE, locale,
                    new CustomFileClassLoader(this.getClass().getClassLoader(), this.directory),
                    new CustomNoFallbackControl());

            if (this.customBundle.getLocale().equals(locale)) {
                this.logger.info("Detected and loaded custom resource bundle for language: " + locale);
            } else {
                this.customBundle = null;
            }
        } catch (MissingResourceException ex) {
            this.customBundle = null;
        }

        this.logger.info(String.format("Locale has been changed. Using locale %s", this.getLocale()));
    }

    public @NotNull String translate(@NotNull final String key) {
        try {
            if (this.customBundle != null) {
                try {
                    return this.customBundle.getString(key);
                } catch (MissingResourceException ex) {
                    this.logger.warning(String.format("Missing translation key '%s' in custom translation file for language: %s",
                            ex.getKey(), this.customBundle.getBaseBundleName()));
                }
            }

            return this.localBundle.getString(key);
        } catch (MissingResourceException ex) {
            this.logger.warning(String.format("Missing translation key '%s' in translation file for language: %s",
                    ex.getKey(), this.getLocale()));

            return this.defaultBundle.getString(key);
        }
    }

    public @NotNull String format(@NotNull final String key,
                                  @NotNull final Object... arguments) {
        if (arguments.length == 0) {
            return this.translate(key);
        }

        final String format = this.translate(key);

        try {
            return MessageFormat.format(format, arguments);
        } catch (IllegalArgumentException ex) {
            this.logger.warning("Invalid translation key '%s': " + ex.getMessage());

            return MessageFormat.format(format.replaceAll("\\{(\\D*?)}", "\\[$1\\]"), arguments);
        }
    }

    public static @NotNull String tl(@NotNull final String key,
                                     @NotNull final Object... arguments) {
        if (instance == null) {
            return "\u00a74Error: \u00a7cMessages not loaded.";
        }

        return instance.format(key, arguments);
    }

    public static @NotNull String tlErr(@NotNull final String key,
                                        @NotNull final Object... arguments) {
        if (instance == null) {
            return "\u00a74Error: \u00a7cMessages not loaded.";
        }

        return instance.translate("prefixError") + " " + instance.format(key, arguments);
    }

    /**
     * Custom ClassLoader for getting resource bundles located in the plugins data folder.
     */
    private static class CustomFileClassLoader extends ClassLoader {

        private final File directory;

        private CustomFileClassLoader(@NotNull final ClassLoader loader, @NotNull final File directory) {
            super(loader);

            this.directory = directory;
        }

        @Override
        public @Nullable URL getResource(@NotNull final String name) {
            final File file = new File(this.directory, name);

            if (file.exists()) {
                try {
                    return file.toURI().toURL();
                } catch (MalformedURLException ignored) {

                }
            }

            return null;
        }

        @Override
        public @Nullable InputStream getResourceAsStream(@NotNull final String name) {
            final File file = new File(this.directory, name);

            if (file.exists()) {
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException ignored) {

                }
            }

            return null;
        }
    }

    /**
     * Custom PropertiesControl for reading properties files as UTF-8 instead of ISO-8859-1.
     */
    private static class CustomPropertiesControl extends ResourceBundle.Control {

        @Override
        public @NotNull List<String> getFormats(@NotNull final String baseName) {
            return ResourceBundle.Control.FORMAT_PROPERTIES;
        }

        @Override
        public @Nullable ResourceBundle newBundle(@NotNull final String baseName,
                                                  @NotNull final Locale locale,
                                                  @NotNull final String format,
                                                  @NotNull final ClassLoader loader,
                                                  final boolean reload) throws IOException {
            final String resource = this.toResourceName(this.toBundleName(baseName, locale), "properties");

            ResourceBundle bundle = null;
            InputStream stream = null;

            if (reload) {
                final URL url = loader.getResource(resource);

                if (url != null) {
                    final URLConnection connection = url.openConnection();

                    if (connection != null) {
                        connection.setUseCaches(false);

                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resource);
            }

            if (stream != null) {
                try {
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                } finally {
                    stream.close();
                }
            }

            return bundle;
        }
    }

    /**
     * Custom PropertiesControl for using no fallback locales.
     */
    private static class CustomNoFallbackControl extends CustomPropertiesControl {

        @Override
        public @Nullable Locale getFallbackLocale(@NotNull final String baseName,
                                                  @NotNull final Locale locale) {
            return null;
        }
    }
}

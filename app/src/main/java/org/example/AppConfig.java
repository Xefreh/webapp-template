package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class AppConfig {

    private final Properties props;

    private AppConfig(Properties props) {
        this.props = props;
    }

    public static AppConfig load() {
        Properties props = new Properties();
        try (InputStream in = AppConfig.class.getResourceAsStream("/config.properties")) {
            if (in == null) {
                throw new IllegalStateException("config.properties not found on classpath");
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                props.load(reader);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read config.properties", e);
        }
        return new AppConfig(props);
    }

    /**
     * Returns the value for {@code key}. An environment variable named after the key
     * (uppercase, dots replaced by underscores, e.g. database.url -> DATABASE_URL)
     * takes precedence over config.properties.
     */
    public String get(String key) {
        String value = System.getenv(key.toUpperCase().replace('.', '_'));
        if (value == null) {
            value = props.getProperty(key);
        }
        if (value == null) {
            throw new IllegalStateException("Missing config key: " + key);
        }
        return value;
    }

    public long getLong(String key) {
        String value = get(key);
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Config key '" + key + "' must be a number, got: '" + value + "'", e);
        }
    }
}

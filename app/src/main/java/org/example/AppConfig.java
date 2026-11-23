package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Function;

/**
 * Application configuration, read from a single properties file: the
 * config.properties bundled on the classpath, or a file given on the command
 * line that replaces it entirely.
 */
public class AppConfig {

	private final Properties props;

	AppConfig(Properties props) {
		this.props = props;
	}

	/** Loads the config.properties bundled on the classpath. */
	public static AppConfig load() {
		try (InputStream in = AppConfig.class.getResourceAsStream("/config.properties")) {
			if (in == null) {
				throw new IllegalStateException("config.properties not found on classpath");
			}
			return new AppConfig(read(in));
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read config.properties", e);
		}
	}

	/**
	 * Loads {@code file} instead of the bundled config.properties: it must define
	 * every key, nothing is taken from the bundled file.
	 */
	public static AppConfig load(Path file) {
		try (InputStream in = Files.newInputStream(file)) {
			return new AppConfig(read(in));
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read configuration file " + file, e);
		}
	}

	private static Properties read(InputStream in) throws IOException {
		Properties props = new Properties();
		try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			props.load(reader);
		}
		return props;
	}

	public String get(String key) {
		String value = props.getProperty(key);
		if (value == null) {
			throw new IllegalStateException("Missing config key: " + key);
		}
		return value;
	}

	public int getInt(String key) {
		return getNumber(key, Integer::parseInt);
	}

	public long getLong(String key) {
		return getNumber(key, Long::parseLong);
	}

	private <T extends Number> T getNumber(String key, Function<String, T> parser) {
		String value = get(key);
		try {
			return parser.apply(value.trim());
		} catch (NumberFormatException e) {
			throw new IllegalStateException("Config key '" + key + "' must be a number, got: '" + value + "'", e);
		}
	}
}

package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class AppConfig {

	private final Properties props;
	private final UnaryOperator<String> env;

	/**
	 * @param props default values
	 * @param env   environment lookup (variable name to value, or null when unset),
	 *              {@code System::getenv} outside tests
	 */
	AppConfig(Properties props, UnaryOperator<String> env) {
		this.props = props;
		this.env = env;
	}

	public static AppConfig load() {
		return load(System::getenv);
	}

	static AppConfig load(UnaryOperator<String> env) {
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
		return new AppConfig(props, env);
	}

	/**
	 * Returns the value for {@code key}. An environment variable named after the
	 * key (uppercase, dots replaced by underscores, e.g. database.url ->
	 * DATABASE_URL) takes precedence over config.properties.
	 */
	public String get(String key) {
		String value = env.apply(envName(key));
		if (value == null) {
			value = props.getProperty(key);
		}
		if (value == null) {
			throw new IllegalStateException("Missing config key: " + key);
		}
		return value;
	}

	/**
	 * Name of the environment variable overriding {@code key}. Locale.ROOT keeps
	 * the mapping independent of the default locale (in Turkish, "i" would
	 * otherwise become a dotted capital I).
	 */
	static String envName(String key) {
		return key.toUpperCase(Locale.ROOT).replace('.', '_');
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

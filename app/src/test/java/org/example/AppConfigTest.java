package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class AppConfigTest {

	private static AppConfig config(Map<String, String> props, Map<String, String> env) {
		Properties properties = new Properties();
		properties.putAll(props);
		return new AppConfig(properties, env::get);
	}

	@Test
	void readsValueFromProperties() {
		AppConfig config = config(Map.of("database.url", "jdbc:postgresql://db/app"), Map.of());

		assertEquals("jdbc:postgresql://db/app", config.get("database.url"));
	}

	@Test
	void environmentVariableOverridesProperties() {
		AppConfig config = config(Map.of("database.maximumPoolSize", "10"), Map.of("DATABASE_MAXIMUMPOOLSIZE", "42"));

		assertEquals(42, config.getInt("database.maximumPoolSize"));
	}

	@Test
	void environmentVariableIsEnoughWithoutProperty() {
		AppConfig config = config(Map.of(), Map.of("SERVER_PORT", "9090"));

		assertEquals("9090", config.get("server.port"));
	}

	@Test
	void missingKeyFailsWithItsName() {
		AppConfig config = config(Map.of(), Map.of());

		IllegalStateException e = assertThrows(IllegalStateException.class, () -> config.get("database.url"));
		assertTrue(e.getMessage().contains("database.url"), e.getMessage());
	}

	@Test
	void numbersAreTrimmed() {
		AppConfig config = config(Map.of("a.int", " 10 ", "a.long", "5000000000 "), Map.of());

		assertEquals(10, config.getInt("a.int"));
		assertEquals(5_000_000_000L, config.getLong("a.long"));
	}

	@Test
	void invalidNumberFailsWithKeyAndValue() {
		AppConfig config = config(Map.of("database.maximumPoolSize", "ten"), Map.of());

		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> config.getInt("database.maximumPoolSize"));
		assertTrue(e.getMessage().contains("database.maximumPoolSize"), e.getMessage());
		assertTrue(e.getMessage().contains("ten"), e.getMessage());
	}

	@Test
	void environmentVariableNameDoesNotDependOnDefaultLocale() {
		Locale previous = Locale.getDefault();
		try {
			// In Turkish, "i".toUpperCase() is a dotted capital I
			Locale.setDefault(Locale.forLanguageTag("tr-TR"));

			assertEquals("DATABASE_MAXIMUMPOOLSIZE", AppConfig.envName("database.maximumPoolSize"));
		} finally {
			Locale.setDefault(previous);
		}
	}

	@Test
	void loadsBundledConfigProperties() {
		AppConfig config = AppConfig.load(name -> null);

		assertEquals("public", config.get("database.schema"));
		assertEquals(10, config.getInt("database.maximumPoolSize"));
	}
}

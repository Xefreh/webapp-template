package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppConfigTest {

	private static AppConfig config(Map<String, String> values) {
		Properties props = new Properties();
		props.putAll(values);
		return new AppConfig(props);
	}

	@Test
	void readsValue() {
		AppConfig config = config(Map.of("database.url", "jdbc:postgresql://db/app"));

		assertEquals("jdbc:postgresql://db/app", config.get("database.url"));
	}

	@Test
	void missingKeyFailsWithItsName() {
		AppConfig config = config(Map.of());

		IllegalStateException e = assertThrows(IllegalStateException.class, () -> config.get("database.url"));
		assertTrue(e.getMessage().contains("database.url"), e.getMessage());
	}

	@Test
	void numbersAreTrimmed() {
		AppConfig config = config(Map.of("a.int", " 10 ", "a.long", "5000000000 "));

		assertEquals(10, config.getInt("a.int"));
		assertEquals(5_000_000_000L, config.getLong("a.long"));
	}

	@Test
	void invalidNumberFailsWithKeyAndValue() {
		AppConfig config = config(Map.of("database.maximumPoolSize", "ten"));

		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> config.getInt("database.maximumPoolSize"));
		assertTrue(e.getMessage().contains("database.maximumPoolSize"), e.getMessage());
		assertTrue(e.getMessage().contains("ten"), e.getMessage());
	}

	@Test
	void loadsBundledConfigProperties() {
		AppConfig config = AppConfig.load();

		assertEquals("public", config.get("database.schema"));
		assertEquals(8080, config.getInt("server.port"));
		assertEquals(10, config.getInt("database.maximumPoolSize"));
	}

	@Test
	void loadsFileAsUtf8(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("config.properties");
		Files.writeString(file, "database.applicationName=célérité\nserver.port=9090\n", StandardCharsets.UTF_8);

		AppConfig config = AppConfig.load(file);

		assertEquals("célérité", config.get("database.applicationName"));
		assertEquals(9090, config.getInt("server.port"));
	}

	@Test
	void fileReplacesBundledConfigEntirely(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("config.properties");
		Files.writeString(file, "server.port=9090\n");

		AppConfig config = AppConfig.load(file);

		// Defined in the bundled config.properties only
		assertThrows(IllegalStateException.class, () -> config.get("database.url"));
	}

	@Test
	void missingFileFailsWithItsPath(@TempDir Path dir) {
		Path file = dir.resolve("missing.properties");

		UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> AppConfig.load(file));
		assertTrue(e.getMessage().contains(file.toString()), e.getMessage());
	}

	@Test
	void dockerConfigDefinesEveryBundledKey() {
		// docker/config.properties replaces the bundled file: it must not miss a key
		AppConfig docker = AppConfig.load(Path.of("../docker/config.properties"));

		for (String key : TestConfig.bundled().stringPropertyNames()) {
			docker.get(key);
		}
		assertEquals("jdbc:postgresql://postgres:5432/webapptemplate", docker.get("database.url"));
	}
}

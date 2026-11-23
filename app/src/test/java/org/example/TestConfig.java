package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.testcontainers.containers.JdbcDatabaseContainer;

/** Configuration for tests: the bundled config.properties with some keys replaced. */
final class TestConfig {

	private TestConfig() {
	}

	/** The bundled config.properties, connected to {@code database}. */
	static Properties forDatabase(JdbcDatabaseContainer<?> database) {
		Properties props = bundled();
		props.setProperty("database.url", database.getJdbcUrl());
		props.setProperty("database.username", database.getUsername());
		props.setProperty("database.password", database.getPassword());
		return props;
	}

	static Properties bundled() {
		Properties props = new Properties();
		try (InputStream in = TestConfig.class.getResourceAsStream("/config.properties");
				Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			props.load(reader);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return props;
	}

	/** Writes {@code props} to a new temporary file, as AppConfig.load(Path) reads it. */
	static Path write(Properties props) throws IOException {
		Path file = Files.createTempFile("config", ".properties");
		file.toFile().deleteOnExit();
		try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			props.store(writer, null);
		}
		return file;
	}
}

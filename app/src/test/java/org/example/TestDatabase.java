package org.example;

import java.util.Map;

import org.jdbi.v3.core.Jdbi;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL container shared by every test class of the JVM, started on first
 * use and migrated with the application's Flyway migrations. Testcontainers
 * removes it when the JVM exits.
 */
final class TestDatabase {

	// Same image as compose.yaml
	private static final String IMAGE = "postgres:18-alpine";

	private static TestDatabase instance;

	private final PostgreSQLContainer container;
	private final Database database;
	private final Jdbi jdbi;

	private TestDatabase() {
		container = new PostgreSQLContainer(IMAGE);
		container.start();

		database = new Database(AppConfig.load(environment()::get));
		database.migrate();
		jdbi = Jdbi.create(database.getDataSource());
	}

	static synchronized TestDatabase get() {
		if (instance == null) {
			instance = new TestDatabase();
		}
		return instance;
	}

	/**
	 * Environment variables pointing the application configuration at the
	 * container.
	 */
	Map<String, String> environment() {
		return Map.of("DATABASE_URL", container.getJdbcUrl(), "DATABASE_USERNAME", container.getUsername(),
				"DATABASE_PASSWORD", container.getPassword());
	}

	Database database() {
		return database;
	}

	Jdbi jdbi() {
		return jdbi;
	}

	/** Deletes every row so each test starts from an empty database. */
	void reset() {
		jdbi.useHandle(handle -> handle.execute("TRUNCATE users RESTART IDENTITY"));
	}
}

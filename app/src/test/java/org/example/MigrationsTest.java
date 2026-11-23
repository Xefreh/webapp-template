package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;

@DatabaseTest
class MigrationsTest {

	private final TestDatabase db = TestDatabase.get();

	private Flyway flyway() {
		return Flyway.configure().dataSource(db.database().getDataSource()).locations("classpath:migrations").load();
	}

	@Test
	void everyMigrationIsAppliedSuccessfully() {
		MigrationInfo[] migrations = flyway().info().all();

		assertEquals(0, flyway().info().pending().length);
		for (MigrationInfo migration : migrations) {
			assertEquals(MigrationState.SUCCESS, migration.getState(), migration.getScript());
		}
	}

	@Test
	void migratingAgainIsANoOp() {
		db.database().migrate();

		assertEquals(0, flyway().migrate().migrationsExecuted);
	}

	@Test
	void usersTableHasExpectedColumns() {
		List<String> columns = db.jdbi()
				.withHandle(handle -> handle
						.createQuery("SELECT column_name FROM information_schema.columns"
								+ " WHERE table_schema = 'public' AND table_name = 'users' ORDER BY ordinal_position")
						.mapTo(String.class).list());

		assertEquals(List.of("id", "name", "email", "password_hash"), columns);
	}
}

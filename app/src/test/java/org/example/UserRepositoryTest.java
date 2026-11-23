package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.List;

import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@DatabaseTest
class UserRepositoryTest {

	private final TestDatabase db = TestDatabase.get();
	private final UserRepository repository = new UserRepository(db.jdbi());

	@BeforeEach
	void resetDatabase() {
		db.reset();
	}

	@Test
	void listIsEmptyOnEmptyDatabase() {
		assertTrue(repository.list().isEmpty());
	}

	@Test
	void createReturnsUserWithGeneratedId() {
		User created = repository.create(new User("Ada", "ada@example.org", "hash"));

		assertNotNull(created.id());
		assertEquals("Ada", created.name());
		assertEquals("ada@example.org", created.email());
		assertEquals("hash", created.passwordHash());
	}

	@Test
	void listReturnsCreatedUsersWithoutPasswordHash() {
		User ada = repository.create(new User("Ada", "ada@example.org", "hash1"));
		User alan = repository.create(new User("Alan", "alan@example.org", "hash2"));

		List<User> users = repository.list();

		assertEquals(List.of(new User(ada.id(), "Ada", "ada@example.org", null),
				new User(alan.id(), "Alan", "alan@example.org", null)), users);
		users.forEach(user -> assertNull(user.passwordHash()));
	}

	@Test
	void duplicateEmailIsAUniqueViolation() {
		repository.create(new User("Ada", "ada@example.org", "hash"));

		UnableToExecuteStatementException e = assertThrows(UnableToExecuteStatementException.class,
				() -> repository.create(new User("Other Ada", "ada@example.org", "hash")));

		// UserServlet relies on this SQLSTATE to answer 409
		SQLException cause = assertInstanceOf(SQLException.class, e.getCause());
		assertEquals("23505", cause.getSQLState());
	}
}

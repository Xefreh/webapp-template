package org.example;

import org.jdbi.v3.core.Jdbi;

import java.util.List;

public class UserRepository {

	private static final String TABLE_NAME = "users";

	private final Jdbi jdbi;

	public UserRepository(Jdbi jdbi) {
		this.jdbi = jdbi;
	}

	public List<User> list() {
		// The password hash is deliberately not selected: listing users never needs it
		return jdbi.withHandle(handle -> handle
				.createQuery("SELECT id, name, email FROM " + TABLE_NAME + " ORDER BY id")
				.map((rs, ctx) -> new User(rs.getInt("id"), rs.getString("name"), rs.getString("email"), null)).list());
	}

	public User create(User user) {
		return jdbi.withHandle(handle -> {
			Integer id = handle
					.createUpdate("INSERT INTO " + TABLE_NAME
							+ " (name, email, password_hash) VALUES (:name, :email, :passwordHash)")
					.bind("name", user.name()).bind("email", user.email()).bind("passwordHash", user.passwordHash())
					.executeAndReturnGeneratedKeys("id").mapTo(Integer.class).one();
			return new User(id, user.name(), user.email(), user.passwordHash());
		});
	}
}

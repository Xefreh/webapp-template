package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class UserTest {

	private final ObjectMapper json = new ObjectMapper();

	@Test
	void jsonNeverExposesPasswordHash() throws Exception {
		String body = json.writeValueAsString(new User(1, "Ada", "ada@example.org", "$argon2id$v=19$secret-hash"));

		assertFalse(body.contains("passwordHash"), body);
		assertFalse(body.contains("secret-hash"), body);
		assertEquals("{\"id\":1,\"name\":\"Ada\",\"email\":\"ada@example.org\"}", body);
	}

	@Test
	void constructorWithoutIdLeavesItNull() {
		User user = new User("Ada", "ada@example.org", "hash");

		assertNull(user.id());
		assertEquals("hash", user.passwordHash());
	}
}

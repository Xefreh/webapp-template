package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** GET and POST /users through HTTP, against PostgreSQL. */
@DatabaseTest
class UserServletTest {

	private static final ObjectMapper JSON = new ObjectMapper();

	private static TestDatabase db;
	private static TestServer server;

	@BeforeAll
	static void startServer() throws Exception {
		db = TestDatabase.get();
		server = TestServer.startApp(new UserRepository(db.jdbi()));
	}

	@AfterAll
	static void stopServer() throws Exception {
		server.close();
	}

	@BeforeEach
	void resetDatabase() {
		db.reset();
	}

	private static HttpResponse<String> createUser(String name, String email) throws Exception {
		return server.postForm("/users", Map.of("name", name, "email", email, "password", "secret"));
	}

	@Test
	void listIsEmptyJsonArray() throws Exception {
		HttpResponse<String> response = server.get("/users");

		assertEquals(200, response.statusCode());
		assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("application/json"));
		assertEquals("[]", response.body());
	}

	@Test
	void createReturnsUserWithoutPasswordHash() throws Exception {
		HttpResponse<String> response = createUser("Ada", "ada@example.org");

		assertEquals(201, response.statusCode());
		JsonNode user = JSON.readTree(response.body());
		assertTrue(user.get("id").isInt(), response.body());
		assertEquals("Ada", user.get("name").asText());
		assertEquals("ada@example.org", user.get("email").asText());
		assertFalse(user.has("passwordHash"), response.body());
	}

	@Test
	void createdUserIsListed() throws Exception {
		createUser("Ada", "ada@example.org");
		createUser("Alan", "alan@example.org");

		JsonNode users = JSON.readTree(server.get("/users").body());

		assertEquals(2, users.size());
		assertEquals("ada@example.org", users.get(0).get("email").asText());
		assertEquals("alan@example.org", users.get(1).get("email").asText());
		users.forEach(user -> assertFalse(user.has("passwordHash"), user.toString()));
	}

	@Test
	void storesArgon2HashOfPassword() throws Exception {
		createUser("Ada", "ada@example.org");

		String hash = db.jdbi().withHandle(handle -> handle
				.createQuery("SELECT password_hash FROM users WHERE email = 'ada@example.org'").mapTo(String.class)
				.one());

		assertTrue(new PasswordHasher().matches("secret", hash));
	}

	@Test
	void duplicateEmailIsAConflict() throws Exception {
		createUser("Ada", "ada@example.org");

		HttpResponse<String> response = createUser("Other Ada", "ada@example.org");

		assertEquals(409, response.statusCode());
		assertEquals(Map.of("error", "email already in use"), JSON.readValue(response.body(), Map.class));
	}

	@Test
	void acceptsNameOfMaximumLength() throws Exception {
		// 255 code points but 510 UTF-16 chars: fits in varchar(255)
		String name = "😀".repeat(255);

		HttpResponse<String> response = createUser(name, "ada@example.org");

		assertEquals(201, response.statusCode(), response.body());
		assertEquals(name, JSON.readTree(server.get("/users").body()).get(0).get("name").asText());
	}

	@Test
	void keepsNonAsciiCharacters() throws Exception {
		createUser("Zoë Ñandú 日本", "zoe@example.org");

		assertEquals("Zoë Ñandú 日本", JSON.readTree(server.get("/users").body()).get(0).get("name").asText());
	}
}

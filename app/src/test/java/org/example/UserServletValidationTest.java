package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Invalid POST /users requests, rejected before reaching the database. */
class UserServletValidationTest {

	private static final ObjectMapper JSON = new ObjectMapper();

	private static TestServer server;

	@BeforeAll
	static void startServer() throws Exception {
		// Jdbi connects lazily: invalid requests never reach this unused database
		server = TestServer.startApp(new UserRepository(Jdbi.create("jdbc:postgresql://localhost:1/unused")));
	}

	@AfterAll
	static void stopServer() throws Exception {
		server.close();
	}

	private static Map<String, String> validForm() {
		return Map.of("name", "Ada", "email", "ada@example.org", "password", "secret");
	}

	/** validForm() with {@code key} set to {@code value}, or removed if null. */
	private static Map<String, String> with(String key, String value) {
		Map<String, String> form = new HashMap<>(validForm());
		if (value == null) {
			form.remove(key);
		} else {
			form.put(key, value);
		}
		return form;
	}

	static Stream<Arguments> invalidForms() {
		String required = "name and email are required";
		String tooLong = "name and email must be at most 255 characters";
		String invalidEmail = "email is not a valid address";
		return Stream.of(Arguments.of("missing name", with("name", null), required),
				Arguments.of("blank name", with("name", "   "), required),
				Arguments.of("missing email", with("email", null), required),
				Arguments.of("empty email", with("email", ""), required),
				Arguments.of("missing password", with("password", null), "password is required"),
				Arguments.of("blank password", with("password", " "), "password is required"),
				Arguments.of("name too long", with("name", "a".repeat(256)), tooLong),
				Arguments.of("email too long", with("email", "a".repeat(244) + "@example.org"), tooLong),
				// 256 code points: each emoji is two UTF-16 chars but one varchar character
				Arguments.of("name too long in code points", with("name", "😀".repeat(256)), tooLong),
				Arguments.of("blank email", with("email", "   "), required),
				Arguments.of("email without @", with("email", "ada.example.org"), invalidEmail),
				Arguments.of("email without domain", with("email", "ada@"), invalidEmail),
				Arguments.of("email with a single domain label", with("email", "ada@localhost"), invalidEmail),
				Arguments.of("email with spaces", with("email", "ada lovelace@example.org"), invalidEmail));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidForms")
	void rejectsInvalidForm(String description, Map<String, String> form, String error) throws Exception {
		HttpResponse<String> response = server.postForm("/users", form);

		assertEquals(400, response.statusCode());
		assertEquals(Map.of("error", error), JSON.readValue(response.body(), Map.class));
	}
}

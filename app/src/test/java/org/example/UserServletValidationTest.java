package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Invalid POST /users requests, rejected before reaching the database. The
 * validation rules themselves are tested by CreateUserRequestTest.
 */
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

	private static List<String> blank(String field) {
		return List.of("\"" + field + "\" must not be blank");
	}

	static Stream<Arguments> invalidForms() {
		List<String> nameTooLong = List
				.of("The size of \"name\" must be less than or equal to 255. The given size is 256");
		List<String> invalidEmail = List.of("\"email\" must be a valid email address");
		return Stream.of(Arguments.of("missing name", with("name", null), Map.of("name", blank("name"))),
				Arguments.of("blank name", with("name", "   "), Map.of("name", blank("name"))),
				Arguments.of("missing email", with("email", null), Map.of("email", blank("email"))),
				Arguments.of("blank email", with("email", "  "), Map.of("email", blank("email"))),
				Arguments.of("missing password", with("password", null), Map.of("password", blank("password"))),
				Arguments.of("blank password", with("password", " "), Map.of("password", blank("password"))),
				Arguments.of("name too long", with("name", "a".repeat(256)), Map.of("name", nameTooLong)),
				Arguments.of("invalid email", with("email", "ada lovelace@example.org"), Map.of("email", invalidEmail)),
				Arguments.of("everything missing", Map.of(),
						Map.of("name", blank("name"), "email", blank("email"), "password", blank("password"))));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidForms")
	void rejectsInvalidForm(String description, Map<String, String> form, Map<String, List<String>> errors)
			throws Exception {
		HttpResponse<String> response = server.postForm("/users", form);

		assertEquals(400, response.statusCode());
		assertEquals(Map.of("errors", errors), JSON.readValue(response.body(), Map.class));
	}
}

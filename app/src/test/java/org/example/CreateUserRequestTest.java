package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CreateUserRequestTest {

	private static Map<String, List<String>> violations(String name, String email, String password) {
		return CreateUserRequest.VALIDATOR.validate(new CreateUserRequest(name, email, password)).messagesByName();
	}

	private static Map<String, List<String>> emailViolations(String email) {
		return violations("Ada", email, "secret");
	}

	@Test
	void validRequestHasNoViolation() {
		assertEquals(Map.of(), violations("Ada", "ada@example.org", "secret"));
	}

	@Test
	void emailIsTrimmedAndLowercased() {
		assertEquals("ada.lovelace@example.org",
				new CreateUserRequest("Ada", "  Ada.Lovelace@Example.ORG \t", "secret").email());
	}

	@Test
	void missingEmailStaysNull() {
		assertNull(new CreateUserRequest("Ada", null, "secret").email());
	}

	@Test
	void emailLowercasingDoesNotDependOnDefaultLocale() {
		Locale previous = Locale.getDefault();
		try {
			// In Turkish, "I".toLowerCase() is a dotless i
			Locale.setDefault(Locale.forLanguageTag("tr-TR"));

			assertEquals("info@example.org", new CreateUserRequest("Ada", "INFO@EXAMPLE.ORG", "secret").email());
		} finally {
			Locale.setDefault(previous);
		}
	}

	@Test
	void everyMissingFieldIsReported() {
		assertEquals(Map.of("name", List.of("\"name\" must not be blank"), "email",
				List.of("\"email\" must not be blank"), "password", List.of("\"password\" must not be blank")),
				violations(null, null, null));
	}

	@Test
	void blankEmailIsOnlyReportedAsBlank() {
		assertEquals(Map.of("email", List.of("\"email\" must not be blank")), emailViolations("   "));
	}

	@Test
	void nameAndEmailAreLimitedTo255CodePoints() {
		assertEquals(Map.of(), violations("a".repeat(255), "a".repeat(243) + "@example.org", "secret"));

		Map<String, List<String>> violations = violations("a".repeat(256), "a".repeat(244) + "@example.org", "secret");

		assertEquals(List.of("The size of \"name\" must be less than or equal to 255. The given size is 256"),
				violations.get("name"));
		assertEquals(List.of("The size of \"email\" must be less than or equal to 255. The given size is 256"),
				violations.get("email"));
	}

	@Test
	void lengthCountsCodePointsAsPostgresql() {
		// One varchar character per emoji, although each one is two UTF-16 chars
		assertEquals(Map.of(), violations("😀".repeat(255), "ada@example.org", "secret"));
		assertTrue(violations("😀".repeat(256), "ada@example.org", "secret").containsKey("name"));

		// "e" + combining acute accent: 2 code points in PostgreSQL, even though NFC
		// would make it the single character "é"
		assertTrue(violations("é".repeat(128), "ada@example.org", "secret").containsKey("name"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "ada@example.org", "ada.lovelace@example.org", "ada+newsletter@example.org", "a@b.co",
			"o'brien@example.org", "ada_l-1@mail.example.co.uk",
			// YAVI's email() only checks the general shape of the address
			"ada@localhost", "ada@1.2.3.4", "zoë@exämple.org" })
	void acceptsValidEmails(String email) {
		assertEquals(Map.of(), emailViolations(email));
	}

	@ParameterizedTest
	@ValueSource(strings = { "ada", "ada@", "@example.org", "ada@@example.org", "ada@ex@ample.org",
			"ada lovelace@example.org", "ada@exa mple.org", ".ada@example.org", "ada.@example.org",
			"ada..lovelace@example.org", "ada@.example.org", "ada@example..org", "ada@[127.0.0.1]",
			"\"ada\"@example.org", "ada(comment)@example.org" })
	void rejectsInvalidEmails(String email) {
		assertEquals(Map.of("email", List.of("\"email\" must be a valid email address")), emailViolations(email));
	}
}

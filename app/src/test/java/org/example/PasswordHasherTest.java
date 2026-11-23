package org.example;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

	private final PasswordHasher hasher = new PasswordHasher();

	@Test
	void hashMatchesOriginalPassword() {
		String hash = hasher.hash("correct horse battery staple");

		assertTrue(hasher.matches("correct horse battery staple", hash));
	}

	@Test
	void hashDoesNotMatchAnotherPassword() {
		String hash = hasher.hash("correct horse battery staple");

		assertFalse(hasher.matches("Correct horse battery staple", hash));
		assertFalse(hasher.matches("", hash));
	}

	@Test
	void hashIsSalted() {
		assertNotEquals(hasher.hash("secret"), hasher.hash("secret"));
	}

	@Test
	void hashNeverContainsThePassword() {
		assertFalse(hasher.hash("secret-password").contains("secret-password"));
	}

	@Test
	void usesArgon2idWithOwaspParameters() {
		String hash = hasher.hash("secret");

		assertTrue(hash.startsWith("$argon2id$v=19$m=19456,t=2,p=1$"), hash);
	}

	@Test
	void rejectsMalformedHash() {
		assertFalse(hasher.matches("secret", "not-a-hash"));
	}
}

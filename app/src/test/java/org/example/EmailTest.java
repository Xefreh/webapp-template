package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

	@Test
	void normalizeLowercasesAndTrims() {
		assertEquals("ada.lovelace@example.org", Email.normalize("  Ada.Lovelace@Example.ORG \t"));
	}

	@Test
	void normalizeKeepsNull() {
		assertNull(Email.normalize(null));
	}

	@Test
	void normalizeDoesNotDependOnDefaultLocale() {
		Locale previous = Locale.getDefault();
		try {
			// In Turkish, "I".toLowerCase() is a dotless i
			Locale.setDefault(Locale.forLanguageTag("tr-TR"));

			assertEquals("info@example.org", Email.normalize("INFO@EXAMPLE.ORG"));
		} finally {
			Locale.setDefault(previous);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "ada@example.org", "ada.lovelace@example.org", "ada+newsletter@example.org",
			"a@b.co", "o'brien@example.org", "ada_l-1@mail.example.co.uk", "ada@my-domain.example",
			"ada@xn--80ak6aa92e.com", "!#$%&'*+/=?^_`{|}~-@example.org" })
	void acceptsValidAddresses(String email) {
		assertTrue(Email.isValid(email), email);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "ada", "ada@", "@example.org", "ada@example", "ada@@example.org",
			"ada@ex@ample.org", "ada lovelace@example.org", "ada@exa mple.org", ".ada@example.org",
			"ada.@example.org", "ada..lovelace@example.org", "ada@.example.org", "ada@example.org.",
			"ada@example..org", "ada@-example.org", "ada@example-.org", "ada@1.2.3.4", "ada@[127.0.0.1]",
			"\"ada\"@example.org", "zoë@example.org", "ada@exämple.org", "ada@example.org\n" })
	void rejectsInvalidAddresses(String email) {
		assertFalse(Email.isValid(email), email);
	}

	@Test
	void rejectsUppercaseSinceAddressesAreNormalizedFirst() {
		assertFalse(Email.isValid("Ada@example.org"));
	}

	@Test
	void rejectsDomainLabelLongerThan63Characters() {
		assertTrue(Email.isValid("ada@" + "a".repeat(63) + ".org"));
		assertFalse(Email.isValid("ada@" + "a".repeat(64) + ".org"));
	}

	@Test
	void rejectsNull() {
		assertFalse(Email.isValid(null));
	}
}

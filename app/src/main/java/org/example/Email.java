package org.example;

import java.util.Locale;
import java.util.regex.Pattern;

/** Normalization and validation of email addresses. */
public final class Email {

	// Run of RFC 5322 "atext" characters (ASCII letters, digits and symbols)
	private static final String ATOM = "[a-z0-9!#$%&'*+/=?^_`{|}~-]+";
	// Domain label: letters, digits and inner hyphens, at most 63 characters
	private static final String LABEL = "[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?";
	// Top-level domain: a label starting with a letter, so that "1.2.3.4" is not a
	// domain
	private static final String TLD = "[a-z](?:[a-z0-9-]{0,61}[a-z0-9])?";

	/**
	 * Pragmatic address syntax, close to the one of HTML's input type=email: atoms
	 * separated by single dots, "@", then at least two domain labels. Applied to
	 * normalized (lowercase) addresses; quoted local parts, IP literals and
	 * non-ASCII addresses are rejected.
	 */
	private static final Pattern PATTERN = Pattern
			.compile(ATOM + "(?:\\." + ATOM + ")*@(?:" + LABEL + "\\.)+" + TLD);

	private Email() {
	}

	/**
	 * Trims and lowercases {@code email} (Locale.ROOT, so that the result does not
	 * depend on the default locale), or returns null if it is null.
	 */
	public static String normalize(String email) {
		return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
	}

	/** Whether {@code email}, already normalized, is a syntactically valid address. */
	public static boolean isValid(String email) {
		return email != null && PATTERN.matcher(email).matches();
	}
}

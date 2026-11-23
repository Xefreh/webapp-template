package org.example;

import java.util.Locale;
import java.util.regex.Pattern;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import jakarta.servlet.http.HttpServletRequest;

/** Form of POST /users and its validation rules. */
public record CreateUserRequest(String name, String email, String password) {

	// Length of the varchar(255) name and email columns of the users table
	private static final int MAX_LENGTH = 255;

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
	private static final Pattern EMAIL = Pattern.compile(ATOM + "(?:\\." + ATOM + ")*@(?:" + LABEL + "\\.)+" + TLD);

	// normalizer(null): count code points as given, like PostgreSQL's varchar(n),
	// instead of after NFC normalization
	public static final Validator<CreateUserRequest> VALIDATOR = ValidatorBuilder.<CreateUserRequest>of()
			.constraint(CreateUserRequest::name, "name", c -> c.normalizer(null).notBlank().lessThanOrEqual(MAX_LENGTH))
			.constraint(CreateUserRequest::email, "email",
					c -> c.normalizer(null).notBlank().lessThanOrEqual(MAX_LENGTH)
							// A blank value is only reported by notBlank()
							.predicate(email -> email.isBlank() || EMAIL.matcher(email).matches(), "email.invalid",
									"\"{0}\" must be a valid email address"))
			.constraint(CreateUserRequest::password, "password", c -> c.notBlank())
			.build();

	/**
	 * Trims and lowercases the email (Locale.ROOT, independent of the default
	 * locale), so that it is stored lowercase and the unique constraint ignores
	 * case.
	 */
	public CreateUserRequest {
		email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
	}

	/** Reads the form parameters, without validating them. */
	public static CreateUserRequest fromForm(HttpServletRequest req) {
		return new CreateUserRequest(req.getParameter("name"), req.getParameter("email"), req.getParameter("password"));
	}
}

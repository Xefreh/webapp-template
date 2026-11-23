package org.example;

import java.util.Locale;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import jakarta.servlet.http.HttpServletRequest;

/** Form of POST /users and its validation rules. */
public record CreateUserRequest(String name, String email, String password) {

	// Length of the varchar(255) name and email columns of the users table
	private static final int MAX_LENGTH = 255;

	// normalizer(null): count code points as given, like PostgreSQL's varchar(n),
	// instead of after NFC normalization
	public static final Validator<CreateUserRequest> VALIDATOR = ValidatorBuilder.<CreateUserRequest>of()
			.constraint(CreateUserRequest::name, "name", c -> c.normalizer(null).notBlank().lessThanOrEqual(MAX_LENGTH))
			.constraint(CreateUserRequest::email, "email",
					// email() accepts "" (the constructor strips blank values to ""), which only
					// notBlank() reports
					c -> c.normalizer(null).notBlank().lessThanOrEqual(MAX_LENGTH).email())
			.constraint(CreateUserRequest::password, "password", c -> c.notBlank()).build();

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

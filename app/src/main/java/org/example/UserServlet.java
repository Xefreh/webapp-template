package org.example;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

import am.ik.yavi.core.ConstraintViolations;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class UserServlet extends BaseServlet {

	// PostgreSQL SQLSTATE for unique_violation
	private static final String UNIQUE_VIOLATION = "23505";

	private final UserRepository userRepository;
	private final PasswordHasher passwordHasher;

	public UserServlet(UserRepository userRepository, PasswordHasher passwordHasher) {
		this.userRepository = userRepository;
		this.passwordHasher = passwordHasher;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		renderJson(resp, userRepository.list());
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		CreateUserRequest form = CreateUserRequest.fromForm(req);

		ConstraintViolations violations = CreateUserRequest.VALIDATOR.validate(form);
		if (!violations.isValid()) {
			renderViolations(resp, violations);
			return;
		}

		String passwordHash = passwordHasher.hash(form.password());

		User user;
		try {
			user = userRepository.create(new User(form.name(), form.email(), passwordHash));
		} catch (UnableToExecuteStatementException e) {
			if (e.getCause() instanceof SQLException sql && UNIQUE_VIOLATION.equals(sql.getSQLState())) {
				renderJson(resp, HttpServletResponse.SC_CONFLICT, Map.of("error", "email already in use"));
				return;
			}
			throw e; // anything else is a real server error
		}

		renderJson(resp, HttpServletResponse.SC_CREATED, user);
	}
}

package org.example;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

import freemarker.template.Configuration;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class UserServlet extends BaseServlet {

	// PostgreSQL SQLSTATE for unique_violation
	private static final String UNIQUE_VIOLATION = "23505";

	private final UserRepository userRepository;
	private final PasswordHasher passwordHasher;

	public UserServlet(Configuration freemarkerCfg, UserRepository userRepository, PasswordHasher passwordHasher) {
		super(freemarkerCfg);
		this.userRepository = userRepository;
		this.passwordHasher = passwordHasher;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		renderJson(resp, userRepository.list());
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String name = req.getParameter("name");
		String email = req.getParameter("email");
		String password = req.getParameter("password");

		if (name == null || name.isBlank() || email == null || email.isBlank()) {
			renderJson(resp, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", "name and email are required"));
			return;
		}
		if (password == null || password.isBlank()) {
			renderJson(resp, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", "password is required"));
			return;
		}

		String passwordHash = passwordHasher.hash(password);

		User user;
		try {
			user = userRepository.create(new User(name, email, passwordHash));
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

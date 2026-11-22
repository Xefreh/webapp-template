package org.example;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

public class PasswordHasher {

	// OWASP recommended Argon2id parameters: 19 MiB memory, 2 iterations,
	// parallelism 1
	private static final int SALT_LENGTH = 16;
	private static final int HASH_LENGTH = 32;
	private static final int PARALLELISM = 1;
	private static final int MEMORY_KIB = 19_456;
	private static final int ITERATIONS = 2;

	// Argon2PasswordEncoder is thread-safe: one instance can be shared across
	// requests
	private final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(SALT_LENGTH, HASH_LENGTH, PARALLELISM,
			MEMORY_KIB, ITERATIONS);

	public String hash(String rawPassword) {
		return encoder.encode(rawPassword);
	}

	public boolean matches(String rawPassword, String hash) {
		return encoder.matches(rawPassword, hash);
	}
}

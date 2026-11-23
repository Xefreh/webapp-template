package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Starts the shadow jar in its own JVM against an empty PostgreSQL, as in
 * production. Run by the smokeTest Gradle task, which builds the jar first.
 */
@Tag("smoke")
@DatabaseTest
class FatJarSmokeTest {

	private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(60);

	private static PostgreSQLContainer postgres;
	private static Process app;
	private static Path output;
	private static URI baseUri;

	private static final HttpClient client = HttpClient.newHttpClient();

	@BeforeAll
	static void startJar() throws Exception {
		String jar = System.getProperty("smokeTest.jar");
		if (jar == null) {
			fail("smokeTest.jar system property not set: run the smokeTest Gradle task");
		}

		// Not the shared TestDatabase: the jar must run the migrations itself
		postgres = new PostgreSQLContainer("postgres:18-alpine");
		postgres.start();

		int port = freePort();
		baseUri = URI.create("http://localhost:" + port);
		output = Files.createTempFile("app-all", ".log");

		// Passed as argument, as compose.yaml does with docker/config.properties
		Properties config = TestConfig.forDatabase(postgres);
		config.setProperty("server.port", String.valueOf(port));
		Path configFile = TestConfig.write(config);

		ProcessBuilder builder = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
				"-jar", jar, configFile.toString());
		builder.redirectErrorStream(true).redirectOutput(output.toFile());
		app = builder.start();

		waitUntilUp();
	}

	@AfterAll
	static void stopJar() throws Exception {
		if (app != null) {
			app.destroy();
			if (!app.waitFor(10, TimeUnit.SECONDS)) {
				app.destroyForcibly();
			}
		}
		if (postgres != null) {
			postgres.stop();
		}
		client.close();
	}

	private static int freePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

	private static String appOutput() throws IOException {
		return Files.readString(output);
	}

	private static void waitUntilUp() throws Exception {
		long deadline = System.nanoTime() + STARTUP_TIMEOUT.toNanos();
		while (System.nanoTime() < deadline) {
			if (!app.isAlive()) {
				fail("The jar exited with code " + app.exitValue() + ":\n" + appOutput());
			}
			try {
				get("/");
				return;
			} catch (ConnectException e) {
				Thread.sleep(200);
			}
		}
		fail("The jar did not answer within " + STARTUP_TIMEOUT + ":\n" + appOutput());
	}

	private static HttpResponse<String> get(String path) throws IOException, InterruptedException {
		return client.send(HttpRequest.newBuilder(baseUri.resolve(path)).build(), HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void servesHelloPage() throws Exception {
		HttpResponse<String> response = get("/");

		assertEquals(200, response.statusCode(), appOutput());
		assertTrue(response.body().contains("Bonjour Monde"), response.body());
	}

	@Test
	void servesFrontendBundle() throws Exception {
		assertEquals(200, get("/assets/app.js").statusCode());
		assertEquals(200, get("/assets/app.css").statusCode());
	}

	@Test
	void ranMigrationsAndCreatesUsers() throws Exception {
		// Argon2 hashing needs Bouncy Castle, the insert needs the migrated users table
		HttpResponse<String> created = client.send(HttpRequest.newBuilder(baseUri.resolve("/users"))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString("name=Ada&email=ada%40example.org&password=secret")).build(),
				HttpResponse.BodyHandlers.ofString());

		assertEquals(201, created.statusCode(), created.body() + "\n" + appOutput());
		assertTrue(get("/users").body().contains("ada@example.org"));
	}
}

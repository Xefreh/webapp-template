package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Routes of the real application that do not touch the database. */
class AppRoutesTest {

	private static TestServer server;

	@BeforeAll
	static void startServer() throws Exception {
		// Jdbi connects lazily: these routes never reach this unused database
		server = TestServer.startApp(new UserRepository(Jdbi.create("jdbc:postgresql://localhost:1/unused")));
	}

	@AfterAll
	static void stopServer() throws Exception {
		server.close();
	}

	@Test
	void rootRendersHelloPage() throws Exception {
		HttpResponse<String> response = server.get("/");

		assertEquals(200, response.statusCode());
		assertEquals("text/html;charset=utf-8", response.headers().firstValue("Content-Type").orElseThrow());
		assertTrue(response.body().contains("<h1>Bonjour Monde</h1>"), response.body());
		assertTrue(response.body().contains("/assets/app.js"), response.body());
	}

	@Test
	void unknownPathIsNotFound() throws Exception {
		assertEquals(404, server.get("/does-not-exist").statusCode());
		assertEquals(404, server.get("/users/42").statusCode());
	}

	@Test
	void postOnRootIsNotAllowed() throws Exception {
		HttpResponse<String> response = server
				.send(HttpRequest.newBuilder(server.uri("/")).POST(HttpRequest.BodyPublishers.noBody()).build());

		assertEquals(405, response.statusCode());
	}

	@Test
	void servesFrontendScript() throws Exception {
		HttpResponse<String> response = server.get("/assets/app.js");

		assertEquals(200, response.statusCode());
		assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("text/javascript"),
				response.headers().toString());
		assertTrue(response.body().contains("mon-compteur"), "bundle should define the mon-compteur element");
	}

	@Test
	void servesFrontendStylesheet() throws Exception {
		HttpResponse<String> response = server.get("/assets/app.css");

		assertEquals(200, response.statusCode());
		assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("text/css"),
				response.headers().toString());
	}

	@Test
	void missingAssetIsNotFound() throws Exception {
		assertEquals(404, server.get("/assets/missing.js").statusCode());
	}
}

package org.example;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

/** Runs a Jetty server on a free port and sends HTTP requests to it. */
final class TestServer implements AutoCloseable {

	private final Server server;
	private final HttpClient client = HttpClient.newHttpClient();

	private TestServer(Server server) throws Exception {
		this.server = server;
		server.start();
	}

	/** Starts the real application, wired to {@code userRepository}. */
	static TestServer startApp(UserRepository userRepository) throws Exception {
		return new TestServer(App.createServer(userRepository, 0));
	}

	/** Starts {@code server}, whose connector must listen on port 0. */
	static TestServer start(Server server) throws Exception {
		return new TestServer(server);
	}

	URI uri(String path) {
		int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
		return URI.create("http://localhost:" + port + path);
	}

	HttpResponse<String> get(String path) throws IOException, InterruptedException {
		return send(HttpRequest.newBuilder(uri(path)).GET().build());
	}

	HttpResponse<String> postForm(String path, Map<String, String> form) throws IOException, InterruptedException {
		String body = form.entrySet().stream().map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
				+ URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
		return send(HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(body)).build());
	}

	HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
		return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	@Override
	public void close() throws Exception {
		client.close();
		server.stop();
	}
}

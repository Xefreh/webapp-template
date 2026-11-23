package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;

import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class LogFilterTest {

	private static final IllegalStateException FAILURE = new IllegalStateException("boom");

	private static TestServer server;

	private final Logger logger = (Logger) LoggerFactory.getLogger(LogFilter.class);
	private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

	@BeforeAll
	static void startServer() throws Exception {
		Server jetty = new Server(0);
		ServletContextHandler context = new ServletContextHandler();
		context.addFilter(LogFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
		context.addServlet(new ServletHolder(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
				resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
			}
		}), "/ok");
		context.addServlet(new ServletHolder(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
				throw FAILURE;
			}
		}), "/fail");
		jetty.setHandler(context);
		server = TestServer.start(jetty);
	}

	@AfterAll
	static void stopServer() throws Exception {
		server.close();
	}

	@BeforeEach
	void captureLogs() {
		appender.start();
		logger.addAppender(appender);
	}

	@AfterEach
	void releaseLogs() {
		logger.detachAppender(appender);
	}

	/**
	 * Log events of the request, waiting for them: the filter logs after the
	 * response is sent.
	 */
	private List<ILoggingEvent> events(int expected) throws InterruptedException {
		long deadline = System.nanoTime() + 5_000_000_000L;
		while (appender.list.size() < expected && System.nanoTime() < deadline) {
			Thread.sleep(10);
		}
		return List.copyOf(appender.list);
	}

	@Test
	void logsMethodPathStatusAndDuration() throws Exception {
		assertEquals(204, server.get("/ok?query=ignored").statusCode());

		List<ILoggingEvent> events = events(1);
		assertEquals(1, events.size(), events.toString());
		assertEquals(Level.INFO, events.get(0).getLevel());
		String message = events.get(0).getFormattedMessage();
		assertTrue(message.matches("\\[GET\\] \\[/ok\\] \\[204\\] \\[\\d+ ms\\]"), message);
	}

	@Test
	void logsUnhandledExceptionAsServerError() throws Exception {
		assertEquals(500, server.get("/fail").statusCode());

		List<ILoggingEvent> events = events(2);
		assertEquals(2, events.size(), events.toString());

		ILoggingEvent error = events.get(0);
		assertEquals(Level.ERROR, error.getLevel());
		assertEquals("Unhandled exception on GET /fail", error.getFormattedMessage());
		assertEquals(FAILURE.getMessage(), error.getThrowableProxy().getMessage());

		String access = events.get(1).getFormattedMessage();
		assertTrue(access.matches("\\[GET\\] \\[/fail\\] \\[500\\] \\[\\d+ ms\\]"), access);
	}
}

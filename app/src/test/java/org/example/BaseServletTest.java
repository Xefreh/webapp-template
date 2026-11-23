package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Map;

import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class BaseServletTest {

	private static TestServer server;

	/** Renders the template named by the "template" parameter, with x = "<b>&". */
	static class TemplateServlet extends BaseServlet {
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			renderTemplate(resp, req.getParameter("template"), Map.of("x", "<b>&"));
		}
	}

	static class JsonServlet extends BaseServlet {
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
			if (req.getParameter("unserializable") != null) {
				// Jackson refuses beans without properties
				renderJson(resp, new Object());
			} else {
				renderJson(resp, HttpServletResponse.SC_ACCEPTED, Map.of("ok", true));
			}
		}
	}

	@BeforeAll
	static void startServer() throws Exception {
		StringTemplateLoader templates = new StringTemplateLoader();
		templates.putTemplate("ok.ftlh", "<p>${x}</p>");
		templates.putTemplate("broken.ftlh", "<p>before the error</p>${missing}");

		// The application's configuration, with in-memory templates
		Configuration cfg = App.freemarkerConfiguration();
		cfg.setTemplateLoader(templates);

		Server jetty = new Server(0);

		ServletContextHandler withTemplates = new ServletContextHandler("/app");
		withTemplates.setDefaultResponseCharacterEncoding("UTF-8");
		withTemplates.setAttribute(BaseServlet.FREEMARKER_CFG_ATTRIBUTE, cfg);
		withTemplates.addServlet(new ServletHolder(new TemplateServlet()), "/template");
		withTemplates.addServlet(new ServletHolder(new JsonServlet()), "/json");

		// No FreeMarker Configuration attribute
		ServletContextHandler withoutTemplates = new ServletContextHandler("/bare");
		withoutTemplates.addServlet(new ServletHolder(new TemplateServlet()), "/template");

		jetty.setHandler(new ContextHandlerCollection(withTemplates, withoutTemplates));
		server = TestServer.start(jetty);
	}

	@AfterAll
	static void stopServer() throws Exception {
		server.close();
	}

	@Test
	void rendersTemplateAsUtf8Html() throws Exception {
		HttpResponse<String> response = server.get("/app/template?template=ok.ftlh");

		assertEquals(200, response.statusCode());
		assertEquals("text/html;charset=utf-8", response.headers().firstValue("Content-Type").orElseThrow());
	}

	@Test
	void escapesHtmlInFtlhTemplates() throws Exception {
		HttpResponse<String> response = server.get("/app/template?template=ok.ftlh");

		assertEquals("<p>&lt;b&gt;&amp;</p>", response.body());
	}

	@Test
	void templateErrorIsAServerErrorWithoutPartialOutput() throws Exception {
		HttpResponse<String> response = server.get("/app/template?template=broken.ftlh");

		assertEquals(500, response.statusCode());
		assertFalse(response.body().contains("before the error"), response.body());
	}

	@Test
	void missingTemplateIsAServerError() throws Exception {
		assertEquals(500, server.get("/app/template?template=missing.ftlh").statusCode());
	}

	@Test
	void missingConfigurationIsAServerError() throws Exception {
		assertEquals(500, server.get("/bare/template?template=ok.ftlh").statusCode());
	}

	@Test
	void rendersJsonWithStatusAndLength() throws Exception {
		HttpResponse<String> response = server.get("/app/json");

		assertEquals(202, response.statusCode());
		assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("application/json"));
		assertEquals("{\"ok\":true}", response.body());
		assertEquals(response.body().length(), response.headers().firstValueAsLong("Content-Length").orElseThrow());
	}

	@Test
	void serializationErrorIsAServerError() throws Exception {
		HttpResponse<String> response = server.get("/app/json?unserializable");

		assertEquals(500, response.statusCode());
	}
}

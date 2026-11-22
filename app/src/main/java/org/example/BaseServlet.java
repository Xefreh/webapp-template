package org.example;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;

public class BaseServlet extends HttpServlet {

	// ObjectMapper is thread-safe once configured: share a single instance
	private static final ObjectMapper JSON = new ObjectMapper();

	// ServletContext attribute holding the shared FreeMarker Configuration
	public static final String FREEMARKER_CFG_ATTRIBUTE = Configuration.class.getName();

	protected void renderTemplate(HttpServletResponse response, String name, Map<String, Object> model)
			throws IOException, ServletException {
		Configuration freemarkerCfg = (Configuration) getServletContext().getAttribute(FREEMARKER_CFG_ATTRIBUTE);
		if (freemarkerCfg == null) {
			throw new ServletException(
					"No FreeMarker Configuration in ServletContext attribute " + FREEMARKER_CFG_ATTRIBUTE);
		}
		Template template = freemarkerCfg.getTemplate(name);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("text/html");

		try {
			template.process(model, response.getWriter());
		} catch (TemplateException e) {
			if (!response.isCommitted()) {
				response.resetBuffer();
			}
			throw new ServletException("Error in template " + name, e);
		}
	}

	protected void renderJson(HttpServletResponse response, Object data) throws IOException {
		renderJson(response, HttpServletResponse.SC_OK, data);
	}

	protected void renderJson(HttpServletResponse response, int status, Object data) throws IOException {
		// Serialize before writing so nothing is sent to the client if serialization
		// fails
		byte[] body = JSON.writeValueAsBytes(data);

		response.setStatus(status);
		response.setContentType("application/json");
		response.setContentLength(body.length);
		response.getOutputStream().write(body);
	}
}
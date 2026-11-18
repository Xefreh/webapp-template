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

	private final Configuration freemarkerCfg;

	public BaseServlet(Configuration freemarkerCfg) {
		this.freemarkerCfg = freemarkerCfg;
	}

	protected void renderTemplate(HttpServletResponse response, String name, Map<String, Object> model)
			throws IOException, ServletException {
		Template template = freemarkerCfg.getTemplate(name);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");

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
		response.setCharacterEncoding("UTF-8");
		response.setContentLength(body.length);
		response.getOutputStream().write(body);
	}
}
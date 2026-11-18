package org.example;

import java.io.IOException;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;

public class BaseServlet extends HttpServlet {

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
}
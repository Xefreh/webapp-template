package org.example;

import java.io.IOException;
import java.util.Map;

import freemarker.template.Configuration;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class HelloServlet extends BaseServlet {

	public HelloServlet(Configuration freemarkerCfg) {
		super(freemarkerCfg);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		renderTemplate(response, "hello.ftlh", Map.of("nom", "Monde"));
	}
}
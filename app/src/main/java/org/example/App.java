package org.example;

import java.util.TimeZone;

import org.eclipse.jetty.ee11.servlet.ResourceServlet;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.ResourceFactory;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

	private static final Logger log = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) throws Exception {
		Configuration cfg = new Configuration(Configuration.VERSION_2_3_35);

		cfg.setClassForTemplateLoading(App.class, "/templates");
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		cfg.setLogTemplateExceptions(false);
		cfg.setWrapUncheckedExceptions(true);
		cfg.setFallbackOnNullLoopVariable(false);
		cfg.setSQLDateAndTimeTimeZone(TimeZone.getDefault());

		Server server = new Server(8080);

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

		context.setBaseResource(ResourceFactory.of(server).newClassLoaderResource("static"));
		ServletHolder staticHolder = context.addServlet(ResourceServlet.class, "/assets/*");
		staticHolder.setInitParameter("pathInfoOnly", "false");

		context.addServlet(new ServletHolder(new HelloServlet(cfg)), "/");

		server.setHandler(context);
		server.start();

		int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
		log.info("Server started at http://localhost:{}/", port);

		server.join();
	}
}
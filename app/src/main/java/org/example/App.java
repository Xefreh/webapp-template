package org.example;

import java.util.EnumSet;
import java.util.TimeZone;

import org.eclipse.jetty.ee11.servlet.ResourceServlet;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import jakarta.servlet.DispatcherType;

public class App {

	private static final Logger log = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) throws Exception {
		AppConfig appConfig = AppConfig.load();

		Database database = new Database(appConfig);
		Runtime.getRuntime().addShutdownHook(new Thread(database::close));

		Flyway.configure().dataSource(database.getDataSource()).locations("classpath:migrations").load().migrate();

		Jdbi jdbi = Jdbi.create(database.getDataSource());
		UserRepository userRepository = new UserRepository(jdbi);
		PasswordHasher passwordHasher = new PasswordHasher();

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

		context.addFilter(LogFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));

		context.setBaseResource(ResourceFactory.of(server).newClassLoaderResource("static"));
		ServletHolder staticHolder = context.addServlet(ResourceServlet.class, "/assets/*");
		staticHolder.setInitParameter("pathInfoOnly", "false");

		context.addServlet(new ServletHolder(new UserServlet(cfg, userRepository, passwordHasher)), "/users");
		context.addServlet(new ServletHolder(new HelloServlet(cfg)), "/");

		server.setHandler(context);
		server.start();

		int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
		log.info("Server started at http://localhost:{}/", port);

		server.join();
	}
}
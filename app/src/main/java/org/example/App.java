package org.example;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.TimeZone;

import org.eclipse.jetty.ee11.servlet.ResourceServlet;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.VirtualThreads;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import jakarta.servlet.DispatcherType;

public class App {

	private static final Logger log = LoggerFactory.getLogger(App.class);

	/**
	 * @param args optionally, the path of a properties file replacing the bundled
	 *             config.properties
	 */
	public static void main(String[] args) throws Exception {
		if (args.length > 1) {
			throw new IllegalArgumentException("Usage: app [config.properties]");
		}
		AppConfig appConfig = args.length == 1 ? AppConfig.load(Path.of(args[0])) : AppConfig.load();

		Database database = new Database(appConfig);
		Runtime.getRuntime().addShutdownHook(new Thread(database::close));

		database.migrate();

		UserRepository userRepository = new UserRepository(Jdbi.create(database.getDataSource()));

		Server server = createServer(userRepository, appConfig.getInt("server.port"));
		server.start();

		int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
		log.info("Server started at http://localhost:{}/", port);

		server.join();
	}

	/**
	 * Builds the Jetty server with every filter and servlet, without starting it.
	 *
	 * @param port the port to listen on, 0 for any free port
	 */
	static Server createServer(UserRepository userRepository, int port) {
		PasswordHasher passwordHasher = new PasswordHasher();

		// Platform threads handle selectors and I/O; request handling (servlets, JDBC
		// calls) runs on virtual threads.
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setName("jetty");
		threadPool.setVirtualThreadsExecutor(VirtualThreads.getNamedVirtualThreadsExecutor("jetty-vt-"));

		Server server = new Server(threadPool);
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		server.addConnector(connector);

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setDefaultRequestCharacterEncoding("UTF-8");
		context.setDefaultResponseCharacterEncoding("UTF-8");
		context.setAttribute(BaseServlet.FREEMARKER_CFG_ATTRIBUTE, freemarkerConfiguration());

		context.addFilter(LogFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));

		context.setBaseResource(ResourceFactory.of(server).newClassLoaderResource("static"));
		ServletHolder staticHolder = context.addServlet(ResourceServlet.class, "/assets/*");
		staticHolder.setInitParameter("pathInfoOnly", "false");

		context.addServlet(new ServletHolder(new UserServlet(userRepository, passwordHasher)), "/users");
		// "" matches the context root only ("/" would make it the default servlet and
		// answer every unknown path with the hello page)
		context.addServlet(new ServletHolder(new HelloServlet()), "");

		server.setHandler(context);
		return server;
	}

	static Configuration freemarkerConfiguration() {
		Configuration cfg = new Configuration(Configuration.VERSION_2_3_35);

		cfg.setClassForTemplateLoading(App.class, "/templates");
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		cfg.setLogTemplateExceptions(false);
		cfg.setWrapUncheckedExceptions(true);
		cfg.setFallbackOnNullLoopVariable(false);
		cfg.setSQLDateAndTimeTimeZone(TimeZone.getDefault());
		return cfg;
	}
}

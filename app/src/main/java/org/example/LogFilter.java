package org.example;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LogFilter implements Filter {

	private static final Logger log = LoggerFactory.getLogger(LogFilter.class);

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		long start = System.nanoTime();
		boolean failed = false;
		try {
			chain.doFilter(request, response);
		} catch (Throwable t) {
			failed = true;
			log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), t);
			throw t;
		} finally {
			long elapsedMs = (System.nanoTime() - start) / 1_000_000;
			int status = failed ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : res.getStatus();
			log.info("[{}] [{}] [{}] [{} ms]", req.getMethod(), req.getRequestURI(), status, elapsedMs);
		}
	}

}

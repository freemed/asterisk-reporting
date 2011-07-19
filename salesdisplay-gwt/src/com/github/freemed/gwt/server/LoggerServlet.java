/**
 * $Id: LoggerServlet.java 1518 2009-04-29 20:14:12Z jbuchbinder $
 * original source : http://whatwouldnickdo.com/wordpress/186/gwt-hosted-mode-and-log4j/
 */

package com.github.freemed.gwt.server;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class LoggerServlet extends HttpServlet {
	static final Logger logger = Logger.getLogger(LoggerServlet.class);

	public void init() throws ServletException {
		System.out.println("LogggerServlet init() starting.");
		String log4jfile = getInitParameter("log4j-properties");
		System.out.println("log4j-properties: " + log4jfile);
		if (log4jfile != null) {
			String propertiesFilename = getServletContext().getRealPath(
					log4jfile);
			PropertyConfigurator.configure(propertiesFilename);
			logger.info("logger configured.");
		} else {
			System.out.println("Error setting up logger.");
		}
		System.out.println("LoggerServlet init() done.");
	}
}

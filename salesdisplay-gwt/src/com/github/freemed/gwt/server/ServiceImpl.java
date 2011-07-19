/**
 * $Id: ServiceImpl.java 1890 2010-01-29 22:07:53Z jbuchbinder $
 */

package com.github.freemed.gwt.server;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.github.freemed.gwt.client.Service;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class ServiceImpl extends RemoteServiceServlet implements Service {

	// private Log log = LogFactory.getLog(ServiceImpl.class);
	private static final Logger logger = Logger.getLogger(ServiceImpl.class);

	private static final int NUM_DATA_FIELDS = 10;

	private static final long serialVersionUID = 4675586263301450980L;

	private static PropertiesConfiguration config = null;

	/**
	 * Generate list of sales extensions.
	 */
	public String[][] getSalesData() {
		logger.debug("getSalesData() called");
		logger.info("Java memory in use = "
				+ (Runtime.getRuntime().totalMemory() - Runtime.getRuntime()
						.freeMemory()));

		Connection c = getConnection();
		CallableStatement cStmt = null;
		try {
			cStmt = c.prepareCall("{ CALL spGetSalesStats() }");
			boolean hadResults = cStmt.execute();

			List<String[]> r = new ArrayList<String[]>();

			if (hadResults) {
				ResultSet rs = cStmt.getResultSet();

				while (rs.next()) {
					List<String> rInner = new ArrayList<String>();
					for (int iter = 1; iter <= NUM_DATA_FIELDS; iter++) {
						try {
							rInner.add(rs.getString(iter));
						} catch (Exception ex) {
							logger.error("Unable to add, will use 0", ex);
							rInner.add("0");
						}
					}

					logger.debug("getSalesData() fetched data for extension "
							+ rInner.get(0));
					r.add((String[]) rInner.toArray(new String[0]));
				}
				logger.debug("getSalesData() returning " + r.size() + " items");
				rs.close();
				cStmt.close();
				return r.toArray(new String[0][0]);
			}

			try {
				cStmt.close();
			} catch (Exception ex) {
			}
			return null;
		} catch (SQLException e) {
			logger.error("getSalesData() - Generated exception", e);
			e.printStackTrace();
			try {
				cStmt.close();
			} catch (Exception ex) {
			}
			return null;
		}
	}

	/**
	 * Generate list of sales extensions.
	 */
	public String[][] getSalesExtensions() {
		logger.debug("getSalesExtensions() called");
		logger.info("Java memory in use = "
				+ (Runtime.getRuntime().totalMemory() - Runtime.getRuntime()
						.freeMemory()));

		Connection c = getConnection();
		CallableStatement cStmt = null;
		try {
			cStmt = c.prepareCall("{ CALL spGetSalesExtensions() }");
			boolean hadResults = cStmt.execute();

			List<String[]> r = new ArrayList<String[]>();

			if (hadResults) {
				ResultSet rs = cStmt.getResultSet();

				while (rs.next()) {
					logger.info("Found " + rs.getString("extension"));

					String[] item = { rs.getString("extension"),
							rs.getString("name"), rs.getString("short_name") };
					r.add(item);
				}

				try {
					rs.close();
				} catch (Exception ex) {
				}
			}

			logger.debug("getSalesExtensions() returning " + r.size()
					+ " items");
			try {
				cStmt.close();
			} catch (Exception ex) {
			}
			return r.toArray(new String[0][0]);
		} catch (NullPointerException npe) {
			logger.error("getSalesExtensions() - NullPointerException", npe);
			// npe.printStackTrace();
			try {
				cStmt.close();
			} catch (Exception ex) {
			}
			return null;
		} catch (SQLException e) {
			logger.error("getSalesExtensions() - SQLException", e);
			try {
				cStmt.close();
			} catch (Exception ex) {
			}
			return null;
		}
	}

	public String[] getGraphColors() {
		return config.getStringArray("graph.colors");
	}

	private Connection getConnection() {
		setConfig();
		String jdbcUrl = null;
		String jdbcDriver = null;
		try {
			jdbcUrl = config.getString("db.url");
			logger.debug("Found db.url string = " + jdbcUrl);
			jdbcDriver = config.getString("db.driver");
			logger.debug("Found db.driver string = " + jdbcDriver);
		} catch (Exception ex) {
			logger.error("Could not get db.url", ex);
			return null;
		}

		try {
			Class.forName(jdbcDriver).newInstance();
		} catch (Exception ex) {
			logger.error("Unable to load driver.", ex);
			ex.printStackTrace();
			return null;
		}

		try {
			Connection conn;
			conn = DriverManager.getConnection(jdbcUrl);
			return conn;
		} catch (Exception ex) {
			logger.error("Unable to create connection.", ex);
			ex.printStackTrace();
		}
		return null;
	}

	/**
	 * Populate the PropertiesConfiguration
	 */
	private void setConfig() {
		if (config == null) {
			String internalConfigPath = getServletContext().getRealPath(
					"/WEB-INF/salesdisplay.properties");
			logger.debug("setConfig(): default properties defined to be "
					+ internalConfigPath);
			String myPath = System.getProperty("properties");
			logger.debug("setConfig(): properties defined to be " + myPath);
			config = new PropertiesConfiguration();
			try {
				config.setFile(new File(myPath));
			} catch (Exception ex) {
				logger.error("Unable to retrieve configuration, using default",
						ex);
				config.setFile(new File(internalConfigPath));
			}
			try {
				config.setReloadingStrategy(new FileChangedReloadingStrategy());
				config.load();
			} catch (ConfigurationException e) {
				e.printStackTrace();
			}
		} else {
			logger.debug("setConfig() indicates properties already loaded");
		}
	}

	public Long getSerialVersion() {
		return new Long(serialVersionUID);
	}

}

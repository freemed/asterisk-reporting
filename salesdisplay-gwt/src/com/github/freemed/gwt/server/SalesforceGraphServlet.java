/**
 * $Id: SalesforceGraphServlet.java 1864 2010-01-04 22:17:06Z jbuchbinder $
 */

package com.github.freemed.gwt.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.log4j.Logger;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlImage;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class SalesforceGraphServlet extends HttpServlet {

	private static final long serialVersionUID = -8941057189852789889L;

	private static final Logger logger = Logger
			.getLogger(SalesforceGraphServlet.class);

	protected WebClient webClient = null;

	private PropertiesConfiguration config = null;

	private TimerTask task = null;

	/**
	 * Polling interval. Default to half hour.
	 */
	private int pollInterval = 1800;

	public void init() throws ServletException {
		super.init(); // initialize here EJB homes if you have ones
		task = new TimerTask() {
			public void run() {
				logger.info("Java memory in use = "
						+ (Runtime.getRuntime().totalMemory() - Runtime
								.getRuntime().freeMemory()));
				logger.info("TimerTask running task");
				try {
					getSalesForceImages();
					gcWebClient();
				} catch (Exception ex) {
					logger.error(ex);
					gcWebClient();
				}
				logger.info("Java memory in use = "
						+ (Runtime.getRuntime().totalMemory() - Runtime
								.getRuntime().freeMemory()));
			}
		};
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(task, 100, pollInterval * 1000);
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		logger.debug("doGet(): started");

		try {
			String test = req.getParameter("test");
			if (test.equals("test")) {
				logger.info("Received request to trigger servlet test");
				task.run();
				logger.info("Done running timer task");
				return;
			}
		} catch (Exception ex) {
		}

		// Sets the content type.
		res.setContentType("image/png");

		// Gets the binary output stream on which the encoded bytes will be sent
		// back to the client browser.
		ServletOutputStream out = res.getOutputStream();

		String id = req.getParameter("id");
		logger.info("doGet(): Using parameter " + id);

		String filename = System.getProperty("home") + "/temp/"
				+ "SalesForceGraph." + id;
		logger.info("Filename = " + filename);

		File file = new File(filename);
		res.setContentLength((int) file.length());

		// Open the file and output streams
		FileInputStream in = new FileInputStream(file);

		// Copy the contents of the file to the output stream
		byte[] buf = new byte[1024];
		int count = 0;
		while ((count = in.read(buf)) >= 0) {
			out.write(buf, 0, count);
		}

		// Closes the servlet output stream.
		out.close();
	}

	/**
	 * Login to Salesforce website and download the sales charts.
	 * 
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws FailingHttpStatusCodeException
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	protected void getSalesForceImages() throws FailingHttpStatusCodeException,
			MalformedURLException, IOException {
		setConfig();
		logger.info("Running getSalesForceImages()");
		logger.info("Attempting Salesforce login");
		webClient = new WebClient(BrowserVersion.FIREFOX_3);
		final HtmlPage loginPage = webClient.getPage(config
				.getString("salesforce.url.login")
				+ "?"
				+ "un="
				+ URLEncoder.encode(config.getString("salesforce.username"),
						"UTF-8")
				+ "&pw="
				+ URLEncoder.encode(config.getString("salesforce.password"),
						"UTF-8"));

		logger.info(loginPage.getTextContent());
		List<String> dashboards = config.getList("salesforce.url.dashboard");
		Iterator<String> iter = dashboards.iterator();
		while (iter.hasNext()) {
			String url = iter.next();
			String id = url.substring(url.lastIndexOf('/') + 1);
			logger.info("Loading dashboard screen, id = " + id);
			final HtmlPage dashboard = webClient.getPage(url);
			logger.info("Attempting to pull chart element");

			try {
				HtmlImage chart = (HtmlImage) dashboard.getByXPath(
						"//img[@class='chart']").get(0);
				logger.info("Found image element : " + chart.toString());
				String outName = System.getProperty("home") + "/temp/"
						+ "SalesForceGraph." + id;
				logger.info("Writing to file " + outName);

				FileOutputStream out = new FileOutputStream(outName, false);
				try {
					javax.imageio.ImageIO.write(((HtmlImage) chart)
							.getImageReader().read(0), "png", out);
				} catch (Exception e) {
					logger.error(e);
				}
				out.close();
				logger.info("Finished writing to file " + outName
						+ " for id = " + id);
			} catch (IndexOutOfBoundsException ex) {
				logger.error("Failed to get image", ex);
			}
		}
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

	/**
	 * Garbage collection routine for WebClient object.
	 */
	private void gcWebClient() {
		logger
				.info("Checking to see if we have to run garbage collection for WebClient object");
		if (this.webClient != null) {
			logger.info("Running garbage collection for WebClient object");
			try {
				this.webClient.closeAllWindows();
			} catch (Exception ex) {
				logger.debug(ex.toString());
			}
			this.webClient = null;
		}
	}

}

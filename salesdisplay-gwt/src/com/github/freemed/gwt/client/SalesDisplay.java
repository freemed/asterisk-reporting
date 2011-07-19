/**
 * $Id: SalesDisplay.java 2041 2010-05-17 18:38:39Z jbuchbinder $
 */

package com.github.freemed.gwt.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.adamtacy.client.ui.NEffectPanel;
import org.adamtacy.client.ui.effects.impl.Fade;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.googlecode.gchart.client.GChart;

public class SalesDisplay implements EntryPoint {

	public final String HEADER_SCREEN_TEXT = "Example, Inc";

	/**
	 * Main page title text.
	 */
	public final String CAPTION_TEXT = "Sales Figures";

	/**
	 * Displayed under CAPTION_TEXT in smaller letters.
	 */
	public final String CAPTION_SUBTEXT = "http://example.com/";

	protected ServiceAsync service = (ServiceAsync) GWT.create(Service.class);

	protected Label statusLabel = new Label();

	protected List<ComparisonBarChart> graphs = new ArrayList<ComparisonBarChart>();

	protected Button updateButton = new Button("Update");

	protected Image loading = new Image("images/transparent.gif");

	protected String baseSalesforceImageUrl = GWT.getHostPageBaseURL()
			+ "SalesforceGraph?id=01Z30000000Q000";

	protected Image salesforceGraph = new Image(baseSalesforceImageUrl + "&ts="
			+ System.currentTimeMillis());

	protected boolean readyToPoll = false;

	protected NEffectPanel effectPanel = new NEffectPanel();
	protected SimplePanel simplePanel = new SimplePanel();
	protected Fade fadeEffect = new Fade();

	/**
	 * Number of minutes between each data polling.
	 */
	protected Integer UPDATE_INTERVAL_MINUTES = 15;

	/**
	 * Number of seconds between screen change.
	 */
	protected Integer SCREEN_CHANGE_SECONDS = 5;

	protected Integer numberOfScreens = 0;

	protected Integer currentScreen = 0;

	protected HashMap<Integer, Widget> screens = new HashMap<Integer, Widget>();

	protected String[] barColors = null;

	public void onModuleLoad() {
		ServiceDefTarget endpoint = (ServiceDefTarget) service;
		endpoint.setServiceEntryPoint(GWT.getHostPageBaseURL() + "service");

		// Decide if we're static or not
		boolean staticDisplay = (Window.Location.getParameter("static") != null);

		service.getGraphColors(new AsyncCallback<String[]>() {
			public void onFailure(Throwable caught) {
				Window.alert("Failed to load graph colors.");
			}

			public void onSuccess(String[] result) {
				barColors = result;
			}
		});

		// Hack from
		// http://gchart.googlecode.com/svn/trunk/doc/com/googlecode/gchart/client/GChart.html#setBlankImageURL(java.lang.String)
		// to get around issues with GChart building components.
		GChart.setBlankImageURL(GWT.getModuleBaseURL()
				+ GChart.DEFAULT_BLANK_IMAGE_URL);

		Frame hPageFrame = new Frame();
		hPageFrame.setUrl(GWT.getHostPageBaseURL() + "splash.html");
		hPageFrame.setSize("98%", "98%");
		addScreen(hPageFrame);

		Frame sfPageFrame = new Frame();
		sfPageFrame.setUrl(GWT.getHostPageBaseURL() + "salesforce.html");
		sfPageFrame.setSize("98%", "98%");
		addScreen(sfPageFrame, 2);

		// Build sales figures page
		VerticalPanel salesFiguresVPanel = new VerticalPanel();
		salesFiguresVPanel.setWidth("100%");
		salesFiguresVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);

		VerticalPanel header = new VerticalPanel();
		header.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		header.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		header.setSpacing(5);
		header.add(new Image("images/logo.png"));
		header.add(new HTML("<h1>" + CAPTION_TEXT + "</h1>"));
		header.add(new HTML("<small><i>" + CAPTION_SUBTEXT + "</i></small>"));
		// header.add(salesforceGraph);

		header.add(loading);

		FlexTable flexTable = new FlexTable();

		flexTable.setWidget(0, 0, header);

		ComparisonBarChart cA = new ComparisonBarChart(barColors);
		cA.setChartTitle("<h2>" + "Number of Calls" + "</h2>");
		cA.setDataIndices(new Integer[] { 1, 4, 7 });
		graphs.add(cA);
		flexTable.setWidget(0, 1, cA);

		ComparisonBarChart cB = new ComparisonBarChart(barColors);
		cB.setChartTitle("<h2>" + "Number of Conversations" + "</h2>");
		cB.setDataIndices(new Integer[] { 2, 5, 8 });
		graphs.add(cB);
		flexTable.setWidget(1, 0, cB);

		ComparisonBarChart cC = new ComparisonBarChart(barColors);
		cC.setChartTitle("<h2>" + "Average Length" + "</h2>");
		cC.setDataIndices(new Integer[] { 3, 6, 9 });
		graphs.add(cC);
		flexTable.setWidget(1, 1, cC);

		salesFiguresVPanel.add(flexTable);

		HorizontalPanel hPanel = new HorizontalPanel();
		hPanel.setSpacing(10);
		hPanel.add(updateButton);
		hPanel.setCellVerticalAlignment(updateButton,
				HasVerticalAlignment.ALIGN_MIDDLE);
		hPanel.add(statusLabel);
		hPanel.setCellVerticalAlignment(statusLabel,
				HasVerticalAlignment.ALIGN_MIDDLE);
		salesFiguresVPanel.add(hPanel);

		updateButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent arg0) {
				pollData();
			}
		});

		// Add sales figures screen to panel.
		addScreen(salesFiguresVPanel, 15);

		// By default, set panel to have header page in it, but only if we're
		// not displaying a "static" sales display
		if (staticDisplay) {
			simplePanel.setWidget(salesFiguresVPanel);
		} else {
			simplePanel.setWidget(hPageFrame);
		}
		// effectPanel.add(simplePanel);

		// Add effect
		// effectPanel.addEffect(fadeEffect);
		// fadeEffect.setDuration(3.0);
		// fadeEffect.setTransitionType(new LinearTransitionPhysics());

		// Add image and button to the RootPanel
		RootPanel.get().add(simplePanel);

		// effectPanel.playEffects(0.0, 1.0);

		// Start actual RPC
		setLoadingStatus(true);
		service.getSalesExtensions(new AsyncCallback<String[][]>() {
			public void onFailure(Throwable caught) {
				setLoadingStatus(false);
				statusLabel.setText("Failed to update");
			}

			public void onSuccess(String[][] result) {
				setLoadingStatus(false);
				if (result != null) {
					readyToPoll = true;
					GWT.log("Got extensions, populating extension data", null);
					Iterator<ComparisonBarChart> iter = graphs.iterator();
					while (iter.hasNext()) {
						ComparisonBarChart o = iter.next();
						o.setExtensions(result);
					}
					pollData();
				} else {
					statusLabel.setText("Got null result");
				}
			}
		});

		// Set timer to repeatedly poll the server for updates.
		Timer timer = new Timer() {
			public void run() {
				pollData();
			}
		};
		timer.scheduleRepeating(1000 * 60 * UPDATE_INTERVAL_MINUTES);

		if (!staticDisplay) {
			Timer switchScreen = new Timer() {
				public void run() {
					cycleScreen();
				}
			};
			switchScreen.scheduleRepeating(1000 * SCREEN_CHANGE_SECONDS);
		}
	}

	public void pollData() {
		if (readyToPoll) {
			setLoadingStatus(true);

			// Update graph from cache
			// salesforceGraph.setUrl(baseSalesforceImageUrl + "&ts="
			// + System.currentTimeMillis());

			// Make actual RPC call
			service.getSalesData(new AsyncCallback<String[][]>() {
				public void onFailure(Throwable caught) {
					setLoadingStatus(false);
					statusLabel.setText("Failed to update data");
				}

				public void onSuccess(String[][] result) {
					setLoadingStatus(false);
					GWT.log("Data polled", null);
					statusLabel.setText("Data accurate as of "
							+ new Date().toString());
					if (result != null) {
						GWT.log("setting data in chart", null);
						Iterator<ComparisonBarChart> iter = graphs.iterator();
						while (iter.hasNext()) {
							ComparisonBarChart o = iter.next();
							o.setData(result);
						}
					} else {
						GWT.log("NULL data sent back to getSalesData() !?!?!",
								null);
						statusLabel.setText("Got null result for data polling");
					}
				}
			});
		}
	}

	public void setLoadingStatus(boolean loadingStatus) {
		loading.setUrl(loadingStatus ? "images/ajax-loader.gif"
				: "images/transparent.gif");
	}

	/**
	 * Cycle to the next available screen.
	 */
	protected void cycleScreen() {
		currentScreen++;
		if ((currentScreen + 1) > numberOfScreens) {
			currentScreen = 0;
		}
		// Window.alert("Cycling screen to be " +
		// screens.get(currentScreen).toString());
		simplePanel.setWidget(screens.get(currentScreen));
	}

	/**
	 * Add a screen to the stack.
	 * 
	 * @param w
	 */
	protected void addScreen(Widget w) {
		screens.put(numberOfScreens, w);
		numberOfScreens++;
	}

	/**
	 * Add a number of iterations of a widget to the stack.
	 * 
	 * @param w
	 * @param iterations
	 */
	protected void addScreen(Widget w, int iterations) {
		for (int i = 0; i < iterations; i++) {
			addScreen(w);
		}
	}

}

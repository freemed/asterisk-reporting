/**
 * $Id: ComparisonBarChart.java 1890 2010-01-29 22:07:53Z jbuchbinder $
 */

package com.github.freemed.gwt.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.googlecode.gchart.client.GChart;

public class ComparisonBarChart extends GChart {
	protected final Label[] groupLabels = { new Label("Last Hour"),
			new Label("Last Day"), new Label("Last Week (Avg)") };
	protected String[] barColors = { "#FF5555", "#5555FF", "#55FF55", "#FF33FF" };
	protected final int MAX_NUMBER = 5;
	protected final int WIDTH = 400;
	protected final int HEIGHT = 280;
	protected final Label updateTimeMsg = new Label();

	protected String title = "";

	protected String[] barLabels = {};
	protected Integer[] dataIdx = {};
	protected String[][] data = null;

	protected double maxValue = 0.00;

	protected boolean initialized = false;

	/**
	 * Assign extensions from RPC retrieved table.
	 * 
	 * @param exts
	 */
	public void setExtensions(String[][] exts) {
		List<String> eList = new ArrayList<String>();
		for (int iter = 0; iter < exts.length; iter++) {
			eList.add(exts[iter][2]);
		}
		barLabels = (String[]) eList.toArray(new String[0]);
	}

	/**
	 * Assign data from RPC retrieved information.
	 * 
	 * @param newData
	 */
	public void setData(String[][] newData) {
		data = newData;
		updateGraph();
	}

	/**
	 * Set indices in data which are read by the various categories.
	 * 
	 * @param newIndices
	 */
	public void setDataIndices(Integer[] newIndices) {
		dataIdx = newIndices;
	}

	/**
	 * Internal method to update data from loaded data.
	 */
	protected void updateGraph() {
		for (int iCurve = 0; iCurve < barLabels.length; iCurve++) {
			Curve c = null;
			if (!initialized) {
				addCurve();
				c = getCurve();
			} else {
				c = getCurve(iCurve);
			}
			c.getSymbol().setSymbolType(SymbolType.VBAR_SOUTHWEST);
			c.getSymbol().setBackgroundColor(barColors[iCurve]);
			c.setLegendLabel(barLabels[iCurve]);
			c.getSymbol().setModelWidth(1.0);
			c.getSymbol().setBorderColor("#AAA");
			c.getSymbol().setBorderWidth(1);

			for (int jGroup = 0; jGroup < groupLabels.length; jGroup++) {
				int dataPos = dataIdx[jGroup];

				// Push value in
				double value = 0.00;
				try {
					value = Double.parseDouble(data[iCurve][dataPos]);
				} catch (Exception ex) {
					GWT.log("Exception caught in data population, using 0", ex);
				}
				maxValue = (value > maxValue) ? value : maxValue;

				// the '+1' creates a bar-sized gap between groups
				if (!initialized) {
					c.addPoint(1 + iCurve + jGroup * (barLabels.length + 1),
							value);
				} else {
					c.getPoint(jGroup).setY(value);
				}
				getCurve().getPoint().setAnnotationText(null);
			}
		}

		if (!initialized)
			for (int i = 0; i < groupLabels.length; i++) {
				// Center the tick-label horizontally on each group:
				getXAxis().addTick(
						barLabels.length / 2. + i * (barLabels.length + 1),
						groupLabels[i]);
			}

		// Readjust graph if what we have won't fit.
		if (maxValue == 0) {
			maxValue = 100;
		}
		if (getYAxis().getAxisMax() < maxValue) {
			getYAxis().setAxisMax(maxValue);
		}

		// Make sure to force initialized tick on
		initialized = true;

		// Force screen update.
		update();
	}

	/**
	 * Returns Grid with code link, update button, & timing messages. Updates a
	 * single bar group (year) with new simulated data.
	 * 
	 * @param sourceCodeLink
	 * @param updateWidget
	 * @param updateTimeMsg
	 * @return
	 */
	private Grid getDemoFootnotes(String sourceCodeLink, Widget updateWidget,
			Label updateTimeMsg) {
		HTML sourceCode = new HTML(sourceCodeLink);
		Widget[] w = { sourceCode, updateWidget, updateTimeMsg };
		String[] wWidth = { "40%", "20%", "40%" };
		Grid result = new Grid(1, w.length);
		for (int i = 0; i < w.length; i++) {
			result.setWidget(0, i, w[i]);
			result.getCellFormatter().setWidth(0, i, wWidth[i]);
			result.getCellFormatter().setHorizontalAlignment(0, i,
					HasHorizontalAlignment.ALIGN_CENTER);
		}
		result.setWidth(getXChartSizeDecorated() + "px");
		return result;
	}

	/**
	 * Set title displayed on top of graph.
	 */
	public void setTitle(String newTitle) {
		title = newTitle;
		setChartTitle("<big><b>" + title + "<br>&nbsp;</b></big>");
		setChartTitleThickness(40);
	}

	public ComparisonBarChart(String[] barColors) {
		setChartSize(WIDTH, HEIGHT);
		setWidth("100%");

		if (barColors != null) {
			this.barColors = barColors;
		}

		getXAxis().setTickCount(0);
		getXAxis().setTickLength(6); // small tick-like gap...
		getXAxis().setTickThickness(0); // but with invisible ticks
		getXAxis().setAxisMin(0); // keeps first bar on chart
		getXAxis().setTickLabelThickness(20); // adds some space
		getXAxis().setAxisLabelThickness(20);

		getYAxis().setAxisMin(0); // Based on sim revenue range
		getYAxis().setAxisMax(MAX_NUMBER); // of 0 to MAX_REVENUE.
		getYAxis().setTickCount(11);
		getYAxis().setHasGridlines(false);
		getYAxis().setTickLabelFormat("#,###");
		setChartFootnotes(getDemoFootnotes("", new Label(""), updateTimeMsg));
		setChartFootnotesThickness(50);
		update();
		// updateTimeMsg.setText("Chart created.");
	}

}

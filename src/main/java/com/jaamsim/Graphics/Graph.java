/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2012 Ausenco Engineering Canada Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.Graphics;


import java.util.ArrayList;

import com.jaamsim.Samples.SampleListInput;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.ColorListInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class Graph extends GraphBasics  {

	// Key Inputs category

	@Keyword(description = "The number of data points that can be displayed on the graph.\n" +
			" This parameter determines the resolution of the graph.",
	         exampleList = {"200"})
	protected final IntegerInput numberOfPoints;

	@Keyword(description = "The unit type for the primary y-axis.",
	         exampleList = {"DistanceUnit"})
	private final UnitTypeInput unitType;

	@Keyword(description = "One or more sources of data to be graphed on the primary y-axis.\n" +
			"Each source is graphed as a separate line and is specified by an Expression. Also" +
			"acceptable are: a constant value, a Probability Distribution, TimeSeries, or a " +
			"Calculation Object.",
	         exampleList = {"{ [Entity1].Output1 } { [Entity2].Output2 }"})
	protected final SampleListInput dataSource;

	@Keyword(description = "A list of colors for the line series to be displayed.\n" +
			"Each color can be specified by either a color keyword or an RGB value.\n" +
			"For multiple lines, each color must be enclosed in braces.\n" +
			"If only one color is provided, it is used for all the lines.",
	         exampleList = {"{ red } { green }"})
	protected final ColorListInput lineColorsList;

	@Keyword(description = "A list of line widths (in pixels) for the line series to be displayed.\n" +
			"If only one line width is provided, it is used for all the lines.",
	         exampleList = {"2 1"})
	protected final ValueListInput lineWidths;

	@Keyword(description = "The unit type for the secondary y-axis.",
	         exampleList = {"DistanceUnit"})
	private final UnitTypeInput secondaryUnitType;

	@Keyword(description = "One or more sources of data to be graphed on the secondary y-axis.\n" +
			"Each source is graphed as a separate line and is specified by an Expression. Also" +
			"acceptable are: a constant value, a Probability Distribution, TimeSeries, or a " +
			"Calculation Object.",
	         exampleList = {"{ [Entity1].Output1 } { [Entity2].Output2 }"})
	protected final SampleListInput secondaryDataSource;

	@Keyword(description = "A list of colors for the secondary line series to be displayed.\n" +
			"Each color can be specified by either a color keyword or an RGB value.\n" +
			"For multiple lines, each color must be enclosed in braces.\n" +
			"If only one color is provided, it is used for all the lines.",
	         exampleList = {"{ red } { green }"})
	protected final ColorListInput secondaryLineColorsList;

	@Keyword(description = "A list of line widths (in pixels) for the seconardy line series to be displayed.\n" +
			"If only one line width is provided, it is used for all the lines.",
	         exampleList = {"2 1"})
	protected final ValueListInput secondaryLineWidths;

	{
		// Key Inputs category

		numberOfPoints = new IntegerInput("NumberOfPoints", "Key Inputs", 100);
		numberOfPoints.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(numberOfPoints);

		unitType = new UnitTypeInput("UnitType", "Key Inputs", UserSpecifiedUnit.class);
		unitType.setRequired(true);
		this.addInput(unitType);

		dataSource = new SampleListInput("DataSource", "Key Inputs", null);
		dataSource.setUnitType(UserSpecifiedUnit.class);
		dataSource.setEntity(this);
		dataSource.setRequired(true);
		this.addInput(dataSource);

		ArrayList<Color4d> defLineColor = new ArrayList<>(0);
		defLineColor.add(ColourInput.getColorWithName("red"));
		lineColorsList = new ColorListInput("LineColours", "Key Inputs", defLineColor);
		lineColorsList.setValidCountRange(1, Integer.MAX_VALUE);
		this.addInput(lineColorsList);
		this.addSynonym(lineColorsList, "LineColors");

		DoubleVector defLineWidths = new DoubleVector(1);
		defLineWidths.add(1.0);
		lineWidths = new ValueListInput("LineWidths", "Key Inputs", defLineWidths);
		lineWidths.setUnitType(DimensionlessUnit.class);
		lineWidths.setValidCountRange(1, Integer.MAX_VALUE);
		this.addInput(lineWidths);

		secondaryUnitType = new UnitTypeInput("SecondaryUnitType", "Key Inputs", UserSpecifiedUnit.class);
		this.addInput(secondaryUnitType);

		secondaryDataSource = new SampleListInput("SecondaryDataSource", "Key Inputs", null);
		secondaryDataSource.setUnitType(UserSpecifiedUnit.class);
		secondaryDataSource.setEntity(this);
		this.addInput(secondaryDataSource);

		ArrayList<Color4d> defSecondaryLineColor = new ArrayList<>(0);
		defSecondaryLineColor.add(ColourInput.getColorWithName("black"));
		secondaryLineColorsList = new ColorListInput("SecondaryLineColours", "Key Inputs", defSecondaryLineColor);
		secondaryLineColorsList.setValidCountRange(1, Integer.MAX_VALUE);
		this.addInput(secondaryLineColorsList);
		this.addSynonym(secondaryLineColorsList, "SecondaryLineColors");

		DoubleVector defSecondaryLineWidths = new DoubleVector(1);
		defSecondaryLineWidths.add(1.0);
		secondaryLineWidths = new ValueListInput("SecondaryLineWidths", "Key Inputs", defSecondaryLineWidths);
		secondaryLineWidths.setUnitType(DimensionlessUnit.class);
		secondaryLineWidths.setValidCountRange(1, Integer.MAX_VALUE);
		this.addInput(secondaryLineWidths);
	}

	public Graph() {

		timeTrace = true;
		this.setXAxisUnit(TimeUnit.class);
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if (in == unitType) {
			Class<? extends Unit> ut = unitType.getUnitType();
			dataSource.setUnitType(ut);
			this.setYAxisUnit(ut);
			return;
		}

		if (in == secondaryUnitType) {
			Class<? extends Unit> ut = secondaryUnitType.getUnitType();
			showSecondaryYAxis = (ut != UserSpecifiedUnit.class);
			secondaryDataSource.setUnitType(ut);
			this.setSecondaryYAxisUnit(ut);
			return;
		}

		if (in == dataSource) {
			// Hack for backwards compatibility
			// When an entity and output are entered, the unit type will be set automatically
			// FIXME remove when backwards compatibility is no longer required
			if (dataSource.getValue() != null && dataSource.getValue().size() > 0) {
				Class<? extends Unit> ut = dataSource.getValue().get(0).getUnitType();
				if (ut != null && ut != UserSpecifiedUnit.class)
					this.setYAxisUnit(ut);
			}
			return;
		}

		if (in == secondaryDataSource) {
			// Hack for backwards compatibility
			// When an entity and output are entered, the unit type will be set automatically
			// FIXME remove when backwards compatibility is no longer required
			if (secondaryDataSource.getValue() != null && secondaryDataSource.getValue().size() > 0) {
				Class<? extends Unit> ut = secondaryDataSource.getValue().get(0).getUnitType();
				if (ut != null && ut != UserSpecifiedUnit.class) {
					this.setSecondaryYAxisUnit(ut);
					showSecondaryYAxis = (ut != UserSpecifiedUnit.class);
				}
			}
			return;
		}

		if (in == lineColorsList) {
			for (int i = 0; i < primarySeries.size(); ++ i) {
				SeriesInfo info = primarySeries.get(i);
				info.lineColour = getLineColor(i, lineColorsList.getValue());
			}
			return;
		}

		if (in == lineWidths) {
			for (int i = 0; i < primarySeries.size(); ++ i) {
				SeriesInfo info = primarySeries.get(i);
				info.lineWidth = getLineWidth(i, lineWidths.getValue());
			}
			return;
		}

		if (in == secondaryLineColorsList) {
			for (int i = 0; i < secondarySeries.size(); ++ i) {
				SeriesInfo info = secondarySeries.get(i);
				info.lineColour = getLineColor(i, secondaryLineColorsList.getValue());
			}
			return;
		}

		if (in == secondaryLineWidths) {
			for (int i = 0; i < secondarySeries.size(); ++ i) {
				SeriesInfo info = secondarySeries.get(i);
				info.lineWidth = getLineWidth(i, secondaryLineWidths.getValue());
			}
			return;
		}
	}

	@Override
	public void earlyInit(){
		super.earlyInit();

		primarySeries.clear();
		secondarySeries.clear();

		// Populate the primary series data structures
		populateSeriesInfo(primarySeries, dataSource);
		populateSeriesInfo(secondarySeries, secondaryDataSource);
	}

	private void populateSeriesInfo(ArrayList<SeriesInfo> infos, SampleListInput data) {
		ArrayList<SampleProvider> sampList = data.getValue();
		if( sampList == null )
			return;
		for (int i = 0; i < sampList.size(); ++i) {
			SeriesInfo info = new SeriesInfo();
			info.samp = sampList.get(i);
			info.yValues = new double[numberOfPoints.getValue()];
			info.xValues = new double[numberOfPoints.getValue()];

			infos.add(info);
		}
	}

	@Override
	public void startUp() {
		super.startUp();
		extraStartGraph();

		for (int i = 0; i < primarySeries.size(); ++ i) {
			SeriesInfo info = primarySeries.get(i);
			info.lineColour = getLineColor(i, lineColorsList.getValue());
			info.lineWidth = getLineWidth(i, lineWidths.getValue());
		}

		for (int i = 0; i < secondarySeries.size(); ++i) {
			SeriesInfo info = secondarySeries.get(i);
			info.lineColour = getLineColor(i, secondaryLineColorsList.getValue());
			info.lineWidth = getLineWidth(i, secondaryLineWidths.getValue());
		}


		double xLength = xAxisEnd.getValue() - xAxisStart.getValue();
		double xInterval = xLength/(numberOfPoints.getValue() -1);

		for (SeriesInfo info : primarySeries) {
			setupSeriesData(info, xLength, xInterval);
		}

		for (SeriesInfo info : secondarySeries) {
			setupSeriesData(info, xLength, xInterval);
		}

		processGraph();
	}

	/**
	 * Hook for sub-classes to do some processing at startup
	 */
	protected void extraStartGraph() {}

	protected Color4d getLineColor(int index, ArrayList<Color4d> colorList) {
		index = Math.min(index, colorList.size()-1);
		return colorList.get(index);
	}

	protected double getLineWidth(int index, DoubleVector widthList) {
		index = Math.min(index, widthList.size()-1);
		return widthList.get(index);
	}

	/**
	 * Initialize the data for the specified series
	 */
	private void setupSeriesData(SeriesInfo info, double xLength, double xInterval) {

		info.numPoints = 0;

		for( int i = 0; i * xInterval < xAxisEnd.getValue(); i++ ) {
			double t = i * xInterval;
			info.numPoints++;
			info.xValues[info.numPoints] = t;
			info.yValues[info.numPoints] = this.getCurrentValue(t, info);
		}
	}

	/**
	 * A hook method for descendant graph types to grab some processing time
	 */
	protected void extraProcessing() {}

	private static class ProcessGraphTarget extends ProcessTarget {
		final Graph graph;

		ProcessGraphTarget(Graph graph) {
			this.graph = graph;
		}

		@Override
		public String getDescription() {
			return graph.getName() + ".processGraph";
		}

		@Override
		public void process() {
			graph.processGraph();
		}
	}

	private final ProcessTarget processGraph = new ProcessGraphTarget(this);

	/**
	 * Calculate values for the data series on the graph
	 */
	public void processGraph() {
		// Give processing time to sub-classes
		extraProcessing();

		// stop the processing loop
		if (primarySeries.isEmpty() && secondarySeries.isEmpty())
			return;

		// Calculate values for the primary y-axis
		for (SeriesInfo info : primarySeries) {
			processGraph(info);
		}

		// Calculate values for the secondary y-axis
		for (SeriesInfo info : secondarySeries) {
			processGraph(info);
		}

		double xLength = xAxisEnd.getValue() - xAxisStart.getValue();
		double xInterval = xLength / (numberOfPoints.getValue() - 1);
		scheduleProcess(xInterval, 7, processGraph);
	}

	/**
	 * Calculate values for the data series on the graph
	 * @param info - the information for the series to be rendered
	 */
	public void processGraph(SeriesInfo info) {

		// Entity has been removed
		if (info.samp == null) {
			return;
		}

		double t = getSimTime() + xAxisEnd.getValue();
		double presentValue = this.getCurrentValue(t, info);
		if (info.numPoints < info.yValues.length) {
			info.xValues[info.numPoints] = t;
			info.yValues[info.numPoints] = presentValue;
			info.numPoints++;
		}
		else {
			System.arraycopy(info.xValues, 1, info.xValues, 0, info.xValues.length - 1);
			System.arraycopy(info.yValues, 1, info.yValues, 0, info.yValues.length - 1);
			info.xValues[info.xValues.length - 1] = t;
			info.yValues[info.yValues.length - 1] = presentValue;
		}
	}

	/**
	 * Return the current value for the series
	 * @return double
	 */
	protected double getCurrentValue(double simTime, SeriesInfo info) {
		return info.samp.getNextSample(simTime);
	}

	public ArrayList<SeriesInfo> getPrimarySeries() {
		return primarySeries;
	}

	public ArrayList<SeriesInfo> getSecondarySeries() {
		return secondarySeries;
	}

	public int getNumberOfPoints() {
		return numberOfPoints.getValue();
	}

}

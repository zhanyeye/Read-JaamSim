/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016 JaamSim Software Inc.
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
package com.jaamsim.ProbabilityDistributions;

import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.TimeSeriesInput;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

/**
 * Non-Stationary Exponential Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 5th Edition", page 479.
 */
public class NonStatExponentialDist extends Distribution {

	@Keyword(description = "A time series containing the expected cumulative number of arrivals as a function of time.",
			exampleList = {"TimeSeries1"})
	private final TimeSeriesInput expectedArrivals;

	private final MRG1999a rng = new MRG1999a();

	{
		minValueInput.setDefaultValue(new SampleConstant(0.0d));

		unitType.setHidden(true);
		unitType.setDefaultValue(TimeUnit.class);
		this.setUnitType(TimeUnit.class);

		expectedArrivals = new TimeSeriesInput("ExpectedArrivals", "Key Inputs", null);
		expectedArrivals.setUnitType(DimensionlessUnit.class);
		expectedArrivals.setRequired(true);
		this.addInput(expectedArrivals);
	}

	public NonStatExponentialDist() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected double getSample(double simTime) {

		long ticksNow = getSimTicks();  // ignore the simTime passed as an argument
		double valueNow = expectedArrivals.getValue().getInterpolatedCumulativeValueForTicks(ticksNow);
		double valueNext = valueNow - Math.log(rng.nextUniform());
		long ticksNext = expectedArrivals.getValue().getInterpolatedTicksForValue(valueNext);

		if (ticksNext == Long.MAX_VALUE)
			return Double.POSITIVE_INFINITY;

		if (ticksNext < ticksNow)
			error("Negative time advance");

		return EventManager.ticksToSecs(ticksNext - ticksNow);
	}

	@Override
	protected double getMean(double simTime) {
		double arrivals = expectedArrivals.getValue().getMaxValue();
		double dt = EventManager.ticksToSecs( expectedArrivals.getValue().getMaxTicksValue() );
		return dt/arrivals;
	}

	@Override
	protected double getStandardDev(double simTime) {
		return 0.0d;
	}

}

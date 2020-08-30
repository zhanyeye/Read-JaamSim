/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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

import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * Triangular Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", page 457.
 */
public class TriangularDistribution extends Distribution {

	@Keyword(description = "The mode of the triangular distribution, i.e. the value with the highest probability.",
	         exampleList = {"5.0"})
	private final ValueInput modeInput;

	private final MRG1999a rng = new MRG1999a();

	{
		minValueInput.setDefaultValue(0.0d);
		maxValueInput.setDefaultValue(2.0d);

		modeInput = new ValueInput("Mode", "Key Inputs", 1.0d);
		modeInput.setUnitType(UserSpecifiedUnit.class);
		this.addInput(modeInput);
	}

	public TriangularDistribution() {}

	@Override
	public void validate() {
		super.validate();

		// The mode must be between the minimum and maximum values
		if( this.getMinValue() > modeInput.getValue() ) {
			throw new InputErrorException( "The input for Mode must be >= than that for MinValue.");
		}
		if( this.getMaxValue() < modeInput.getValue() ) {
			throw new InputErrorException( "The input for Mode must be <= than that for MaxValue.");
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected void setUnitType(Class<? extends Unit> specified) {
		super.setUnitType(specified);
		modeInput.setUnitType(specified);
	}

	@Override
	protected double getNextSample() {

		double sample;
		double min = this.getMinValue();
		double max = this.getMaxValue();

		// Select the random value
		double rand = rng.nextUniform();

		// Calculate the normalised mode
		double m = ( modeInput.getValue() - min )/ ( max - min );

		// Use the inverse transform method to calculate the normalised random sample
		// (triangular distribution with min = 0, max = 1, and mode = m)
		if( rand <= m ) {
			sample = Math.sqrt( m * rand );
		}
		else {
			sample = 1.0 - Math.sqrt( ( 1.0 - m )*( 1.0 - rand ) );
		}

		// Adjust for the desired min and max values
		return  min + sample * ( max - min );
	}

	@Override
	protected double getMeanValue() {
		return ( ( this.getMinValue() + modeInput.getValue() + this.getMaxValue() ) / 3.0 );
	}

	@Override
	protected double getStandardDeviation() {
		double a = this.getMinValue();
		double b = this.getMaxValue();
		double m = modeInput.getValue();
		return  Math.sqrt( ( a*a + b*b + m*m - a*b - a*m - b*m ) / 18.0 );
	}
}

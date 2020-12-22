/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.CalculationObjects;

import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleListInput;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * The WeightedSum object returns a weighted sum of its input values.
 * @author Harry King
 *
 */
public class WeightedSum extends DisplayEntity implements SampleProvider {

	@Keyword(description = "The unit type for the inputs to the weighted sum and for the value "
	                     + "returned.",
	         exampleList = {"DistanceUnit"})
	protected final UnitTypeInput unitType;

	@Keyword(description = "The list of inputs to the weighted sum. All inputs must have the "
	                     + "same unit type.\n"
	                     + "The inputs can be any entity that returns a number, such as an "
	                     + "expression, a CalculationObject, a ProbabilityDistribution, or a "
	                     + "TimeSeries.",
	         exampleList = {"{ 1 m } { TimeSeries1 } {'2[m] * [Queue1].QueueLength'}"})
	private final SampleListInput inputValueList;

	@Keyword(description = "The list of dimensionless coefficients to be applied to the input "
	                     + "values. If left blank, the input values are simply added without "
	                     + "applying any coefficients.",
	         exampleList = {"2.0  1.5"})
	private final ValueListInput coefficientList;

	{
		unitType = new UnitTypeInput("UnitType", "Key Inputs", UserSpecifiedUnit.class);
		unitType.setRequired(true);
		this.addInput(unitType);

		ArrayList<SampleProvider> def = new ArrayList<>();
 		def.add(new SampleConstant(0.0));
 		inputValueList = new SampleListInput("InputValueList", "Key Inputs", def);
		inputValueList.setUnitType(UserSpecifiedUnit.class);
		this.addInput(inputValueList);

		coefficientList = new ValueListInput("CoefficientList", "Key Inputs", null);
		coefficientList.setUnitType(DimensionlessUnit.class);
		this.addInput(coefficientList);
	}

	public WeightedSum() {}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == unitType) {
			inputValueList.setUnitType(unitType.getUnitType());
			FrameBox.reSelectEntity();  // Update the units in the Output Viewer
			return;
		}
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType.getUnitType();
	}

	@Override
	public Class<? extends Unit> getUserUnitType() {
		return unitType.getUnitType();
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the number of entries in the CoeffientList matches the EntityList
		if (coefficientList.getValue() != null
				&& coefficientList.getValue().size() != inputValueList.getValue().size()) {
			throw new InputErrorException("If set, the number of entries for CoefficientList "
					+ "must match the entries for InputValueList");
		}
	}

	@Override
	@Output(name = "Value",
	 description = "The calculated value for the weighted sum.",
	    unitType = UserSpecifiedUnit.class)
	public double getNextSample(double simTime) {
		double val = 0.0;

		// Calculate the unweighted sum of the inputs
		if (coefficientList.getValue() == null) {
			for (int i=0; i<inputValueList.getValue().size(); i++) {
				val += inputValueList.getValue().get(i).getNextSample(simTime);
			}
		}

		// Calculate the weighted sum of the inputs
		else {
			for (int i=0; i<inputValueList.getValue().size(); i++) {
				val += coefficientList.getValue().get(i)
						* inputValueList.getValue().get(i).getNextSample(simTime);
			}
		}

		return val;
	}

	@Override
	public double getMeanValue(double simTime) {
		return 0;
	}

	@Override
	public double getMinValue() {
		return Double.NEGATIVE_INFINITY;
	}

	@Override
	public double getMaxValue() {
		return Double.POSITIVE_INFINITY;
	}

}

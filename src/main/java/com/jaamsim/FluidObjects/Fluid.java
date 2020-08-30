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
package com.jaamsim.FluidObjects;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.units.AccelerationUnit;
import com.jaamsim.units.DensityUnit;
import com.jaamsim.units.ViscosityUnit;

/**
 * Fluid defines the properties of the fluid being used in a hydraulic calculation.
 * @author Harry King
 *
 */
public class Fluid extends DisplayEntity {

	@Keyword(description = "The density of the fluid (default = water).",
	         example = "Fluid1 Density { 1000 kg/m3 }")
	private final ValueInput densityInput;

	@Keyword(description = "The dynamic viscosity of the fluid (default = water).",
	         example = "Fluid1 Viscosity { 0.001002 Pa-s }")
	private final ValueInput viscosityInput;

	@Keyword(description = "The colour of the product, defined using a colour keyword or RGB values.",
	         example = "Fluid1 Colour { red }")
	private final ColourInput colourInput;

	@Keyword(description = "The acceleration of gravity.",
	         example = "Fluid1 Gravity { 9.81 m/s2 }")
	private final ValueInput gravityInput;

	{
		densityInput = new ValueInput( "Density", "Key Inputs", 1000.0d);
		densityInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		densityInput.setUnitType( DensityUnit.class );
		this.addInput( densityInput);

		viscosityInput = new ValueInput( "Viscosity", "Key Inputs", 0.001002d);
		viscosityInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		viscosityInput.setUnitType( ViscosityUnit.class );
		this.addInput( viscosityInput);

		colourInput = new ColourInput( "Colour", "Key Inputs", ColourInput.RED);
		this.addInput(colourInput);
		this.addSynonym(colourInput, "Color");

		gravityInput = new ValueInput( "Gravity", "Key Inputs", 9.81d);
		gravityInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		gravityInput.setUnitType( AccelerationUnit.class );
		this.addInput( gravityInput);
	}

	public double getDensity() {
		return densityInput.getValue();
	}

	public double getViscosity() {
		return viscosityInput.getValue();
	}

	public Color4d getColour() {
		return colourInput.getValue();
	}

	public double getGravity() {
		return gravityInput.getValue();
	}

	public double getDensityxGravity() {
		return densityInput.getValue() * gravityInput.getValue();
	}

	public double getKinematicViscosity() {
		return viscosityInput.getValue() / densityInput.getValue();
	}
}

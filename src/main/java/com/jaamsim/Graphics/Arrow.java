/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2011 Ausenco Engineering Canada Inc.
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

import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;

public class Arrow extends DisplayEntity {

	@Keyword(description = "The width of the Arrow line segments in pixels.",
	         exampleList = {"1"})
	private final ValueInput width;

	@Keyword(description = "A set of { x, y, z } numbers that define the size of the arrowhead " +
	                "in those directions at the end of the connector.",
	         exampleList = {"0.165 0.130 0.0 m"})
	private final Vec3dInput arrowHeadSize;

	@Keyword(description = "The colour of the arrow, defined using a colour keyword or RGB values.",
	         exampleList = {"red"})
	private final ColourInput color;

	{
		width = new ValueInput("Width", "Graphics", 1.0d);
		width.setUnitType(DimensionlessUnit.class);
		width.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(width);

		arrowHeadSize = new Vec3dInput( "ArrowSize", "Graphics", new Vec3d(0.1d, 0.1d, 0.0d) );
		arrowHeadSize.setUnitType(DistanceUnit.class);
		this.addInput( arrowHeadSize );

		color = new ColourInput("Color", "Graphics", ColourInput.BLACK);
		this.addInput(color);
		this.addSynonym(color, "Colour");
	}

	public Arrow() {}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput(in);

		// If Points were input, then use them to set the start and end coordinates
		if( in == pointsInput || in == color || in == width ) {
			invalidateScreenPoints();
			return;
		}
	}

	@Override
	public PolylineInfo[] buildScreenPoints() {
		int w = Math.max(1, width.getValue().intValue());
		PolylineInfo[] ret = new PolylineInfo[1];
		ret[0] = new PolylineInfo(pointsInput.getValue(), color.getValue(), w);
		return ret;
	}

	public Vec3d getArrowHeadSize() {
		return arrowHeadSize.getValue();
	}

}

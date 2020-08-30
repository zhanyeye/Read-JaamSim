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
package com.jaamsim.Samples;

import com.jaamsim.units.Unit;

public class SampleConstant implements SampleProvider {
	private Class<? extends Unit> unitType;
	private final double val;

	public SampleConstant(Class<? extends Unit> unitType, double val) {
		this.unitType = unitType;
		this.val = val;
	}

	public SampleConstant(double val) {
		this.unitType = Unit.class;
		this.val = val;
	}

	void setUnitType(Class<? extends Unit> ut) {
		unitType = ut;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType;
	}

	@Override
	public double getNextSample(double simTime) {
		return val;
	}

	@Override
	public double getMeanValue(double simTime) {
		return val;
	}

	@Override
	public double getMinValue() {
		return val;
	}

	@Override
	public double getMaxValue() {
		return val;
	}

	@Override
	public String toString() {
		StringBuilder tmp = new StringBuilder();
		tmp.append(val/Unit.getDisplayedUnitFactor(unitType));
		return tmp.toString();
	}
}

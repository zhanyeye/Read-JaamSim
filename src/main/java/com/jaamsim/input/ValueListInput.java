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
package com.jaamsim.input;

import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class ValueListInput extends ListInput<DoubleVector> {
	private Class<? extends Unit> unitType = DimensionlessUnit.class;
	private double minValue = Double.NEGATIVE_INFINITY;
	private double maxValue = Double.POSITIVE_INFINITY;
	private double sumValue = Double.NaN;
	private double sumTolerance = 1e-10d;
	private int[] validCounts = null; // valid list sizes not including units
	private int monotonic = 0;  // -1 = monotonically decreasing, +1 = monotonically increasing

	public ValueListInput(String key, String cat, DoubleVector def) {
		super(key, cat, def);
	}

	public void setUnitType(Class<? extends Unit> units) {
		if (units != unitType)
			this.reset();
		unitType = units;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		DoubleVector temp = Input.parseDoubles(kw, minValue, maxValue, unitType);
		Input.assertCount(temp, validCounts);
		Input.assertCountRange(temp, minCount, maxCount);
		Input.assertMonotonic(temp, monotonic);
		if (!Double.isNaN(sumValue))
			Input.assertSumTolerance(temp, sumValue, sumTolerance);

		value = temp;
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	public void setValidRange(double min, double max) {
		minValue = min;
		maxValue = max;
	}

	public void setValidSum(double sum, double tol) {
		sumValue = sum;
		sumTolerance = tol;
	}

	public void setValidCounts(int... list) {
		validCounts = list;
	}

	public void setMonotonic(int dir) {
		monotonic = dir;
	}

	@Override
	public String getDefaultString() {
		if (defValue == null || defValue.size() == 0)
			return "";

		StringBuilder tmp = new StringBuilder();
		for (int i = 0; i < defValue.size(); i++) {
			if (i > 0)
				tmp.append(SEPARATOR);
			tmp.append(defValue.get(i));
		}

		if (unitType != Unit.class) {
			tmp.append(SEPARATOR);
			tmp.append(Unit.getSIUnit(unitType));
		}

		return tmp.toString();
	}
}

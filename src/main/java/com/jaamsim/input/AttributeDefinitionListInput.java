/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import java.util.ArrayList;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

/**
 * AttributeDefinitionListInput is an object for parsing inputs consisting of a list of
 * Attribute definitions using the syntax:
 * Entity AttributeDefinitionList { { AttibuteName1 Value1 Unit1 } { AttibuteName2 Value2 Unit2 } ... }
 * @author Harry King
 */
public class AttributeDefinitionListInput extends ListInput<ArrayList<AttributeHandle>> {

	private Entity ent;

	public AttributeDefinitionListInput(Entity e, String key, String cat, ArrayList<AttributeHandle> def) {
		super(key, cat, def);
		ent = e;
	}

	@Override
	public void parse(KeywordIndex kw) throws InputErrorException {

		// Divide up the inputs by the inner braces
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		ArrayList<AttributeHandle> temp = new ArrayList<>(subArgs.size());

		// Parse the inputs within each inner brace
		for (int i = 0; i < subArgs.size(); i++) {
			KeywordIndex subArg = subArgs.get(i);
			Input.assertCount(subArg, 2, 3);
			try {
				// Parse the attribute name
				String name = subArg.getArg(0);
				if (OutputHandle.hasOutput(ent.getClass(), name)) {
					throw new InputErrorException("Attribute name is the same as existing output name: %s", name);
				}

				// Parse the unit type
				double factor = 1.0;
				Class<? extends Unit> unitType = DimensionlessUnit.class;
				if (subArg.numArgs() == 3) {
					Unit unit = Input.parseUnit(subArg.getArg(2));
					unitType = unit.getClass();
					factor = unit.getConversionFactorToSI();
				}

				// Parse the initial value
				double val;
				try {
					val = factor * Double.valueOf(subArg.getArg(1));
				} catch (Exception e) {
					throw new InputErrorException(INP_ERR_DOUBLE, subArg.getArg(1));
				}

				// Save the data for this attribute
				AttributeHandle h = (AttributeHandle) ent.getOutputHandle(name);
				if (h == null)
					h = new AttributeHandle(ent, name);
				h.setUnitType(unitType);
				h.setInitialValue(val);
				h.setValue(val);
				temp.add(h);

			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i+1, e.getMessage());
			}
		}

		// Save the data for each attribute
		value = temp;
	}

	@Override
	public void copyFrom(Input<?> in) {
		super.copyFrom(in);
		value = new ArrayList<>();
		@SuppressWarnings("unchecked")
		ArrayList<AttributeHandle> inValue = (ArrayList<AttributeHandle>) (in.value);
		for (AttributeHandle h : inValue) {
			AttributeHandle hNew = new AttributeHandle(ent, h.getName());
			hNew.setUnitType(h.getUnitType());
			hNew.setInitialValue(h.getInitialValue());
			hNew.setValue(h.getValueAsDouble(0.0d, 0.0d));
			value.add(hNew);
		}
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	@Override
	public String getDefaultString() {
		if (defValue == null || defValue.isEmpty()) return "";
		return this.getInputString(defValue);
	}

	private String getInputString(ArrayList<AttributeHandle> handleList) {

		StringBuilder tmp = new StringBuilder();
		for (int i = 0; i < handleList.size(); i++) {
			if (i > 0) tmp.append(SEPARATOR);
			AttributeHandle h = handleList.get(i);
			tmp.append("{ ");
			tmp.append(h.getName());
			tmp.append(SEPARATOR);

			double val = h.getInitialValue();
			String unitString = Unit.getSIUnit(h.getUnitType());

			// Check for a preferred unit
			Unit unit = Unit.getPreferredUnit(h.getUnitType());
			if (unit != null) {
				unitString = unit.toString();
				val = h.getValueAsDouble(0.0d, 0.0d, unit);
			}
			tmp.append(val);

			// Print the unit unless it is dimensionless
			if (h.getUnitType() != DimensionlessUnit.class) {
				tmp.append(SEPARATOR);
				tmp.append(unitString);
			}
			tmp.append(" }");
		}
		return tmp.toString();
	}

}

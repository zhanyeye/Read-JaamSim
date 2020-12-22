/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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

import java.util.ArrayList;
import java.util.Collections;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.ListInput;
import com.jaamsim.units.Unit;

public class SampleListInput extends ListInput<ArrayList<SampleProvider>> {

	private ArrayList<Class<? extends Unit>> unitTypeList;
	private Entity thisEnt;
	private double minValue = Double.NEGATIVE_INFINITY;
	private double maxValue = Double.POSITIVE_INFINITY;

	public SampleListInput(String key, String cat, ArrayList<SampleProvider> def) {
		super(key, cat, def);
		unitTypeList = new ArrayList<>();
	}

	public void setUnitTypeList(ArrayList<Class<? extends Unit>> utList) {

		if (utList.equals(unitTypeList))
			return;

		// Save the new unit types
		unitTypeList = new ArrayList<>(utList);
		this.setValid(false);

		// Set the units for the default value column in the Input Editor
		if (defValue == null)
			return;
		for (int i=0; i<defValue.size(); i++) {
			SampleProvider p = defValue.get(i);
			if (p instanceof SampleConstant)
				((SampleConstant) p).setUnitType(getUnitType(i));
		}
	}

	public void setUnitType(Class<? extends Unit> u) {
		ArrayList<Class<? extends Unit>> utList = new ArrayList<>(1);
		utList.add(u);
		this.setUnitTypeList(utList);
	}

	/**
	 * Returns the unit type for the specified expression.
	 * <p>
	 * If the number of expressions exceeds the number of unit types
	 * then the last unit type in the list is returned.
	 * @param i - index of the expression
	 * @return unit type for the expression
	 */
	public Class<? extends Unit> getUnitType(int i) {
		if (unitTypeList.isEmpty())
			return null;
		int k = Math.min(i, unitTypeList.size()-1);
		return unitTypeList.get(k);
	}

	public void setEntity(Entity ent) {
		thisEnt = ent;
	}

	public void setValidRange(double min, double max) {
		minValue = min;
		maxValue = max;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		ArrayList<SampleProvider> temp = new ArrayList<>(subArgs.size());
		for (int i = 0; i < subArgs.size(); i++) {
			KeywordIndex subArg = subArgs.get(i);
			try {
				SampleProvider sp = Input.parseSampleExp(subArg, thisEnt, minValue, maxValue, getUnitType(i));
				temp.add(sp);
			}
			catch (InputErrorException e) {
				if (subArgs.size() == 1)
					throw new InputErrorException(e.getMessage());
				else
					throw new InputErrorException(INP_ERR_ELEMENT, i+1, e.getMessage());
			}
		}
		value = temp;
		this.setValid(true);
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<>();
		for (Entity each : Entity.getClonesOfIterator(Entity.class, SampleProvider.class)) {
			SampleProvider samp = (SampleProvider)each;
			if (unitTypeList.contains(samp.getUnitType()))
				list.add(each.getName());
		}
		Collections.sort(list, Input.uiSortOrder);
		return list;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null) return;

		for (int i = 0; i < value.size(); i++) {
			toks.add("{");
			toks.add(value.get(i).toString());
			toks.add("}");
		}
	}

	@Override
	public void validate() {
		super.validate();

		if (value == null)
			return;

		for (int i=0; i<value.size(); i++) {
			SampleProvider sp = value.get(i);

			if (sp instanceof SampleExpression) continue;
			if (sp instanceof SampleConstant) continue;

			Input.assertUnitsMatch(sp.getUnitType(), getUnitType(i));

			if (sp.getMinValue() < minValue)
				throw new InputErrorException("The minimum value allowed for keyword: '%s' is: %s.\n" +
						"The specified entity: '%s' can return values as small as: %s.",
						this.getKeyword(), minValue, ((Entity)sp).getName(), sp.getMinValue());

			if (sp.getMaxValue() > maxValue)
				throw new InputErrorException("The maximum value allowed for keyword: '%s' is: %s.\n" +
						"The specified entity: '%s' can return values as large as: %s.",
						this.getKeyword(), maxValue, ((Entity)sp).getName(), sp.getMaxValue());
		}
	}

	@Override
	public String getDefaultString() {
		if (defValue == null || defValue.size() == 0) {
			return "";
		}

		StringBuilder tmp = new StringBuilder();
		for (int i = 0; i < defValue.size(); i++) {
			if (i > 0)
				tmp.append(SEPARATOR);

			tmp.append("{ ");
			tmp.append(defValue.get(i));
			tmp.append(" }");
		}

		return tmp.toString();
	}

}

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
package com.jaamsim.StringProviders;

import java.util.ArrayList;
import java.util.Collections;

import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.units.Unit;

public class StringProvInput extends Input<StringProvider> {

	private Class<? extends Unit> unitType;
	private Entity thisEnt;

	public StringProvInput(String key, String cat, StringProvider def) {
		super(key, cat, def);
		unitType = null;
		thisEnt = null;
	}

	public void setUnitType(Class<? extends Unit> ut) {
		if (ut != unitType)
			this.reset();
		unitType = ut;
	}

	public void setEntity(Entity ent) {
		thisEnt = ent;
	}

	@Override
	public void parse(KeywordIndex kw) throws InputErrorException {
		value = Input.parseStringProvider(kw, thisEnt, unitType);
	}

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<>();
		for (Entity each : Entity.getClonesOfIterator(Entity.class, SampleProvider.class)) {
			SampleProvider sp = (SampleProvider)each;
			if (sp.getUnitType() == unitType)
				list.add(each.getName());
		}
		Collections.sort(list);
		return list;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null) return;

		if (value instanceof StringProvOutput) {
			toks.add(value.toString());
			return;
		}

		super.getValueTokens(toks);
	}

}

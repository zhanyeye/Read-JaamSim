/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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

public class EntityListListInput<T extends Entity> extends ListInput<ArrayList<ArrayList<T>>> {
	private Class<T> entClass;
	private boolean unique; // flag to determine if inner lists must be unique or not

	public EntityListListInput(Class<T> aClass, String key, String cat, ArrayList<ArrayList<T>> def) {
		super(key, cat, def);
		entClass = aClass;
		unique = true;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		// Check if number of outer lists violate minCount or maxCount
		if (subArgs.size() < minCount || subArgs.size() > maxCount)
			throw new InputErrorException(INP_ERR_RANGECOUNT, minCount, maxCount, kw.argString());

		value = Input.parseListOfEntityLists(kw, entClass, unique);
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	@Override
	public String getDefaultString() {
		if (defValue == null)
			return "";

		if (defValue.size() ==0)
			return "";

		StringBuilder tmp = new StringBuilder();
		for (ArrayList<T> each: defValue) {

			// blank space between elements
			if (tmp.length() > 0)
				tmp.append(SEPARATOR);

			if (each == null) {
				tmp.append("");
				continue;
			}
			if (each.isEmpty()) {
				tmp.append("");
				continue;
			}

			tmp.append("{");
			tmp.append(SEPARATOR);
			for (T ent:each) {
				tmp.append(ent.getName());
				tmp.append(SEPARATOR);
			}
			tmp.append("}");
		}
		return tmp.toString();
	}
}

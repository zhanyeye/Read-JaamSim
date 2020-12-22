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
package com.jaamsim.input;

import java.util.ArrayList;
import java.util.Collections;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.EntityLabel;
import com.jaamsim.Graphics.OverlayEntity;
import com.jaamsim.Graphics.Region;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.EntityInput;

public class RelativeEntityInput extends EntityInput<DisplayEntity> {

	private DisplayEntity thisEnt;  // entity that owns this input

	public RelativeEntityInput(String key, String cat, DisplayEntity def) {
		super(DisplayEntity.class, key, cat, def);
	}

	public void setEntity(DisplayEntity ent) {
		thisEnt = ent;
	}

	@Override
	public void parse(KeywordIndex kw) throws InputErrorException {
		Input.assertCount(kw, 1);
		DisplayEntity ent = Input.parseEntity(kw.getArg(0), DisplayEntity.class);
		if (isCircular(ent))
			throw new InputErrorException("The assignment of %s to RelativeEntity would create a circular loop.", ent);
		value = ent;
	}

	private boolean isCircular(DisplayEntity ent) {
		while (ent != null) {
			if (ent == thisEnt)
				return true;
			ent = ent.getRelativeEntity();
		}
		return false;
	}

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<>();
		for (DisplayEntity each: Entity.getClonesOfIterator(DisplayEntity.class)) {
			if (each.testFlag(Entity.FLAG_GENERATED))
				continue;

			if (each instanceof OverlayEntity || each instanceof Region || each instanceof EntityLabel)
				continue;

			if (isCircular(each))
				continue;

			list.add(each.getName());
		}
		Collections.sort(list, Input.uiSortOrder);
		return list;
	}

}

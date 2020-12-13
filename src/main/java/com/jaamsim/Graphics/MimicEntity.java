/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017 JaamSim Software Inc.
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

import java.util.ArrayList;

import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.EntityProviders.EntityProvInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.render.DisplayModelBinding;

public class MimicEntity extends DisplayEntity {

	@Keyword(description = "The entity whose graphics are to be copied.",
	         exampleList = {"Server1", "this.ent"})
	private final EntityProvInput<DisplayEntity> sourceEntity;

	private ArrayList<DisplayModelBinding> sourceBindings;

	{
		sourceEntity = new EntityProvInput<>(DisplayEntity.class, "SourceEntity", KEY_INPUTS, null);
		sourceEntity.addInvalidClass(MimicEntity.class);
		sourceEntity.addInvalidClass(TextBasics.class);
		sourceEntity.addInvalidClass(OverlayEntity.class);
		this.addInput(sourceEntity);
	}

	public MimicEntity() {}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == sourceEntity) {
			this.clearBindings();
			return;
		}
	}

	@Override
	public ArrayList<DisplayModel> getDisplayModelList() {
		if (sourceEntity.getValue() == null) {
			return super.getDisplayModelList();
		}
		return sourceEntity.getValue().getNextEntity(0.0d).getDisplayModelList();
	}

	@Override
	public ArrayList<DisplayModelBinding> getDisplayBindings() {
		if (sourceEntity.getValue() != null) {
			DisplayEntity ent = sourceEntity.getValue().getNextEntity(0.0d);
			if (sourceBindings != ent.getDisplayBindings()) {
				sourceBindings = ent.getDisplayBindings();
				this.clearBindings();
			}
		}
		return super.getDisplayBindings();
	}

}

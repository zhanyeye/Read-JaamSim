/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016 JaamSim Software Inc.
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
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.input.ExpParser.Expression;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class NamedExpressionListInput extends ListInput<ArrayList<NamedExpression>> {

	private final Entity ent;

	public NamedExpressionListInput(Entity e, String key, String cat,
			ArrayList<NamedExpression> def) {
		super(key, cat, def);
		ent = e;
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	@Override
	public void parse(KeywordIndex kw) throws InputErrorException {

		// Divide up the inputs by the inner braces
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		ArrayList<NamedExpression> temp = new ArrayList<>(subArgs.size());

		// Parse the inputs within each inner brace
		for (int i = 0; i < subArgs.size(); i++) {
			KeywordIndex subArg = subArgs.get(i);
			Input.assertCount(subArg, 2, 3);
			try {
				// Parse the expression name
				String name = subArg.getArg(0);
				if (OutputHandle.hasOutput(ent.getClass(), name)) {
					throw new InputErrorException("Expression name is the same as existing output name: %s", name);
				}

				String expString = subArg.getArg(1);
				Expression exp = ExpParser.parseExpression(ExpEvaluator.getParseContext(ent, expString), expString);

				Class<? extends Unit> unitType = DimensionlessUnit.class;
				if (subArg.numArgs() == 3) {
					ObjectType t = Input.parseEntity(subArg.getArg(2), ObjectType.class);
					unitType = Input.checkCast(t.getJavaClass(), Unit.class);
				}

				// Save the data for this expression
				NamedExpression ne = new NamedExpression(name, exp, unitType);
				temp.add(ne);

			} catch (ExpError e) {
				InputAgent.logStackTrace(e);
				throw new InputErrorException(e.toString());
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i+1, e.getMessage());
			}
		}

		// Save the data for each attribute
		value = temp;
	}

}

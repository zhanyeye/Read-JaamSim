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

import com.jaamsim.basicsim.Entity;

public class AttributeHandle extends OutputHandle {
	private final String attributeName;
	private double initialValue;
	private double value;

	public AttributeHandle(Entity e, String outputName) {
		super(e);
		this.attributeName = outputName;
	}

	public void setInitialValue(double val) {
		initialValue = val;
	}

	public double getInitialValue() {
		return initialValue;
	}

	public void setValue(double val) {
		value = val;
	}

	@Override
	public <T> T getValue(double simTime, Class<T> klass) {
		if (!ent.hasAttribute(attributeName)) {
			return null;
		}
		if (!double.class.equals(klass)) {
			return null;
		}
		return klass.cast(value);
	}
	@Override
	public double getValueAsDouble(double simTime, double def) {
		return value;
	}

	@Override
	public Class<?> getReturnType() {
		return double.class;
	}
	@Override
	public Class<?> getDeclaringClass() {
		return Entity.class;
	}
	@Override
	public String getDescription() {
		return "User defined attribute";
	}
	@Override
	public String getName() {
		return attributeName;
	}
	@Override
	public boolean isReportable() {
		return true;
	}
	@Override
	public int getSequence() {
		return Integer.MAX_VALUE;
	}
	@Override
	public boolean canCache() {
		return false;
	}

}

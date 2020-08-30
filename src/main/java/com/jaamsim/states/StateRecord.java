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
package com.jaamsim.states;

public class StateRecord {
	public final String name;
	long initTicks;
	long totalTicks;
	long completedCycleTicks;
	long currentCycleTicks;
	long startTick;
	public final boolean working;

	StateRecord(String state, boolean work) {
		name = state;
		working = work;
	}

	public long getStartTick() {
		return startTick;
	}

	@Override
	public String toString() {
		return name;
	}
}

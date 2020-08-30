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
package com.jaamsim.basicsim;

import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.ui.GUIFrame;

public class PauseModelTarget extends ProcessTarget {

	public PauseModelTarget() {}

	@Override
	public String getDescription() {
		return "SimulationPaused";
	}

	@Override
	public void process() {

		// If specified, terminate the simulation run
		if (Simulation.getExitAtPauseCondition()) {
			Simulation.endRun();
			GUIFrame.shutdown(0);
		}

		// Pause the simulation run
		EventManager.current().pause();

		// When the run is resumed, continue to check the pause condition
		Simulation.getInstance().doPauseCondition();
	}

}

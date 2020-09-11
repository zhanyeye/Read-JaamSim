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
package com.jaamsim.basicsim;

import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;

public class InitModelTarget extends ProcessTarget {
	public InitModelTarget() {}

	@Override
	public String getDescription() {
		return "SimulationInit";
	}

	@Override
	public void process() {

		// Initialise each entity
		for (int i = 0; i < Entity.getAll().size(); i++) {
			Entity.getAll().get(i).earlyInit();
		}

		// Initialise each entity a second time
		for (int i = 0; i < Entity.getAll().size(); i++) {
			Entity.getAll().get(i).lateInit();
		}

		// Start each entity
        // 将 每个实体的StartUpTarget事件添加到事件管理器的队列当中
		double startTime = Simulation.getStartTime();
		for (int i = Entity.getAll().size() - 1; i >= 0; i--) {
			EventManager.scheduleSeconds(startTime, 0, false, new StartUpTarget(Entity.getAll().get(i)), null);
		}

		// Schedule the initialisation period
		if (Simulation.getInitializationTime() > 0.0) {
			double clearTime = startTime + Simulation.getInitializationTime();
			EventManager.scheduleSeconds(clearTime, 5, false, new ClearStatisticsTarget(), null);
		}

		// Schedule the end of the simulation run
		double endTime = Simulation.getEndTime();
		// 将结束仿真的模型的执行目标加入事件管理器的事件队列
		EventManager.scheduleSeconds(endTime, 5, false, new EndModelTarget(), null);

		// Start checking the pause condition
		Simulation.getInstance().doPauseCondition();
	}
}

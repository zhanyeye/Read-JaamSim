/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
package com.jaamsim.BasicObjects;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.TimeUnit;

public class EntityGate extends LinkedService {

	@Keyword(description = "The time delay before each queued entity is released.\n" +
			"Entities arriving at an open gate are not delayed.",
	         exampleList = {"3.0 h", "ExponentialDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'"})
	private final SampleInput releaseDelay;

	private DisplayEntity servedEntity; // the entity about to be released from the queue

	{
		releaseDelay = new SampleInput("ReleaseDelay", "Key Inputs", new SampleConstant(0.0));
		releaseDelay.setUnitType(TimeUnit.class);
		releaseDelay.setValidRange(0.0, Double.POSITIVE_INFINITY);
		releaseDelay.setEntity(this);
		this.addInput(releaseDelay);
	}

	public EntityGate() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		servedEntity = null;
	}

	@Override
	public void addEntity(DisplayEntity ent) {

		// If the gate is closed, in maintenance or breakdown, or other entities are already
		// queued, then add the entity to the queue
		Queue queue = waitQueue.getValue();
		if (!queue.isEmpty() || !this.isIdle()) {
			queue.addEntity(ent);
			return;
		}

		// If the gate is open and there are no other entities still in the queue, then send the entity to the next component
		this.registerEntity(ent);
		this.sendToNextComponent(ent);
	}

	@Override
	protected boolean startProcessing(double simTime) {

		// Determine the match value
		Integer m = this.getNextMatchValue(getSimTime());

		// Stop if the queue has become empty
		if (waitQueue.getValue().getMatchCount(m) == 0) {
			return false;
		}

		// Select the next entity to release
		servedEntity = this.getNextEntityForMatch(m);
		this.moveToProcessPosition(servedEntity);

		return true;
	}

	/**
	 * Loop recursively through the queued entities, releasing them one by one.
	 */
	@Override
	protected void endProcessing(double simTime) {

		// Release the first element in the queue and send to the next component
		this.sendToNextComponent(servedEntity);
		servedEntity = null;
	}

	@Override
	protected double getProcessingTime(double simTime) {
		return releaseDelay.getValue().getNextSample(simTime);
	}

}

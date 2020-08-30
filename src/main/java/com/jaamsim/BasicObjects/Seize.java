/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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

import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleExpListInput;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.DimensionlessUnit;

public class Seize extends LinkedService {

	@Keyword(description = "The Resource(s) to be seized.",
	         exampleList = {"Resource1 Resource2"})
	private final EntityListInput<Resource> resourceList;

	@Keyword(description = "The number of units to seize from the Resource(s).",
	         exampleList = {"{ 2 } { 1 }", "{ DiscreteDistribution1 } { 'this.obj.attrib1 + 1' }"})
	private final SampleExpListInput numberOfUnitsList;

	{
		processPosition.setHidden(true);

		resourceList = new EntityListInput<>(Resource.class, "Resource", "Key Inputs", null);
		resourceList.setRequired(true);
		this.addInput(resourceList);

		ArrayList<SampleProvider> def = new ArrayList<>();
		def.add(new SampleConstant(1));
		numberOfUnitsList = new SampleExpListInput("NumberOfUnits", "Key Inputs", def);
		numberOfUnitsList.setEntity(this);
		numberOfUnitsList.setValidRange(0, Double.POSITIVE_INFINITY);
		numberOfUnitsList.setUnitType(DimensionlessUnit.class);
		this.addInput(numberOfUnitsList);
	}

	@Override
	public void queueChanged() {
		this.startAction();
	}

	@Override
	public void startAction() {

		// Determine the match value
		Integer m = this.getNextMatchValue(getSimTime());

		// Stop if the queue is empty, there are insufficient resources, or a threshold is closed
		while (this.isReadyToStart()) {

			// If sufficient units are available, then seize them and pass the entity to the next component
			this.seizeResources();
			DisplayEntity ent = this.getNextEntityForMatch(m);
			this.sendToNextComponent(ent);
		}
		this.setBusy(false);
	}

	@Override
	public void endAction() {
		// not required
	}

	public boolean isReadyToStart() {
		Integer m = this.getNextMatchValue(getSimTime());
		return waitQueue.getValue().getMatchCount(m) != 0 && this.checkResources() && this.isOpen();
	}

	/**
	 * Determine whether the required Resources are available.
	 * @return = TRUE if all the resources are available
	 */
	public boolean checkResources() {
		double simTime = this.getSimTime();

		// Temporarily set the obj entity to the first one in the queue
		DisplayEntity oldEnt = this.getReceivedEntity(simTime);
		this.setReceivedEntity(waitQueue.getValue().getFirst());

		ArrayList<Resource> resList = resourceList.getValue();
		ArrayList<SampleProvider> numberList = numberOfUnitsList.getValue();
		for (int i=0; i<resList.size(); i++) {
			if (resList.get(i).getAvailableUnits() < (int) numberList.get(i).getNextSample(simTime)) {
				this.setReceivedEntity(oldEnt);
				return false;
			}
		}
		return true;
	}

	/**
	 * Seize the required Resources.
	 * @return
	 */
	public void seizeResources() {
		double simTime = this.getSimTime();
		ArrayList<Resource> resList = resourceList.getValue();
		ArrayList<SampleProvider> numberList = numberOfUnitsList.getValue();
		for (int i=0; i<resList.size(); i++) {
			resList.get(i).seize((int)numberList.get(i).getNextSample(simTime));
		}
	}

	public Queue getQueue() {
		return waitQueue.getValue();
	}

	/**
	 * Is the specified Resource required by this Seize object?
	 * @param res = the specified Resource.
	 * @return = TRUE if the Resource is required.
	 */
	public boolean requiresResource(Resource res) {
		if (resourceList.getValue() == null)
			return false;
		return resourceList.getValue().contains(res);
	}

}

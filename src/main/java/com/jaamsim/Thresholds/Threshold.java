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
package com.jaamsim.Thresholds;

import java.util.ArrayList;

import com.jaamsim.DisplayModels.ShapeModel;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.math.Color4d;
import com.jaamsim.states.StateEntity;
import com.jaamsim.units.DimensionlessUnit;

public class Threshold extends StateEntity {

	@Keyword(description = "The colour of the threshold graphic when the threshold is open.",
	         example = "Threshold1  OpenColour { green }")
	private final ColourInput openColour;

	@Keyword(description = "The colour of the threshold graphic when the threshold is closed.",
			example = "Threshold1  ClosedColour { red }")
	private final ColourInput closedColour;

	@Keyword(description = "A Boolean value.  If TRUE, the threshold is displayed when it is open.",
	         example = "Threshold1 ShowWhenOpen { FALSE }")
	private final BooleanInput showWhenOpen;

	@Keyword(description = "A Boolean value.  If TRUE, the threshold is displayed when it is closed.",
	         example = "Threshold1 ShowWhenClosed { FALSE }")
	private final BooleanInput showWhenClosed;

	private final ArrayList<ThresholdUser> userList;

	private boolean open;

	{
		openColour = new ColourInput( "OpenColour", "Graphics", ColourInput.GREEN );
		this.addInput( openColour );
		this.addSynonym( openColour, "OpenColor" );

		closedColour = new ColourInput( "ClosedColour", "Graphics", ColourInput.RED );
		this.addInput( closedColour );
		this.addSynonym( closedColour, "ClosedColor" );

		showWhenOpen = new BooleanInput("ShowWhenOpen", "Graphics", true);
		this.addInput(showWhenOpen);

		showWhenClosed = new BooleanInput("ShowWhenClosed", "Graphics", true);
		this.addInput(showWhenClosed);
	}

	public Threshold() {
		userList = new ArrayList<>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		thresholdChangedTarget.users.clear();
		open = true;

		userList.clear();
		for (Entity each : Entity.getAll()) {
			if (each instanceof ThresholdUser) {
				ThresholdUser tu = (ThresholdUser)each;
				if (tu.getThresholds().contains(this))
					userList.add(tu);
			}
		}
	}

	/**
	 * Get the name of the initial state this Entity will be initialized with.
	 * @return
	 */
	@Override
	public String getInitialState() {
		return "Open";
	}

	/**
	 * Tests the given state name to see if it is valid for this Entity.
	 * @param state
	 * @return
	 */
	@Override
	public boolean isValidState(String state) {
		return "Open".equals(state) || "Closed".equals(state);
	}

	/**
	 * Tests the given state name to see if it is counted as working hours when in
	 * that state..
	 * @param state
	 * @return
	 */
	@Override
	public boolean isValidWorkingState(String state) {
		return "Open".equals(state);
	}

	private static final EventHandle thresholdChangedHandle = new EventHandle();
	private static final ThresholdChangedTarget thresholdChangedTarget = new ThresholdChangedTarget();

	private static class ThresholdChangedTarget extends ProcessTarget {
		public final ArrayList<ThresholdUser> users = new ArrayList<>();

		public ThresholdChangedTarget() {}

		@Override
		public void process() {
			for( int i = 0; i < users.size(); i++ )
				users.get( i ).thresholdChanged();

			users.clear();
		}

		@Override
		public String getDescription() {
			return "UpdateAllThresholdUsers";
		}
	}

	public boolean isOpen() {
		return open;
	}

	public final void setOpen(boolean bool) {
		// If setting to the same value as current, return
		if (open == bool)
			return;

		open = bool;
		if (open)
			setPresentState("Open");
		else
			setPresentState("Closed");

		for (ThresholdUser user : this.userList) {
			if (!thresholdChangedTarget.users.contains(user))
				thresholdChangedTarget.users.add(user);
		}
		if (!thresholdChangedTarget.users.isEmpty() && !thresholdChangedHandle.isScheduled())
			this.scheduleProcessTicks(0, 2, false, thresholdChangedTarget, thresholdChangedHandle);
	}

	private static class DoOpenTarget extends EntityTarget<Threshold> {
		public DoOpenTarget(Threshold ent, String method) {
			super(ent, method);
		}

		@Override
		public void process() {
			ent.doOpen();
		}
	}

	public final ProcessTarget doOpen = new DoOpenTarget(this, "doOpen");

	public void doOpen() {
		this.trace( "open" );
		this.setOpen( true );
	}

	private static class DoCloseTarget extends EntityTarget<Threshold> {
		public DoCloseTarget(Threshold ent, String method) {
			super(ent, method);
		}

		@Override
		public void process() {
			ent.doClose();
		}
	}

	public final ProcessTarget doClose = new DoCloseTarget(this, "doClose");

	public void doClose() {
		this.trace( "close" );
		this.setOpen( false );
	}

	@Override
	public void updateGraphics( double time ) {
		super.updateGraphics(time);

		// Determine the colour for the square
		Color4d col;
		if (open) {
			col = openColour.getValue();
			setTagVisibility(ShapeModel.TAG_CONTENTS, showWhenOpen.getValue());
			setTagVisibility(ShapeModel.TAG_OUTLINES, showWhenOpen.getValue());
		}
		else {
			col = closedColour.getValue();
			setTagVisibility(ShapeModel.TAG_CONTENTS, showWhenClosed.getValue());
			setTagVisibility(ShapeModel.TAG_OUTLINES, showWhenClosed.getValue());

		}

		setTagColour( ShapeModel.TAG_CONTENTS, col );
		setTagColour( ShapeModel.TAG_OUTLINES, ColourInput.BLACK );
	}

	@Output(name = "Open",
	 description = "If open, then return TRUE.  Otherwise, return FALSE.",
	    unitType = DimensionlessUnit.class,
	    sequence = 0)
	public Boolean getOpen(double simTime) {
		return open;
	}

	@Output(name = "OpenFraction",
	 description = "The fraction of total simulation time that the threshold is open.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 1)
	public double getOpenFraction(double simTime) {
		long simTicks = EventManager.secsToNearestTick(simTime);
		long openTicks = this.getTicksInState(simTicks, getState("Open"));
		long closedTicks = this.getTicksInState(simTicks, getState("Closed"));
		long totTicks = openTicks + closedTicks;

		return (double)openTicks / totTicks;
	}

	@Output(name = "ClosedFraction",
	 description = "The fraction of total simulation time that the threshold is closed.",
	    unitType = DimensionlessUnit.class,
	    sequence = 2)
	public double getClosedFraction(double simTime) {
		long simTicks = EventManager.secsToNearestTick(simTime);
		long openTicks = this.getTicksInState(simTicks, getState("Open"));
		long closedTicks = this.getTicksInState(simTicks, getState("Closed"));
		long totTicks = openTicks + closedTicks;

		return (double)closedTicks / totTicks;
	}
}

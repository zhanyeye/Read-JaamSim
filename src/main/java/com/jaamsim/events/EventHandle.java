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
package com.jaamsim.events;

/**
 * An EventHandle provides a means to remember a future scheduled event in order
 * to manage it's execution.  Examples of this control would be killing the event
 * or executing it earlier than otherwise scheduled.
 */
public class EventHandle {
	BaseEvent event = null;

	public EventHandle() {}

	/**
	 * Returns true if this handle is currently tracking a future event.
	 */
	public final boolean isScheduled() {
		return event != null;
	}

	@Override
	public String toString() {
		return Boolean.toString(isScheduled());
	}
}

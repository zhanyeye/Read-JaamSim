/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
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
package com.jaamsim.basicsim;

import com.jaamsim.input.ExpError;

/**
 * Custom exception thrown when a program error is encountered.
 */
public class ErrorException extends RuntimeException {

	public String entName;
	public String source;
	public int position;

	public ErrorException(int pos, String src, String name, String msg) {
		super(msg);
		entName = name;
		source = src;
		position = pos;
	}

	public ErrorException(String format, Object... args) {
		this(-1, "", "", String.format(format, args));
	}

	public ErrorException(Entity ent, String msg) {
		this(-1, "", ent.getName(), msg);
	}

	public ErrorException(Entity ent, ExpError e) {
		this(e.pos, e.source, ent.getName(), e.getMessage());
	}

	public ErrorException( Throwable cause ) {
		super( cause );
		entName = "";
		source = "";
		position = -1;
	}

	@Override
	public String getMessage() {
		if (entName.isEmpty()) {
			return super.getMessage();
		}
		StringBuilder sb = new StringBuilder();
		sb.append(entName).append(": ");
		sb.append(super.getMessage());
		return sb.toString();
	}

}

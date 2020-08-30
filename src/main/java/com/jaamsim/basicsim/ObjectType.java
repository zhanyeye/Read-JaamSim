/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ClassInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.ImageInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DistanceUnit;

public class ObjectType extends Entity {
	private static final ArrayList<ObjectType> allInstances;
	private static final HashMap<Class<? extends Entity>, ObjectType> objectTypeMap;

	@Keyword(description = "The java class of the object type",
	         example = "This is placeholder example text")
	private final ClassInput javaClass;

	@Keyword(description = "The package to which the object type belongs",
	         example = "This is placeholder example text")
	private final StringInput palette;

	@Keyword(description = "Only for DisplayEntity",
	         example = "This is placeholder example text")
	private final EntityInput<DisplayModel> defaultDisplayModel;

	@Keyword(description = "This is placeholder description text",
	         example = "This is placeholder example text")
	private final BooleanInput dragAndDrop;

	@Keyword(description = "The (optional) image to be used in the Model Builder as the icon for this object type." +
			"The normal image size is 24x24 pixels.",
	         example = "This is placeholder example text")
	private final ImageInput iconFile;

	@Keyword(description = "The default size for the instances of this class.",
	         example = "DisplayEntity DefaultSize { 1.0 1.0 1.0 m }")
	private final Vec3dInput defaultSize;

	@Keyword(description = "The default alignment for the instances of this class.",
	         example = "DisplayEntity DefaultAlignment { 0.0 0.0 -0.5 m }")
	private final Vec3dInput defaultAlignment;

	private final ArrayList<DisplayModel> displayEntityDefault = new ArrayList<>(1);

	static {
		allInstances = new ArrayList<>();
		objectTypeMap = new HashMap<>();
	}

	{
		javaClass = new ClassInput( "JavaClass", "Key Inputs", null );
		this.addInput( javaClass );

		palette = new StringInput("Palette", "Key Inputs", null);
		this.addInput( palette );

		defaultDisplayModel = new EntityInput<>(DisplayModel.class, "DefaultDisplayModel", "Key Inputs", null);
		this.addInput(defaultDisplayModel);

		dragAndDrop = new BooleanInput("DragAndDrop", "Key inputs", true);
		this.addInput(dragAndDrop);

		iconFile = new ImageInput("IconFile", "Key inputs", null);
		this.addInput(iconFile);

		defaultSize = new Vec3dInput("DefaultSize", "Key inputs", new Vec3d(1.0d, 1.0d, 1.0d));
		defaultSize.setUnitType(DistanceUnit.class);
		this.addInput(defaultSize);

		defaultAlignment = new Vec3dInput("DefaultAlignment", "Key inputs", new Vec3d(0.0d, 0.0d, 0.0d));
		this.addInput(defaultAlignment);
	}

	public ObjectType() {
		synchronized (allInstances) {
			allInstances.add(this);
		}
	}

	@Override
	public void updateForInput(Input<?> in) {

		if (in == defaultDisplayModel) {
			displayEntityDefault.clear();
			if (defaultDisplayModel.getValue() != null)
				displayEntityDefault.add(defaultDisplayModel.getValue());
			return;
		}

		if (in == javaClass) {
			synchronized (objectTypeMap) {
				objectTypeMap.put(javaClass.getValue(), this);
			}
		}

		super.updateForInput(in);
	}

	public static ArrayList<ObjectType> getAll() {
		synchronized (allInstances) {
			return allInstances;
		}
	}

	public static ObjectType getObjectTypeForClass(Class<? extends Entity> klass) {
		synchronized (objectTypeMap) {
			return objectTypeMap.get(klass);
		}
	}

	@Override
	public void kill() {
		super.kill();
		allInstances.remove(this);
		objectTypeMap.remove(javaClass.getValue());
	}

	public Class<? extends Entity> getJavaClass() {
		return javaClass.getValue();
	}

	public String getPaletteName() {
		String s = palette.getValue();
		if (s != null)
			return s;

		return "Default";
	}

	public ArrayList<DisplayModel> getDefaultDisplayModel() {
		return displayEntityDefault;
	}

	public boolean isDragAndDrop() {
		return dragAndDrop.getValue();
	}

	public BufferedImage getIconImage() {
		return iconFile.getValue();
	}

	public Vec3d getDefaultSize() {
		return defaultSize.getValue();
	}

	public Vec3d getDefaultAlignment() {
		return defaultAlignment.getValue();
	}

}

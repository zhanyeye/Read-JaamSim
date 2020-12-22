/*
 * JaamSim Discrete Event Simulation
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
package com.jaamsim.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.EntityLabel;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.math.Vec3d;

public class ContextMenu {
	private static final ArrayList<ContextMenuItem> menuItems = new ArrayList<>();

	private ContextMenu() {}

	public static final void addCustomMenuHandler(ContextMenuItem i) {
		synchronized (menuItems) {
			menuItems.add(i);
		}
	}

	private static class UIMenuItem extends JMenuItem implements ActionListener {
		final ContextMenuItem i;
		final Entity ent;
		final int x;
		final int y;

		UIMenuItem(ContextMenuItem i, Entity ent, int x, int y) {
			super(i.getMenuText());
			this.i = i;
			this.ent = ent;
			this.x = x;
			this.y = y;
			this.addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			i.performAction(ent, x, y);
		}
	}

	/**
	 * Adds menu items to the right click (context) menu for the specified entity.
	 * @param ent - entity whose context menu is to be generated
	 * @param menu - context menu to be populated with menu items
	 * @param x - screen coordinate for the menu
	 * @param y - screen coordinate for the menu
	 */
	public static void populateMenu(JPopupMenu menu, final Entity ent, final int x, final int y) {
		// 1) Input Editor
		JMenuItem inputEditorMenuItem = new JMenuItem( "Input Editor" );
		inputEditorMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				InputAgent.applyArgs(Simulation.getInstance(), "ShowInputEditor", "TRUE");
				FrameBox.setSelectedEntity(ent);
			}
		} );
		menu.add( inputEditorMenuItem );

		// 2) Property Viewer
		JMenuItem propertyViewerMenuItem = new JMenuItem( "Property Viewer" );
		propertyViewerMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				InputAgent.applyArgs(Simulation.getInstance(), "ShowPropertyViewer", "TRUE");
				FrameBox.setSelectedEntity(ent);
			}
		} );
		menu.add( propertyViewerMenuItem );

		// 3) Output Viewer
		JMenuItem outputViewerMenuItem = new JMenuItem( "Output Viewer" );
		outputViewerMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				InputAgent.applyArgs(Simulation.getInstance(), "ShowOutputViewer", "TRUE");
				FrameBox.setSelectedEntity(ent);
			}
		} );
		menu.add( outputViewerMenuItem );

		// 4) Duplicate
		JMenuItem duplicateMenuItem = new JMenuItem( "Duplicate" );
		duplicateMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				Entity copiedEntity = InputAgent.defineEntityWithUniqueName(ent.getClass(),
						ent.getName(), "_Copy", true);

				// Match all the inputs
				copiedEntity.copyInputs(ent);

				// Position the duplicated entity next to the original
				if (copiedEntity instanceof DisplayEntity) {
					DisplayEntity dEnt = (DisplayEntity)copiedEntity;

					Vec3d pos = dEnt.getPosition();
					pos.x += 0.5d * dEnt.getSize().x;
					pos.y -= 0.5d * dEnt.getSize().y;

					dEnt.setPosition(pos);

					// Set the input for the "Position" keyword to the new value
					KeywordIndex kw = InputAgent.formatPointInputs("Position", pos, "m");
					InputAgent.apply(dEnt, kw);
				}

				// Show the duplicated entity in the editors and viewers
				FrameBox.setSelectedEntity(copiedEntity);
			}
		} );
		if (ent.testFlag(Entity.FLAG_GENERATED)) {
			duplicateMenuItem.setEnabled(false);
		}
		menu.add( duplicateMenuItem );

		// 5) Delete
		JMenuItem deleteMenuItem = new JMenuItem( "Delete" );
		deleteMenuItem.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent event ) {
				ent.kill();
				FrameBox.setSelectedEntity(null);
			}
		} );
		menu.add( deleteMenuItem );

		// DisplayEntity menu items
		if (ent instanceof DisplayEntity) {
			ContextMenu.populateDisplayEntityMenu(menu, (DisplayEntity)ent, x, y);
		}

		synchronized (menuItems) {
			for (ContextMenuItem each : menuItems) {
				if (each.supportsEntity(ent))
					menu.add(new UIMenuItem(each, ent, x, y));
			}
		}
	}

	public static void populateDisplayEntityMenu(JPopupMenu menu, final DisplayEntity ent, final int x, final int y) {

		if (!RenderManager.isGood())
			return;

		// 1) Change Graphics
		JMenuItem changeGraphicsMenuItem = new JMenuItem( "Change Graphics" );
		changeGraphicsMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GraphicBox graphicBox = GraphicBox.getInstance(ent, x, y);
				graphicBox.setVisible( true );
			}
		} );
		if (ent.getDisplayModelList() == null) {
			changeGraphicsMenuItem.setEnabled(false);
		}
		menu.add( changeGraphicsMenuItem );

		// 2) Show Label
		final EntityLabel label = EntityLabel.getLabel(ent);
		boolean bool = label != null && label.getShow();
		final JMenuItem showLabelMenuItem = new JCheckBoxMenuItem( "Show Label", bool );
		showLabelMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				if (showLabelMenuItem.isSelected()) {

					// If required, create the EntityLabel object
					if (label == null) {
						EntityLabel newLabel = InputAgent.defineEntityWithUniqueName(EntityLabel.class, ent.getName() + "_Label", "", true);
						InputAgent.applyArgs(newLabel, "TargetEntity", ent.getName());

						InputAgent.applyArgs(newLabel, "RelativeEntity", ent.getName());
						if (ent.getCurrentRegion() != null)
							InputAgent.applyArgs(newLabel, "Region", ent.getCurrentRegion().getName());

						double ypos = -0.15 - 0.5*ent.getSize().y;
						InputAgent.apply(newLabel, InputAgent.formatPointInputs("Position", new Vec3d(0.0, ypos, 0.0), "m"));
						InputAgent.applyArgs(newLabel, "TextHeight", "0.15", "m");
						newLabel.resizeForText();
						return;
					}

					// Show the label
					InputAgent.applyArgs(label, "Show", "TRUE");
				}
				else {

					// Hide the label if it already exists
					if (label != null)
						InputAgent.applyArgs(label, "Show", "FALSE");
				}
			}
		} );
		if (ent instanceof EntityLabel || ent.testFlag(Entity.FLAG_GENERATED)) {
			showLabelMenuItem.setEnabled(false);
		}
		menu.add( showLabelMenuItem );

		// 3) Set RelativeEntity
		JMenu setRelativeEntityMenu = new JMenu( "Set RelativeEntity" );
		ArrayList<String> entNameList = new ArrayList<>();
		entNameList.add("<None>");
		entNameList.addAll(ent.getRelativeEntityOptions());
		String presentEntName = "<None>";
		if (ent.getRelativeEntity() != null) {
			presentEntName = ent.getRelativeEntity().getName();
		}
		for (final String entName : entNameList) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(entName);
			if (entName.equals(presentEntName)) {
				item.setSelected(true);
			}
			item.addActionListener( new ActionListener() {

				@Override
				public void actionPerformed( ActionEvent event ) {
					Vec3d pos = ent.getGlobalPosition();
					if (entName.equals("<None>")) {
						InputAgent.applyArgs(ent, "RelativeEntity");
					}
					else {
						InputAgent.applyArgs(ent, "RelativeEntity", entName);
					}
					ent.setInputForGlobalPosition(pos);
				}
			} );
			setRelativeEntityMenu.add(item);
		}
		if (ent instanceof EntityLabel	|| ent.testFlag(Entity.FLAG_GENERATED)) {
			setRelativeEntityMenu.setEnabled(false);
		}
		menu.add( setRelativeEntityMenu );

		// 4) Set Region
		JMenu setRegionMenu = new JMenu( "Set Region" );
		ArrayList<String> regionNameList = new ArrayList<>();
		regionNameList.add("<None>");
		regionNameList.addAll(ent.getRegionOptions());
		String presentRegionName = "<None>";
		if (ent.getCurrentRegion() != null) {
			presentRegionName = ent.getCurrentRegion().getName();
		}
		for (final String regionName : regionNameList) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(regionName);
			if (regionName.equals(presentRegionName)) {
				item.setSelected(true);
			}
			item.addActionListener( new ActionListener() {

				@Override
				public void actionPerformed( ActionEvent event ) {
					Vec3d pos = ent.getGlobalPosition();
					if (regionName.equals("<None>")) {
						InputAgent.applyArgs(ent, "Region");
					}
					else {
						InputAgent.applyArgs(ent, "Region", regionName);
					}
					ent.setInputForGlobalPosition(pos);
				}
			} );
			setRegionMenu.add(item);
		}
		if (ent instanceof EntityLabel	|| ent.testFlag(Entity.FLAG_GENERATED)) {
			setRegionMenu.setEnabled(false);
		}
		menu.add( setRegionMenu );

		// 5) Centre in View
		JMenuItem centerInViewMenuItem = new JMenuItem( "Center in View" );
		final View v = RenderManager.inst().getActiveView();
		centerInViewMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				// Move the camera position so that the entity is in the centre of the screen
				Vec3d viewPos = new Vec3d(v.getGlobalPosition());
				viewPos.sub3(v.getGlobalCenter());
				viewPos.add3(ent.getPosition());
				v.setCenter(ent.getPosition());
				v.setPosition(viewPos);
			}
		} );
		if (v == null) {
			centerInViewMenuItem.setEnabled(false);
		}
		menu.add( centerInViewMenuItem );
	}
}

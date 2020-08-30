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
package com.jaamsim.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;

public class FrameBox extends JFrame {

	private static final ArrayList<FrameBox> allInstances;

	private static volatile Entity selectedEntity;
	private static volatile long simTicks;

	protected static final Color TABLE_SELECT = new Color(255, 250, 180);

	protected static final Font boldFont;
	protected static final TableCellRenderer colRenderer;

	private static final UIUpdater uiUpdater = new UIUpdater();

	static {
		allInstances = new ArrayList<>();

		boldFont = UIManager.getDefaults().getFont("TabbedPane.font").deriveFont(Font.BOLD);

		GUIFrame.getRateLimiter().registerCallback(new Runnable() {
			@Override
			public void run() {
				SwingUtilities.invokeLater(uiUpdater);
			}
		});

		colRenderer = new DefaultCellRenderer();
	}

	public FrameBox(String title) {
		super(title);
		setType(Type.UTILITY);
		setAutoRequestFocus(false);
		allInstances.add(this);
	}

	public static void clear() {
		ArrayList<FrameBox> boxes = new ArrayList<>(allInstances);
		for (FrameBox each : boxes) {
			each.dispose();
		}
	}

	public static WindowAdapter getCloseListener(String key) {
		return new CloseListener(key);
	}

	/**
	 * Listens for window events for the GUI and sets the appropriate keyword
	 * controlling visibility.
	 */
	private static class CloseListener extends WindowAdapter {
		final KeywordIndex kw;
		public CloseListener(String keyword) {
			ArrayList<String> arg = new ArrayList<>(1);
			arg.add("FALSE");
			kw = new KeywordIndex(keyword, arg, null);
		}

		@Override
		public void windowClosing(WindowEvent e) {
			InputAgent.apply(Simulation.getInstance(), kw);
		}
	}

	@Override
	public void dispose() {
		allInstances.remove(this);
		super.dispose();
	}

	public static final void setSelectedEntity(Entity ent) {
		if (ent == selectedEntity)
			return;

		if (selectedEntity != null)
			selectedEntity.handleSelectionLost();

		selectedEntity = ent;
		RenderManager.setSelection(ent);
		valueUpdate();
	}

	// This is equivalent to calling setSelectedEntity again with the same entity as used previously
	public static final void reSelectEntity() {
		valueUpdate();
	}

	public static final void timeUpdate(long tick) {
		if (tick == simTicks)
			return;

		simTicks = tick;
		RenderManager.updateTime(tick);
		valueUpdate();
	}

	public static final void valueUpdate() {
		GUIFrame.getRateLimiter().queueUpdate();
	}

	public void setEntity(Entity ent) {}
	public void updateValues(double simTime) {}

	private static class UIUpdater  implements Runnable {

		@Override
		public void run() {
			double callBackTime = EventManager.ticksToSecs(simTicks);

			GUIFrame.instance().setClock(callBackTime);

			for (int i = 0; i < allInstances.size(); i++) {
				try {
					FrameBox each = allInstances.get(i);
					each.setEntity(selectedEntity);
					each.updateValues(callBackTime);
				}
				catch (IndexOutOfBoundsException e) {
					// reschedule and try again
					valueUpdate();
					return;
				}
			}
		}
	}

	public static void fitTableToLastColumn(JTable tab) {
		TableColumnModel model = tab.getColumnModel();
		TableColumn lastCol = model.getColumn(model.getColumnCount() - 1);

		int delta = tab.getSize().width;
		for(int i = 0; i < model.getColumnCount(); i++) {
			delta -= model.getColumn(i).getWidth();
		}
		int newWidth = lastCol.getWidth() + delta;
		lastCol.setWidth(newWidth);
	}
}

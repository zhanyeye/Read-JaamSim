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
package com.jaamsim.ui;

import java.util.ArrayList;

import javax.swing.JOptionPane;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.controllers.VideoRecorder;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.IntegerListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;

public class VideoRecorderEntity extends DisplayEntity {

	@Keyword(description = "Simulation time at which to capture the first frame.",
	         exampleList = {"200 h"})
	private final ValueInput captureStartTime;

	@Keyword(description = "Simulation time between captured frames.",
	         exampleList = {"60 s"})
	private final ValueInput captureInterval;

	@Keyword(description = "Total number of frames to capture for the video.\n"
	                     + "The recorded video assumes 30 frames per second. Therefore, if a "
	                     + "2 minute video is required, the number of frames should be set to "
	                     + "120 x 30 = 3600.",
	         exampleList = {"3600"})
	private final IntegerInput captureFrames;

	@Keyword(description = "The size of the video/image, expressed as the number of horizontal "
	                     + "and vertical pixels.\n"
	                     + "The top left hand corner of the captured frames will be the same as "
	                     + "the top left hand corner of the image on the monitor. If the "
	                     + "specified image size is larger than the monitor resolution, then the "
	                     + "image will be extented beyond the bottom and/or right sides of the "
	                     + "monitor.",
	         exampleList = {"1920 1080"})
	private final IntegerListInput captureArea;

	@Keyword(description = "The list of View windows to be captured.",
	         exampleList = {"View1 View2 View3"})
	private final EntityListInput<View> captureViews;

	@Keyword(description = "The background color for the captured frames.\n"
	                     + "Only the 3D view portion of the specified windows will be captured. "
	                     + "The remainder of the frame, such as the Control Panel or any gaps "
	                     + "between the view windows, will be replaced by the background color.",
	         exampleList = {"skyblue", "135 206 235"})
	private final ColourInput videoBGColor;

	@Keyword(description = "A label to append to the run name when the AVI file is saved.\n"
	                     + "The saved file will be named <run name>_<VideoName>.avi.",
	         exampleList = {"video"})
	private final StringInput videoName;

	@Keyword(description = "If TRUE, an individual PNG file will be saved for each frame.",
	         exampleList = {"TRUE"})
	private final BooleanInput saveImages;

	@Keyword(description = "If TRUE, an AVI file containing the video will be saved.\n"
	                     + "The AVI file will be encoded using the VP8 codec, which is NOT "
	                     + "supported by Windows Media Player. Furthermore, the present encoding "
	                     + "algorithm is quite inefficient making the file size much larger than "
	                     + "necessary. Both problems can be solved by recoding the video using "
	                     + "free open-source software such as HandBrake (https://handbrake.fr/).",
	         exampleList = {"TRUE"})
	private final BooleanInput saveVideo;

	private boolean hasRunStartup;
	private int numFramesWritten;
	private final EventHandle captureHandle = new EventHandle();

	{
		attributeDefinitionList.setHidden(true);

		captureStartTime = new ValueInput("CaptureStartTime", "Key Inputs", 0.0d);
		captureStartTime.setUnitType(TimeUnit.class);
		captureStartTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(captureStartTime);

		captureInterval = new ValueInput("CaptureInterval", "Key Inputs", 3600.0d);
		captureInterval.setUnitType(TimeUnit.class);
		captureInterval.setValidRange(0.1d, Double.POSITIVE_INFINITY);
		this.addInput(captureInterval);

		captureFrames = new IntegerInput("CaptureFrames", "Key Inputs", 0);
		captureFrames.setValidRange(0, 30000);
		this.addInput(captureFrames);

		IntegerVector defArea = new IntegerVector(2);
		defArea.add(1920);
		defArea.add(1080);
		captureArea = new IntegerListInput("CaptureArea", "Key Inputs", defArea);
		captureArea.setValidCount(2);
		captureArea.setValidRange(0, 3000);
		this.addInput(captureArea);

		captureViews = new EntityListInput<>(View.class, "CaptureViews", "Key Inputs", new ArrayList<View>(0));
		captureViews.setRequired(true);
		this.addInput(captureViews);

		videoBGColor = new ColourInput("VideoBackgroundColor", "Key Inputs", ColourInput.WHITE);
		this.addInput(videoBGColor);
		this.addSynonym(videoBGColor, "Colour");

		videoName = new StringInput("VideoName", "Key Inputs", "");
		this.addInput(videoName);

		saveImages = new BooleanInput("SaveImages", "Key Inputs", false);
		this.addInput(saveImages);

		saveVideo = new BooleanInput("SaveVideo", "Key Inputs", false);
		this.addInput(saveVideo);
	}

	@Override
	public void validate() {
		super.validate();

		if( ( saveImages.getValue() || saveVideo.getValue() ) && captureViews.getValue().size() == 0 )
			throw new InputErrorException( "CaptureViews must be set when SaveImages or SaveVideo is TRUE" );
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		hasRunStartup = false;
		numFramesWritten = 0;
	}

	@Override
	public void startUp() {
		super.startUp();

		if (saveVideo.getValue() || saveImages.getValue())
			startProcess(new CaptureNetworkTarget(this));

		this.hasRunStartup = true;
	}

	@Override
    public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == saveVideo) {
			// Start the capture if we are already running and we set the input
			// to true
			if (hasRunStartup && saveVideo.getValue())
				EventManager.scheduleTicks(0, 10, false, new CaptureNetworkTarget(this), null);
		}
	}

	private static class CaptureNetworkTarget extends ProcessTarget {
		final VideoRecorderEntity rec;

		CaptureNetworkTarget(VideoRecorderEntity rec) {
			this.rec = rec;
		}

		@Override
		public String getDescription() {
			return rec.getName() + ".doCaptureNetwork";
		}

		@Override
		public void process() {
			rec.doCaptureNetwork();
		}
	}
	/**
	 * Capture JPEG images of the screen at regular simulated intervals
	 */
	public void doCaptureNetwork() {

		// If the capture network is already in progress, then stop the previous network
		EventManager.killEvent(captureHandle);
		simWait(captureStartTime.getValue(), 10, captureHandle);

		if (!RenderManager.isGood()) {
			RenderManager.initialize(false);
		}

		if (!RenderManager.canRenderOffscreen()) {
			JOptionPane.showMessageDialog(null, "Your hardware does not support Video Recording.");
			return;
		}

		int width = captureArea.getValue().get(0);
		int height = captureArea.getValue().get(1);

		ArrayList<View> views = captureViews.getValue();

		String videoFileName = String.format("%s_%s", InputAgent.getRunName(), videoName.getValue());
		String fullVideoFile = InputAgent.getReportFileName(videoFileName); // getReportFileName() prepends the report directory onto a filename

		VideoRecorder recorder = new VideoRecorder(views, fullVideoFile, width, height, captureFrames.getDefaultValue(),
		                             saveImages.getValue(), saveVideo.getValue(), videoBGColor.getValue());

		// Otherwise, start capturing
		while (saveVideo.getValue() || saveImages.getValue()) {

			RenderManager.inst().blockOnScreenShot(recorder);
			++numFramesWritten;

			if (numFramesWritten == captureFrames.getValue()) {
				break;
			}

			// Wait until the next time to capture a frame
			// (priority 10 is used to allow higher priority events to complete first)
			simWait(captureInterval.getValue(), 10, captureHandle);
		}

		recorder.freeResources();
		recorder = null;
	}

}

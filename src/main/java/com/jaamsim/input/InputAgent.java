/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2011 Ausenco Engineering Canada Inc.
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
package com.jaamsim.input;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.FileEntity;
import com.jaamsim.basicsim.Group;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.events.EventManager;
import com.jaamsim.math.Vec3d;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.ui.GUIFrame;
import com.jaamsim.ui.LogBox;

public class InputAgent {
	private static final String recordEditsMarker = "RecordEdits";

	private static int numErrors = 0;
	private static int numWarnings = 0;
	private static FileEntity logFile;

	private static long lastTickForTrace;

	private static File configFile;           // present configuration file
	private static boolean batchRun;
	private static boolean sessionEdited;     // TRUE if any inputs have been changed after loading a configuration file
	private static boolean recordEditsFound;  // TRUE if the "RecordEdits" marker is found in the configuration file
	private static boolean recordEdits;       // TRUE if input changes are to be marked as edited.

	private static final String INP_ERR_DEFINEUSED = "The name: %s has already been used and is a %s";
	private static final String[] EARLY_KEYWORDS = {"AttributeDefinitionList", "UnitType", "UnitTypeList", "TickLength"};

	private static File reportDir;
	private static FileEntity reportFile;     // file to which the output report will be written

	static {
		recordEditsFound = false;
		sessionEdited = false;
		batchRun = false;
		configFile = null;
		reportDir = null;
		reportFile = null;
		lastTickForTrace = -1l;
	}

	public static void clear() {
		logFile = null;
		numErrors = 0;
		numWarnings = 0;
		recordEditsFound = false;
		sessionEdited = false;
		configFile = null;
		reportDir = null;
		lastTickForTrace = -1l;
		setReportDirectory(null);
	}

	private static String getReportDirectory() {
		if (reportDir != null)
			return reportDir.getPath() + File.separator;

		if (configFile != null)
			return configFile.getParentFile().getPath() + File.separator;

		return null;
	}

	public static String getReportFileName(String name) {
		return getReportDirectory() + name;
	}

	public static void setReportDirectory(File dir) {
		reportDir = dir;
		if (reportDir == null)
			return;
		if (!reportDir.exists() && !reportDir.mkdirs())
			throw new InputErrorException("Was unable to create the Report Directory: %s", reportDir.toString());
	}

	public static void prepareReportDirectory() {
		if (reportDir != null) reportDir.mkdirs();
	}

	/**
	 * Sets the present configuration file.
	 *
	 * @param file - the present configuration file.
	 */
	public static void setConfigFile(File file) {
		configFile = file;
	}

	/**
	 * Returns the present configuration file.
	 * <p>
	 * Null is returned if no configuration file has been loaded or saved yet.
	 * <p>
	 * @return the present configuration file.
	 */
	public static File getConfigFile() {
		return configFile;
	}

	/**
	 * Returns the name of the simulation run.
	 * <p>
	 * For example, if the configuration file name is "case1.cfg", then the
	 * run name is "case1".
	 * <p>
	 * @return the name of simulation run.
	 */
	public static String getRunName() {

		if( InputAgent.getConfigFile() == null )
			return "";

		String name = InputAgent.getConfigFile().getName();
		int index = name.lastIndexOf( "." );
		if( index == -1 )
			return name;

		return name.substring( 0, index );
	}

	/**
	 * Specifies whether a RecordEdits marker was found in the present configuration file.
	 *
	 * @param bool - TRUE if a RecordEdits marker was found.
	 */
	public static void setRecordEditsFound(boolean bool) {
		recordEditsFound = bool;
	}

	/**
	 * Indicates whether a RecordEdits marker was found in the present configuration file.
	 *
	 * @return - TRUE if a RecordEdits marker was found.
	 */
	public static boolean getRecordEditsFound() {
		return recordEditsFound;
	}

	/**
	 * Returns the "RecordEdits" mode for the InputAgent.
	 * <p>
	 * When RecordEdits is TRUE, any model inputs that are changed and any objects that
	 * are defined are marked as "edited". When FALSE, model inputs and object
	 * definitions are marked as "unedited".
	 * <p>
	 * RecordEdits mode is used to determine the way JaamSim saves a configuration file
	 * through the graphical user interface. Object definitions and model inputs
	 * that are marked as unedited will be copied exactly as they appear in the original
	 * configuration file that was first loaded.  Object definitions and model inputs
	 * that are marked as edited will be generated automatically by the program.
	 *
	 * @return the RecordEdits mode for the InputAgent.
	 */
	public static boolean recordEdits() {
		return recordEdits;
	}

	/**
	 * Sets the "RecordEdits" mode for the InputAgent.
	 * <p>
	 * When RecordEdits is TRUE, any model inputs that are changed and any objects that
	 * are defined are marked as "edited". When FALSE, model inputs and object
	 * definitions are marked as "unedited".
	 * <p>
	 * RecordEdits mode is used to determine the way JaamSim saves a configuration file
	 * through the graphical user interface. Object definitions and model inputs
	 * that are marked as unedited will be copied exactly as they appear in the original
	 * configuration file that was first loaded.  Object definitions and model inputs
	 * that are marked as edited will be generated automatically by the program.
	 *
	 * @param b - boolean value for the RecordEdits mode
	 */
	public static void setRecordEdits(boolean b) {
		recordEdits = b;
	}

	public static boolean isSessionEdited() {
		return sessionEdited;
	}

	public static void setBatch(boolean batch) {
		batchRun = batch;
	}

	public static boolean getBatch() {
		return batchRun;
	}

	private static int getBraceDepth(ArrayList<String> tokens, int startingBraceDepth, int startingIndex) {
		int braceDepth = startingBraceDepth;
		for (int i = startingIndex; i < tokens.size(); i++) {
			String token = tokens.get(i);

			if (token.equals("{"))
				braceDepth++;

			if (token.equals("}"))
				braceDepth--;

			if (braceDepth < 0) {
				InputAgent.logBadInput(tokens, "Extra closing braces found");
				tokens.clear();
			}

			if (braceDepth > 3) {
				InputAgent.logBadInput(tokens, "Maximum brace depth (3) exceeded");
				tokens.clear();
			}
		}

		return braceDepth;
	}

	private static URI resRoot;
	private static final String res = "/resources/";

	static {

		try {
			// locate the resource folder, and create
			resRoot = InputAgent.class.getResource(res).toURI();
		}
		catch (URISyntaxException e) {}
	}

	private static void rethrowWrapped(Exception ex) {
		StringBuilder causedStack = new StringBuilder();
		for (StackTraceElement elm : ex.getStackTrace())
			causedStack.append(elm.toString()).append("\n");
		throw new InputErrorException("Caught exception: %s", ex.getMessage() + "\n" + causedStack.toString());
	}

	public static final void readResource(String res) {
		if (res == null)
			return;

		try {
			readStream(null, null, res);
			GUIFrame.instance().setProgressText(null);
		}
		catch (URISyntaxException ex) {
			rethrowWrapped(ex);
		}

	}

	public static final boolean readStream(String root, URI path, String file) throws URISyntaxException {
		String shortName = file.substring(file.lastIndexOf('/') + 1, file.length());
		GUIFrame.instance().setProgressText(shortName);
		URI resolved = getFileURI(path, file, root);

		URL url = null;
		try {
			url = resolved.normalize().toURL();
		}
		catch (MalformedURLException e) {
			rethrowWrapped(e);
		}

		if (url == null) {
			InputAgent.logError("Unable to resolve path %s%s - %s", root, path.toString(), file);
			return false;
		}

		BufferedReader buf = null;
		try {
			InputStream in = url.openStream();
			buf = new BufferedReader(new InputStreamReader(in));
		} catch (IOException e) {
			InputAgent.logError("Could not read from %s", url.toString());
			return false;
		}

		try {
			ArrayList<String> record = new ArrayList<>();
			int braceDepth = 0;

			ParseContext pc = new ParseContext(resolved, root);

			while (true) {
				String line = buf.readLine();
				// end of file, stop reading
				if (line == null)
					break;

				int previousRecordSize = record.size();
				Parser.tokenize(record, line, true);
				braceDepth = InputAgent.getBraceDepth(record, braceDepth, previousRecordSize);
				if( braceDepth != 0 )
					continue;

				if (record.size() == 0)
					continue;

				InputAgent.echoInputRecord(record);

				if ("DEFINE".equalsIgnoreCase(record.get(0))) {
					InputAgent.processDefineRecord(record);
					record.clear();
					continue;
				}

				if ("INCLUDE".equalsIgnoreCase(record.get(0))) {
					try {
						InputAgent.processIncludeRecord(pc, record);
					}
					catch (URISyntaxException ex) {
						rethrowWrapped(ex);
					}
					record.clear();
					continue;
				}

				if ("RECORDEDITS".equalsIgnoreCase(record.get(0))) {
					InputAgent.setRecordEditsFound(true);
					InputAgent.setRecordEdits(true);
					record.clear();
					continue;
				}

				// Otherwise assume it is a Keyword record
				InputAgent.processKeywordRecord(record, pc);
				record.clear();
			}

			// Leftover Input at end of file
			if (record.size() > 0)
				InputAgent.logBadInput(record, "Leftover input at end of file");
			buf.close();
		}
		catch (IOException e) {
			// Make best effort to ensure it closes
			try { buf.close(); } catch (IOException e2) {}
		}

		return true;
	}

	private static void processIncludeRecord(ParseContext pc, ArrayList<String> record) throws URISyntaxException {
		if (record.size() != 2) {
			InputAgent.logError("Bad Include record, should be: Include <File>");
			return;
		}
		InputAgent.readStream(pc.jail, pc.context, record.get(1).replaceAll("\\\\", "/"));
	}

	private static void processDefineRecord(ArrayList<String> record) {
		if (record.size() < 5 ||
		    !record.get(2).equals("{") ||
		    !record.get(record.size() - 1).equals("}")) {
			InputAgent.logError("Bad Define record, should be: Define <Type> { <names>... }");
			return;
		}

		Class<? extends Entity> proto = null;
		try {
			if( record.get( 1 ).equalsIgnoreCase( "ObjectType" ) ) {
				proto = ObjectType.class;
			}
			else {
				proto = Input.parseEntityType(record.get(1));
			}
		}
		catch (InputErrorException e) {
			InputAgent.logError("%s", e.getMessage());
			return;
		}

		// Loop over all the new Entity names
		for (int i = 3; i < record.size() - 1; i++) {
			InputAgent.defineEntity(proto, record.get(i), InputAgent.recordEdits());
		}
	}

	public static <T extends Entity> T generateEntityWithName(Class<T> proto, String key) {
		if (key != null && !isValidName(key)) {
			InputAgent.logError("Entity names cannot contain spaces, tabs, { or }: %s", key);
			return null;
		}

		T ent = null;
		try {
			ent = proto.newInstance();
			ent.setFlag(Entity.FLAG_GENERATED);
			if (key != null)
				ent.setName(key);
			else
				ent.setName(proto.getSimpleName() + "-" + ent.getEntityNumber());
		}
		catch (InstantiationException e) {}
		catch (IllegalAccessException e) {}
		finally {
			if (ent == null) {
				InputAgent.logError("Could not create new Entity: %s", key);
				return null;
			}
		}
		return ent;
	}

	/**
	 * Like defineEntity(), but will generate a unique name if a name collision exists
	 * @param proto
	 * @param key
	 * @param sep
	 * @param addedEntity
	 * @return
	 */
	public static <T extends Entity> T defineEntityWithUniqueName(Class<T> proto, String key, String sep, boolean addedEntity) {

		// Has the provided name been used already?
		if (Entity.getNamedEntity(key) == null) {
			return defineEntity(proto, key, addedEntity);
		}

		// Try the provided name plus "1", "2", etc. until an unused name is found
		int entityNum = 1;
		while(true) {
			String name = String.format("%s%s%d", key, sep, entityNum);
			if (Entity.getNamedEntity(name) == null) {
				return defineEntity(proto, name, addedEntity);
			}

			entityNum++;
		}
	}

	private static boolean isValidName(String key) {
		for (int i = 0; i < key.length(); ++i) {
			final char c = key.charAt(i);
			if (c == ' ' || c == '\t' || c == '{' || c == '}')
				return false;
		}
		return true;
	}

	/**
	 * if addedEntity is true then this is an entity defined
	 * by user interaction or after added record flag is found;
	 * otherwise, it is from an input file define statement
	 * before the model is configured
	 * @param proto
	 * @param key
	 * @param addedEntity
	 */
	private static <T extends Entity> T defineEntity(Class<T> proto, String key, boolean addedEntity) {
		Entity existingEnt = Input.tryParseEntity(key, Entity.class);
		if (existingEnt != null) {
			InputAgent.logError(INP_ERR_DEFINEUSED, key, existingEnt.getClass().getSimpleName());
			return null;
		}

		if (!isValidName(key)) {
			InputAgent.logError("Entity names cannot contain spaces, tabs, { or }: %s", key);
			return null;
		}

		T ent = null;
		try {
			ent = proto.newInstance();
			if (addedEntity) {
				ent.setFlag(Entity.FLAG_ADDED);
				sessionEdited = true;
			}
		}
		catch (InstantiationException e) {}
		catch (IllegalAccessException e) {}
		finally {
			if (ent == null) {
				InputAgent.logError("Could not create new Entity: %s", key);
				return null;
			}
		}

		ent.setName(key);
		return ent;
	}

	/**
	 * Assigns a new name to the given entity.
	 * @param ent - entity to be renamed
	 * @param newName - new name for the entity
	 */
	public static void renameEntity(Entity ent, String newName) {

		// Check that the entity was defined AFTER the RecordEdits command
		if (!ent.testFlag(Entity.FLAG_ADDED))
			throw new ErrorException("Cannot rename an entity that was defined before the RecordEdits command.");

		// Check that the new name is valid
		if (newName.contains(" ") || newName.contains("\t") || newName.contains("{") || newName.contains("}"))
			throw new ErrorException("Entity names cannot contain spaces, tabs, or braces ({}).");

		// Check that the name has not been used already
		Entity existingEnt = Input.tryParseEntity(newName, Entity.class);
		if (existingEnt != null && existingEnt != ent)
			throw new ErrorException("Entity name: %s is already in use.", newName);

		// Rename the entity
		ent.setName(newName);
	}

	public static void processKeywordRecord(ArrayList<String> record, ParseContext context) {
		Entity ent = Input.tryParseEntity(record.get(0), Entity.class);
		if (ent == null) {
			InputAgent.logError("Could not find Entity: %s", record.get(0));
			return;
		}

		// Validate the tokens have the Entity Keyword { Args... } Keyword { Args... }
		ArrayList<KeywordIndex> words = InputAgent.getKeywords(record, context);
		for (KeywordIndex keyword : words) {
			try {
				InputAgent.processKeyword(ent, keyword);
			}
			catch (Throwable e) {
				InputAgent.logInpError("Entity: %s, Keyword: %s - %s", ent.getName(), keyword.keyword, e.getMessage());
				if (e.getMessage() == null) {
					for (StackTraceElement each : e.getStackTrace())
						InputAgent.logMessage(each.toString());
				}
			}
		}
	}

	private static ArrayList<KeywordIndex> getKeywords(ArrayList<String> input, ParseContext context) {
		ArrayList<KeywordIndex> ret = new ArrayList<>();

		int braceDepth = 0;
		int keyWordIdx = 1;
		for (int i = 1; i < input.size(); i++) {
			String tok = input.get(i);
			if ("{".equals(tok)) {
				braceDepth++;
				continue;
			}

			if ("}".equals(tok)) {
				braceDepth--;
				if (braceDepth == 0) {
					// validate keyword form
					String keyword = input.get(keyWordIdx);
					if (keyword.equals("{") || keyword.equals("}") || !input.get(keyWordIdx + 1).equals("{"))
						throw new InputErrorException("The input for a keyword must be enclosed by braces. Should be <keyword> { <args> }");

					ret.add(new KeywordIndex(keyword, input, keyWordIdx + 2, i, context));
					keyWordIdx = i + 1;
					continue;
				}
			}
		}

		if (keyWordIdx != input.size())
			throw new InputErrorException("The input for a keyword must be enclosed by braces. Should be <keyword> { <args> }");

		return ret;
	}

	// Load the run file
	public static void loadConfigurationFile( File file) throws URISyntaxException {

		String inputTraceFileName = InputAgent.getRunName() + ".log";
		// Initializing the tracing for the model
		try {
			System.out.println( "Creating trace file" );

			URI confURI = file.toURI();
			URI logURI = confURI.resolve(new URI(null, inputTraceFileName, null)); // The new URI here effectively escapes the file name

			// Set and open the input trace file name
			logFile = new FileEntity( logURI.getPath());
		}
		catch( Exception e ) {
			InputAgent.logWarning("Could not create trace file");
		}

		URI dirURI = file.getParentFile().toURI();
		InputAgent.readStream("", dirURI, file.getName());

		// The session is not considered to be edited after loading a configuration file
		sessionEdited = false;

		// Save and close the input trace file
		if (logFile != null) {
			if (InputAgent.numWarnings == 0 && InputAgent.numErrors == 0) {
				logFile.close();
				logFile.delete();
				logFile = new FileEntity( inputTraceFileName);
			}
		}

		//  Check for found errors
		if( InputAgent.numErrors > 0 )
			throw new InputErrorException("%d input errors and %d warnings found", InputAgent.numErrors, InputAgent.numWarnings);

		if (Simulation.getPrintInputReport())
			InputAgent.printInputFileKeywords();
	}

	/**
	 * Prepares the keyword and input value for processing.
	 *
	 * @param ent - the entity whose keyword and value has been entered.
	 * @param keyword - the keyword.
	 * @param value - the input value String for the keyword.
	 */
	public static void applyArgs(Entity ent, String keyword, String... args){
		// Keyword
		ArrayList<String> tokens = new ArrayList<>(args.length);
		for (String each : args)
			tokens.add(each);

		// Parse the keyword inputs
		KeywordIndex kw = new KeywordIndex(keyword, tokens, null);
		InputAgent.apply(ent, kw);
	}

	public static final void apply(Entity ent, KeywordIndex kw) {
		Input<?> in = ent.getInput(kw.keyword);
		if (in == null) {
			InputAgent.logError("Keyword %s could not be found for Entity %s.", kw.keyword, ent.getName());
			return;
		}

		InputAgent.apply(ent, in, kw);
		FrameBox.valueUpdate();
	}

	public static final void apply(Entity ent, Input<?> in, KeywordIndex kw) {
		// If the input value is blank, restore the default
		if (kw.numArgs() == 0) {
			in.reset();
		}
		else {
			in.parse(kw);
			in.setTokens(kw);
		}

		// Only mark the keyword edited if we have finished initial configuration
		if (InputAgent.recordEdits()) {
			in.setEdited(true);
			ent.setFlag(Entity.FLAG_EDITED);
			if (!ent.testFlag(Entity.FLAG_GENERATED) && in.isPromptReqd())
				sessionEdited = true;
		}

		ent.updateForInput(in);
	}

	public static void processKeyword(Entity entity, KeywordIndex key) {
		if (entity.testFlag(Entity.FLAG_LOCKED))
			throw new InputErrorException("Entity: %s is locked and cannot be modified", entity.getName());

		Input<?> input = entity.getInput( key.keyword );
		if (input != null) {
			InputAgent.apply(entity, input, key);
			FrameBox.valueUpdate();
			return;
		}

		if (!(entity instanceof Group))
			throw new InputErrorException("Not a valid keyword");

		Group grp = (Group)entity;
		grp.saveGroupKeyword(key);

		// Store the keyword data for use in the edit table
		for( int i = 0; i < grp.getList().size(); i++ ) {
			Entity ent = grp.getList().get( i );
			InputAgent.apply(ent, key);
		}
	}

	/*
	 * write input file keywords and values
	 *
	 * input file format:
	 *  Define Group { <Group names> }
	 *  Define <Object> { <Object names> }
	 *
	 *  <Object name> <Keyword> { < values > }
	 *
	 */
	public static void printInputFileKeywords() {
		// Create report file for the inputs
		String inputReportFileName = InputAgent.getReportFileName(InputAgent.getRunName() + ".inp");

		FileEntity inputReportFile = new FileEntity( inputReportFileName);
		inputReportFile.flush();

		// Loop through the entity classes printing Define statements
		for (ObjectType type : ObjectType.getAll()) {
			Class<? extends Entity> each = type.getJavaClass();

			// Loop through the instances for this entity class
			int count = 0;
			for (Entity ent : Entity.getInstanceIterator(each)) {
				boolean hasinput = false;

				for (Input<?> in : ent.getEditableInputs()) {
					if (in.isSynonym())
						continue;
					// If the keyword has been used, then add a record to the report
					if (in.getValueString().length() != 0) {
						hasinput = true;
						count++;
						break;
					}
				}

				if (hasinput) {
					String entityName = ent.getName();
					if ((count - 1) % 5 == 0) {
						inputReportFile.write("Define");
						inputReportFile.write("\t");
						inputReportFile.write(type.getName());
						inputReportFile.write("\t");
						inputReportFile.write("{ " + entityName);
						inputReportFile.write("\t");
					}
					else if ((count - 1) % 5 == 4) {
						inputReportFile.write(entityName + " }");
						inputReportFile.newLine();
					}
					else {
						inputReportFile.write(entityName);
						inputReportFile.write("\t");
					}
				}
			}

			if (!Entity.getInstanceIterator(each).hasNext()) {
				if (count % 5 != 0) {
					inputReportFile.write(" }");
					inputReportFile.newLine();
				}
				inputReportFile.newLine();
			}
		}

		for (ObjectType type : ObjectType.getAll()) {
			Class<? extends Entity> each = type.getJavaClass();

			// Get the list of instances for this entity class
			// sort the list alphabetically
			ArrayList<? extends Entity> cloneList = Entity.getInstancesOf(each);

			// Print the entity class name to the report (in the form of a comment)
			if (cloneList.size() > 0) {
				inputReportFile.write("\" " + each.getSimpleName() + " \"");
				inputReportFile.newLine();
				inputReportFile.newLine(); // blank line below the class name heading
			}

			Collections.sort(cloneList, new Comparator<Entity>() {
				@Override
				public int compare(Entity a, Entity b) {
					return a.getName().compareTo(b.getName());
				}
			});

			// Loop through the instances for this entity class
			for (int j = 0; j < cloneList.size(); j++) {

				// Make sure the clone is an instance of the class (and not an instance of a subclass)
				if (cloneList.get(j).getClass() == each) {
					Entity ent = cloneList.get(j);
					String entityName = ent.getName();
					boolean hasinput = false;

					// Loop through the editable keywords for this instance
					for (Input<?> in : ent.getEditableInputs()) {
						if (in.isSynonym())
							continue;

						// If the keyword has been used, then add a record to the report
						if (in.getValueString().length() != 0) {

							if (!in.getCategory().contains("Graphics")) {
								hasinput = true;
								inputReportFile.write("\t");
								inputReportFile.write(entityName);
								inputReportFile.write("\t");
								inputReportFile.write(in.getKeyword());
								inputReportFile.write("\t");
								if (in.getValueString().lastIndexOf("{") > 10) {
									String[] item1Array;
									item1Array = in.getValueString().trim().split(" }");

									inputReportFile.write("{ " + item1Array[0] + " }");
									for (int l = 1; l < (item1Array.length); l++) {
										inputReportFile.newLine();
										inputReportFile.write("\t\t\t\t\t");;
										inputReportFile.write(item1Array[l] + " } ");
									}
									inputReportFile.write("	}");
								}
								else {
									inputReportFile.write("{ " + in.getValueString() + " }");
								}
								inputReportFile.newLine();
							}
						}
					}
					// Put a blank line after each instance
					if (hasinput) {
						inputReportFile.newLine();
					}
				}
			}
		}

		// Close out the report
		inputReportFile.flush();
		inputReportFile.close();

	}

	public static void closeLogFile() {
		if (logFile == null)
			return;

		logFile.flush();
		logFile.close();

		if (numErrors ==0 && numWarnings == 0) {
			logFile.delete();
		}
		logFile = null;
	}

	private static final String errPrefix = "*** ERROR *** %s%n";
	private static final String inpErrPrefix = "*** INPUT ERROR *** %s%n";
	private static final String wrnPrefix = "***WARNING*** %s%n";

	public static int numErrors() {
		return numErrors;
	}

	public static int numWarnings() {
		return numWarnings;
	}

	private static void echoInputRecord(ArrayList<String> tokens) {
		if (logFile == null)
			return;

		boolean beginLine = true;
		for (int i = 0; i < tokens.size(); i++) {
			if (!beginLine)
				logFile.write("  ");
			String tok = tokens.get(i);
			logFile.write(tok);
			beginLine = false;
			if (tok.startsWith("\"")) {
				logFile.newLine();
				beginLine = true;
			}
		}
		// If there were any leftover string written out, make sure the line gets terminated
		if (!beginLine)
			logFile.newLine();

		logFile.flush();
	}

	private static void logBadInput(ArrayList<String> tokens, String msg) {
		InputAgent.echoInputRecord(tokens);
		InputAgent.logError("%s", msg);
	}

	public static void logMessage(String fmt, Object... args) {
		String msg = String.format(fmt, args);
		LogBox.logLine(msg);

		if (logFile == null)
			return;

		logFile.write(msg);
		logFile.newLine();
		logFile.flush();
	}

	public static void trace(int indent, Entity ent, String meth, String... text) {
		// Create an indent string to space the lines
		StringBuilder ind = new StringBuilder("");
		for (int i = 0; i < indent; i++)
			ind.append("   ");
		String spacer = ind.toString();

		// Print a TIME header every time time has advanced
		long traceTick = EventManager.simTicks();
		if (lastTickForTrace != traceTick) {
			System.out.format(" \nTIME = %.5f\n", EventManager.current().ticksToSeconds(traceTick));
			lastTickForTrace = traceTick;
		}

		// Output the traces line(s)
		System.out.format("%s%s %s\n", spacer, ent.getName(), meth);
		for (String line : text) {
			System.out.format("%s%s\n", spacer, line);
		}

		System.out.flush();
	}

	public static void logWarning(String fmt, Object... args) {
		numWarnings++;
		String msg = String.format(fmt, args);
		InputAgent.logMessage(wrnPrefix, msg);
	}

	public static void logError(String fmt, Object... args) {
		numErrors++;
		String msg = String.format(fmt, args);
		InputAgent.logMessage(errPrefix, msg);
	}

	public static void logInpError(String fmt, Object... args) {
		numErrors++;
		String msg = String.format(fmt, args);
		InputAgent.logMessage(inpErrPrefix, msg);
	}

	/**
	 * Prints the present state of the model to a new configuration file.
	 *
	 * @param fileName - the full path and file name for the new configuration file.
	 */
	public static void printNewConfigurationFileWithName( String fileName ) {

		// 1) WRITE LINES FROM THE ORIGINAL CONFIGURATION FILE

		// Copy the original configuration file up to the "RecordEdits" marker (if present)
		// Temporary storage for the copied lines is needed in case the original file is to be overwritten
		ArrayList<String> preAddedRecordLines = new ArrayList<>();
		if( InputAgent.getConfigFile() != null ) {
			try {
				BufferedReader in = new BufferedReader( new FileReader(InputAgent.getConfigFile()) );
				String line;
				while ( ( line = in.readLine() ) != null ) {
					preAddedRecordLines.add( line );
					if ( line.startsWith( recordEditsMarker ) ) {
						break;
					}
				}
				in.close();
			}
			catch ( Exception e ) {
				throw new ErrorException( e );
			}
		}

		// Create the new configuration file and copy the saved lines
		FileEntity file = new FileEntity( fileName);
		for( int i=0; i < preAddedRecordLines.size(); i++ ) {
			file.format("%s%n", preAddedRecordLines.get( i ));
		}

		// If not already present, insert the "RecordEdits" marker at the end of the original configuration file
		if( ! InputAgent.getRecordEditsFound() ) {
			file.format("%n%s%n", recordEditsMarker);
			InputAgent.setRecordEditsFound(true);
		}

		// 2) WRITE THE DEFINITION STATEMENTS FOR NEW OBJECTS

		// Determine all the new classes that were created
		ArrayList<Class<? extends Entity>> newClasses = new ArrayList<>();
		for (Entity ent : Entity.getAll()) {
			if (!ent.testFlag(Entity.FLAG_ADDED) || ent.testFlag(Entity.FLAG_GENERATED))
				continue;

			if (!newClasses.contains(ent.getClass()))
				newClasses.add(ent.getClass());
		}

		// Add a blank line before the first object definition
		if( !newClasses.isEmpty() )
			file.format("%n");

		// Print the first part of the "Define" statement for this object type
		for( Class<? extends Entity> newClass : newClasses ) {
			ObjectType o = ObjectType.getObjectTypeForClass(newClass);
			if (o == null)
				throw new ErrorException("Cannot find object type for class: " + newClass.getName());
			file.format("Define %s {", o.getName());

			// Print the new instances that were defined
			for (Entity ent : Entity.getAll()) {
				if (!ent.testFlag(Entity.FLAG_ADDED) || ent.testFlag(Entity.FLAG_GENERATED))
					continue;

				if (ent.getClass() == newClass)
					file.format(" %s ", ent.getName());

			}
			// Close the define statement
			file.format("}%n");
		}

		// 3) WRITE THE INPUTS FOR SPECIAL KEYWORDS THAT MUST COME BEFORE THE OTHERS
		for (Entity ent : Entity.getAll()) {
			if (!ent.testFlag(Entity.FLAG_EDITED))
				continue;
			if (ent.testFlag(Entity.FLAG_GENERATED))
				continue;

			boolean blankLinePrinted = false;
			for (int i = 0; i < EARLY_KEYWORDS.length; i++) {
				final Input<?> in = ent.getInput(EARLY_KEYWORDS[i]);
				if (in != null && in.isEdited()) {
					if (!blankLinePrinted) {
						file.format("%n");
						blankLinePrinted = true;
					}
					writeInputOnFile_ForEntity(file, ent, in);
				}
			}
		}

		// 4) WRITE THE INPUTS FOR KEYWORDS THAT WERE EDITED

		// Identify the entities whose inputs were edited
		for (Entity ent : Entity.getAll()) {
			if (!ent.testFlag(Entity.FLAG_EDITED))
				continue;
			if (ent.testFlag(Entity.FLAG_GENERATED))
				continue;

			file.format("%n");

			ArrayList<Input<?>> deferredInputs = new ArrayList<>();
			// Print the key inputs first
			for (Input<?> in : ent.getEditableInputs()) {
				if (in.isSynonym())
					continue;
				if (!in.isEdited() || matchesKey(in.getKeyword(), EARLY_KEYWORDS))
					continue;

				// defer all inputs outside the Key Inputs category
				if (!"Key Inputs".equals(in.getCategory())) {
					deferredInputs.add(in);
					continue;
				}

				writeInputOnFile_ForEntity(file, ent, in);
			}

			for (Input<?> in : deferredInputs) {
				writeInputOnFile_ForEntity(file, ent, in);
			}
		}

		// Close the new configuration file
		file.flush();
		file.close();

		sessionEdited = false;
	}

	private static boolean matchesKey(String key, String[] keys) {
		for (int i=0; i<keys.length; i++) {
			if (keys[i].equals(key))
				return true;
		}
		return false;
	}

	static void writeInputOnFile_ForEntity(FileEntity file, Entity ent, Input<?> in) {
		file.format("%s %s { %s }%n",
		            ent.getName(), in.getKeyword(), in.getValueString());
	}

	/**
	 * Prints the output report for the simulation run.
	 * @param simTime - simulation time at which the report is printed.
	 */
	public static void printReport(double simTime) {

		// Create the report file
		if (reportFile == null) {
			StringBuilder tmp = new StringBuilder("");
			tmp.append(InputAgent.getReportFileName(InputAgent.getRunName()));
			tmp.append(".rep");
			reportFile = new FileEntity(tmp.toString());
		}

		// Print run number header when multiple runs are to be performed
		if (Simulation.isMultipleRuns())
			reportFile.format("%s%n%n", Simulation.getRunHeader());

		// Identify the classes that were used in the model
		ArrayList<Class<? extends Entity>> newClasses = new ArrayList<>();
		for (Entity ent : Entity.getAll()) {

			if (ent.testFlag(Entity.FLAG_GENERATED))
				continue;

			if (!ent.isReportable())
				continue;

			if (!newClasses.contains(ent.getClass()))
				newClasses.add(ent.getClass());
		}

		// Sort the classes alphabetically by the names of their object types
		Collections.sort(newClasses, new ClassComparator());

		// Loop through the classes and identify the instances
		for (Class<? extends Entity> newClass : newClasses) {
			ArrayList<Entity> entList = new ArrayList<>();
			for (Entity ent : Entity.getAll()) {

				if (ent.testFlag(Entity.FLAG_GENERATED))
					continue;

				if (ent.getClass() == newClass)
					entList.add(ent);
			}

			// Sort the entities alphabetically by their names
			Collections.sort(entList, new EntityComparator());

			// Print a header for this class
			if (newClass != Simulation.class)
				reportFile.format("*** %s ***%n%n", ObjectType.getObjectTypeForClass(newClass));

			// Print each entity to the output report
			for (Entity ent : entList) {
				ent.printReport(reportFile, simTime);
				reportFile.format("%n");
			}
		}

		// Close the report file
		if (Simulation.isLastRun()) {
			reportFile.close();
			reportFile = null;
		}
	}

	private static class ClassComparator implements Comparator<Class<? extends Entity>> {
		@Override
		public int compare(Class<? extends Entity> class0, Class<? extends Entity> class1) {

			// Place the Simulation class in the first position
			if (class0 == Simulation.class && class1 == Simulation.class)
				return 0;
			if (class0 == Simulation.class && class1 != Simulation.class)
				return -1;
			if (class0 != Simulation.class && class1 == Simulation.class)
				return 1;

			// Sort alphabetically by Object Type name
			ObjectType ot0 = ObjectType.getObjectTypeForClass(class0);
			ObjectType ot1 = ObjectType.getObjectTypeForClass(class1);
			return ot0.getName().compareTo(ot1.getName());
		}
	}

	private static class EntityComparator implements Comparator<Entity> {
		@Override
		public int compare(Entity ent0, Entity ent1) {
			return ent0.getName().compareTo(ent1.getName());
		}
	}

	/**
	 * Returns the relative file path for the specified URI.
	 * <p>
	 * The path can start from either the folder containing the present
	 * configuration file or from the resources folder.
	 * <p>
	 * @param uri - the URI to be relativized.
	 * @return the relative file path.
	 */
	static public String getRelativeFilePath(URI uri) {

		// Relativize the file path against the resources folder
		String resString = resRoot.toString();
		String inputString = uri.toString();
		if (inputString.startsWith(resString)) {
			return String.format("<res>/%s", inputString.substring(resString.length()));
		}

		// Relativize the file path against the configuration file
		try {
			URI configDirURI = InputAgent.getConfigFile().getParentFile().toURI();
			return String.format("%s", configDirURI.relativize(uri).getPath());
		}
		catch (Exception ex) {
			return String.format("%s", uri.getPath());
		}
	}

	/**
	 * Loads the default configuration file.
	 */
	public static void loadDefault() {

		// Read the default configuration file
		InputAgent.readResource("<res>/inputs/default.cfg");

		// A RecordEdits marker in the default configuration must be ignored
		InputAgent.setRecordEditsFound(false);

		// Set the model state to unedited
		sessionEdited = false;
	}

	public static KeywordIndex formatPointsInputs(String keyword, ArrayList<Vec3d> points, Vec3d offset) {
		ArrayList<String> tokens = new ArrayList<>(points.size() * 6);
		for (Vec3d v : points) {
			tokens.add("{");
			tokens.add(String.format((Locale)null, "%.3f", v.x + offset.x));
			tokens.add(String.format((Locale)null, "%.3f", v.y + offset.y));
			tokens.add(String.format((Locale)null, "%.3f", v.z + offset.z));
			tokens.add("m");
			tokens.add("}");
		}

		// Parse the keyword inputs
		return new KeywordIndex(keyword, tokens, null);
	}

	public static KeywordIndex formatPointInputs(String keyword, Vec3d point, String unit) {
		ArrayList<String> tokens = new ArrayList<>(4);
		tokens.add(String.format((Locale)null, "%.6f", point.x));
		tokens.add(String.format((Locale)null, "%.6f", point.y));
		tokens.add(String.format((Locale)null, "%.6f", point.z));
		if (unit != null)
			tokens.add(unit);

		// Parse the keyword inputs
		return new KeywordIndex(keyword, tokens, null);
	}

	/**
	 * Split an input (list of strings) down to a single level of nested braces, this may then be called again for
	 * further nesting.
	 * @param input
	 * @return
	 */
	public static ArrayList<ArrayList<String>> splitForNestedBraces(List<String> input) {
		ArrayList<ArrayList<String>> inputs = new ArrayList<>();

		int braceDepth = 0;
		ArrayList<String> currentLine = null;
		for (int i = 0; i < input.size(); i++) {
			if (currentLine == null)
				currentLine = new ArrayList<>();

			currentLine.add(input.get(i));
			if (input.get(i).equals("{")) {
				braceDepth++;
				continue;
			}

			if (input.get(i).equals("}")) {
				braceDepth--;
				if (braceDepth == 0) {
					inputs.add(currentLine);
					currentLine = null;
					continue;
				}
			}
		}

		return inputs;
	}

	/**
	 * Converts a file path String to a URI.
	 * <p>
	 * The specified file path can be either relative or absolute. In the case
	 * of a relative file path, a 'context' folder must be specified. A context
	 * of null indicates an absolute file path.
	 * <p>
	 * To avoid bad input accessing an inappropriate file, a 'jail' folder can
	 * be specified. The URI to be returned must include the jail folder for it
	 * to be valid.
	 * <p>
	 * @param context - full file path for the folder that is the reference for relative file paths.
	 * @param filePath - string to be resolved to a URI.
	 * @param jailPrefix - file path to a base folder from which a relative cannot escape.
	 * @return the URI corresponding to the context and filePath.
	 */
	public static URI getFileURI(URI context, String filePath, String jailPrefix) throws URISyntaxException {

		// Replace all backslashes with slashes
		String path = filePath.replaceAll("\\\\", "/");

		int colon = path.indexOf(':');
		int openBrace = path.indexOf('<');
		int closeBrace = path.indexOf('>');
		int firstSlash = path.indexOf('/');

		// Add a leading slash if needed to convert from Windows format (e.g. from "C:" to "/C:")
		if (colon == 1)
			path = String.format("/%s", path);

		// 1) File path starts with a tagged folder, using the syntax "<tagName>/"
		URI ret = null;
		if (openBrace == 0 && closeBrace != -1 && firstSlash == closeBrace + 1) {
			String specPath = path.substring(openBrace + 1, closeBrace);

			// Resources folder in the Jar file
			if (specPath.equals("res")) {
				ret = new URI(resRoot.getScheme(), resRoot.getSchemeSpecificPart() + path.substring(closeBrace+2), null).normalize();

			}
		}
		// 2) Normal file path
		else {
			URI pathURI = new URI(null, path, null).normalize();

			if (context != null) {
				if (context.isOpaque()) {
					// Things are going to get messy in here
					URI schemeless = new URI(null, context.getSchemeSpecificPart(), null);
					URI resolved = schemeless.resolve(pathURI).normalize();

					// Note: we are using the one argument constructor here because the 'resolved' URI is already encoded
					// and we do not want to double-encode (and schemes should never need encoding, I hope)
					ret = new URI(context.getScheme() + ":" + resolved.toString());
				} else {
					ret = context.resolve(pathURI).normalize();
				}
			} else {
				// We have no context, so append a 'file' scheme if necessary
				if (pathURI.getScheme() == null) {
					ret = new URI("file", pathURI.getPath(), null);
				} else {
					ret = pathURI;
				}
			}
		}

		// Check that the file path includes the jail folder
		if (jailPrefix != null && ret.toString().indexOf(jailPrefix) != 0) {
			LogBox.format("Failed jail test: %s\n"
					+ "jail: %s\n"
					+ "context: %s\n",
					ret.toString(), jailPrefix, context.toString());
			LogBox.getInstance().setVisible(true);
			return null; // This resolved URI is not in our jail
		}

		return ret;
	}

	/**
	 * Determines whether or not a file exists.
	 * <p>
	 * @param filePath - URI for the file to be tested.
	 * @return true if the file exists, false if it does not.
	 */
	public static boolean fileExists(URI filePath) {

		try {
			InputStream in = filePath.toURL().openStream();
			in.close();
			return true;
		}
		catch (MalformedURLException ex) {
			return false;
		}
		catch (IOException ex) {
			return false;
		}
	}

}

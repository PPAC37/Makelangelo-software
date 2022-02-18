package com.marginallyclever.makelangelo.makeArt.io.vector;

import com.marginallyclever.convenience.ColorRGB;
import com.marginallyclever.convenience.CommandLineOptions;
import com.marginallyclever.makelangelo.Makelangelo;
import com.marginallyclever.makelangelo.Translator;
import com.marginallyclever.makelangelo.makeArt.ResizeTurtleToPaperAction;
import com.marginallyclever.makelangelo.paper.Paper;
import com.marginallyclever.makelangelo.plotter.Plotter;
import com.marginallyclever.makelangelo.plotter.plotterRenderer.Machines;
import com.marginallyclever.makelangelo.plotter.plotterRenderer.PlotterRenderer;
import com.marginallyclever.makelangelo.preview.Camera;
import com.marginallyclever.makelangelo.preview.PreviewPanel;
import com.marginallyclever.makelangelo.turtle.Turtle;
import com.marginallyclever.makelangelo.turtle.turtleRenderer.TurtleRenderFacade;
import com.marginallyclever.util.PreferencesHelper;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JPanel;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
/**
 * {@link LoadScratch3} loads a limited set of Scratch 3.0 commands into memory. 
 * We ignore monitors, which are visual displays of variables, booleans, and lists
 * They don't contain any real information we need.
 * 
 * See https://en.scratch-wiki.info/wiki/Scratch_File_Format
 * See https://github.com/LLK/scratch-blocks/tree/develop/blocks_vertical
 * 
 * @author Dan Royer
 * @since 7.31.0
 */
public class LoadScratch3 implements TurtleLoader {
	private static final Logger logger = LoggerFactory.getLogger(LoadScratch3.class);
	private final String PROJECT_JSON = "project.json";
	
	boolean verboseParse = false;
	
	private class Scratch3Variable implements Cloneable {
		public String name;
		
		public String uniqueID;
		public Object value;

		public Scratch3Variable(String name,String uniqueID,Object defaultValue) {
			this.name=name;
			this.uniqueID=uniqueID;
			this.value=defaultValue;
		}
		
		@Override
		public String toString() {
			return //uniqueID+" "+
					name+"="+value;
		}
	};
	
	private class Scratch3List {
		public String name;
		public ArrayList<Double> contents;

		public Scratch3List(String _name) {
			name=_name;
			contents=new ArrayList<Double>();
		}
	};
	
	@SuppressWarnings("serial")
	private class Scratch3Variables extends ArrayList<Scratch3Variable> {
		public Scratch3Variables deepCopy() {
			Scratch3Variables copy = new Scratch3Variables();
			for(Scratch3Variable v : this) {
				copy.add(new Scratch3Variable(v.name,v.uniqueID,null));
			}
			return copy;
		}
	}

	private class Scratch3Procedure {
		public String proccode;  // name of procedure
		public String uniqueID;
		public Scratch3Variables parameters = new Scratch3Variables();
		
		public Scratch3Procedure(String uniqueID,String proccode) {
			this.uniqueID = uniqueID;
			this.proccode = proccode;
		}
		
		public String toString() {
			return //uniqueID+" "+
					proccode+parameters.toString();
		}
	}
	
	private FileNameExtensionFilter filter = new FileNameExtensionFilter(Translator.get("FileTypeScratch3"),"SB3");
	
	private Scratch3Variables scratchGlobalVariables;
	private Stack<Scratch3Variables> myStack = new Stack<>();
	
	private List<Scratch3List> scratchLists = new ArrayList<>();
	private List<Scratch3Procedure> scratchProcedures = new ArrayList<>();
	private JSONObject blocks;
	private Set<String> blockKeys;
	private Turtle myTurtle;
	
	@Override
	public FileNameExtensionFilter getFileNameFilter() {
		return filter;
	}

	@Override
	public boolean canLoad(String filename) {
		String filenameExtension = filename.substring(filename.lastIndexOf('.')).toUpperCase();
		return filenameExtension.equalsIgnoreCase(".SB3");
	}
	
	@Override
	public Turtle load(InputStream in) throws Exception {
		if ( verboseParse ) logger.debug("Loading...");
		JSONObject tree = getTreeFromInputStream(in);
		
		if(!confirmAtLeastVersion3(tree)) throw new Exception("File must be at least version 3.0.0.");
		if(!confirmHasPenExtension(tree)) throw new Exception("File must include pen extension.");
		
		readScratchVariables(tree);
		readScratchLists(tree);
		findBlocks(tree);
		readScratchProcedures();
		readScratchInstructions();

		return myTurtle;
	}

	private JSONObject getTreeFromInputStream(InputStream in) throws FileNotFoundException, IOException {
		File tempZipFile = extractProjectJSON(in);
		
        if ( verboseParse ) logger.debug("Parsing JSON file...");
        JSONTokener tokener = new JSONTokener(tempZipFile.toURI().toURL().openStream());
        JSONObject tree = new JSONObject(tokener);

		tempZipFile.delete();
		
		return tree;
	}

	private File extractProjectJSON(InputStream in) throws FileNotFoundException, IOException {
		if ( verboseParse ) logger.debug("Searching for project.json...");
		try (ZipInputStream zipInputStream = new ZipInputStream(in)) {
			ZipEntry entry;
			File tempZipFile = null;
			while ((entry = zipInputStream.getNextEntry()) != null) {
				if (entry.getName().equals(PROJECT_JSON)) {
					if ( verboseParse ) logger.debug("Found project.json...");

					// read buffered stream into temp file.
					tempZipFile = File.createTempFile("project", "json");
					tempZipFile.setReadable(true);
					tempZipFile.setWritable(true);
					tempZipFile.deleteOnExit();
					try (FileOutputStream fos = new FileOutputStream(tempZipFile)) {
						byte[] buffer = new byte[2048];
						int len;
						while ((len = zipInputStream.read(buffer)) > 0) {
							fos.write(buffer, 0, len);
						}
						return tempZipFile;
					}
				}
			}
		}
		throw new FileNotFoundException("SB3 missing project.json");
	}

	private void findBlocks(JSONObject tree) throws Exception {
		JSONArray targets = (JSONArray)tree.get("targets");
		Iterator<?> targetIter = targets.iterator();
		while(targetIter.hasNext()) {
			JSONObject targetN = (JSONObject)targetIter.next();
			if( (Boolean)targetN.get("isStage") == true ) continue;
			blocks = targetN.getJSONObject("blocks");
			// we found the blocks.
			logger.debug("found {} blocks", blocks.length());
			// get the keys, too.
			blockKeys = blocks.keySet();
			
			return;
		}
		throw new Exception("targets > blocks missing");
	}
	
	int nbClicOnTheGreenFlag = 1;// Lets be basic one clic for now.
	/**
	 * parse blocks in scratch
	 * @param tree the JSONObject tree read from the project.json/zip file.
	 * @throws Exception
	 */
	private void readScratchInstructions() throws Exception {
		if ( verboseParse ) logger.debug("readScratchInstructions ( and do a flagclicked {} times ) ",nbClicOnTheGreenFlag);
		myTurtle = new Turtle();// needed to be init here in case multiple "event_whenflagclicked"
		int nbGreenFlagParsed_Total = 0;
		for (int i = 0; i < nbClicOnTheGreenFlag; i++) {
			// find the first block with opcode=event_whenflagclicked.
			int block_opcode_event_whenflagclicked__count = 0;

			for (String k : blockKeys) {
				Object getTmp = blocks.get(k);
				if (getTmp instanceof JSONObject) {
					JSONObject block = (JSONObject) getTmp;
					final String key_scratch_block_opcode = "opcode";
					if (block.has(key_scratch_block_opcode)) {
						String opcode = block.getString(key_scratch_block_opcode);
						if (opcode.equals("event_whenflagclicked")) {
							parseScratchCode(k);// TODO if multiple event_whenflagclicked this is in the Scratch3 interpretor reverse order ...
							//return; // nop can have multiple "event_whenflagclicked"
							block_opcode_event_whenflagclicked__count++;
						}
					} else {
						// starge no opcode ... but to be resilient no exception .
						logger.debug("no {} for block {} : {} // instanceof {}",key_scratch_block_opcode, k, getTmp, getTmp.getClass().toString());
					}
				} else {					
					if ( getTmp != null ){
						// somethinge is not what expected ... maybe a lost variable in the scratch projet ...
						logger.debug("not expected for block {} : {} // instanceof {}", k, getTmp, getTmp.getClass().toString());
					}else{
						// normaly should not happend but juste to be sure ...
						logger.debug("not expected for block {} : {} // null", k, getTmp);
					}
				}
			}
			nbGreenFlagParsed_Total += block_opcode_event_whenflagclicked__count;
		}
		if (nbGreenFlagParsed_Total == 0) {
			throw new Exception("WhenFlagClicked block not found.");
		}
	}
	
	private JSONObject getBlock(String key) {
		return blocks.getJSONObject(key);
	}
	
	private String findNextBlockKey(JSONObject currentBlock) {
		Object key = currentBlock.opt("next");
		if(key==null || key == JSONObject.NULL) return null;
		return (String)key;
	}
	
	private void parseScratchCode(String currentKey) throws Exception {
		if ( verboseParse ) logger.debug("parseScratchCode {}",currentKey);
		JSONObject currentBlock = getBlock(currentKey);
				
		while(currentBlock!=null) {
			String opcode = (String)currentBlock.get("opcode");			
			switch(opcode) {
			// control blocks start
			case "event_whenflagclicked":	doStart(currentBlock);					break;
			case "control_repeat":			doRepeat(currentBlock);  				break;
			case "control_repeat_until":	doRepeatUntil(currentBlock);			break;
			case "control_forever":			doRepeatForever(currentBlock);			break;
			case "control_if":				doIf(currentBlock);						break;
			case "control_if_else":			doIfElse(currentBlock);					break;
			case "control_stop":
				//throw new Exception("control_stop not supported.");
				return;
			case "procedures_call":			doCall(currentBlock);					break;
			// control blocks end

			case "data_setvariableto":		setVariableTo(currentBlock);			break;
			case "data_changevariableby":	changeVariableBy(currentBlock);			break;
/*			case "data_variable":													break;
			case "data_hidevariable":												break;
			case "data_showvariable":												break;
			case "data_listcontents":												break;
			case "data_addtolist":													break;
			case "data_deleteoflist":												break;
			case "data_deletealloflist":											break;
			case "data_insertatlist":												break;
			case "data_replaceitemoflist":											break;
			case "data_itemoflist":													break;
			case "data_itemnumoflist":												break;
			case "data_lengthoflist":												break;
			case "data_listcontainsitem":											break;
*/
			case "motion_gotoxy": 			doMotionGotoXY(currentBlock);  			break;
			case "motion_pointindirection": doMotionPointInDirection(currentBlock);	break;
			case "motion_turnleft":			doMotionTurnLeft(currentBlock);  		break;
			case "motion_turnright":		doMotionTurnRight(currentBlock);  		break;
			case "motion_movesteps":		doMotionMoveSteps(currentBlock);		break;
			//case "motion_pointtowards": 	doMotionPointTowards(currentBlock);  break;
			case "motion_changexby": 		doMotionChangeX(currentBlock);  		break;
			case "motion_changeyby": 		doMotionChangeY(currentBlock);  		break;
			case "motion_setx": 			doMotionSetX(currentBlock);  			break;
			case "motion_sety": 			doMotionSetY(currentBlock);  			break;
			case "pen_penDown":				myTurtle.penDown();						break;
			case "pen_penUp":				myTurtle.penUp();						break;
			case "pen_setPenColorToColor":	doSetPenColor(currentBlock);			break;
			case "pen_changePenColorParamBy":	doSetChangeColorBy(currentBlock);break;
			case "pen_setPenHueToNumber":	doSetPenHueToNumber(currentBlock);break;
			//
			default: logger.debug("Ignored {}", opcode);
			}

			currentKey = findNextBlockKey(currentBlock);
			if(currentKey==null) break;
			
			if ( verboseParse ) logger.debug("next block {}",currentKey);
			currentBlock = getBlock(currentKey);
		}
	}

	private void doStart(JSONObject currentBlock) {
		if ( verboseParse ) logger.debug("START a block opcode event_whenflagclicked ...");
	}

	private void doIfElse(JSONObject currentBlock) throws Exception {
		if ( verboseParse ) logger.debug("IF ELSE");
		String condition = (String)findInputInBlock(currentBlock,"CONDITION");
		String substack = (String)findInputInBlock(currentBlock,"SUBSTACK");
		String substack2 = (String)findInputInBlock(currentBlock,"SUBSTACK2");
		if(resolveBoolean(getBlock(condition))) {
			parseScratchCode(substack);
		} else {
			parseScratchCode(substack2);
		}
	}
	
	private void doCall(JSONObject currentBlock) throws Exception {
		String proccode = (String)findMutationInBlock(currentBlock,"proccode");
		ArrayList<Object> args = resolveArgumentsForProcedure(currentBlock);
		if ( verboseParse ) logger.debug("CALL {}({})",proccode,args.toString());
		
		Scratch3Procedure p = findProcedureWithProccode(proccode);
		pushStack(p,args);
		parseScratchCode(getBlock(p.uniqueID).getString("next"));
		myStack.pop();
	}
	
	private ArrayList<Object> resolveArgumentsForProcedure(JSONObject currentBlock) throws Exception {
		ArrayList<Object> args = new ArrayList<>();
		
		JSONObject inputs = currentBlock.getJSONObject("inputs");
		JSONArray argumentids = new JSONArray((String)findMutationInBlock(currentBlock,"argumentids"));
		Iterator<Object> iter = argumentids.iterator();
		while(iter.hasNext()) {
			JSONArray key = (JSONArray)inputs.get((String)iter.next());
			args.add(resolveValue(key.get(1)));
		}

		return args;
	}

	// copy the parameters, set the values based on what was passed into the procedure, and then push that onto the stack.
	private void pushStack(Scratch3Procedure p, ArrayList<Object> args) {
		Scratch3Variables list = p.parameters.deepCopy();
		for(int i=0;i<list.size();++i) {
			list.get(i).value = args.get(i);
		}
		
		myStack.push(list);
	}

	private Scratch3Procedure findProcedureWithProccode(String proccode) {
		Iterator<Scratch3Procedure> iter = scratchProcedures.iterator();
		while(iter.hasNext()) {
			Scratch3Procedure p = iter.next();
			if(p.proccode.equals(proccode)) return p;
		}
		return null;
	}

	private void doIf(JSONObject currentBlock) throws Exception {
		if ( verboseParse ) logger.debug("IF");
		String condition = (String)findInputInBlock(currentBlock,"CONDITION");
		String substack = (String)findInputInBlock(currentBlock,"SUBSTACK");
		if(resolveBoolean(getBlock(condition))) {
			parseScratchCode(substack);
		}
	}

	int loopNbCountInstadeOfForever = 1;// resiliant : we can draw something, but it maybe incomplet ...
	boolean foreverThrowAnException = false;// To be resiliant and user friendly this is not an error...
	/**
	 * dummy doReapeatForever. N.B. : For the current Makelangelo implementation
	 * we need to assert the Scratch programme have an end. (is not infinit ...
	 * This is the easy case.) ReapeatForever have to be altered (loop only n
	 * time) or seen as an error.
	 *
	 *
	 * @param currentBlock
	 * @throws Exception
	 */
	private void doRepeatForever(JSONObject currentBlock) throws Exception {
		if ( foreverThrowAnException ){
			throw new Exception(Translator.get("LoadScratch3.foreverNotAllowed"));
		}		
		if ( verboseParse ) logger.debug("REPEAT FOREVER ( if allowed ({}) will only repeat {} times. )",!foreverThrowAnException,loopNbCountInstadeOfForever);
		String substack = (String)findInputInBlock(currentBlock,"SUBSTACK");		
		for (int i = 0 ; i < loopNbCountInstadeOfForever ; i++){			
			//while(true) { // technically this would work and the program would never end.  It is here for reference.
				parseScratchCode(substack);
			//}
		}		
	}

	private void doRepeatUntil(JSONObject currentBlock) throws Exception {
		if ( verboseParse ) logger.debug("REPEAT UNTIL");
		String condition = (String)findInputInBlock(currentBlock,"CONDITION");
		String substack = (String)findInputInBlock(currentBlock,"SUBSTACK");
		
		while(!resolveBoolean(getBlock(condition))) {
			parseScratchCode(substack);
		}
	}

	private void doRepeat(JSONObject currentBlock) throws Exception {
		int count = (int)resolveValue(findInputInBlock(currentBlock,"TIMES"));
		String substack = (String)findInputInBlock(currentBlock,"SUBSTACK");
		if ( verboseParse ) logger.debug("REPEAT {}",count);
		for(int i=0;i<count;++i) {
			parseScratchCode(substack);
		}		
	}

	// relative change
	private void changeVariableBy(JSONObject currentBlock) throws Exception {
		Scratch3Variable v = getScratchVariable((String)findFieldsInBlock(currentBlock,"VARIABLE"));
		double newValue = resolveValue(findInputInBlock(currentBlock,"VALUE"));
		// set and report
		v.value = (double)v.value + newValue;
		if ( verboseParse ) logger.debug("Set {} to {}", v.name, v.value);
	}

	// absolute change
	private void setVariableTo(JSONObject currentBlock) throws Exception {
		Scratch3Variable v = getScratchVariable((String)findFieldsInBlock(currentBlock,"VARIABLE"));
		double newValue = resolveValue(findInputInBlock(currentBlock,"VALUE"));
		// set and report
		v.value = newValue;
		if ( verboseParse ) logger.debug("Set {} to {}", v.name, v.value);
	}

	private void doMotionGotoXY(JSONObject currentBlock) throws Exception {
		double px = resolveValue(findInputInBlock(currentBlock,"X"));
		double py = resolveValue(findInputInBlock(currentBlock,"Y"));
		if ( verboseParse ) logger.debug("GOTO {} {}",px,py);
		myTurtle.moveTo(px, py);
	}

	private void doMotionPointInDirection(JSONObject currentBlock) throws Exception {
		double v = resolveValue(findInputInBlock(currentBlock,"DIRECTION"));
		if ( verboseParse ) logger.debug("POINT AT {}",v);
		myTurtle.setAngle(v-90.0);// 0° orientation in turtle = 90° orientation in scratch.
	}
	
	private void doMotionTurnLeft(JSONObject currentBlock) throws Exception {
		double v = resolveValue(findInputInBlock(currentBlock,"DEGREES"));
		if ( verboseParse ) logger.debug("LEFT {}",v);
		myTurtle.setAngle(myTurtle.getAngle()+v);//myTurtle.turn(v);
	}
	
	private void doMotionTurnRight(JSONObject currentBlock) throws Exception {
		double v = resolveValue(findInputInBlock(currentBlock,"DEGREES"));
		if ( verboseParse ) logger.debug("RIGHT {}",v);
		myTurtle.setAngle(myTurtle.getAngle()-v);//myTurtle.turn(-v);
	}

	private void doMotionMoveSteps(JSONObject currentBlock) throws Exception {
		double v = resolveValue(findInputInBlock(currentBlock,"STEPS"));
		if ( verboseParse ) logger.debug("MOVE {}",v);
		myTurtle.forward(v);
	}
	
	private void doMotionChangeX(JSONObject currentBlock) throws Exception {
		double v = resolveValue(findInputInBlock(currentBlock,"DX"));
		if ( verboseParse ) logger.debug("MOVE X {}",v);
		myTurtle.moveTo(myTurtle.getX()+v,myTurtle.getY());
	}

	private void doMotionChangeY(JSONObject currentBlock) throws Exception {
		double v = resolveValue(findInputInBlock(currentBlock,"DY"));
		if ( verboseParse ) logger.debug("MOVE Y {}",v);
		myTurtle.moveTo(myTurtle.getX(),myTurtle.getY()+v);
		
	}

	private void doMotionSetX(JSONObject currentBlock) throws Exception {
		double v = resolveValue(findInputInBlock(currentBlock,"X"));
		if ( verboseParse ) logger.debug("SET X {}",v);
		myTurtle.moveTo(v,myTurtle.getY());
	}

	private void doMotionSetY(JSONObject currentBlock) throws Exception {
		double v = resolveValue(findInputInBlock(currentBlock,"Y"));
		if ( verboseParse ) logger.debug("SET Y {}",v);
		myTurtle.moveTo(myTurtle.getX(),v);
	}
	
	boolean ignoreDoSetPenColor = false; // As setColor can bug the Makelangelo Render a quick/bad hack to enable/disable this implementation.
	private void doSetPenColor(JSONObject currentBlock) throws Exception {
		ColorRGB c = new ColorRGB((int)resolveValue(findInputInBlock(currentBlock,"COLOR")));		
		if ( !ignoreDoSetPenColor ){
			if ( verboseParse ) logger.debug("SET COLOR {}",c);
			myTurtle.setColor(c);
		}else{
			if ( verboseParse ) logger.debug("SET COLOR {} ignored",c);
		}
	}
	boolean ignoredoChangePenColorParamBy = false;
	/**
	 * TODO 
	 * @param currentBlock
	 * @throws Exception 
	 */
	private void doSetChangeColorBy(JSONObject currentBlock) throws Exception {
		if ( verboseParse ) logger.debug("pen_changePenColorParamBy COLOR");
		JSONObject inputs = (JSONObject) currentBlock.get("inputs");
		JSONArray jColor = (JSONArray) inputs.get("VALUE");
		double dColor = resolveValue(jColor.get(1));
		//??COLOR_PARAM = color ?
		//?? HSB/HSV colors cf https://en.scratch-wiki.info/wiki/Computer_Colors
		//https://stackoverflow.com/questions/2997656/how-can-i-use-the-hsl-colorspace-in-java
		
		Color cOld = new Color(myTurtle.getColor().toInt());
		float[] RGBtoHSB = cOld.RGBtoHSB(cOld.getRed(), cOld.getGreen(), cOld.getBlue(), null);
		float hue = RGBtoHSB[0];// 0.9f; //hue
		float saturation = RGBtoHSB[1];//1.0f; //saturation
		float brightness = RGBtoHSB[2];//0.8f; //brightness

		//???
		hue = hue + (float)(360.0/dColor);
		
		Color cTmp = Color.getHSBColor(hue, saturation, brightness);
		if ( !ignoredoChangePenColorParamBy )
		myTurtle.setColor(new ColorRGB(cTmp));
		// KO not rgb color .... // myTurtle.setColor(new ColorRGB((int)(myTurtle.getColor().toInt()+((int)dColor))));
	}
	private void doSetPenHueToNumber(JSONObject currentBlock) throws Exception {
		logger.debug("pen_setPenHueToNumber COLOR");
		JSONObject inputs = (JSONObject) currentBlock.get("inputs");
		JSONArray jColor = (JSONArray) inputs.get("HUE");
		double dColor = resolveValue(jColor.get(1));
		//??COLOR_PARAM = color ?
		//?? HSB/HSV colors cf https://en.scratch-wiki.info/wiki/Computer_Colors
		//https://stackoverflow.com/questions/2997656/how-can-i-use-the-hsl-colorspace-in-java
		
//		Color cOld = new Color(myTurtle.getColor().toInt());
//		float[] RGBtoHSB = cOld.RGBtoHSB(cOld.getRed(), cOld.getGreen(), cOld.getBlue(), null);
		float hue =(float)dColor ; //hue
		float saturation = 0.84f; //saturation
		float brightness = 0.5f; //brightness

		Color cTmp = Color.getHSBColor(hue, saturation, brightness);
		if ( !ignoredoChangePenColorParamBy)
		myTurtle.setColor(new ColorRGB(cTmp));
		// KO not rgb color .... // myTurtle.setColor(new ColorRGB((int)(myTurtle.getColor().toInt()+((int)dColor))));
	}
	/**
	 * Find and return currentBlock/fields/subKey/(first element). 
	 * @param currentBlock the block to search.
	 * @param subKey the key name inside currentBlock.
	 * @return the first element of currentBlock/inputs/subKey
	 * @throws Exception if any part of the operation fails, usually because of non-existent key.
	 */
	private Object findFieldsInBlock(JSONObject currentBlock,String subKey) throws Exception {
		JSONObject inputs = currentBlock.getJSONObject("fields");
		JSONArray subKeyArray = (JSONArray)inputs.get(subKey);
		return subKeyArray.get(1);
	}
		
	/**
	 * Find and return currentBlock/inputs/subKey/(first element). 
	 * @param currentBlock the block to search.
	 * @param subKey the key name inside currentBlock.
	 * @return the first element of currentBlock/inputs/subKey
	 * @throws Exception if any part of the operation fails, usually because of non-existent key.
	 */
	private Object findInputInBlock(JSONObject currentBlock,String subKey) throws Exception {
		JSONObject inputs = currentBlock.getJSONObject("inputs");
		JSONArray subKeyArray = (JSONArray)inputs.get(subKey);
		return subKeyArray.get(1);
	}

	/**
	 * Find and return currentBlock/mutation/subKey. 
	 * @param currentBlock the block to search.
	 * @param subKey the key name inside currentBlock.
	 * @return the element currentBlock/mutation/subKey
	 * @throws Exception if any part of the operation fails, usually because of non-existent key.
	 */
	private Object findMutationInBlock(JSONObject currentBlock,String subKey) throws Exception {
		JSONObject mutation = currentBlock.getJSONObject("mutation");
		return mutation.get(subKey);
	}

	/**
	 * Find and return the variable with uniqueID.  Search the top of myStack first, then the globals. 
	 * @param uniqueID the id to match.
	 * @return the variable found.
	 * @throws Exception if variable not found.
	 */
	private Scratch3Variable getScratchVariable(String uniqueID) throws Exception {
		if(!myStack.isEmpty()) {
			for(Scratch3Variable sv : myStack.peek()) {
				if(sv.uniqueID.equals(uniqueID)) return sv;
			}
		}
		
		for(Scratch3Variable sv : scratchGlobalVariables) {
			if(sv.uniqueID.equals(uniqueID)) return sv;
		}
		
		throw new Exception("Variable '"+uniqueID+"' not found.");
	}

	/**
	 * Confirm this is version 3
	 * @param tree the JSONObject tree read from the project.json/zip file.
	 * @throws Exception
	 */
	private boolean confirmAtLeastVersion3(JSONObject tree) throws Exception {
		JSONObject meta = (JSONObject)tree.get("meta");  // this cannot be getJSONObject because it changes the exception response.
		if(meta==null) return false;
		
		String semver = (String)meta.get("semver");  // this cannot be getJSONObject because it changes the exception response.
		if(semver==null) return false;
		
		return ( semver.compareTo("3.0.0") <= 0 ); 
	}
	
	private boolean confirmHasPenExtension(JSONObject tree) throws Exception {
		JSONArray extensions = (JSONArray)tree.get("extensions");  // this cannot be getJSONObject because it changes the exception response.
		if(extensions==null) return false;
		
		Iterator<Object> i = extensions.iterator();
		while(i.hasNext()) {
			Object o = i.next();
			if(o instanceof String && o.equals("pen")) return true;
		}
		return false;
	}
	
	/**
	 * read the list of Scratch variables
	 * @param tree the JSONObject tree read from the project.json/zip file.
	 * @throws Exception
	 */
	private void readScratchVariables(JSONObject tree) throws Exception {
		if ( verboseParse ) logger.debug("readScratchVariables");
		scratchGlobalVariables = new Scratch3Variables();
		JSONArray targets = tree.getJSONArray("targets");
		Iterator<?> targetIter = targets.iterator();
		while(targetIter.hasNext()) {
			JSONObject targetN = (JSONObject)targetIter.next();
//			if( (Boolean)targetN.get("isStage") == false ) continue;
			
			JSONObject variables = targetN.getJSONObject("variables");
			Iterator<?> keys = variables.keySet().iterator();
			while(keys.hasNext()) {
				String k=(String)keys.next();
				JSONArray details = variables.getJSONArray(k);
				String name = details.getString(0);
				Object valueUnknown = details.get(1);
				Number value;
				try {
				if(valueUnknown instanceof String) value = Double.parseDouble((String)valueUnknown); 
				else value = (Number)valueUnknown;
					double d = value.doubleValue();
					if ( verboseParse ) logger.debug("Variable {} {} {}", k, name, d);
					scratchGlobalVariables.add(new Scratch3Variable(name,k,d));
				} catch (Exception e) {
					// TODO special case a texte varaible value ...
					scratchGlobalVariables.add(new Scratch3Variable(name,k,valueUnknown));
					//throw new Exception("Variables must be numbers. "+name+" : \""+valueUnknown+"\"", e);
				}
			}
		}
	}

	/**
	 * read the list of Scratch lists
	 * @param tree the JSONObject tree read from the project.json/zip file.
	 * @throws Exception
	 */
	private void readScratchLists(JSONObject tree) throws Exception {
		if ( verboseParse ) logger.debug("readScratchLists");
		JSONArray targets = tree.getJSONArray("targets");
		Iterator<?> targetIter = targets.iterator();
		while(targetIter.hasNext()) {
			JSONObject targetN = (JSONObject)targetIter.next();
			if( (Boolean)targetN.get("isStage") == false ) continue;
			JSONObject listOfLists = targetN.getJSONObject("lists");
			if(listOfLists == null) return;
			Set<?> keys = listOfLists.keySet();
			Iterator<?> keyIter = keys.iterator();
			while( keyIter.hasNext() ) {
				String key = (String)keyIter.next();
				if ( verboseParse ) logger.debug("list key:{}", key);
				JSONArray elem = listOfLists.getJSONArray(key);
				String listName = elem.getString(0);
				if ( verboseParse ) logger.debug("  list name:{}", listName);
				Object contents = elem.get(1);
				Scratch3List list = new Scratch3List(listName);
				// fill the list with any given contents
				if( contents != null && contents instanceof JSONArray ) {
					JSONArray arr = (JSONArray)contents;

					Iterator<?> scriptIter = arr.iterator();
					while(scriptIter.hasNext()) {
						Object varValue = scriptIter.next();
						double value;
						if(varValue instanceof Number) {
							Number num = (Number)varValue;
							value = (float)num.doubleValue();
							if ( verboseParse ) logger.debug("  list float:{}", value);
							list.contents.add(value);
						} else if(varValue instanceof String) {
							try {
								value = Double.parseDouble((String)varValue);
								if ( verboseParse ) logger.debug("  list string:{}", value);
								list.contents.add(value);
							} catch (Exception e) {
								throw new Exception("List variables must be numbers.", e);
							}
						} else throw new Exception("List variable "+listName+"("+list.contents.size()+") is "+varValue.toString());
					}
				}
				// add the list to the list-of-lists.
				scratchLists.add(list);		
			}
		}
	}
	
	/**
	 * Read in and store the description of procedures (methods)
	 * @throws Exception
	 */
	private void readScratchProcedures() throws Exception {
		if ( verboseParse ) logger.debug("readScratchProcedures");

		// find the blocks with opcode=procedures_definition.
		for( String k : blockKeys ) {
			String uniqueID = k.toString();
			Object obj = blocks.get(uniqueID);
			if(!(obj instanceof JSONObject)) continue;

			JSONObject currentBlock = blocks.getJSONObject(uniqueID);
			String opcode = currentBlock.getString("opcode");
			if(opcode.equals("procedures_definition")) {
				// the procedures_definition block points to the procedures_prototype block
				JSONObject prototypeBlock = getBlock((String)findInputInBlock(currentBlock,"custom_block"));
				// which contains the human-readable name of the procedure
				String proccode = (String)findMutationInBlock(prototypeBlock,"proccode");
				
				Scratch3Procedure p = new Scratch3Procedure(uniqueID,proccode);
				scratchProcedures.add(p);
				buildParameterListForProcedure(prototypeBlock,p);
				if ( verboseParse ) logger.debug("procedure found: {}",p.toString());
			}
		}
	}
	
	private void buildParameterListForProcedure(JSONObject prototypeBlock, Scratch3Procedure p) throws Exception {
		JSONArray argumentIDs = new JSONArray((String)findMutationInBlock(prototypeBlock,"argumentids"));
		JSONArray argumentNames = new JSONArray((String)findMutationInBlock(prototypeBlock,"argumentnames"));

		//JSONArray argumentDefaults = new JSONArray((String)findMutationInBlock(prototypeBlock,"argumentdefaults"));
		for(int i=0;i<argumentIDs.length();++i) {
			String uniqueID = argumentIDs.getString(i);
			//String defaultValue = argumentDefaults.getString(i);
			String name = argumentNames.getString(i);

			p.parameters.add(new Scratch3Variable(name,uniqueID,0/*defaultValue*/));
			// TODO set defaults?
		}
	}

	/**
	 * Scratch block contains a boolean or boolean operator
	 * @param obj a String, Number, or JSONArray of elements to be calculated. 
	 * @return the calculated final value.
	 * @throws Exception
	 */
	private boolean resolveBoolean(JSONObject currentBlock) throws Exception {
		if(currentBlock.has("opcode")) {
			// is equation
			String opcode = currentBlock.getString("opcode");
			switch(opcode) {
			case "operator_lt":		return doLessThan(currentBlock);
			case "operator_gt":		return doGreaterThan(currentBlock);
			case "operator_equals": return doEquals(currentBlock);
			case "operator_and":	return doAnd(currentBlock);
			case "operator_or":		return doOr(currentBlock);
			case "operator_not":	return doNot(currentBlock);
			default: throw new Exception("resolveBoolean unsupported opcode "+opcode);
			}
		}
		throw new Exception("Parse error (resolveBoolean missing opcode)");
	}
	
	private boolean doNot(JSONObject currentBlock) throws Exception {
		JSONObject inputs = currentBlock.getJSONObject("inputs");
		JSONArray OPERAND = (JSONArray)inputs.getJSONArray("OPERAND");
		boolean a = resolveBoolean(getBlock(OPERAND.getString(1)));
		return !a;
	}

	private boolean doOr(JSONObject currentBlock) throws Exception {
		JSONObject inputs = currentBlock.getJSONObject("inputs");
		JSONArray OPERAND1 = (JSONArray)inputs.getJSONArray("OPERAND1");
		JSONArray OPERAND2 = (JSONArray)inputs.getJSONArray("OPERAND2");
		boolean a = resolveBoolean(getBlock(OPERAND1.getString(1)));
		boolean b = resolveBoolean(getBlock(OPERAND2.getString(1)));
		return a || b;
	}

	private boolean doAnd(JSONObject currentBlock) throws Exception {
		JSONObject inputs = currentBlock.getJSONObject("inputs");
		JSONArray OPERAND1 = (JSONArray)inputs.getJSONArray("OPERAND1");
		JSONArray OPERAND2 = (JSONArray)inputs.getJSONArray("OPERAND2");
		boolean a = resolveBoolean(getBlock(OPERAND1.getString(1)));
		boolean b = resolveBoolean(getBlock(OPERAND2.getString(1)));
		return a && b;
	}

	private boolean doEquals(JSONObject currentBlock) throws Exception {
		JSONObject inputs = currentBlock.getJSONObject("inputs");
		JSONArray OPERAND1 = (JSONArray)inputs.getJSONArray("OPERAND1");
		JSONArray OPERAND2 = (JSONArray)inputs.getJSONArray("OPERAND2");
		double a = resolveValue(OPERAND1.get(1));
		double b = resolveValue(OPERAND2.get(1)); 
		return a == b;
	}

	private boolean doGreaterThan(JSONObject currentBlock) throws Exception {
		JSONObject inputs = currentBlock.getJSONObject("inputs");
		JSONArray OPERAND1 = (JSONArray)inputs.getJSONArray("OPERAND1");
		JSONArray OPERAND2 = (JSONArray)inputs.getJSONArray("OPERAND2");
		double a = resolveValue(OPERAND1.get(1));
		double b = resolveValue(OPERAND2.get(1)); 
		return a > b;
	}

	private boolean doLessThan(JSONObject currentBlock) throws Exception {
		JSONObject inputs = currentBlock.getJSONObject("inputs");
		JSONArray OPERAND1 = (JSONArray)inputs.getJSONArray("OPERAND1");
		JSONArray OPERAND2 = (JSONArray)inputs.getJSONArray("OPERAND2");
		double a = resolveValue(OPERAND1.get(1));
		double b = resolveValue(OPERAND2.get(1)); 
		return a < b;
	}

	/**
	 * Scratch block contains an Operator (variable, constant, or math combination of the two). 
	 * @param obj a String, Number, or JSONArray of elements to be calculated.
	 * @return the calculated final value.
	 * @throws Exception
	 */
	private double resolveValue(Object currentObject) throws Exception {
		if(currentObject instanceof JSONArray) {
			JSONArray currentArray = (JSONArray)currentObject;
			switch(currentArray.getInt(0)) {
			case 4:  // number
			case 5:  // positive number
			case 6:  // positive integer
			case 7:  // integer
			case 8:  // angle
			case 9:  // color (#rrggbbaa)			
			case 10:  // string, try to parse as number
				return parseNumber(currentArray.get(1));
			case 12:  // variable
				return (double)getScratchVariable(currentArray.getString(2)).value; 
			// 13 is list [name,id,x,y]
			default: throw new Exception("resolveValue unknown value type "+currentArray.getInt(0));
			}
		} else if(currentObject instanceof String) {
			JSONObject currentBlock = getBlock((String)currentObject);
			// is equation
			String opcode = currentBlock.getString("opcode");
			switch(opcode) {
			case "operator_add":					return doAdd(currentBlock);
			case "operator_subtract":				return doSubstract(currentBlock);
			case "operator_multiply":				return doMultiply(currentBlock);
			case "operator_divide":					return doDivide(currentBlock);
			case "operator_mod":					return doModulus(currentBlock);
			case "operator_random":					return doRandom(currentBlock);
			case "operator_mathop":					return doMathOp(currentBlock);
			case "operator_round":					return doRound(currentBlock);
			case "motion_direction":				return doMotionDirection(currentBlock);
			case "motion_xposition":				return doMotionXPosition(currentBlock);
			case "motion_yposition":				return doMotionYPosition(currentBlock);
			case "argument_reporter_string_number":	return (double)doReporterStringValue(currentBlock);
			case "argument_reporter_boolean":		return (double)doReporterStringValue(currentBlock);
			case "sensing_answer":		return 0.0f;//(double)doReporterStringValue(currentBlock);
			default: throw new Exception("resolveValue unsupported opcode "+opcode);
			}
		}
		throw new Exception("resolveValue unknown object type "+currentObject.getClass().getSimpleName());
	}
	
	private double parseNumber(Object object) {
		if(object instanceof String) {
			String str = (String)object;
			if(str.startsWith("#")) return (double)Integer.parseInt(str.substring(1),16);
			return Double.parseDouble(str);
		} else if(object instanceof Double) 
			return (double)object;
		else //if(object instanceof Integer)
			return (double)(int)object;
	}

	private double doAdd(JSONObject currentBlock) throws Exception {
		JSONObject inputs = currentBlock.getJSONObject("inputs");
		JSONArray NUM1 = inputs.getJSONArray("NUM1");
		JSONArray NUM2 = inputs.getJSONArray("NUM2");
		double a = resolveValue(NUM1.get(1));
		double b = resolveValue(NUM2.get(1));
		return a + b;
	}

	private double doSubstract(JSONObject currentBlock) throws Exception {
		JSONObject inputs = currentBlock.getJSONObject("inputs");
		JSONArray NUM1 = inputs.getJSONArray("NUM1");
		JSONArray NUM2 = inputs.getJSONArray("NUM2");
		double a = resolveValue(NUM1.get(1));
		double b = resolveValue(NUM2.get(1));
		return a - b;
	}

	private double doMultiply(JSONObject currentBlock) throws Exception {
		JSONObject inputs = currentBlock.getJSONObject("inputs");
		JSONArray NUM1 = inputs.getJSONArray("NUM1");
		JSONArray NUM2 = inputs.getJSONArray("NUM2");
		double a = resolveValue(NUM1.get(1));
		double b = resolveValue(NUM2.get(1));
		return a * b;
	}

	private double doDivide(JSONObject currentBlock) throws Exception {
		JSONObject inputs = currentBlock.getJSONObject("inputs");
		JSONArray NUM1 = inputs.getJSONArray("NUM1");
		JSONArray NUM2 = inputs.getJSONArray("NUM2");
		double a = resolveValue(NUM1.get(1));
		double b = resolveValue(NUM2.get(1));
		return a / b;
	}

	private double doModulus(JSONObject currentBlock) throws Exception {
		JSONObject inputs = currentBlock.getJSONObject("inputs");
		JSONArray NUM1 = inputs.getJSONArray("NUM1");
		JSONArray NUM2 = inputs.getJSONArray("NUM2");
		double a = resolveValue(NUM1.get(1));
		double b = resolveValue(NUM2.get(1));
		return a % b;
	}
	
	private double doRandom(JSONObject currentBlock) throws Exception {
		JSONObject inputs = currentBlock.getJSONObject("inputs");
		JSONArray FROM = inputs.getJSONArray("FROM");
		JSONArray TO = inputs.getJSONArray("TO");
		double a = resolveValue(FROM.get(1));
		double b = resolveValue(TO.get(1));
		return Math.random() * (b-a) + a;
	}
	
	private double doRound(JSONObject currentBlock) throws Exception {
		JSONObject inputs = currentBlock.getJSONObject("inputs");
		JSONArray NUM = inputs.getJSONArray("NUM");
		double a = resolveValue(NUM.get(1));
		return Math.round(a);
	}
	
	private double doMathOp(JSONObject currentBlock) throws Exception {
		JSONObject inputs = currentBlock.getJSONObject("inputs");
		JSONArray NUM = inputs.getJSONArray("NUM");
		double v = resolveValue(NUM.get(1));

		JSONObject fields = currentBlock.getJSONObject("fields");
		JSONArray OPERATOR = fields.getJSONArray("OPERATOR");
		switch(OPERATOR.getString(0)) {
		case "abs": 	return Math.abs(v);
		case "floor":   return Math.floor(v);
		case "ceiling": return Math.ceil(v);
		case "sqrt":    return Math.sqrt(v);
		case "sin":		return Math.sin(Math.toRadians(v));
		case "cos": 	return Math.cos(Math.toRadians(v));
		case "tan": 	return Math.tan(Math.toRadians(v));
		case "asin":    return Math.asin(Math.toRadians(v));
		case "acos":    return Math.acos(Math.toRadians(v));
		case "atan":    return Math.atan(Math.toRadians(v));
		case "ln":  	return Math.log(v);
		case "log": 	return Math.log10(v);
		case "e ^": 	return Math.exp(v);
		case "10 ^": 	return Math.pow(10,v);
		default: throw new Exception("doMathOp unknown operator "+OPERATOR.getString(1)); 
		}
	}

	private double doMotionDirection(JSONObject currentBlock) throws Exception {
		return myTurtle.getAngle();
	}
	
	private double doMotionXPosition(JSONObject currentBlock) throws Exception {
		return myTurtle.getX();
	}
	
	private double doMotionYPosition(JSONObject currentBlock) throws Exception {
		return myTurtle.getY();
	}
	
	private Object doReporterStringValue(JSONObject currentBlock) throws Exception {
		String name = currentBlock.getJSONObject("fields").getJSONArray("VALUE").getString(0);
		
		if(!myStack.isEmpty()) {
			for(Scratch3Variable sv : myStack.peek()) {
				if(sv.name.equals(name)) return sv.value;
			}
		}
		throw new Exception("Variable '"+name+"' not found.");
	}

	private int getListID(Object obj) throws Exception {
		if(!(obj instanceof String)) throw new Exception("List name not a string.");
		String listName = obj.toString();
		Iterator<Scratch3List> iter = scratchLists.iterator();
		int index=0;
		while(iter.hasNext()) {
			Scratch3List i = iter.next();
			if(i.name.equals(listName)) return index;
			++index;
		}
		throw new Exception("List '"+listName+"' not found.");
	}
	
	/**
	 * Find the requested index in a list.
	 * @param o2 the index value.  could be "random", "last", or an index number
	 * @param o3 the list name.
	 * @return the resolved value as an integer.
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private int resolveListIndex(Object o2,Object o3) throws Exception {
		String index = (String)o2;
		String listName = (String)o3;
		Scratch3List list = scratchLists.get(getListID(listName)); 
		int listIndex;
		if(index.equals("last")) {
			listIndex = list.contents.size()-1;
		} else if(index.equals("random")) {
			listIndex = (int) (Math.random() * list.contents.size());
		} else {
			listIndex = Integer.parseInt(index);
		}

		return listIndex;
	}
	
	/**
	 * PPAC37 test
	 * @param args 
	 */
	public static void main(String[] args) {
		// File srcDir = new File("src" + File.separator + "main" + File.separator + "java");
		File srcDir = new File("/home/q6/0_mcM_test/scratch3");
		try {

			PreferencesHelper.start();
			CommandLineOptions.setFromMain(args);
			Translator.start();

			Makelangelo.logger = LoggerFactory.getLogger(Makelangelo.class);
			Makelangelo makelangeloProgram = new Makelangelo();
			//makelangeloProgram.getCamera().zoomToFit(Paper.DEFAULT_WIDTH, Paper.DEFAULT_HEIGHT);
			makelangeloProgram.camera.zoomToFit(Paper.DEFAULT_WIDTH, Paper.DEFAULT_HEIGHT);
			Dimension dim = new Dimension(480, 640);

			//			Camera camera = new Camera();
//						Paper myPaper = new Paper();
			//			//logger.debug("Starting robot...");
			//			Plotter myPlotter = new Plotter();
			//			
			//			PlotterRenderer myPlotterRenderer = Machines.MAKELANGELO_5.getPlotterRenderer();
			//
			//			JPanel contentPane = new JPanel(new BorderLayout());
			//			contentPane.setOpaque(true);
			//			//logger.debug("  create PreviewPanel...");
			//			PreviewPanel previewPanel = new PreviewPanel();
			//			previewPanel.setCamera(camera);
			//			previewPanel.addListener(myPaper);
			//			previewPanel.addListener(myPlotter);
			//			TurtleRenderFacade myTurtleRenderer = new TurtleRenderFacade();
			//			previewPanel.addListener(myTurtleRenderer);
			//			//		addPlotterRendererToPreviewPanel();
			//			previewPanel.addListener((gl2)->{
			//						if(myPlotterRenderer!=null) {
			//							myPlotterRenderer.render(gl2, myPlotter);
			//						}
			//					});
			//			//		
			//			//createRangeSlider(contentPane);
			//
			//			contentPane.add(previewPanel, BorderLayout.CENTER);
			//Component cToScreenShoot = contentPane;
			Component cToScreenShoot = makelangeloProgram.createContentPane();
			cToScreenShoot.setSize(dim);

			List<File> files = listFiles(srcDir.toPath(), ".sb3");
			List<File> filesInError = new ArrayList<>();

			// test the files ...
			files.forEach(file -> {
				logger.debug("# ? {}", file);
				try (FileInputStream in = new FileInputStream(file)) {
					LoadScratch3 lsb3 = new LoadScratch3();
					try {
						Turtle t = lsb3.load(in);
						//load.history.size();
						logger.debug("# {} {}", t.history.size(), file);

						//						// by popular demand, resize turtle to fit paper
						//						ResizeTurtleToPaperAction resize = new ResizeTurtleToPaperAction(myPaper, false, "");
						//						t = resize.run(t);
						//						myTurtleRenderer.setTurtle(t);
						makelangeloProgram.setTurtle(new Turtle());// TO EMPTY
						makelangeloProgram.setTurtle(t);
						
						doACapture(cToScreenShoot, file.getParentFile(), file.getName() + "_0.png", true);
						
						ResizeTurtleToPaperAction resize = new ResizeTurtleToPaperAction(makelangeloProgram.myPaper,false,"");
						t = resize.run(t);
						makelangeloProgram.setTurtle(t);

						doACapture(cToScreenShoot, file.getParentFile(), file.getName() + "_1.png", true);
					} catch (Exception e) {
						logger.warn("Can read file {}", file, e);
						filesInError.add(file);
					}
					//searchInAFile(file, results);
				} catch (FileNotFoundException ex) {

				} catch (IOException ex) {

				}

			});

			logger.debug("Total .sb3 files {}", files.size());
			logger.debug("Total .sb3 filesInError {}", filesInError.size());
			
			// KO ask if ok to quit and exception has i do not have strat as usual the mainFrame is null .
			//java.lang.NullPointerException: Cannot invoke "javax.swing.JFrame.setDefaultCloseOperation(int)" because "this.mainFrame" is null
			makelangeloProgram.onClosing();
		} catch (Exception e) {
			logger.warn("Can read srcDir {}", srcDir, e);
		}
	}

	/**
	 *
	 * @param cToScreenShoot the value of cToScreenShoot
	 * @param fileTested the value of fileTested
	 * @param fileDest the value of fileDest
	 * @param inHeadlessMode the value of inHeadlessMode to ste to true to force a addNotify and a doLayout to see the sub JComponent in headless mode (not added to an visible JFrame/JComponent)
	 * @throws IOException
	 */
	public static void doACapture(Component cToScreenShoot, File fileTested, String fileDest, boolean inHeadlessMode) throws IOException {
		if ( inHeadlessMode ){
			cToScreenShoot.addNotify();// Needed in an headless env to doLayout recursively of all sub component
			cToScreenShoot.doLayout();
		}
		
		BufferedImage bufferImage = new BufferedImage(cToScreenShoot.getWidth(), cToScreenShoot.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D bufferGraphics = bufferImage.createGraphics();
		// Clear the buffer:
		bufferGraphics.clearRect(0, 0, cToScreenShoot.getWidth(), cToScreenShoot.getHeight());
		//define some rendering option (optional ?)
		bufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		bufferGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		// make the JComponent paint on the bufferedGraphics to get a copy of the render.
		cToScreenShoot.paintAll(bufferGraphics);
		final File fileOutputDest = new File(fileTested, fileDest);
		System.out.println("writing "+fileOutputDest.toString());
		// save the "image" painted in the bufferImage to a file
		ImageIO.write(bufferImage, "png", fileOutputDest);
		
		//		    to save a resize image to
		if (false) {
			int dimW = cToScreenShoot.getWidth() / 2;
			int dimH = cToScreenShoot.getHeight() / 2;
			Image scaledInstance = bufferImage.getScaledInstance(dimW, dimH, Image.SCALE_DEFAULT);
			ImageIO.write(convertToBufferedImage(scaledInstance), "png", new File("cframeImage_vignette.png"));
		}
		
		if ( cToScreenShoot.isVisible() && !inHeadlessMode){
		javax.swing.SwingUtilities.invokeLater(() -> {
try{
//				cToScreenShoot.notify();
				cToScreenShoot.doLayout();
//				cToScreenShoot.repaint();
				final File fileOutputDestRobot = new File(fileTested, "robot_"+fileDest);
		System.out.println("writing "+fileOutputDest.toString());
		
							Robot robot = new Robot();
							// ko in this context robot.waitForIdle();//delay(500);
							//cToScreenShoot.getBounds() // KO ...
							Rectangle r = new Rectangle(cToScreenShoot.getLocation(), cToScreenShoot.getSize());
							BufferedImage screenShot = robot.createScreenCapture(r);
							ImageIO.write(screenShot, "png", fileOutputDestRobot);
		
					} catch (AWTException ex) {
						logger.error("{}",ex.getMessage(),ex);
					} catch (IOException ex) {
						logger.error("{}",ex.getMessage(),ex);
					}
	});
		}
			
	}

	/*
	
				if (doFullJFrame) javax.swing.SwingUtilities.invokeLater(() -> {

					try {
						if (doFullJFrame) {
							//https://stackoverflow.com/questions/58305/is-there-a-way-to-take-a-screenshot-using-java-and-save-it-to-some-sort-of-image
							//	18
							//If you'd like to capture all monitors, you can use the following code:
							GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
							GraphicsDevice[] screens = ge.getScreenDevices();

							Rectangle allScreenBounds = new Rectangle();
							for (GraphicsDevice screen : screens) {
								Rectangle screenBounds = screen.getDefaultConfiguration().getBounds();

								allScreenBounds.width += screenBounds.width;
								allScreenBounds.height = Math.max(allScreenBounds.height, screenBounds.height);
							}

							//Rectangle 					rFrame = null;
							if (rFramef != null) {
								allScreenBounds = rFramef;
							}
							Robot robot = new Robot();
							//
							BufferedImage screenShot = robot.createScreenCapture(allScreenBounds);
							ImageIO.write(screenShot, "png", new File(dirScreenShotDest,"screenshot_robot.png"));
						}
					} catch (AWTException ex) {
						logger.error("{}",ex.getMessage(),ex);
					} catch (IOException ex) {
						logger.error("{}",ex.getMessage(),ex);
					}

				});
	*/
	
    /**
     * List all files and sub files in this path. Using
     * <code>Files.walk(path)</code> (so this take care of recursive path
     * exploration ) And applying filter ( RegularFile and ReadableFile ) and
     * filtering FileName ...
     *
     * @param path where to look.
     * @param fileNameEndsWithSuffix use ".java" to get only ... ( this is not a
     * regexp so no '.' despecialization required ) can be set to
     * <code>""</code> to get all files.
     * @return a list of files (may be empty if nothing is found) or null if
     * something is wrong.
     * @throws IOException
     */
    public static List<File> listFiles(Path path, String fileNameEndsWithSuffix) throws IOException {
        List<File> result;
        try ( Stream<Path> walk = Files.walk(path)) {
            result = walk
                    .filter(Files::isRegularFile)
                    .filter(Files::isReadable)
                    .map(Path::toFile)
                    .filter(f -> f.getName().endsWith(fileNameEndsWithSuffix))
                    .collect(Collectors.toList());
        }
        return result;
    }
	
	
	/**
	 * Convert Image to BufferedImage. Source :
	 * https://mkyong.com/java/how-to-write-an-image-to-file-imageio/
	 *
	 * @param img
	 * @return
	 */
	public static BufferedImage convertToBufferedImage(Image img) {

		if (img instanceof BufferedImage) {
			return (BufferedImage) img;
		}

		// Create a buffered image with transparency
		BufferedImage bi = new BufferedImage(
				img.getWidth(null), img.getHeight(null),
				BufferedImage.TYPE_INT_ARGB);

		Graphics2D graphics2D = bi.createGraphics();
		graphics2D.drawImage(img, 0, 0, null);
		graphics2D.dispose();

		return bi;
	}

	/**
	 * list out all the image file supported formats. Source :
	 * https://mkyong.com/java/how-to-write-an-image-to-file-imageio/
	 */
	private static void listImageTypeSupported() {
		String writerNames[] = ImageIO.getWriterFormatNames();
		Arrays.stream(writerNames).sorted().forEach(System.out::println);
	}

	private static String listImageTypeSupportedAsSimpleLowerCaseString() {
		String writerNames[] = ImageIO.getWriterFormatNames();
		
		SortedSet<String> sset = new TreeSet<>();		
		Arrays.stream(writerNames).forEach(wn -> sset.add(wn.toLowerCase()));
		
		StringBuilder sb = new StringBuilder();
		sset.forEach(s -> {sb.append(s);sb.append("/");});
		if ( sb.length()>0 ) {sb.setLength(sb.length()-1);}
		
		return sb.toString();
	}
	
}

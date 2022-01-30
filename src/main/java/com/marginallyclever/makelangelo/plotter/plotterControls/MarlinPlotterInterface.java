package com.marginallyclever.makelangelo.plotter.plotterControls;

import com.marginallyclever.communications.NetworkSessionEvent;
import com.marginallyclever.convenience.Point2D;
import com.marginallyclever.convenience.StringHelper;
import com.marginallyclever.makelangelo.plotter.Plotter;
import com.marginallyclever.makelangelo.plotter.PlotterEvent;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MarlinPlotterInterface} is a {@link MarlinInterface} with extra
 * instructions for interaction with a plotter robot.
 * 
 * @author Dan Royer
 * @since 7.28.0
 */
public class MarlinPlotterInterface extends MarlinInterface {
	private static final long serialVersionUID = -7114823910724405882L;

	private static final Logger logger = LoggerFactory.getLogger(MarlinPlotterInterface.class);

	private static final String STR_FEEDRATE = "echo:  M203";
	private static final String STR_ACCELERATION = "echo:  M201";

	private Plotter myPlotter;

	public MarlinPlotterInterface(Plotter plotter, ChooseConnection chooseConnection) {
		super(chooseConnection);

		myPlotter = plotter;

		plotter.addPlotterEventListener(this::onPlotterEvent);
	}

	private void onPlotterEvent(PlotterEvent e) {
		switch (e.type) {
		case PlotterEvent.HOME_FOUND:
			// logger.debug("MarlinPlotterInterface heard plotter home.");
			sendFindHome();
			break;
		case PlotterEvent.POSITION:
			 logger.debug("MarlinPlotterInterface heard plotter move.");
			sendGoto();
			break;
		case PlotterEvent.PEN_UPDOWN:
			// logger.debug("MarlinPlotterInterface heard plotter up/down.");
			sendPenUpDown();
			break;
		case PlotterEvent.MOTORS_ENGAGED:
			// logger.debug("MarlinPlotterInterface heard plotter engage.");
			sendEngage();
			break;
		case PlotterEvent.TOOL_CHANGE:
			// logger.debug("MarlinPlotterInterface heard plotter tool change.");
			sendToolChange((int) e.extra);
			break;
		default:
			break;
		}
	}

	private void sendToolChange(int toolNumber) {
		queueAndSendCommand(getToolChangeString(toolNumber));
	}

	
	private void sendFindHome() {
	//	queueAndSendCommand("G28 XY");
	}

	private void sendPenUpDown() {
		String str = myPlotter.getPenIsUp() ? MarlinPlotterInterface.getPenUpString(myPlotter)
				: MarlinPlotterInterface.getPenDownString(myPlotter);
		queueAndSendCommand(str);
	}

	private void sendEngage() {
		queueAndSendCommand(myPlotter.getMotorsEngaged() ? "M17" : "M18");
	}

	private void sendGoto() {
		Point2D p = myPlotter.getPos();
		String msg = myPlotter.getPenIsUp() 
				? MarlinPlotterInterface.getTravelToString(myPlotter, p.x, p.y)
				: MarlinPlotterInterface.getDrawToString(myPlotter, p.x, p.y);
		queueAndSendCommand(msg);
	}

	@Override
	protected void onDataReceived(NetworkSessionEvent evt) {
		super.onDataReceived(evt);

		if(evt.flag == NetworkSessionEvent.DATA_RECEIVED) {
			String message = ((String)evt.data).trim();
			//logger.debug("MarlinPlotterInterface received '"+message.trim()+"'.");
			if (message.startsWith("X:") && message.contains("Count")) {
				onHearM114(message);
			} else if (message.startsWith(STR_FEEDRATE)) {
				onHearFeedrate(message);
			} else if (message.startsWith(STR_ACCELERATION)) {
				onHearAcceleration(message);
			}
		}
	}

	// format is normally X:0.00 Y:270.00 Z:0.00 Count X:0 Y:0 Z:0 U:0 V:0 W:0
	// trim everything after and including "Count", then read the state data.
	protected void onHearM114(String message) {
		try {
			message = message.substring(0, message.indexOf("Count"));
			String[] majorParts = message.split("\s");
			Point2D pos = myPlotter.getPos();

			for (String s : majorParts) {
				String[] minorParts = s.split(":");
				double v = Double.parseDouble(minorParts[1].trim());
				if (minorParts[0].equalsIgnoreCase("X")) pos.x = v;
				if (minorParts[0].equalsIgnoreCase("Y")) pos.y = v;
			}

			myPlotter.setPos(pos.x,pos.y);
		} catch (NumberFormatException e) {
			logger.error("M114 error: {}", message, e);
		}
	}

	// format is "echo: M201 X5400.00 Y5400.00 Z5400.00"
	// I only care about the x value when reading.
	protected void onHearAcceleration(String message) {
		try {
			message = message.substring(STR_ACCELERATION.length());
			String[] parts = message.split("\s");
			if (parts.length != 4)
				throw new Exception("M201 format bad: " + message);
			double v = Double.parseDouble(parts[1].substring(1));
			logger.debug("MarlinPlotterInterface found acceleration {}", v);
			myPlotter.getSettings().setAcceleration(v);
		} catch (Exception e) {
			logger.warn("M201 error: {}", message, e);
		}
	}

	// format is "echo: M203 X5400.00 Y5400.00 Z5400.00"
	// I only care about the x value when reading.
	protected void onHearFeedrate(String message) {
		try {
			message = message.substring(STR_FEEDRATE.length());
			String[] parts = message.split("\s");
			if (parts.length != 4)
				throw new Exception("M203 format bad: " + message);
			double v = Double.parseDouble(parts[1].substring(1));
			logger.debug("MarlinPlotterInterface found feedrate {}", v);
			myPlotter.getSettings().setDrawFeedRate(v);
		} catch (Exception e) {
			logger.warn("M203 error: {}", message, e);
		}
	}

	// "By convention, most G-code generators use G0 for non-extrusion movements"
	// https://marlinfw.org/docs/gcode/G000-G001.html
	public static String getTravelToString(Plotter p,double x, double y) {
		return "G0 " + getPosition(x, y) ;//+ " F" + p.getSettings().getTravelFeedRate();
	}

	// "By convention, most G-code generators use G0 for non-extrusion movements"
	// https://marlinfw.org/docs/gcode/G000-G001.html
	public static String getDrawToString(Plotter p,double x, double y) {
		return "G1 " + getPosition(x, y) + " F" + p.getSettings().getDrawFeedRate();
	}

	private static String getPosition(double x, double y) {
		return "X" + StringHelper.formatDouble(x) + " Y" + StringHelper.formatDouble(y);
	}

	public static String getPenUpString(Plotter p) {
		return "M280 P0 S" + (int)p.getPenUpAngle() + " T" + (int) p.getPenLiftTime();
	}

	public static String getPenDownString(Plotter p) {
		return "M280 P0 S" + (int)p.getPenDownAngle() + " T50";
	}

	/**
	 * String.formate("M0 Ready %s and click", getColorName(toolNumber & 0xFFFFFF) ) ; 
	 * String pattern = String.formate("M0 Ready %s and click", "(.*)" )
	 * @param toolNumber
	 * @return 
	 */
	public static String getToolChangeString(int toolNumber) {
		String colorName = getColorName(toolNumber & 0xFFFFFF);
		return "M0 Ready " + colorName + " and click";// TODO as a format ? ( This should be keep and not modified to ensure LoadGCode color identification from the M0 texte.
	}

	public static String getSpeakerString(double x_s, double y_p) {
		   if ( x_s == -1 && y_p == -1){
			return "M300";
		    }else if ( x_s == -1 ){
			return "M300 P"+(int)(y_p);
		    }else if ( y_p == -1 ){
			return "M300 S"+(int)(x_s);
		    }else{
			return "M300 S"+(int)(x_s)+" P"+(int)(y_p);
		    }
		
	}
	/**
	 * TODO the contente of this methode should be in ColorRGB as static utility methodes.
	 * And we need the reverse. String to int to ColorRGB ...
	 * <br>
	 * TODO be conforme to https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/Transformations#color 
	 * ? http://www.w3.org/TR/css3-color/
	 * 
	 * @param toolNumber in fact normaly this is a ColorRGB int int value.
	 * @return 
	 */
	private static String getColorName(int toolNumber) {
		String name = "";
		switch (toolNumber) {
		case 0xff0000:
			name = "red";
			break;
		case 0x00ff00:
			name = "green"; //lime !!! TODO be conforme ? as w3c greem = #008000
			break;
		case 0x0000ff:
			name = "blue";
			break;
		case 0x000000:
			name = "black";
			break;
		case 0x00ffff:
			name = "cyan";
			break;
		case 0xff00ff:
			name = "magenta";
			break;
		case 0xffff00:
			name = "yellow";
			break;
		case 0xffffff:
			name = "white";
			break;
		default:
			name = "0x" + Integer.toHexString(toolNumber);
			break; // display unknown RGB value as hex
		}
		return name;
	}
	
	private static TreeMap<String,Integer> treeMapColorIntValToColorName = null;	
	private static TreeMap<String,Integer> getColorMap(){
	    if ( treeMapColorIntValToColorName == null){
		TreeMap<String,Integer> tm = new TreeMap<>();
		tm.put("red", 0xff0000);//red		
		tm.put("green", 0x00ff00);//lime !!! TODO be conforme ? as w3c greem = #008000
		tm.put("blue", 0x0000ff);//blue
		tm.put("black", 0x000000);//black
		tm.put("cyan", 0x00ffff);//cyan
		tm.put("magenta", 0xff00ff);//magenta		
		tm.put("yellow", 0xffff00);//yellow
		tm.put("white", 0xffffff);//white
		// orange #ffa500
		// grey #808080 // to avoid ??? this is the background color of the preview ?
		// ... https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/Transformations#color 
		// http://www.w3.org/TR/css3-color/
		treeMapColorIntValToColorName= tm;		
	    }	    
	    return treeMapColorIntValToColorName;
	}	
	/**
	 * ? https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/Transformations#color 
	 * ? http://www.w3.org/TR/css3-color/
	 * 
	 * @param colorName
	 * @return 
	 */
	public static int getColorValueFromStringName(String colorName){
	    if ( getColorMap().containsKey(colorName) ) {
		return getColorMap().get(colorName);
	    }
	    else{
		if ( colorName == null || colorName.length() != 2 +6 ) {
		    // throw exception or else try other format ? -> a masque / transformation on the integer . parse int
		}
		if ( colorName.startsWith("0x") ){
		    //default -> "0x" + Integer.toHexString(toolNumber);		 // display unknown RGB value as hex
		    try{
			return Integer.parseInt(colorName.substring(2), 16);
		    }catch (Exception e){
		    // ? throw exception or will return -1;
		    }
		}
	    }	    
	    return -1; // ? default value
	}
}

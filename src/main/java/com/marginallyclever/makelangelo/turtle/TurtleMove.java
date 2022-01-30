package com.marginallyclever.makelangelo.turtle;

import java.awt.Color;

import com.marginallyclever.convenience.ColorRGB;
import com.marginallyclever.convenience.StringHelper;

public class TurtleMove {
	public static final int TRAVEL=0;  // move without drawing
	public static final int DRAW_LINE=1;  // move while drawing
	/**
	 * x stoque alors la couleur au format int , et y le diametre.
	 */
	public static final int TOOL_CHANGE=2; 
	/**
	 * SPEAKER for M300 : Play Tone. TODO.
	 * https://marlinfw.org/docs/gcode/M300.html
	 * 
	 * <pre>{@code 
	 * M300 [P<ms>] [S<Hz>]
	 * Parameters : 
	 *  [P<ms>]	Duration (1ms)
	 *  [S<Hz>]	Frequency (260Hz)
	 * Examples Play a tune.
	 *  M300 S440 P200
	 *  M300 S660 P250
	 *  M300 S880 P300
	 * }</pre>
	 */
	public static final int BIP=3; // ADDED BY PPAC37 : TODO full ! ( -> gcode generator and plotter control ... and ? ) 
	
	public int type;
	public double x,y;  // destination
	
	public TurtleMove(double x0,double y0,int type0) {
		super();
		this.x=x0;
		this.y=y0;
		this.type=type0;
	}
	
	public TurtleMove(TurtleMove m) {
		this(m.x,m.y,m.type);
	}

	/** The color asked for the turtle tool change.
	 * TODO Only valable if type == TOOL_CHANGE or else this is the x value. so throw an exception if not a type == TOOL_CHANGE.
	 * @return a colorRGB as a int cast of the x value.
	 */
	public ColorRGB getColor() {
		return new ColorRGB((int)x);
	}
	
	/** The diameter asked for the turtle tool change.
	 *  TODO Only valable if type == TOOL_CHANGE or else this is the y value. so throw an exception if not a type == TOOL_CHANGE.
	 * @return a diameter ( in mm ? )
	 */
	public double getDiameter() {
		return y;
	}
	
	public String toString() {
		switch(type) {
		case TOOL_CHANGE:
			Color c = new Color((int)x);
			return "TOOL R"+c.getRed()+" G"+c.getGreen()+" B"+c.getBlue()+" D"+StringHelper.formatDouble(y);
		case TRAVEL:
			return "TRAVEL X"+StringHelper.formatDouble(x)+" Y"+StringHelper.formatDouble(y);
		case BIP:
		    if ( x == -1 && y == -1){
			return "BIP";
		    }else if ( x == -1 ){
			return "BIP P"+(int)(y);
		    }else if ( y == -1 ){
			return "BIP S"+(int)(x);
		    }else{
			return "BIP S"+(int)(x)+" P"+(int)(y);
		    }
		default:
			return "DRAW_LINE X"+StringHelper.formatDouble(x)+" Y"+StringHelper.formatDouble(y);
		}
	}
}
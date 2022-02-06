package com.marginallyclever.makelangelo.makeArt.io.vector;

import java.io.InputStream;
import java.util.Scanner;

import javax.swing.filechooser.FileNameExtensionFilter;

import com.marginallyclever.convenience.ColorRGB;
import com.marginallyclever.convenience.CommandLineOptions;
import com.marginallyclever.convenience.log.Log;
import com.marginallyclever.makelangelo.Makelangelo;
import com.marginallyclever.makelangelo.Translator;
import com.marginallyclever.makelangelo.makelangeloSettingsPanel.LanguagePreferences;
import com.marginallyclever.makelangelo.paper.Paper;
import com.marginallyclever.makelangelo.plotter.plotterControls.MarlinPlotterInterface;
import com.marginallyclever.makelangelo.turtle.Turtle;
import com.marginallyclever.util.PreferencesHelper;
import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class LoadGCode implements TurtleLoader {
	private FileNameExtensionFilter filter = new FileNameExtensionFilter("GCode", "gcode");
	
	@Override
	public FileNameExtensionFilter getFileNameFilter() {
		return filter;
	}

	@Override
	public boolean canLoad(String filename) {
		String ext = filename.substring(filename.lastIndexOf('.'));
		return ext.equalsIgnoreCase(".gcode");
	}
	
	// search all tokens for one that starts with key and return it.
	protected String tokenExists(String key,String[] tokens) {
		for( String t : tokens ) {
			if(t.startsWith(key)) return t;
		}
		return null;
	}

	// returns angle of dy/dx as a value from 0...2PI
	private double atan3(double dy, double dx) {
		double a = Math.atan2(dy, dx);
		if (a < 0) a = (Math.PI * 2.0) + a;
		return a;
	}
	
	boolean asFlavoredMarlinPolargraphe = true;
	boolean asFlavoredMakelangeloFirmware = false;
	
	public ColorRGB getColorForT_Index(int t){
	    // TODO should have a way to be redefined ( like a static map editable by the user.)
	    // TODO and so this map should be storeed (save on exit app , and reloaded on app start) by the Prefrences system implemented ...
	    switch(t){
		case 0 : return new ColorRGB(Color.PINK);
		case 1 : return new ColorRGB(Color.BLUE);
	    }
	    
	    return new ColorRGB(Color.YELLOW);// default color black ?
	}
	/**
	 * ?? pour les test trouver une methode pour comparer avec le resultat d'un fonction draw arc ?
	 * http://underpop.online.fr/j/java/help/drawing-arcs-graphics-and-java-2d.html.gz
	 * <pre>{@code public void paintComponent( Graphics g )
11 {
12 super.paintComponent( g ); // call superclass's paintComponent
13
14 // start at 0 and sweep 360 degrees
15 g.setColor( Color.RED );
16 g.drawRect( 15, 35, 80, 80 );
17 g.setColor( Color.BLACK );
18 drawArc( 15, 35, 80, 80, 0, 360 ); }</pre>
	 */
	@Override
	public Turtle load(InputStream in) throws Exception {
		Turtle turtle = new Turtle();
		if (asFlavoredMarlinPolargraphe){
		    // is the ok for all case ?
		    turtle.penUp();
//		    turtle.setX(0);turtle.setY(0);
		}
		ColorRGB penDownColor = turtle.getColor();
		double scaleXY=1;
		boolean isAbsolute=true;
		
		double oz=turtle.isUp()?90:0;
		double ox=turtle.getX();
		double oy=turtle.getY();
		double oe=0;//
		
		int lineNumber=0;
		Scanner scanner = new Scanner(in);	
		String line="";
		try {
		    // MarlinPlotterInterface.getToolChangeString(colorAs?) :: "M0 Ready " + colorName + " and click"
			while (scanner.hasNextLine()) {
				line = scanner.nextLine();
				lineNumber++;
				// lose anything after a ; because it's a comment 
				String[] pieces = line.split(";");
				if (pieces.length == 0) continue;
				// the line isn't empty.
	
				String[] tokens = pieces[0].split("\\s");
				if (tokens.length == 0) continue;
	
				double nx = turtle.getX();
				double ny = turtle.getY();
				double nz = oz;
				double ni = nx;
				double nj = ny;
				
				double ne = oe;
				
				boolean codeFound=false;
				
				//
				String tCodeToken = tokenExists("T",tokens);
				if(tCodeToken!=null && tokens.length ==1) {	
					   int marlinToolNum = Integer.parseInt(tCodeToken.substring(1));
					   penDownColor = getColorForT_Index(marlinToolNum);
					turtle.setColor(penDownColor);
				}
				//
				
				String mCodeToken=tokenExists("M",tokens);
				if(mCodeToken!=null) {
					codeFound=true;
					int mCode = Integer.parseInt(mCodeToken.substring(1));
					switch(mCode) {
					case 0:
					    if ("Ready".equals(tokens[1])) {
						//String pattern = String.formate("M0 Ready %s and click", "(.*)" )
						String cNameOrHexaValue = tokens[2];						
						logger.debug("READ MO Ready {} ... line {}", cNameOrHexaValue, lineNumber);						
						int tmpColor = MarlinPlotterInterface.getColorValueFromStringName(cNameOrHexaValue);
						if (tmpColor != -1) {
						    ColorRGB colorRGB = new ColorRGB(tmpColor);
						    logger.debug("SET colorRGB {} line {}", colorRGB.toString(), lineNumber);
						    turtle.setColor(colorRGB);
						} else {
						    logger.debug("M0 Ready : Parse error color \"{}\" line {} in \"{}\"", cNameOrHexaValue, lineNumber, line);
						}
					    } else {
						logger.debug("Not a \"M0 Ready <color>\" line {} in \"{}\"", lineNumber, line);
					    }
					    break;
					case 6:
						// tool change
						
					    /*
					    // PPAC37 To reveiw should have a setColor of the color defined for the tool num ?
					    in 3D printing slicer use marlin G-code T0 for the first tool/extrudeur , T1 , T...
					    So the paraméter in marlin g-code flavor is an tool index and not a color.
					    So we need a default marlin Tool index to color , to set a color for the setColor of the turtle (on not a 0 or 1 ... )
					    */
						String color = tokenExists("T",tokens);
						if ( asFlavoredMakelangeloFirmware){
						    penDownColor = new ColorRGB(Integer.parseInt(color.substring(1)));
						}else{
						   int marlinToolNum = Integer.parseInt(color.substring(1));
						   penDownColor = getColorForT_Index(marlinToolNum);
						}
						turtle.setColor(penDownColor);
						break;
					case 280:
						// z move
						double v = Double.valueOf(tokenExists("S",tokens).substring(1));
						nz = isAbsolute ? v : nz+v;
						if(nz!=oz) {
							// z change
							if(turtle.isUp()) turtle.penDown();
							else turtle.penUp();
							oz=nz;
						}
						break;
					case 300: 
					    //TODO ! 
					   
					    int s = tokenExists("S",tokens) != null ? Integer.valueOf(tokenExists("S",tokens).substring(1)) : -1;
					    int p = tokenExists("P",tokens) != null ? Integer.valueOf(tokenExists("P",tokens).substring(1)) : -1;
					    logger.debug("M300 S{} P{} TODO!",s,p);
					    turtle.bip(s, p);
					    /**
					    * SPEAKER for M300 : Play Tone. TODO Turle hack. (like as pen color / diameter )
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
					    break;
					default:
						// ignore all others
						break;
					}
				}
				
				if(!codeFound) {
					String gCodeToken = tokenExists("G", tokens);
					if (gCodeToken != null) {
						int gCode = Integer.parseInt(gCodeToken.substring(1));
						switch(gCode) {
						case 20: scaleXY=25.4;  break;  // in -> mm
						case 21: scaleXY= 1.0;  break;  // mm
						case 90: isAbsolute=true;	break;  // absolute mode
						case 91: isAbsolute=false;	break;  // relative mode
							case 28:
								turtle.setX(0);
								turtle.setY(0);
								break; // not true in all case ( some home can be done else where ...)
							//92 !
						default:
							break;
						}
						
						if (tokenExists("X", tokens) != null) {
							double v = Float.valueOf(tokenExists("X", tokens).substring(1)) * scaleXY;
							nx = isAbsolute ? v : nx + v;
						}
						if (tokenExists("Y", tokens) != null) {
							double v = Float.valueOf(tokenExists("Y", tokens).substring(1)) * scaleXY;
							ny = isAbsolute ? v : ny + v;
						}
						if (tokenExists("Z", tokens) != null) {
							double v = Float.valueOf(tokenExists("Z", tokens).substring(1));  // do not scale
							nz = isAbsolute ? v : nz + v;
						}
						if (tokenExists("I", tokens) != null) {
							double v = Float.valueOf(tokenExists("I", tokens).substring(1)) * scaleXY;
							ni = isAbsolute ? v : ni + v;
						}
						if (tokenExists("J", tokens) != null) {
							double v = Float.valueOf(tokenExists("J", tokens).substring(1)) * scaleXY;
							nj = isAbsolute ? v : nj + v;
						}

						// need to see if e change for G2/G3 
						boolean haveE = false;
						if (tokenExists("E", tokens) != null) {
							haveE = true;
							double v = Float.valueOf(tokenExists("E", tokens).substring(1)) * scaleXY;
							ne = isAbsolute ? v : ne + v;
						}

						if(gCode==0 || gCode==1) {
							if (asFlavoredMarlinPolargraphe) {
								// Only for marlin-polargraph flavored .gcode
								if (gCode == 0) {
									// not totaly true ... G0 E1 is posible ...
									turtle.penUp();
								} else {
									turtle.penDown();
								}
								if (haveE) {
									// but may by not
									if (ne == oe) { // so no extrusion !? ( excepte a G92 E... or ... ?
										turtle.penUp();
									} else {
										turtle.penDown();
									}
								}

							}
							if (asFlavoredMakelangeloFirmware) {
								// Only for Makelangelo-firmware flavored .gcode
							if(nz!=oz) {
								// z change
								if(turtle.isUp()) turtle.penDown();
								else turtle.penUp();
								oz=nz;
								}
							}
							if(nx!=ox || ny!=oy) {
								turtle.moveTo(nx, ny);
								ox = nx;
								oy = ny;
							}
							if (haveE) {
								oe = ne;
							}
						} else if(gCode==2 || gCode==3) {
							// a G2 or G3 mouve have an E argument ... posibly a "draw"
							if (haveE) {
								// but may by not
								if (ne == oe) { // so no extrusion !? ( excepte a G92 E... or ... ?
									turtle.penUp();
								} else {
									turtle.penDown();
								}
							} else {
								turtle.penUp();
							}
							// arc
							int dir = (gCode == 2) ? -1 : 1;

							double dx = ox - ni;
							double dy = oy - nj;
							double radius = Math.sqrt(dx * dx + dy * dy);

							// find angle of arc (sweep)
							double angle1 = atan3(dy, dx);
							double angle2 = atan3(ny - nj, nx - ni);
							double theta = angle2 - angle1;
		
							if (dir > 0 && theta < 0) angle2 += Math.PI * 2.0;
							else if (dir < 0 && theta > 0) angle1 += Math.PI * 2.0;
		
							theta = angle2 - angle1;

							double len = Math.abs(theta) * radius;
							double angle3, scale;

							// TODO turtle support for arcs https://marlinfw.org/docs/gcode/G002-G003.html
							// Draw the arc from a lot of little line segments.
							for (double k = 0; k < len; k++) {
								scale = k / len;
								angle3 = theta * scale + angle1;
								double ix = ni + Math.cos(angle3) * radius;
								double iy = nj + Math.sin(angle3) * radius;

								turtle.moveTo(ix, iy);
							}
							turtle.moveTo(nx,ny);
							ox=nx;
							oy=ny;
							oe = ne;
						}
					}
					// else do nothing.
				}
				
				// TODO ? some slicer use some "specific" commente to add information ( Slicing option (like marlin flavor, slicer name and version, ... ),  encoded as text vignettes/thumb images preview, object , layer num, ... )
			}// <- end while scanner.hasNextLine
		}
		catch(Exception e) {
		        logger.error("{}",e);
			throw new Exception("GCODE parse failure ("+lineNumber+"): "+line);
		}
		finally {
			scanner.close();
		}

		return turtle;
	}

	//
	// Dev speed test (GUI)
	    // 
	private static Logger logger = LoggerFactory.getLogger(LoadGCode.class);

	public static void main(String[] args) {
		
		//listImageTypeSupported();
		listImageTypeSupportedAsSimpleLowerCaseString();
		boolean doFullJFrame = true;
		// 
		Log.start();
		// lazy init to be able to purge old files
		//logger = LoggerFactory.getLogger(Makelangelo.class);

		PreferencesHelper.start();
		CommandLineOptions.setFromMain(args);
		Translator.start();

		if (Translator.isThisTheFirstTimeLoadingLanguageFiles()) {
			LanguagePreferences.chooseLanguage();
		}

		//setSystemLookAndFeel();
		Makelangelo.setSystemLookAndFeel(); // modified to be public

		javax.swing.SwingUtilities.invokeLater(() -> {

			Rectangle rFrame = null;
			try {
				Makelangelo makelangeloProgram = new Makelangelo();
				//some hack ...
				makelangeloProgram.doNotAskIfOkOnExit = true;
				makelangeloProgram.doNotShowLoadDialogu = true;
				//to use it witout jframe ...

				Component cToScreenShoot = null;
				if (doFullJFrame) {
					makelangeloProgram.run();//in CI throws HeadlessException as it create a new JFrame ... and display it

					JFrame mainFrame = makelangeloProgram.getMainFrame();
					//mainFrame.getRootPane();
					// n.b. if using a JFrame / Frame we do not get the OS décoration ( window bar title ... )
					//
					cToScreenShoot = (Component) mainFrame;
					cToScreenShoot = (Component) mainFrame.getRootPane(); // have to set a size !
					//

					rFrame = new Rectangle(mainFrame.getLocation(), mainFrame.getSize());

				} else {
					// if i do not whant the JFrame to be created ...
					//https://stackoverflow.com/questions/17026803/headless-painting
					cToScreenShoot = makelangeloProgram.createContentPane();
					// /!\ the component have to have a setSize seted or else later in paintAll a 0,0 sized component will create an exception.
					//cToScreenShoot.setSize(cToScreenShoot.getPreferredSize());
					cToScreenShoot.addNotify();// Needed in an headless env to doLayout recursively of all sub component			
					cToScreenShoot.doLayout();
					// Needed : 
					makelangeloProgram.camera.zoomToFit(Paper.DEFAULT_WIDTH, Paper.DEFAULT_HEIGHT);

				}
				Dimension dim = new Dimension(480, 640);
				rFrame.setSize(dim);
				cToScreenShoot.setSize(dim);

				// a way to do a kind of print screen ...
				// load a .gcode (test set todo)
				try{
					makelangeloProgram.openLoadFile("/home/q6/g2_g3_test.gcode");
				}catch (Exception e){
					e.printStackTrace();
				}
				// n.b. if in a headlees env will throw an Exception if some dialog (message/error/...) a thrown ...

				//https://stackoverflow.com/questions/19621105/save-image-from-jpanel-after-draw/19621211
				//https://stackoverflow.com/questions/31744516/save-a-drawn-picture-on-a-jpanel-in-a-file-java
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
				// If you whant to add some to the normaly paint result ( like a date)
				BasicStroke stroke = new BasicStroke(4);
				bufferGraphics.setStroke(stroke);
				bufferGraphics.setColor(Color.RED);
				bufferGraphics.drawString("Coucou", cToScreenShoot.getWidth() / 2, cToScreenShoot.getHeight() / 2);
				// save the "image" painted in the bufferImage to a file
				ImageIO.write(bufferImage, "png", new File("cframeImage.png"));
				//		    // and save a resize image to
				//		    int dimW = cToScreenShoot.getWidth() / 2;
				//		    int dimH = cToScreenShoot.getHeight() / 2;
				//		    Image scaledInstance = bufferImage.getScaledInstance(dimW, dimH, Image.SCALE_DEFAULT);
				//		    ImageIO.write(convertToBufferedImage(scaledInstance), "png", new File("cframeImage_vignette.png"));
				//
				final Rectangle rFramef = rFrame;
				javax.swing.SwingUtilities.invokeLater(() -> {

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
							//robot.waitForIdle();//?
							BufferedImage screenShot = robot.createScreenCapture(allScreenBounds);
							ImageIO.write(screenShot, "png", new File("screenshot_robot.png"));
						}
					} catch (AWTException ex) {
						java.util.logging.Logger.getLogger(LoadGCode.class.getName()).log(Level.SEVERE, null, ex);
					} catch (IOException ex) {
						java.util.logging.Logger.getLogger(LoadGCode.class.getName()).log(Level.SEVERE, null, ex);
					}

				});
				//System.exit(0);
			} catch (IOException ex) {
				java.util.logging.Logger.getLogger(LoadGCode.class.getName()).log(Level.SEVERE, null, ex);
			}

			//System.exit(0);
		});

		//
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

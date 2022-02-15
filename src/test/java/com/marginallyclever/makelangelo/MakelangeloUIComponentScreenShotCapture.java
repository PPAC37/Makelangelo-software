
package com.marginallyclever.makelangelo;

import com.marginallyclever.convenience.CommandLineOptions;
import com.marginallyclever.convenience.log.Log;
import static com.marginallyclever.makelangelo.LanguagesXmlValidationForNoDupKeyTest.ressourceStringForXmlShemaFile;
import com.marginallyclever.makelangelo.makelangeloSettingsPanel.LanguagePreferences;
import com.marginallyclever.makelangelo.paper.Paper;
import com.marginallyclever.util.PreferencesHelper;
import com.marginallyclever.util.PropertiesFileHelper;
import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
/**
 *
 * @author PPAC
 */
public class MakelangeloUIComponentScreenShotCapture {
	
    private static final Logger logger = LoggerFactory.getLogger(MakelangeloTest.class);

    @Test
    public void checkVersion() throws IllegalStateException {
	 String version = PropertiesFileHelper.getMakelangeloVersion();
        logger.debug("version {}", version);
        String versionGit = PropertiesFileHelper.getMakelangeloGitVersion();
        logger.debug("version {}", versionGit);
		try{
			String [] args = {};
			main(args);
		}catch (Exception e){
			logger.error("{}",e.getMessage(),e);
		}
		
	}
	
	
	
	public static void main(String[] args) {
		File dirScreenShotDest = new File("tmp_screenShot");
		dirScreenShotDest.mkdirs();
		
		//listImageTypeSupported();
		listImageTypeSupportedAsSimpleLowerCaseString();
		boolean doFullJFrame = false;
		// 
		Log.start();
		// lazy init to be able to purge old files
		//
		Makelangelo.logger = LoggerFactory.getLogger(Makelangelo.class);

		PreferencesHelper.start();
		CommandLineOptions.setFromMain(args);
		Translator.start();

		if (Translator.isThisTheFirstTimeLoadingLanguageFiles()) {
			LanguagePreferences.chooseLanguage();
		}

		//setSystemLookAndFeel();
		//
		Makelangelo.setSystemLookAndFeel(); // modified to be public

//		javax.swing.SwingUtilities.invokeLater(() -> {

			Rectangle rFrame = null;
			try {
				Makelangelo makelangeloProgram = new Makelangelo();
				//some hack ...
				makelangeloProgram.uiAskToConfirmQuit = false;
				makelangeloProgram.uiCreateAndShowOpenLoadFileDialogue = false;
				makelangeloProgram.uiAddToRecentFileOnOpenLoadFileDialogue = false;
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
					makelangeloProgram.getCamera().zoomToFit(Paper.DEFAULT_WIDTH, Paper.DEFAULT_HEIGHT);

				}
				Dimension dim = new Dimension(480, 640);
				if ( rFrame != null ) rFrame.setSize(dim);
				cToScreenShoot.setSize(dim);

				// a way to do a kind of print screen ...
				// load a .gcode (test set todo)
				try{
					//
					//final String circlesvg = "/circle.svg";//Eule.svg
					//
					final String circlesvg = "/Eule.svg";//
					
					//makelangeloProgram.openFile("/home/q6/g2_g3_test.gcode");
					
					URL schemaFile = MakelangeloUIComponentScreenShotCapture.class.getResource(circlesvg);

					assertNotNull(schemaFile, "The test need a redable schema xsd file (" + circlesvg + ") to validate the language files");

					makelangeloProgram.openFile(schemaFile.toURI().getPath());
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
				if ( false ){
					BasicStroke stroke = new BasicStroke(4);
					bufferGraphics.setStroke(stroke);
					bufferGraphics.setColor(Color.RED);
					bufferGraphics.drawString("Coucou", cToScreenShoot.getWidth() / 2, cToScreenShoot.getHeight() / 2);
				}
				// save the "image" painted in the bufferImage to a file
				ImageIO.write(bufferImage, "png", new File(dirScreenShotDest,"cframeImage.png"));
				//		    // and save a resize image to
				//		    int dimW = cToScreenShoot.getWidth() / 2;
				//		    int dimH = cToScreenShoot.getHeight() / 2;
				//		    Image scaledInstance = bufferImage.getScaledInstance(dimW, dimH, Image.SCALE_DEFAULT);
				//		    ImageIO.write(convertToBufferedImage(scaledInstance), "png", new File("cframeImage_vignette.png"));
				//
				final Rectangle rFramef = rFrame;
//				if (!doFullJFrame) {
//					makelangeloProgram.setDummyMainFrame();
//					makelangeloProgram.onClosing();
//				}
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
							//robot.waitForIdle();//?
							BufferedImage screenShot = robot.createScreenCapture(allScreenBounds);
							ImageIO.write(screenShot, "png", new File(dirScreenShotDest,"screenshot_robot.png"));
						}
					} catch (AWTException ex) {
						logger.error("{}",ex.getMessage(),ex);
					} catch (IOException ex) {
						logger.error("{}",ex.getMessage(),ex);
					}

				});
				
			} catch (IOException ex) {
				logger.error("{}",ex.getMessage(),ex);
			}

//		});


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

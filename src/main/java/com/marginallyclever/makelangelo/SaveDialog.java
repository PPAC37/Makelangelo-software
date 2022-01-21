package com.marginallyclever.makelangelo;

import com.marginallyclever.convenience.FileAccess;
import com.marginallyclever.makelangelo.makeArt.io.vector.TurtleFactory;
import com.marginallyclever.makelangelo.turtle.Turtle;
import com.marginallyclever.util.PreferencesHelper;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.prefs.Preferences;

public class SaveDialog {
	private static final Logger logger = LoggerFactory.getLogger(SaveDialog.class);
	
	private JFileChooser fc = new JFileChooser();
		
	private File lastDirUsed = null; 
	private static final String PREF_KEY_SAVE_DIALOG_LAST_DIR = SaveDialog.class.getSimpleName()+".lastDirUsed";
	private Preferences prefNode = PreferencesHelper.getPreferenceNode(PreferencesHelper.MakelangeloPreferenceKey.FILE); // TODO is FILE? or do we have to create a new node ? and find a way to short this ...
	
	public SaveDialog() {
		for( FileNameExtensionFilter ff : TurtleFactory.getSaveExtensions() ) {
			fc.addChoosableFileFilter(ff);
		}
		// do not allow wild card (*.*) file extensions
		fc.setAcceptAllFileFilterUsed(false);
		
		// load lastDirUsed from the prefrences.
		//String lastDirUsedDefault = FileAccess.getUserDirectory();//=System.getProperty("user.dir");
		String lastDirUsedDefault = System.getProperty("user.home");
		String lastDirUsedFromPref = prefNode.get(PREF_KEY_SAVE_DIALOG_LAST_DIR,lastDirUsedDefault);		
		logger.debug("lastDirUsedFromPref = {}",lastDirUsedFromPref);
		lastDirUsed = new File(lastDirUsedFromPref);
	}
	
	/**
	 * PPAC37: Event if not currently used.
	 * @param lastDir 
	 */
	public SaveDialog(String lastDir) {
		this();// call the "basic" constructor
		if ( lastDir != null ) {		    
		    lastDirUsed = new File(lastDir);
		}
		// not needed anymore.
		/*
		// remember the last path used, if any
		fc.setCurrentDirectory((lastDir==null?null : new File(lastDir)));
		*/
	}
	
	public void run(Turtle t,JFrame parent) throws Exception {
		if ( lastDirUsed!= null ) {		
		    if ( lastDirUsed.exists() && lastDirUsed.canWrite() ){
			fc.setCurrentDirectory(lastDirUsed);
		    }
		}
		if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
			String selectedFile = fc.getSelectedFile().getAbsolutePath();
			lastDirUsed = fc.getCurrentDirectory();// To save the dir in used
			prefNode.put(PREF_KEY_SAVE_DIALOG_LAST_DIR,lastDirUsed.toString());
			String withExtension = addExtension(selectedFile,((FileNameExtensionFilter)fc.getFileFilter()).getExtensions());
			logger.debug("File selected by user: {}", withExtension);
			TurtleFactory.save(t,withExtension);
		}
		// Only if we want to save the dir even if canceled
		//lastDirUsed = fc.getCurrentDirectory();// To save the dir in used
		//memoPreferenceNode.put(PREF_KEY_SAVE_DIALOG_LAST_DIR,lastDirUsed.toString());
	}

	private String addExtension(String name, String [] extensions) {
		for( String e : extensions ) {
			if(FilenameUtils.getExtension(name).equalsIgnoreCase(e)) return name;
		}
		
		return name + "." + extensions[0];
	}
}

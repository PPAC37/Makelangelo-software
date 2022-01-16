package com.marginallyclever.makelangelo;

import com.marginallyclever.convenience.FileAccess;
import com.marginallyclever.util.MarginallyCleverTranslationXmlFileHelper;
import com.marginallyclever.util.PreferencesHelper;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

/**
 * MultilingualSupport is the translation engine.  You ask for a string it finds the matching string in the currently selected language.
 * See <a href="http://www.java-samples.com/showtutorial.php?tutorialid=152">XML and Java - Parsing XML using Java Tutorial</a>
 * @author Dan Royer
 * @author Peter Colapietro
 */
public final class Translator {
	private static final Logger logger = LoggerFactory.getLogger(Translator.class);
	
	// Working directory. This represents the directory where the java executable launched the jar from.
	public static final String WORKING_DIRECTORY = /*File.separator + */"languages"/*+File.separator*/;
	// The name of the preferences node containing the user's choice.
	private static final String LANGUAGE_KEY = "language";
	// TODO get a better way to store user preference.
	private static final Preferences languagePreferenceNode = PreferencesHelper.getPreferenceNode(PreferencesHelper.MakelangeloPreferenceKey.LANGUAGE);
	// The default choice when nothing has been selected.
	private static String defaultLanguage = "English";
	// The current choice
	private static String currentLanguage;
	// a list of all languages and their translations strings
	private static final Map<String, TranslatorLanguage> languages = new HashMap<String, TranslatorLanguage>();

	public static void start() {
		logger.debug("starting translator...");
		
		Locale locale = Locale.getDefault();
		defaultLanguage = locale.getDisplayLanguage(Locale.ENGLISH);
		logger.debug("Default language = {}", defaultLanguage);
		
		loadLanguages();
		loadConfig();
	}
	

	/**
	 * @return true if this is the first time loading language files (probably on install)
	 */
	public static boolean isThisTheFirstTimeLoadingLanguageFiles() {
		// Did the language file disappear?  Offer the language dialog.
		try {
			if (doesLanguagePreferenceExist()) {

				// Does the language preference have a language name value 
				// that matches a language name value in an available language .xml file
				String languageNameFromPref = languagePreferenceNode.get(LANGUAGE_KEY, defaultLanguage);
				if (!languages.containsKey(languageNameFromPref)) {
					logger.debug("isThisTheFirstTimeLoadingLanguageFiles() Language Name '{}' not available ...", languageNameFromPref);

					// To avoid some null issues in Translator.get(String key),
					// lets say it's the first run (to ask the user to select a valid language name)
					return true;
				}

				return false;
			}
		} catch (BackingStoreException e) {
			logger.error("Failed to load language", e);
		}
		return true;
	}

	/**
	 * @return true if a preferences node exists
	 * @throws BackingStoreException
	 */
	static private boolean doesLanguagePreferenceExist() throws BackingStoreException {
		return Arrays.asList(languagePreferenceNode.keys()).contains(LANGUAGE_KEY);
	}

	/**
	 * save the user's current language choice
	 */
	public static void saveConfig() {
		logger.debug("saveConfig()");
		languagePreferenceNode.put(LANGUAGE_KEY, currentLanguage);
	}

	/**
	 * load the user's language choice
	 */
	public static void loadConfig() {
		logger.debug("loadConfig: {}={}", languagePreferenceNode.toString(), defaultLanguage);
		currentLanguage = languagePreferenceNode.get(LANGUAGE_KEY, defaultLanguage);
	}

	/**
	 * Scan folder for language files.
	 * See http://stackoverflow.com/questions/1429172/how-do-i-list-the-files-inside-a-jar-file
	 * @throws IllegalStateException No language files found
	 */
	public static void loadLanguages() {
		languages.clear();
		
		try {
			int found=0;
			Stream<Path> walk = getLanguagePaths();
			Iterator<Path> it = walk.iterator();
			while (it.hasNext()) {
				Path p = it.next();
				String name = p.toString();
				//if( f.isDirectory() || f.isHidden() ) continue;
				if (FilenameUtils.getExtension(name).equalsIgnoreCase("xml") ) {
					if (name.endsWith("pom.xml")) {
						continue;
					}
					
					// found an XML file in the /languages folder.  Good sign!
					if (attemptToLoadLanguageXML(name)) found++;
				}
			}
			walk.close();
			
			//logger.debug("total found: "+found);
	
			if(found==0) {
				throw new IllegalStateException("No translations found.");
			}
		}
		catch(Exception e) {
			logger.error("{}. Defaulting to {}. Language folder expected to be located at {}", e.getMessage(), defaultLanguage, WORKING_DIRECTORY);
			final TranslatorLanguage languageContainer  = new TranslatorLanguage();
			String path = MarginallyCleverTranslationXmlFileHelper.getDefaultLanguageFilePath();
			logger.debug("default path requested: {}", path);
			URL pathFound = Translator.class.getClassLoader().getResource(path);
			logger.debug("path found: {}", pathFound);
			try (InputStream s = pathFound.openStream()) {
				languageContainer.loadFromInputStream(s);
			} catch (IOException ie) {
				logger.error(ie.getMessage());
			}
			languages.put(languageContainer.getName(), languageContainer);
		}
	}

	/**
	 * Set public for the need of ...
	 * @return
	 * @throws Exception 
	 */
	public static Stream<Path> getLanguagePaths() throws Exception {
		URI uri = Translator.class.getClassLoader().getResource(WORKING_DIRECTORY).toURI();
		logger.trace("Looking for translations in {}", uri.toString());
		
		Path myPath;
		if (uri.getScheme().equals("jar")) {
			FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap());
			myPath = fileSystem.getPath(WORKING_DIRECTORY);
		} else {
			myPath = Paths.get(uri);
		}

		Path rootPath = FileSystems.getDefault().getPath(FileAccess.getUserDirectory());
		logger.trace("rootDir={}", rootPath.toString());

		// we'll look inside the JAR file first, then look in the working directory.
		// this way new translation files in the working directory will replace the old JAR files.
		Stream<Path> walk = Stream.concat(
				Files.walk(myPath, 1),	// check inside the JAR file.
				Files.walk(rootPath,1)	// then check the working directory
				);

		return walk;
	}


	/**
	 * TODO To REVIEW : Seem to try to load all .xml file found ( to avoid loading non language related files, .xml language file should have a patern a the end of the filename like "_en.xml" or "_fr_FR.xml" ( _<CodeLang>[_<ContryCode>]).xml easyly matchable with a regexp ...) .
	 * <br>
	 * TODO to REVIEW : .xml DOCTYPE language  sould have a DTD to be validable ...
	 * <br>
	 * TODO to REVIEW : Currently if the user have a .xml file in PWD (WORKING_DIRECTORY) it will errase all values from a posible ( in the jar ) .xml ... (should notifiy of this )
	 * <br>
	 * TODO to REVIEW : Can we do a sort of Key history ( for the rename case ) (in version XYZ you have key "Shift" and in version XYZ+1 it becom "Paper.Shift" )
	 * <br>
	 * TODO to review : Unique Key is not currently assert in the .xml file as the key is not and id ( and as we map.put(key,val) (only the last val remain) (should be notifiy )
	 * 
	 * 
				From this : 
				<code>
				<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE language [

<!ELEMENT language (meta,string+)>

<!ELEMENT meta (name,author)>
<!ELEMENT name (#PCDATA)>
<!ELEMENT author (#PCDATA)>
<!ELEMENT string (key,value,hint?)>
<!ELEMENT key (#PCDATA)>				
<!ELEMENT value (#PCDATA)>
<!ELEMENT hint (#PCDATA)>
]>
</code>
				
				To this (but imply a review of the .xml format / read and write implementation ... as the key element should be migrated as a attribut id of each string tag ...)
		<code>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE language [

<!ELEMENT language (meta,string+)>
<!ATTLIST language   xml:lang NMTOKEN 'en'>

<!ELEMENT meta (name,author)>
<!ELEMENT name (#PCDATA)>
<!ELEMENT author (#PCDATA)>
<!ELEMENT string (value,hint?)>

<!ATTLIST string id ID #IMPLIED>
<!ELEMENT value (#PCDATA)>
<!ELEMENT hint (#PCDATA)>
]>		
			</code>	
				
	 * 
	 * <br>
	 * TODO to Review : Find an essay way to auto / colaborative traduction knowledg base. ( have a tools / front application to modifiy traductions ( so a user can't mess the XML / have a key renaming history / have tools to validate the xml / have version and propose a batch of traductions with version ...)
	 * <br>
	 * 
	 * @param name
	 * @return
	 * @throws Exception 
	 */
	private static boolean attemptToLoadLanguageXML(String name) throws Exception {
		String nameInsideJar = WORKING_DIRECTORY+"/"+FilenameUtils.getName(name);
		InputStream stream = Translator.class.getClassLoader().getResourceAsStream(nameInsideJar);
		String actualFilename = "Jar:"+nameInsideJar;
		File externalFile = new File(name);
		if(externalFile.exists()) {
			stream = new FileInputStream(new File(name));
			actualFilename = name;
		}
		if( stream == null ) return false;
		
		logger.debug("Found {}", actualFilename); // was a trace set as debug by PPAC37 to review some issues.
		TranslatorLanguage lang = new TranslatorLanguage();
		try {
			lang.loadFromInputStream(stream);
			logger.debug("Found {} // {} ", actualFilename,lang.getName()); // currenty a english.xml can have a language name value "dutch" ... this is disturbing not to be aware of that.
		} catch(Exception e) {
			logger.error("Failed to load {}", actualFilename);
			e.printStackTrace();// added by PPAC37. TODO should be remove after having review some issues.
			// if the xml file is invalid then an exception can occur.
			// make sure lang is empty in case of a partial-load failure.
			lang = new TranslatorLanguage();
		}
		
		if( !lang.getName().isEmpty() && 
			!lang.getAuthor().isEmpty()) {
			// we loaded a language file that seems pretty legit.
			if ( languages.containsKey(lang.getName())){
			    // So ... what ... lets overwirte it ?
			    // TODO maybe fusion it ? 
			    // But at least warm the user ?
			        logger.debug(String.format("SAME language Name \"%s\" ... using last one ...",lang.getName()));
				
			}
			languages.put(lang.getName(), lang);
			return true;
		}
		
		return false;
	}


	/**
	 * @param key name of key to find in translation list
	 * @return the translated value for key, or "missing:key".
	 */
	public static String get(String key) {
		return languages.get(currentLanguage).get(key);
	}
	
	public static void writeMissingKeyXmlFile(){
	    languages.get(currentLanguage).generatePartialXmlFileWithMissingKey();
	}

	/**
	 * Translates a string and fills in some details.  String contains the special character sequence "%N", where N is the n-th parameter passed to get()
	 * A %1 is replaced with the first parameter, %2 with the second, and so on.  There is no escape character.
	 * @param key name of key to find in translation list
	 * @param params 
	 * @return the translated value for key, or "missing:key".
	 */
	public static String get(String key,String [] params) {
		String modified = get(key);
		int n=1;
		for(String p : params) {
			modified = modified.replaceAll("%"+n, p);
			++n;
		}
		return modified;
	}

	/**
	 * @return the list of language names
	 */
	public static String[] getLanguageList() {
		final String[] choices = new String[languages.keySet().size()];
		final Object[] lang_keys = languages.keySet().toArray();

		for (int i = 0; i < lang_keys.length; ++i) {
			choices[i] = (String) lang_keys[i];
		}

		return choices;
	}

	/**
	 * @param currentLanguage the name of the language to make active.
	 */
	public static void setCurrentLanguage(String currentLanguage) {
		Translator.currentLanguage = currentLanguage;
	}

	public static int getCurrentLanguageIndex() {
		String [] set = getLanguageList();
		// find the current language
		for( int i=0;i<set.length; ++i) {
			if( set[i].equals(Translator.currentLanguage)) return i;
		}
		// now try the default
		for( int i=0;i<set.length; ++i) {
			if( set[i].equals(Translator.defaultLanguage)) return i;
		}
		// failed both, return 0 for the first option.
		return 0;
	}
	
	//
	// Needed be the TrnslatorTableModel
	//

	public static String getCurrentLanguage() {
	    return currentLanguage;
	}

	public static Map<String, TranslatorLanguage> getLanguages() {
	    return languages;
	}

	
	
}

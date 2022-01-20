/*
 * Copyright (C) 2022 Marginally Clever Robots, Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.marginallyclever.util;

import com.marginallyclever.makelangelo.Translator;
import com.marginallyclever.makelangelo.TranslatorLanguage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * TOD can it be done in the build ( ant and/or maven ) 
 * https://stackoverflow.com/questions/1527847/xml-dtd-schema-validation-in-maven
 * <br>
 * Appliquer une validation ... pour DTD vs XSD un validation avec un DTD est
 * moins puissant (en therme de control) et plus complex (en mise en ouvre car
 * c'est un langage a par entiérer et il doit etre définis dans le fichier .xml
 * (il peut etre embarqé ou mentionné ( une peus comme une feuille de style qui
 * peut etre dans le .html ou dans un .css mentionné dans le .html))) mais dans
 * le cas où il n'ai pas embarqué mais mentionner, la validation si le .xsd n'ai
 * pas disponivle ( ex un fichier embarque dans le jar pose des difficulté ...)
 * une validation par un XSD semble plus puissante .
 *
 * Avec un moteur XML il est posible de transformer un fichier .xml via un .xsl
 *
 * @author q6
 */
public class LanguageXmlValidation {

    public static void main(String[] args) {
	try {
	    PreferencesHelper.start();
	    Translator.start();
	    Stream<Path> walk = Translator.getLanguagePaths();
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
					   System.out.println(""+p.toString());
					    xmlValidationWithXsd(p.toString());
					//if (attemptToLoadLanguageXML(name)) found++;
				}
			}
		    
	       //
	} catch (Exception ex) {
	    Logger.getLogger(LanguageXmlValidation.class.getName()).log(Level.SEVERE, null, ex);
	}
	
	System.out.println("");
	String[] deletedKeyTab = {
	    //https://github.com/MarginallyClever/Makelangelo-software/commit/0b01453acdacfffe4851bebf40dfc2a02357e61a
	    "SetHome",
	    "GoHome",
	    "FindHome",
	    "DisengageMotors",
	    "EngageMotors",
	    //https://github.com/MarginallyClever/Makelangelo-software/commit/e50290298190a4b747751f356da0922366fc2c8c#diff-7254ebb4a67247586f3497b7148165f8330c029f478bf6d63ff0b4e26fa75dc2
	    "FileTypeGCode",
	    "FileTypeDXF",
	    "FileTypeScratch2",
	    //// "FileTypeScratch3",// come back later : https://github.com/MarginallyClever/Makelangelo-software/commit/39d338f571f8ee974273470e945da5283e338c61
	    "FileTypeSVG",
	    // (renamed but value change ... so to del ) https://github.com/MarginallyClever/Makelangelo-software/commit/b84463651e632d92a3d23fc73d62b864b2fa2626
	    "ChangeToolPrefix",
	    "ChangeToolPostfix",
	    //https://github.com/MarginallyClever/Makelangelo-software/commit/e9f859b16e980cd56374a1609d1d441ae2142e10
	    "Unpause",
	    "FindHome",
	    "MenuSaveGCODEAs",
	    "",
	    "",
	    "",
	};
	SortedSet<String> kb_deletedKey = new TreeSet<>();
	for ( String k : deletedKeyTab){
	    kb_deletedKey.add(k);
	}
	
	// TODO multiple rename case and posible cyclic  ? A -> b -> C -> A
	SortedMap<String,String> kb_renamedKey = new TreeMap<>();
	//https://github.com/MarginallyClever/Makelangelo-software/commit/9463279394b671ae0b3d9475f9678d283c026fac
	kb_renamedKey.put("PaperWidth","PaperSettings.PaperWidth");
	kb_renamedKey.put("PaperHeight","PaperSettings.PaperHeight");
	kb_renamedKey.put("OpenControls","RobotMenu.OpenControls");
	kb_renamedKey.put("paper color","PaperSettings.PaperColor");
	//?? https://github.com/MarginallyClever/Makelangelo-software/blame/6d77ba26fffc293020d74aefcb1d337bf9e24e71/src/main/resources/languages/english.xml
	//??kb_renamedKey.put("MenuSaveGCODEAs","RobotMenu.SaveGCode");
	kb_renamedKey.put("SaveGCode","RobotMenu.SaveGCode");
	//
	kb_renamedKey.put("converterWanderCMYK","ConverterWanderCMYK");
	kb_renamedKey.put("spiralToCorners","Spiral.toCorners");
	kb_renamedKey.put("SpiralPulseSpacing","SpiralPulse.spacing");
	kb_renamedKey.put("SpiralPulseHeight","SpiralPulse.height");
	kb_renamedKey.put("Sandy Noble Style","SandyNoble.title");
	//https://github.com/MarginallyClever/Makelangelo-software/commit/9791112bb72e1a0d8779d43a6466eaed9a7cb482
	kb_renamedKey.put("MenuGraphicsPenUp","GFXPreferences.showPenUp");
	kb_renamedKey.put("MenuGraphicsAntialias","GFXPreferences.antialias");
	kb_renamedKey.put("MenuGraphicsSpeedVSQuality","GFXPreferences.speedVSQuality");
	kb_renamedKey.put("MenuGraphicsDrawWhileRunning","GFXPreferences.showAllWhileDrawing");
	kb_renamedKey.put("MenuPreview","MenuView");
	kb_renamedKey.put("ZoomIn","MenuView.zoomIn");
	kb_renamedKey.put("ZoomOut","MenuView.zoomOut");
	kb_renamedKey.put("ZoomFit","MenuView.zoomFit");
	//https://github.com/MarginallyClever/Makelangelo-software/commit/0b01453acdacfffe4851bebf40dfc2a02357e61a
	kb_renamedKey.put("PenUp","JogInterface.PenUp");
	kb_renamedKey.put("PenDown","JogInterface.PenDown");
	//https://github.com/MarginallyClever/Makelangelo-software/commit/e9f859b16e980cd56374a1609d1d441ae2142e10
	kb_renamedKey.put("Pause","PlotterControls.Pause");
	kb_renamedKey.put("SaveGCode","PlotterControls.SaveGCode");
	kb_renamedKey.put("Rewind","PlotterControls.Rewind");
	kb_renamedKey.put("Play","PlotterControls.Play");	
	kb_renamedKey.put("Step","PlotterControls.Step");
	//https://github.com/MarginallyClever/Makelangelo-software/commit/021b83ec5d1134ad111ebdf2bbd4d4452c511aa5 // arf en to all ...
	kb_renamedKey.put("KochCurveName","KochTreeName");	
	kb_renamedKey.put("Diameter","AdjustPulleySize");	//?
	kb_renamedKey.put("MenuHilbertCurve","HilbertCurveName");//?
	//FlipForGlass
	//FileNotOpened
	//
//	kb_renamedKey.put("","");	
//	kb_renamedKey.put("","");	
//	kb_renamedKey.put("","");
	//
//	kb_renamedKey.put("","");	
//	kb_renamedKey.put("","");	
//	kb_renamedKey.put("","");
	
	// Special case fusion
	/*AboutHTMLAfterVersionNumber + "%VERSION%" + AboutHTMLBeforeVersionNumber -> AboutHTML
https://github.com/MarginallyClever/Makelangelo-software/commit/7605a228af378ff724c25162587be90fb65993fd*/
	
	System.out.println("");
	try{
	    String oldLanguageXmlFilePath = "/home/q6/tmp_git_try/PPAC37/Makelangelo-software/target/classes/languages/french_893a98a573a4e07b86b2704f50d380fa11602440.xml";
	    File fsrc = new File ( oldLanguageXmlFilePath);

	    xmlValidationWithXsd(fsrc.toString());
	      // Let try with what we have (but this have to be redone cause i whant the hint ...)
	    TranslatorLanguage tlOld = new TranslatorLanguage();
	    tlOld.loadFromString(oldLanguageXmlFilePath);
	    //
	    String actualEnglishXmlPath ="/home/q6/tmp_git_try/PPAC37/Makelangelo-software/target/classes/languages/english.xml";
	    xmlValidationWithXsd(actualEnglishXmlPath);
	    
	    TranslatorLanguage tlActualEng = new TranslatorLanguage();
	    tlActualEng.loadFromString(actualEnglishXmlPath);
	    
	    Map<String, String> mapOld = tlOld.getStrings();
	    Map<String, String> mapRef = tlActualEng.getStrings();
	    
	    SortedSet<String> ssKeyToKeep = new TreeSet<>();
	    SortedSet<String> ssKeyToResolve = new TreeSet<>();
	    for ( String kOld : mapOld.keySet()){
		if ( mapRef.keySet().contains(kOld)){
		    // key to keep
		    ssKeyToKeep.add(kOld);
		}else{
		    // not containe in actual eng xml ...
		    // have to determine if a del renamed or fusined key ...
		    //System.out.println(" ? "+kOld);
		    ssKeyToResolve.add(kOld);
		}
	    }
	    //
	    System.out.printf("found %d key , %d known, %d to resolv using english.xml as reference.\n",mapOld.keySet().size(), ssKeyToKeep.size(),ssKeyToResolve.size());
	    
	    Iterator<String> kToResolvIterator = ssKeyToResolve.iterator();
	    while ( kToResolvIterator.hasNext()){
		String k = kToResolvIterator.next();
		
		if ( kb_deletedKey.contains(k)){
		    System.out.println(" DEL "+k);
		}else if ( kb_renamedKey.keySet().contains(k) ){
		    System.out.println(" REN "+k+" -> "+kb_renamedKey.get(k));
		} else{
		    System.out.println(" ??? "+k);
		}
		// TODO have a knowledge base on what to do .. .maybe to keep maybe to drop maybe to fusion ...
		
		// deleted -> drop 
		
		// renamed -> 
		
		// fusionned -> ...
	    }
	    
	}catch (Exception e){
	    e.printStackTrace();
	}
    }

    /**
     * https://stackoverflow.com/questions/15732/whats-the-best-way-to-validate-an-xml-file-against-an-xsd-file
     *
     */
    public static void xmlValidationWithXsd(String languageFilePath) throws Exception {
	//URL schemaFile = new URL("http://host:port/filename.xsd");
	
	//
	URL schemaFile = LanguageXmlValidation.class.getResource("/languages/language.xsd");
	//URL schemaFile = LanguageXmlValidation.class.getResource("/languages/language_v0.dtd");//ko
	
	//System.out.println("using xsd : "+schemaFile.toString());
	// webapp example xsd: 
	// URL schemaFile = new URL("http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd");
	// local file example:
	// File schemaFile = new File("/location/to/localfile.xsd"); // etc.
	//Source xmlFile = new StreamSource(new File("target/classes/languages/english.xml"));
	Source xmlFile = new StreamSource(new File(languageFilePath));
	SchemaFactory schemaFactory = SchemaFactory
		.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	try {
	    Schema schema = schemaFactory.newSchema(schemaFile);
	    Validator validator = schema.newValidator();
	    validator.validate(xmlFile);
	    System.out.println(String.format("[ %5s ] %s","valid",xmlFile.getSystemId()));
	} catch (SAXException e) {
	    System.out.println(String.format("[ %5s ] %s\n%s","error",xmlFile.getSystemId() , e));
	   // e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    //
    //
    //
    public static void verifyValidatesInternalXsd(String filename) throws Exception {
	InputStream xmlStream = new FileInputStream(filename);
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	factory.setValidating(true);
	factory.setNamespaceAware(true);
	factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
		"http://www.w3.org/2001/XMLSchema");
	DocumentBuilder builder = factory.newDocumentBuilder();
	builder.setErrorHandler(new RaiseOnErrorHandler());
	builder.parse(new InputSource(xmlStream));
	xmlStream.close();
    }

    public static class RaiseOnErrorHandler implements ErrorHandler {

	@Override
	public void warning(SAXParseException e) throws SAXException {
	    throw new RuntimeException(e);
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
	    throw new RuntimeException(e);
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
	    throw new RuntimeException(e);
	}
    }
//
//    //
//    //
//    //
//    public static void validationWithRessourceResolver(File xmlFileLocation) throws Exception {
//	Source xmlFile = new StreamSource(xmlFileLocation);
//	SchemaFactory schemaFactory = SchemaFactory
//		.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
//	Schema schema = schemaFactory.newSchema();
//	Validator validator = schema.newValidator();
//	validator.setResourceResolver(new LSResourceResolver() {
//	    @Override
//	    public LSInput resolveResource(String type, String namespaceURI,
//		    String publicId, String systemId, String baseURI) {
//		InputSource is = new InputSource(
//			getClass().getResourceAsStream(
//				"some_local_file_in_the_jar.xsd"));
//		// or lookup by URI, etc...
//		//return new Input(is); // for class Input see 
//		return new Input(publicId, systemId, resourceAsStream);
//		// https://stackoverflow.com/a/2342859/32453
//	    }
//	});
//	validator.validate(xmlFile);
//    }
//
//    public class Input implements LSInput {
//
//	private String publicId;
//
//	private String systemId;
//
//	public String getPublicId() {
//	    return publicId;
//	}
//
//	public void setPublicId(String publicId) {
//	    this.publicId = publicId;
//	}
//
//	public String getBaseURI() {
//	    return null;
//	}
//
//	public InputStream getByteStream() {
//	    return null;
//	}
//
//	public boolean getCertifiedText() {
//	    return false;
//	}
//
//	public Reader getCharacterStream() {
//	    return null;
//	}
//
//	public String getEncoding() {
//	    return null;
//	}
//
//	public String getStringData() {
//	    synchronized (inputStream) {
//		try {
//		    byte[] input = new byte[inputStream.available()];
//		    inputStream.read(input);
//		    String contents = new String(input);
//		    return contents;
//		} catch (IOException e) {
//		    e.printStackTrace();
//		    System.out.println("Exception " + e);
//		    return null;
//		}
//	    }
//	}
//
//	public void setBaseURI(String baseURI) {
//	}
//
//	public void setByteStream(InputStream byteStream) {
//	}
//
//	public void setCertifiedText(boolean certifiedText) {
//	}
//
//	public void setCharacterStream(Reader characterStream) {
//	}
//
//	public void setEncoding(String encoding) {
//	}
//
//	public void setStringData(String stringData) {
//	}
//
//	public String getSystemId() {
//	    return systemId;
//	}
//
//	public void setSystemId(String systemId) {
//	    this.systemId = systemId;
//	}
//
//	public BufferedInputStream getInputStream() {
//	    return inputStream;
//	}
//
//	public void setInputStream(BufferedInputStream inputStream) {
//	    this.inputStream = inputStream;
//	}
//
//	private BufferedInputStream inputStream;
//
//	public Input(String publicId, String sysId, InputStream input) {
//	    this.publicId = publicId;
//	    this.systemId = sysId;
//	    this.inputStream = new BufferedInputStream(input);
//	}
//    }
}

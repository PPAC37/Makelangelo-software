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

import java.io.File;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Iterator;
import java.util.SortedMap;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stream.StreamResult;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author q6
 */
public class FindAllTraductionXMLGenerator {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FindAllTraductionXMLGenerator.class);

    public static final String XML_TAG_LANGUAGE = "language";//"language"

    // FROM PR 455 TODO Get them from TranslatorLanguage.java if PR 455 is merdged.
    public static final String XML_TAG_NAME = "name";
    public static final String XML_TAG_AUTHOR = "author";
    //
    public static final String XML_TAG_STRING = "string";
    //
    public static final String XML_TAG_KEY = "key";
    public static final String XML_TAG_VALUE = "value";
    public static final String XML_TAG_HINT = "hint";

    /**
     * PPAC37 : TODO to help adding missing key ...
     *
     * https://examples.javacodegeeks.com/core-java/xml/parsers/documentbuilderfactory/create-xml-file-in-java-using-dom-parser-example/
     *
     * https://mkyong.com/java/how-to-create-xml-file-in-java-dom/
     *
     */
    public static void generatePartialXmlFileWithMissingKey(SortedMap<String, ArrayList<FindAllTraductionResult>> groupIdenticalMissingKey) {
	generatePartialXmlFileWithMissingKey(groupIdenticalMissingKey, new StreamResult(System.out));
    }

    public static void generatePartialXmlFileWithMissingKey(SortedMap<String, ArrayList<FindAllTraductionResult>> groupIdenticalMissingKey, File xmlFileOutput) {
	if (xmlFileOutput == null) {
	    generatePartialXmlFileWithMissingKey(groupIdenticalMissingKey, new StreamResult(System.out));
	} else {
	    generatePartialXmlFileWithMissingKey(groupIdenticalMissingKey, new StreamResult(xmlFileOutput));
	}
    }

    public static void generatePartialXmlFileWithMissingKey(SortedMap<String, ArrayList<FindAllTraductionResult>> groupIdenticalMissingKey, StreamResult streamResult) {
	boolean generatJustElementsStringAndNoHeaderOrParents = false;
	try {
	    //SortedSet<String> missingKeys = new TreeSet<>();
	    String name = "auto_generated_missing_key";//LanguageName
	    String author = "none";//
	    String textTraductionValueTODO = "";//TODO:";
	    String textTraductionHintTODO = " ";//TODO";// avoid empty string or you get self closing element that do not help edition...

	    Iterator<String> it = groupIdenticalMissingKey.keySet().iterator();
	    
	    DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
	    Document doc = documentBuilder.newDocument();
	    
	    
	    // root element
	    Element root = doc.createElement(XML_TAG_LANGUAGE);
	    
	    final long currentTimeMillis = System.currentTimeMillis();
	    Date dateGenerated = new Date(currentTimeMillis);
//		     root.setAttribute("timestamp", ""+currentTimeMillis);
	    SimpleDateFormat sdf = new SimpleDateFormat();
//		     root.setAttribute("date", sdf.format(dateGenerated));


	    String usedAsMetaLanguageNameAndInComment = name + "_" + sdf.format(dateGenerated);// The "_" at the end is to avoid having the same language name. (or this will replace all traduction ... when loaded)

	     Comment comment = doc.createComment(" language meta name : "+usedAsMetaLanguageNameAndInComment+"\n\t in the contente of <"+XML_TAG_VALUE+"> for special characters like '<', '&', \nneed <![CDATA[...]]> or encoding like :\n\t\"&lt;\" for '<' or \"&amp;\" for '&' ... see ?. ");
	     
	    // add xml comment
	    doc.appendChild(comment);
	    
	    //a way not to include the "meta" 
	    if ( !generatJustElementsStringAndNoHeaderOrParents ){
		doc.appendChild(root);
		 // add xml comment
	    //root.appendChild(comment);
	    
	    Element elemMeta = doc.createElement("meta");
	    root.appendChild(elemMeta);

	    Element elemLanguageName = doc.createElement(XML_TAG_NAME);
	    elemMeta.appendChild(elemLanguageName);
		
	    // TODO : find a best setTextContent ( posibly with the language name used to make the map used ..)
	    elemLanguageName.setTextContent(usedAsMetaLanguageNameAndInComment);
	    Element elemAuthor = doc.createElement(XML_TAG_AUTHOR);
	    elemMeta.appendChild(elemAuthor);
	    elemAuthor.setTextContent(author);

	   
	  
	    }else{
		// a way to avoid DomException as we do not create a unique root element (maybe a better way ?)
		doc.setStrictErrorChecking(false);
		
	    }

	    while (it.hasNext()) {
		String k = it.next();
		//
		Element elemString = doc.createElement(XML_TAG_STRING);
		elemString.setAttribute("id", k.replace(' ', '_').replace('*', '_'));
		Element elemKey = doc.createElement(XML_TAG_KEY);
		elemKey.appendChild(doc.createTextNode(k));
		Element elemValue = doc.createElement(XML_TAG_VALUE);
		
		elemValue.setAttribute("lang", "en");
		
		//		     // add xml CDATA
		//		    CDATASection cdataSection = doc.createCDATASection("HTML tag <code>testing</code>");
		//		    elemValue.appendChild(cdataSection);
		elemValue.appendChild(doc.createTextNode(textTraductionValueTODO + k));
		//
		Element elemHint = doc.createElement(XML_TAG_HINT);
		elemHint.appendChild(doc.createTextNode(textTraductionHintTODO));

		elemString.appendChild(elemKey);
		elemString.appendChild(elemValue);
		elemString.appendChild(elemHint);
		
		Comment afterHintComment = doc.createComment("\t\""+k+"\"");
		for (FindAllTraductionResult tr : groupIdenticalMissingKey.get(k)) {
		    String sFormatUsedInSrcInfo = String.format("\n\tused in \"%s\" line %d", tr.pSrc, tr.lineInFile);
		    //Comment afterHintComment = doc.createComment(sFormatUsedInSrcInfo);
		    afterHintComment.appendData(sFormatUsedInSrcInfo);
		    //logger.error(" used in \"{}\" line {}", tr.pSrc, tr.lineInFile);
		    elemString.appendChild(afterHintComment);
		}
		afterHintComment.appendData("\n");
		
		if ( !generatJustElementsStringAndNoHeaderOrParents ){
		    // append to the root element 
		    root.appendChild(elemString);
		}else{
		    // directely append to the document
		    doc.appendChild(elemString);
		}
	    }
	    // create the xml file
	    //transform the DOM Object to an XML File
	    
	    TransformerFactory transformerFactory = TransformerFactory.newInstance();
	    logger.debug("default transformerFactory class : {}",transformerFactory.getClass().getName());
	    try{
		// xalan // transformerFactory = TransformerFactory.newInstance("org.apache.xalan.processor.TransformerFactoryImpl",null);
		
		// saxon // 
		transformerFactory = TransformerFactory.newInstance("org.apache.xalan.processor.TransformerFactoryImpl",null);
	    }catch (Exception e){}
	    logger.debug("using transformerFactory class : {}",transformerFactory.getClass().getName());
	    
	    Transformer transformer = transformerFactory.newTransformer();
	    logger.debug("transformer class : {}",transformer.getClass().getName());
	    
	    // pretty print XML
	    if ( true ){
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	    }

	    //
	    if ( generatJustElementsStringAndNoHeaderOrParents ){
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	    }
	    // This set the texte content of the element valeur in a <![CDATA[...]]>
	    //transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "value");
	    
	    //transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
	    
	    //
	    transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "language_v0.dtd");
	    
	    DOMSource domSource = new DOMSource(doc);

//		String xmlFilePath = "missing_language.xml";
//		final File file = new File(xmlFilePath);
//		StreamResult streamResult = new StreamResult(file);
	    // If you use
//		 StreamResult streamResult = new StreamResult(System.out);
	    // the output will be pushed to the standard output ...
	    // You can use that for debugging 
	    transformer.transform(domSource, streamResult);//new StAXResult() ???
//		System.out.println("Done creating XML File: " + file.getAbsolutePath());
	} catch (TransformerException ex) {
	    logger.error("{} {}", ex.getMessage(), ex);
	} catch (ParserConfigurationException ex) {
	    logger.error("{} {}", ex.getMessage(), ex);
	}
    }
    
    public static void main(String[] args) {
	FindAllTraductionGet.main(args);
    }
}

package com.marginallyclever.makelangelo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TranslatorLanguage {

	private static final Logger logger = LoggerFactory.getLogger(TranslatorLanguage.class);
	
	private String name = "";
	private String author = "";
	private Map<String, String> strings = new HashMap<String, String>();


	/**
	 * @param languageFile
	 */
	public void loadFromString(String languageFile) {
		final DocumentBuilder db = getDocumentBuilder();
		if (db == null) {
			return;
		}
		Document dom = null;
		try {
			//Using factory get an instance of document builder
			//parse using builder to get DOM representation of the XML file
			dom = db.parse(languageFile);
		} catch (SAXException | IOException e) {
			logger.error("Failed to load file {}", languageFile, e);
		}
		if (dom == null) {
			return;
		}
		load(dom);
	}

	/**
	 * @param inputStream
	 */
	public void loadFromInputStream(InputStream inputStream) {
		final DocumentBuilder db = getDocumentBuilder();
		if (db == null) {
			return;
		}
		try {
			Document dom = db.parse(inputStream);
			load(dom);
		} catch (SAXException | IOException e) {
			logger.error("Failed to parse language file", e);
		}
	}

	private void load(Document dom) {
		final Element docEle = dom.getDocumentElement();

		name = docEle.getElementsByTagName(XML_TAG_NAME).item(0).getFirstChild().getNodeValue();
		author = docEle.getElementsByTagName(XML_TAG_AUTHOR).item(0).getFirstChild().getNodeValue();

		NodeList nl = docEle.getElementsByTagName(XML_TAG_STRING);
		if (nl != null && nl.getLength() > 0) {
			for (int i = 0; i < nl.getLength(); i++) {

				//get the element
				Element el = (Element) nl.item(i);
				String key = getTextValue(el, XML_TAG_KEY);
				String value = getTextValue(el, XML_TAG_VALUE);

				// store key/value pairs into a map
				//logger.debug(language_file +"\t"+key+"\t=\t"+value);// KO language_file no in this scoop
				
				if ( strings.containsKey(key)){
				    // This sould not occure.
				    // TO REVIEW : this is due to the fact that the xml is manualy edited.
				    // so ther is posibly multiple identical Key (that should be unique ).
				//Key Unicity can be done with xml validation ... but this mean using the key as an id attribut 
				//and adding a DTD to do .xml language DOCTYPE validation.
				
				// but in xml id value have some limitation (should be a NCName ... )
				
				    // ? ligne / pos in the file / stream ?
				    logger.debug(String.format("SAME Key \"%s\" new value \"%s\" old value \"%s\"",key,value,strings.get(key)));
				}
				strings.put(key, value);
			}
		}
	}
	
	public static final String XML_TAG_NAME = "name";
	public static final String XML_TAG_AUTHOR = "author";
	//
	public static final String XML_TAG_STRING = "string";
	//
	public static final String XML_TAG_KEY = "key";
	public static final String XML_TAG_VALUE = "value";
	public static final String XML_TAG_HINT = "hint";
    

	private DocumentBuilder getDocumentBuilder() {
		DocumentBuilder db = null;
		try {
			db = buildDocumentBuilder().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			logger.error("Failed to create a new document", e);
		}
		return db;
	}

	private DocumentBuilderFactory buildDocumentBuilder() {
		return DocumentBuilderFactory.newInstance();
	}

	public SortedSet<String> missingKeys = new TreeSet();
	
	public String get(String key) {
		if(strings.containsKey(key)) {
			return strings.get(key);
		} else {
			// a sorted set of all the missing key to generat a essay .xml 
			missingKeys.add(key);		    
			return "Missing:"+key;
		}
	}

	/**
	 * https://examples.javacodegeeks.com/core-java/xml/parsers/documentbuilderfactory/create-xml-file-in-java-using-dom-parser-example/
	 *
	 * https://mkyong.com/java/how-to-create-xml-file-in-java-dom/
	 *
	 */
	public void generateParialXmlFileWithMissingKey() {
	    try {
		//FileWriter fw = new FileWriter("missing_language.xml");
		Iterator<String> it = missingKeys.iterator();
		DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
		Document doc = documentBuilder.newDocument();
		// root element
		Element root = doc.createElement("language");
		final long currentTimeMillis = System.currentTimeMillis();
		Date dateGenerated = new Date(currentTimeMillis);
		     root.setAttribute("timestamp", ""+currentTimeMillis);
		     SimpleDateFormat sdf = new SimpleDateFormat();
		     root.setAttribute("date", sdf.format(dateGenerated));
		     
		doc.appendChild(root);
		
		Element elemMeta = doc.createElement("meta");
		root.appendChild(elemMeta);
		
		Element elemLanguageName = doc.createElement(XML_TAG_NAME);
		elemMeta.appendChild(elemLanguageName);
		elemLanguageName.setTextContent(name+"_");
		
		Element elemAuthor = doc.createElement(XML_TAG_AUTHOR);
		elemMeta.appendChild(elemAuthor);
		elemAuthor.setTextContent(author);
		
		// add xml comment
		Comment comment = doc.createComment("for special characters like < &, need CDATA");
		elemMeta.appendChild(comment);
		while (it.hasNext()) {
		    String k = it.next();
		    //
		    Element elemString = doc.createElement(XML_TAG_STRING);
		    Element elemKey = doc.createElement(XML_TAG_KEY);
		    elemKey.appendChild(doc.createTextNode(k));
		    Element elemValue = doc.createElement(XML_TAG_VALUE);
    //		     // add xml CDATA
    //		    CDATASection cdataSection = doc.createCDATASection("HTML tag <code>testing</code>");
    //		    elemValue.appendChild(cdataSection);
		    elemValue.appendChild(doc.createTextNode("todo:" + k));
		    //
		    Element elemHint = doc.createElement(XML_TAG_HINT);
		    elemHint.appendChild(doc.createTextNode("todo"));
		    elemString.appendChild(elemKey);
		    elemString.appendChild(elemValue);
		    elemString.appendChild(elemHint);
		    root.appendChild(elemString);
		}
		// create the xml file
		//transform the DOM Object to an XML File
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		// pretty print XML
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource domSource = new DOMSource(doc);
		String xmlFilePath = "missing_language.xml";
		final File file = new File(xmlFilePath);
		StreamResult streamResult = new StreamResult(file);
		// If you use
		// StreamResult result = new StreamResult(System.out);
		// the output will be pushed to the standard output ...
		// You can use that for debugging 
		transformer.transform(domSource, streamResult);
		System.out.println("Done creating XML File: " + file.getAbsolutePath());
	    } catch (TransformerException ex) {
		java.util.logging.Logger.getLogger(TranslatorLanguage.class.getName()).log(Level.SEVERE, null, ex);
	    } catch (ParserConfigurationException ex) {
		java.util.logging.Logger.getLogger(TranslatorLanguage.class.getName()).log(Level.SEVERE, null, ex);
	    }
	}

	/**
	 * <p>
	 * When a newline character "\n" was being read in from an xml file,
	 * it was being escaped ("\\n") and thus not behaving as an actual newline.
	 * This method replaces any "\\n" with "\n".
	 * </p>
	 * <p>
	 * <p>
	 * I take a xml element and the tag name, look for the tag and get
	 * the text content
	 * i.e for <employee><name>John</name></employee> xml snippet if
	 * the Element points to employee node and tagName is 'name' I will return John
	 * </p>
	 *
	 * @param ele     XML element
	 * @param tagName name of 'tag' or child XML element of ele
	 * @return text value of tagName
	 */
	private String getTextValue(Element ele, String tagName) {
		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);
		if (nl != null && nl.getLength() > 0) {
			Element el = (Element) nl.item(0);
			textVal = el.getFirstChild().getNodeValue();
		}
		textVal = textVal.replace("\\n", "\n");
		return textVal;
	}

	public String getName() {
		return name;
	}

	public String getAuthor() {
		return author;
	}
}

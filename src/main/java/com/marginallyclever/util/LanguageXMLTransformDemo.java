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
import static com.marginallyclever.util.LanguageXmlValidation.outputlLocalInfo;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Locale;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;

/**
 * To do some xml transformation.
 * <br>
 * Hopping this can be usefull to help traductors. *
 *
 *
 *
 * @author PPAC37
 */
public class LanguageXMLTransformDemo {

    // ? a curated liste of currently implemented ( available ) language .xml file with there code Lang , code contry to match a Local ?
    // ? auto match local from file name / or from the contente //language/meta/name ? 
    // english.xml -> _en_GB ?
    // dutch.xml -> _nl
    // ...
    // for RessourcesBundel .properties file compatibility ? define a default (that can be change ? )
    // cf MyBundel.properties , MyBundel_en.properties, MyBundel_en_CA.properties, MyBundel_fr_FR.properties
    public static void main(String[] args) throws IOException, URISyntaxException, TransformerException {
	try {
	    //final File inputXmlFile = new File("src/main/resources/languages/english.xml");
	    //final File inputXmlFile = new File("src/main/resources/languages/dutch.xml");
	    final File inputXmlFile = new File("src/main/resources/languages/german.xml");
	    String result = computeGitHashObjectForAFile(inputXmlFile);
	    //System.out.println(sGITHASHTOPREPED);
	    System.out.println(result);
	    //
	    File fileXSLT = new File("src/main/resources/languages/language_v0_To_v1.xsl");
	    // File fileOutput = new File("src/main/resources/languages/english_out.xml");
	    //File fileOutput = new File("src/main/resources/languages/dutch_out.xml");
	    File fileOutput = new File("src/main/resources/languages/german_out.xml");
	    xmlTransformLanguagesXmlFile(fileXSLT, inputXmlFile, fileOutput);

	    fileXSLT = new File("src/main/resources/languages/language_v0_To_properties.xsl");
	    fileOutput = new File("src/main/resources/languages/lang.properties");
	    xmlTransformLanguagesXmlFile(fileXSLT, inputXmlFile, fileOutput);

	    PreferencesHelper.start();
	    Translator.start();
	    Stream<Path> walk = Translator.getLanguagePaths();
	    Iterator<Path> it = walk.iterator();
	    while (it.hasNext()) {
		Path p = it.next();
		String name = p.toString();
		//if( f.isDirectory() || f.isHidden() ) continue;
		if (FilenameUtils.getExtension(name).equalsIgnoreCase("xml")) {
		    if (name.endsWith("pom.xml")) {
			continue;
		    }

		    //
		    if (!name.contains("languages")) {
			continue;
		    }

		    String codeLang = "???";
		    // found an XML file in the /languages folder.  Good sign!
		    System.out.println(">>" + p.toString());
		    try {
			//LocaleUtils.toLocale(
			Locale l = Locale.forLanguageTag(p.getFileName().toString().replaceAll(".xml", "").substring(0, 3));
			//l = new Locale.Builder().setLanguageTag(p.getFileName().toString().replaceAll(".xml", "").substring(0,3)).build();
			outputlLocalInfo(l);

			for (String sLangIso : Locale.getISOLanguages()) {
			    Locale tmpLocal = Locale.forLanguageTag(sLangIso);
			    //outputlLocalInfo(tmpLocal);
			    if (tmpLocal.getISO3Language().equalsIgnoreCase(l.getISO3Language())) {
				l = tmpLocal;
				break;
			    }
			}
			//

			//https://en.wikipedia.org/wiki/IETF_language_tag
			codeLang = l.getLanguage();
			if (codeLang.length() > 2) {
			    if (codeLang.equalsIgnoreCase("fre")) {
				codeLang = "fr";
			    } else if (codeLang.equalsIgnoreCase("ger")) {
				codeLang = "de";
			    } else if (codeLang.equalsIgnoreCase("chi")) {
				codeLang = "zh";
			    } else if (codeLang.equalsIgnoreCase("dut")) {
				codeLang = "nl";
			    } else if (codeLang.equalsIgnoreCase("pig")) {
				codeLang = "pi"; //?
			    } else if (codeLang.equalsIgnoreCase("gre")) {
				codeLang = "el";
			    } else {
				outputlLocalInfo(l);
			    }
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		    //xmlValidationWithXsd(p.toString());
		    //if (attemptToLoadLanguageXML(name)) found++;
		    File fileSrc = p.toFile();
		    fileXSLT = new File("src/main/resources/languages/language_v0_To_properties.xsl");
		    fileOutput = new File("src/main/resources/languages/lang_" + codeLang + ".properties");
		    xmlTransformLanguagesXmlFile(fileXSLT, fileSrc, fileOutput);

		}
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * XSTL transformation of an xml file. ( TODO : is this xstl v1.0 engine or
     * xstl v2.0 engine ... seem to be v1.0 engine but ??? how to find that info
     * ? )
     * <br>
     * Currently 2 parameter are feed to the XSTL trnasformer
     * <code>transformer.setParameter("src", inputXmlFile.toString());</code>.
     *
     * Then inside your XSLT document declare the parameter using
     * <code><xsl:param name="yourParamName" /></code>
     *
     * and you can then use it in your XSLT for example this way:
     * <code><xsl:value-of select="$yourParamName" /></code>.
     * <br>
     * Important to note that <code><xsl:param> </code> must be declared at the
     * top level of the stylesheet. If you declare it within a template, it will
     * be considered a parameter to the template not to the whole stylesheet.
     *
     * <br>
     * Sources :
     * https://stackoverflow.com/questions/4604497/xslt-processing-with-java
     * https://stackoverflow.com/questions/7681037/execute-xslt-transform-from-java-with-parameter
     *
     * <br>
     * for posible encoding complication in the output : (but was my editor
     * defaut encoding used on .properties file and not the current
     * implementation that have some bad encoding management ... )
     * <br>
     * (en)
     * https://softwareengineering.stackexchange.com/questions/200037/what-encoding-is-used-by-javax-xml-transform-transformer
     * (en)
     * https://stackoverflow.com/questions/36247144/xslt-transforms-utf-8-characters-to-a-different-encoding
     * (fr)
     * https://askcodez.com/transformer-setoutputproperty-outputkeys-encoding-utf-8-ne-fonctionne-pas.html
     *
     * <br>
     * XSLT 2.0 support with Saxon 9.x before 9.8 or XSLT 3.0 support (except
     * streaming, higher order functions, xsl:evaluate, schema-awareness,
     * backwards compatibility) with 9.8. For full XSLT 3.0 support you need to
     * download Saxon 9 PE or EE from Saxonica and put it together with a
     * license you buy or a trial licence you request on the classpath.
     * https://sourceforge.net/projects/saxon/files/
     * https://stackoverflow.com/questions/41317022/how-can-i-use-xslt-2-0-and-xslt-3-0-in-java
     *
     * @param fileXSLT
     * @param inputXmlFile
     * @param fileOutput
     * @throws TransformerConfigurationException
     * @throws TransformerFactoryConfigurationError
     * @throws TransformerException
     * @throws IOException
     */
    public static void xmlTransformLanguagesXmlFile(final File fileXSLT, final File inputXmlFile, final File fileOutput) throws TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException, IOException {
	TransformerFactory factory = TransformerFactory.newInstance();
	// Or to be sure to use Xalan ?
	//TransformerFactory factory = TransformerFactory.newInstance("org.apache.xalan.processor.TransformerFactoryImpl", null);	

	Source xslt = new StreamSource(fileXSLT);
	Transformer transformer = factory.newTransformer(xslt);

	transformer.setParameter("src", inputXmlFile.toString());
	transformer.setParameter("timestamp", System.currentTimeMillis());

	FileOutputStream out = new FileOutputStream(fileOutput);

	//	
	transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	transformer.setOutputProperty(OutputKeys.INDENT, "yes");

	Source text = new StreamSource(inputXmlFile);
	//transformer.transform(text, new StreamResult(fileOutput));
	transformer.transform(text, new StreamResult(out));
    }

    //
    //
    //
    /**
     * Sould return the same as <code>git hash-object</code>. a sha1 chechsum
     * computed from the file contente pre-appended with "blob
     * "+file.length()+char(0);
     * <br>
     * <code>git hash-object &lt;(file)</code>
     * <br>
     * See
     * https://stackoverflow.com/questions/415953/how-can-i-generate-an-md5-hash-in-java
     *
     * <br>
     * https://stackoverflow.com/questions/21538752/find-git-history-and-current-file-path-from-object-hash
     * ??? System.out.printf("git log %s --name-only",result);
     *
     * @param file
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static String computeGitHashObjectForAFile(final File file) throws NoSuchAlgorithmException, IOException {
	String sGITHASHTOPREPED = String.format("blob %d%c", file.length(), 0);

	// TODO ? for big file maybe a byte buffer and a loop ...
	//https://stackoverflow.com/questions/858980/file-to-byte-in-java
	byte[] fileContent = Files.readAllBytes(file.toPath());

	final MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
	messageDigest.reset();

	//
	messageDigest.update(sGITHASHTOPREPED.getBytes(Charset.forName("UTF8")));

	messageDigest.update(fileContent);

	//
	final byte[] resultByte = messageDigest.digest();

	// Now to have a Hexa representation of the digest result ... 
	// TODO better / simpler way ?
	// final String result = new String(Hex.encodeHex(resultByte));
	// or someting like 
	StringBuilder sb = new StringBuilder();
	for (int i = 0; i < resultByte.length; ++i) {
	    sb.append(Integer.toHexString((resultByte[i] & 0xFF) | 0x100).substring(1, 3));
	}
	String result = sb.toString();
	return result;
    }

}

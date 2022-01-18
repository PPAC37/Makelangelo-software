/**
 * source : https://gist.github.com/dilipkumarg/e1bae01b6d3976ec271b
 */
package com.marginallyclever.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * To replace all occurence of a pattern on a large file. TODO add limitation ?
 * control ? simulation ?
 * <br>
 * source : https://gist.github.com/dilipkumarg/e1bae01b6d3976ec271b
 * <br>
 * modified by PPAC.
 *
 * @author q6
 */
public class FileContentReplacer {

    private final Pattern pattern;
    private final String replaceMent;

    
    private boolean debug = true;
    private long currentLineNum = 0;
    private long modifiedLineCount = 0;
    private long onlyThisLine = -1;

    /**
     * ?TODO as pattern is a kind of reg exp propose a utility fonction to
     * despecialize regexp special char (like '.' (or ('(' , ')' '[' ']' ...)
     * have to be transform in '\.' to match a real '.' ... )
     *
     * @param pattern
     * @param replaceMent
     */
    public FileContentReplacer(String pattern, String replaceMent) {
	this.pattern = Pattern.compile(pattern,Pattern.LITERAL);
	this.replaceMent = replaceMent;
    }

    public String matchAndReplace(long lineNum, String line) {
	//return pattern.matcher(line).replaceAll(replaceMent);
	Matcher matcher = pattern.matcher(line);
	if (matcher.find()) {
	    if (onlyThisLine != -1 && lineNum != onlyThisLine) {
		//toodo in debug info skiped cause not the line
		System.out.println(String.format("line %d skip : %s", lineNum, line));
		return line;
	    }
	    System.out.println(String.format("line %d old : %s", lineNum, line));
	    String res = matcher.replaceAll(replaceMent);
	    System.out.println(String.format("line %d new : %s", lineNum, res));
	    modifiedLineCount++;
	    return res;
	} else {
	    return line;
	}
    }

    public void matchAndReplace(File srcFile, File outFile) throws IOException {
	modifiedLineCount = 0;
	currentLineNum = 0; // reset the line num for debug / simulation 
	System.out.println(String.format("pattern     : %s", pattern.pattern()));
	System.out.println(String.format("replacement : %s", replaceMent));
	System.out.println(String.format("srcFile : %s", srcFile.toString()));

	BufferedReader bufferedReader = new BufferedReader(new FileReader(srcFile));
	if (!outFile.exists()) {
	    outFile.createNewFile();
	}

	System.out.println(String.format("outFile : %s", outFile.toString()));

	BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outFile));

	try {
	    String line;
	    while ((line = bufferedReader.readLine()) != null) {
		currentLineNum++;
		bufferedWriter.write(matchAndReplace(currentLineNum, line));
		bufferedWriter.newLine();
	    }
	} finally {
	    bufferedReader.close();
	    bufferedWriter.flush();
	    bufferedWriter.close();
	}

	System.out.println(String.format("modified Line Count = %d", modifiedLineCount));
    }

    /**
     *
     * @param src
     * @param line -1 to replace in all lines that containe the pattern, or a
     * spécific line number to replace only matches at the specified line.
     * @param pattern
     * @param replacement
     * @param dest
     * @return TODO ... number of remplacement vs the outputFile ... ?
     */
    public static int replaceAllPatternByTextInFile(File src, long line, String pattern, String replacement, File dest) {
	try {
	    FileContentReplacer replacer = new FileContentReplacer(pattern, replacement);
	    File inFile = src;
	    if (dest == null) {
		dest = new File(src.getParentFile(), src.getName() + "_out_" + System.currentTimeMillis());
	    }
	    File outFile = dest;
	    replacer.onlyThisLine = line;
	    replacer.matchAndReplace(inFile, outFile);

	    // todo ssi ?
	    if (replacer.modifiedLineCount > 0 ) {

		// File remplacement rename the srcFile as .old and rename the destFile as the srcFile....
		String curentFileName = src.getName();
		File oldFile = new File(src.getParent(), curentFileName + "_" + System.currentTimeMillis() + ".old");
		boolean keep = src.renameTo(oldFile);
		if (keep) {
		    dest.renameTo(src);
		} else {
		    System.err.println("??? rename error ...");
		}
	    }else{
		outFile.delete();
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}

	return 0;
    }

    public static void mainA(String[] args) throws IOException {
	if (args.length >= 4) {
	    FileContentReplacer replacer = new FileContentReplacer(args[0], args[1]);
	    File inFile = new File(args[2]);
	    File outFile = new File(args[3]);
	    replacer.matchAndReplace(inFile, outFile);
	} else {
	    System.out.println("Please enter 4 args");
	}
    }

    public static void main(String[] args) {
	// Demo on translation . xml file renaming a key
	// first the new name sould be a not used key ... or this is posible trouble for maintnace.
	// then the modification sould be memorised to be used on a new .xml file based on a old english.xml ... 
	// all modification should be reversible or at list we make a imestamped save of the original file
	// then we sould simultad the modification and have a way to validate it ( have done what we want, and just what we want and do not produce an invalide output file...)
	// ...
	// no concurent modification ( the faile is not in edition and we only do one modification at the time )
	// 

	final File inputXmlFile = new File("src/main/resources/languages/english.xml");

	replaceAllPatternByTextInFile(inputXmlFile, -1, "PaperSettings.PaperHeight", "PaperSettings.PaperHeightAAAAA", null);

    }
}

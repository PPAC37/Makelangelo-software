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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A Try to find in the source code the Tratuction keys ...
 *
 *
 * @author q6
 */
public class FindAllTraductionGet {

    /**
     * Try to search a src java project for a specifik pattern ( like
     * Traduction.get(...) )
     *
     * @param args
     */
    public static void main(String[] args) {

	try {
	    String baseDirToSearch = "src" + File.separator + "main" + File.separator + "java";
	    System.out.printf("PDW=%s\n", new File(".").getAbsolutePath());
	    File srcDir = new File(".", baseDirToSearch);
	    try {
		System.out.printf("srcDir=%s\n", srcDir.getCanonicalPath());
	    } catch (IOException ex) {
		Logger.getLogger(FindAllTraductionGet.class.getName()).log(Level.SEVERE, null, ex);
	    }
	    // list all .java files in srcDir.

	    List<Path> paths = listFiles(srcDir.toPath(), ".java");
	    // search in the file ...
	    paths.forEach(x -> searchAFile(x, srcDir.toPath()));
	} catch (IOException ex) {
	    Logger.getLogger(FindAllTraductionGet.class.getName()).log(Level.SEVERE, null, ex);
	}

	System.out.printf("totalMatchCountInAllFiles=%d\n", totalMatchCountInAllFiles);
    }

    private static int totalMatchCountInAllFiles = 0;
    private static boolean debugPaser = false;

    /**
     * line by line scanner then token matcher ... TODO if i have a multiline ?
     *
     * @param x
     */
    public static int searchAFile(Path x, Path baseDir) {
	int totalMatchCount = 0;
	try {
	    if (debugPaser) {
		System.out.println(x);
	    }
	    // TODO parse the file
	    int posVarCombinationsFromController = 0;

	    // See pattern regexp (fr) https://cyberzoide.developpez.com/tutoriels/java/regex/
	    // normaly '(' and ')' are used to define group in patern matcher so to match a real '(' you have to déspécialise it by adding a trailling '\' ( and as we are in a string you have to add a '\' befor the '\' ... )
	    // '.' in regexp is for any caractére if you whant to match a '.' you have to despecialise it by adding a '\' ...
	    // [^\\).]
	    // \s Un caractère blanc : [ \t\n\x0B\f\r]
	    String patternString1 = "Translator\\s*\\.\\s*get\\s*\\(([^\\)]*)\\)";
	    Pattern patternS1 = Pattern.compile(patternString1);

	    // not line by line ... 
	    try ( Scanner sc = new Scanner(x)) {
		Pattern pat = Pattern.compile(patternString1);
		List<String> n = sc.findAll(pat)
			.map(MatchResult::group)
			.collect(Collectors.toList());
		System.out.printf("::%d\n", n.size());
	    }

	    // line by line ( this can miss some ... )
	    Scanner scanner = new Scanner(x);
	    int lineNum = 0;
	    while (scanner.hasNextLine()) {
		lineNum++;
		String nextToken = scanner.nextLine();

		Matcher matcherPatS1 = patternS1.matcher(nextToken);

		int matchCount = 0;
		while (matcherPatS1.find()) {
		    totalMatchCountInAllFiles++;
		    totalMatchCount++;
		    matchCount++;
		    if (debugPaser) {
			System.out.println("#found: " + matcherPatS1.group(0));
			System.out.flush();
			System.out.println("#found gp count : " + matcherPatS1.groupCount());
			System.out.flush();
		    }
		    System.out.printf("\tat %s(:%d)\n%s\n", baseDir.relativize(x), lineNum, matcherPatS1.group(1));
		    //
		}
	    }
	} catch (IOException ex) {
	    Logger.getLogger(FindAllTraductionGet.class.getName()).log(Level.SEVERE, null, ex);
	}
	return totalMatchCount;
    }

    /**
     * List all file in this path.Using <code>Files.walk(path)</code> (so this
     * take care of recursive path exploration ) And applying filter (
     * RegularFile and ReadableFile ) and filterring FileName ...
     *
     * @param path
     * @param fileNameEndsWithSuffix use ".java" to get only ...
     * @return
     * @throws IOException
     */
    public static List<Path> listFiles(Path path, String fileNameEndsWithSuffix) throws IOException {

	List<Path> result;
	try ( Stream<Path> walk = Files.walk(path)) {
	    result = walk
		    .filter(Files::isRegularFile)
		    .filter(Files::isReadable)
		    .map(Path::toFile)
		    .filter(f -> f.getName().endsWith(fileNameEndsWithSuffix))
		    .map(File::toPath)
		    .collect(Collectors.toList());
	}
	return result;

    }

}

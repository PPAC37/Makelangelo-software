package com.marginallyclever.util;


/*


"Translator\\s*\\.\\s*get\\s*\\(([^\\)]*)\\)"

// npt all arg if combined
Translator\s*\.\s*get\s*\(([^\)]*)\)
// over last ) 
Translator\s*\.\s*get\s*\((.*)\)

Translator\s*\.\s*get\s*\(((?:\((.*)\))*)\)


??https://stackoverflow.com/questions/18906514/regex-for-matching-functions-and-capturing-their-arguments/18908330
https://docs.microsoft.com/en-us/dotnet/standard/base-types/grouping-constructs-in-regular-expressions?redirectedfrom=MSDN#balancing_group_definition

...
... Translator.get("Z_COMMENTE_ENDLINE") ...
 */

import com.marginallyclever.makelangelo.Translator;

/**
 * Dummy class to use as test set for regexp pattern. (Sould not be commited)<br>
 * <br>
 * https://www.debuggex.com/
 * <br>
 * https://regex101.com/
 * ?? in java pattern ? for nested func? balancing open/close '(' ')' https://stackoverflow.com/questions/18906514/regex-for-matching-functions-and-capturing-their-arguments/18908330
 * <br>
 *
 * <code>//Translator.get("Z_COMMENTE_ENDLINE_IN_CODE_TAG_IN_COMMENT_BLOCK");</code>
 * <br>
 * <code>Translator.get("Z_COMMENTE_ENDLINE");</code>
 * <br>
 * <code>Traduction.get(...)</code> in all .java file.
 * <br>
 * //
 * file://src/main/resources/languages/english.xml
 * file://./src/main/java/com/marginallyclever/util/FindAllTraductionGet.java
 * @author PPAC37
 */
public class FindAllTraduction_TestSet {

    /*
    // DEV "Test set" commented :
    Translator.get("Z_COMMENTE_BLOCK");
    //Translator.get("Z_COMMENTE_ENDLINE_IN_BLOCK");
     */
    String s = Translator.get("OK_A");//Translator.get("Z_COMMENTE_ENDLINE_A");
    //Translator.get("Z_COMMENTE_ENDLINE_B");
     // Translator.get("Z_COMMENTE_ENDLINE_C"); //Translator.get("Z_COMMENTE_ENDLINE_D");
    public FindAllTraduction_TestSet() {
	//Translator.get("s");
	System.out.println(Translator.get("OK_B"));
	//System.out.println( Translator . get( "OK_C" ) ); // aleat space
	System.out.println( Translator . get( "OK_C" ) ); // aleat space ( will be change if code formated please keep as the on above commented.
	System.out.println(Translator.get("OK_D" + "OK_E"));
	System.out.println(Translator.get(FindAllTraduction_TestSet.class.getSimpleName() + "OK_F"));

	//
	//
	//
	System.out.println( /**/Translator./**/
			get("OK_ML_A"));
	System.out.println( /**/Translator.
			get("OK_ML_B"));
	//
	System.out.println( /**/Translator.
			get("OK_ML_C" + "_D")+Translator.get("OK_X"));
    }

}

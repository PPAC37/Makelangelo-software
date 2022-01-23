<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : language_v0_To_csv.xsl
    Created on : 14 janvier 2022, 16:31
    Author     : q6
    Description:
        A try to generate a .properties file from a .xml DTD language ( juste so i can use the lang .properties edit under Netbeans ...  )
        
        BUG : multiline CDATA converte  ( normaly a value sould be on a single ligne , but, int CDATA you may have multiple line ... ? \n replacement ??? )
        
        
            remove new line and sapce only for CDATA ????
                https://stackoverflow.com/questions/552762/removing-all-n-r-characters-from-a-node-xslt
            Unfortunately, the normalize-space() function (used in the answer of andynormancx) does more than deleting newlines.
   <xsl:variable name="temp" select="value" />
              <xsl:value-of select="normalize-space($temp)" />
            //ok ?           
            <xsl:variable name="temp" select="value" /> 
<xsl:value-of select="translate($temp, '&#xa;', '')" />
            
            https://stackoverflow.com/questions/1492736/xslt-1-0-replace-new-line-character-with
            
           ///
        
        A way to automaticaly rename output file ( MakelangeloLanguage_codeLang_CodeCountry.properties )
        // ? only in the invocation of the convertion ( cf have to do this in a specific .java class ? or in the arguments used to do the transformation ) 
        
        
        As xml engine ( can have diffrent implementation/comportement like xalan vs .NETxmlEmbed vs ... ) this may not work ...
        https://stackoverflow.com/questions/723226/producing-a-new-line-in-xslt
        (TODO : Im using NetBeans 12.6 xml commands via toolsbar icons (so i even dont actualy know what xml engine i use ... )
        
        
        
         https://www.w3schools.com/xml/xsl_elementref.asp
         https://www.w3schools.com/xml/xsl_functions.asp
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" >
    <xsl:output method="text" encoding="utf-8"  omit-xml-declaration="yes" 
                standalone="no" /><!--  method="text" ?? encoding='iso-8859-1'? indent="yes" encoding="Windows-1252" -->
    <xsl:param name="timestamp"/> 
    <xsl:param name="src"/> 
    
    <xsl:template match="/">        
       
        <xsl:text># éAuto-generated file the </xsl:text>
        <xsl:value-of select="$timestamp" />
        <xsl:text> from </xsl:text>
        <xsl:value-of select="$src" />
        <xsl:text>&#xa;</xsl:text><!--Add a \r\n (as "&#xd;" is normaly a carriage return) and ( "&#xa;" is normaly a line feed / new line  ) -->
        
        <xsl:text># Language name:</xsl:text>
        <xsl:value-of select="//language/meta/name"/>
        <xsl:text>&#xa;</xsl:text><!--Add a \r\n (as "&#xd;" is normaly a carriage return) and ( "&#xa;" is normaly a line feed / new line  ) -->
                
        <xsl:text># Author:</xsl:text>
        <xsl:value-of select="//language/meta/author"/>
        <xsl:text>&#xa;</xsl:text>
        
        <xsl:for-each select="//language/string">
            <xsl:text>&#xa;</xsl:text><!--Add a \r\n (as "&#xd;" is normaly a carriage return) and ( "&#xa;" is normaly a line feed / new line  ) -->
            
            <xsl:text># </xsl:text>
            <xsl:value-of select="hint" />
            <xsl:text>&#xa;</xsl:text><!--Add a \r\n (as "&#xd;" is normaly a carriage return) and ( "&#xa;" is normaly a line feed / new line  ) -->
            
            <xsl:variable name="key_for_id" select="key" />
            <xsl:value-of select="translate($key_for_id, ' *', '__')" />
            <!--<xsl:value-of select="key" />-->
            <xsl:text>=</xsl:text>
         
           
            <xsl:variable name="temp" select="value" />
            <xsl:value-of select="translate($temp, '&#xa;', '')" />
            <!-- 
           
           // ? best compromise remove line returns ( but keep original tabs and spaces )
            <xsl:variable name="temp" select="value" />
             <xsl:value-of select="translate($temp, '&#xa;', '')" />
              
               
                //.*=.* $
            // remove also leading and trailling spaces so not good to keep whanted end space ( but lovely collaps tab and spaces ) 
             <xsl:variable name="temp" select="value" />
            <xsl:value-of select="normalize-space($temp)" />
             
           // Keep line retrun so not good for .properties files
            <xsl:value-of select="value" disable-output-escaping="yes"/> 
            -->
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>

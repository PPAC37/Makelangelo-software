<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : language_v0_To_properties.xsl
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
       <!-- Dans un fichier properties toute ligne avant un ligne key=val sont des commentaires/description ... -->
        <xsl:text># Auto-generated file the </xsl:text>
        <xsl:value-of select="$timestamp" />
        <xsl:text> from </xsl:text>
        <xsl:value-of select="$src" />
        <xsl:text>&#xa;</xsl:text><!--Add a \r\n (as "&#xd;" is normaly a carriage return) and ( "&#xa;" is normaly a line feed / new line  ) -->
        
        <xsl:text># // Charset test set : é ù ö (TODO to remove juste to control no change / good charset(s) used for input and output in xml transform impementation or it can be your editor default charset to use (like with netbean, .properties file ar open in ??? force use project charset encoding) )</xsl:text>   
        <xsl:text>&#xa;</xsl:text>
        
        <xsl:text>language.meta.src</xsl:text>
        <xsl:text>=</xsl:text>
        <xsl:value-of select="$src" />
        <xsl:text>&#xa;</xsl:text>

        <xsl:text>language.meta.timestamp</xsl:text>
        <xsl:text>=</xsl:text>
        <xsl:value-of select="$timestamp" />
        <xsl:text>&#xa;</xsl:text>


        <xsl:text># Language name:</xsl:text>
        <xsl:value-of select="//language/meta/name"/>
        <xsl:text>&#xa;</xsl:text><!--Add a \r\n (as "&#xd;" is normaly a carriage return) and ( "&#xa;" is normaly a line feed / new line  ) -->
                
                
        <xsl:text>language.meta.name</xsl:text>
        <xsl:text>=</xsl:text>
        <xsl:value-of select="//language/meta/name"/>
        <xsl:text>&#xa;</xsl:text>


                
        <xsl:text># Author:</xsl:text>
        <xsl:value-of select="//language/meta/author"/>
        <xsl:text>&#xa;</xsl:text>
        
               
                
        <xsl:text>language.meta.author</xsl:text>
        <xsl:text>=</xsl:text>
        <xsl:value-of select="//language/meta/author"/>
        <xsl:text>&#xa;</xsl:text>

        
        <xsl:for-each select="//language/string">
            <xsl:text>&#xa;</xsl:text><!--Add a \r\n (as "&#xd;" is normaly a carriage return) and ( "&#xa;" is normaly a line feed / new line  ) -->
            
            
            
            <!--<xsl:value-of select="hint" />--><!-- TODO in properties file a comment can be multiline rempalce \n with \n# ? ) -->            
                <!-- Currently only the first hint TODO ? for each hint
                <xsl:call-template name="replaceNewLineWithNewLineSharpForPropertiesFileComment">
                    <xsl:with-param name="text" select="hint" />
                </xsl:call-template>            
                <xsl:text>&#xa;</xsl:text>
            -->
            <xsl:for-each select="hint">
            
            <xsl:text># </xsl:text>
            
                <xsl:call-template name="replaceNewLineWithNewLineSharpForPropertiesFileComment">
                    <xsl:with-param name="text" select="." />
                </xsl:call-template>
            
            <xsl:text>&#xa;</xsl:text><!--Add a \r\n (as "&#xd;" is normaly a carriage return) and ( "&#xa;" is normaly a line feed / new line  ) -->
            </xsl:for-each>
            
            
            <xsl:variable name="key_for_id" select="key" />
            <xsl:value-of select="translate($key_for_id, ' *=', '___')" /><!-- TODO keep track of the original key from the xml src as in properties file a key should not containe ' ' or '*' or '=' -->
            <!--<xsl:value-of select="key" />-->
            <xsl:text>=</xsl:text>
         
           
            <xsl:variable name="temp" select="value" />
               <xsl:call-template name="break">
            <xsl:with-param name="text" select="value" />
          </xsl:call-template>
            <!-- https://stackoverflow.com/questions/561235/xslt-replace-n-with-br-only-in-one-node
            // dans un fichier .properties les retour a la ligne dans une valeur sont transformé c-a-d '\n' -> "\"+"n"
            donc ? : <xsl:value-of select="replace($temp, '&#xa;', '\\n')" /> ???  function that has been introduced with XSLT/XPath 2.0.
           
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
    
    
    <!-- Modified to add a '\n' and not a "<br/>
    Source : https://stackoverflow.com/questions/561235/xslt-replace-n-with-br-only-in-one-node
    
    Like this (it will work on the current node):

        <xsl:template match="msg">
          <xsl:call-template name="break" />
        </xsl:template>

    or like this, explicitly passing a parameter:

        <xsl:template match="someElement">
          <xsl:call-template name="break">
            <xsl:with-param name="text" select="msg" />
          </xsl:call-template>
        </xsl:template>
        
    I think you are working with an XSLT 1.0 processor, whereas replace() is a function that has been introduced with XSLT/XPath 2.0.
    -->
<xsl:template name="break">
  <xsl:param name="text" select="string(.)"/>
  <xsl:choose>
    <xsl:when test="contains($text, '&#xa;')">
      <xsl:value-of select="substring-before($text, '&#xa;')"/>
      
      <!--<br/>-->
      <xsl:text>\n</xsl:text>
      
      <xsl:call-template name="break">
        <xsl:with-param 
          name="text" 
          select="substring-after($text, '&#xa;')"
        />
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$text"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- et en generique ?? pour ressemble a fn:replace mais sans reg exp ??? 3 argument  srcText, textToReplace, textReplacement-->   
<xsl:template name="replaceNewLineWithNewLineSharpForPropertiesFileComment">
  <xsl:param name="text" select="string(.)"/>
  <xsl:choose>
    <xsl:when test="contains($text, '&#xa;')">
      <xsl:value-of select="substring-before($text, '&#xa;')"/>
      
      <!--<br/>-->
      <xsl:text>&#xa;#</xsl:text>
      
      <xsl:call-template name="replaceNewLineWithNewLineSharpForPropertiesFileComment">
        <xsl:with-param 
          name="text" 
          select="substring-after($text, '&#xa;')"
        />
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$text"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>


</xsl:stylesheet>

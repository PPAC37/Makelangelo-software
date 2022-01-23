<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : language_v0_To_csv.xsl
    Created on : 14 janvier 2022, 16:31
    Author     : q6
    Description:
        A try to generate a .csv file from a .xml DTD language 
         https://www.w3schools.com/xml/xsl_elementref.asp
         https://www.w3schools.com/xml/xsl_functions.asp
         
         
         TODO !
         
         les meta en cle 
         
         fichier, hash, timestamp,
         
         string key
         
         line pos ?
         
         string value*
         
         hints*
         
         
         normaly in csv (comma separated value) format we need to quot a string only if it containe ',' '"' or '\n' ?
         no space debor or after the comma for value separation or need to cote the valor.
         n.b. if using '"' as string delimiter ,  for a literal '"' you have to double it. (as in SQL? for a literal ')
         
         quot '"' = &#x22; = &quot;
         new line '\n' = &#xA;
         comma ',' = 
         
         https://stackoverflow.com/questions/365312/xml-to-csv-using-xslt
         https://stackoverflow.com/a/61820507
         
         
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" >
    <xsl:output method="text" encoding="utf-8"  omit-xml-declaration="yes" 
                standalone="no" />
    <xsl:param name="timestamp"/> 
    <xsl:param name="src"/> 
    
    <xsl:template match="/">        
       
        <xsl:variable name="memo" >
            
            <xsl:call-template name="CsvEscape">
                <xsl:with-param name="value" select="normalize-space($src)"/>
            </xsl:call-template>
            <xsl:text>,</xsl:text>
            
            <xsl:call-template name="CsvEscape">
                <xsl:with-param name="value" select="normalize-space(//language/meta/name)"/>
            </xsl:call-template>
            <xsl:text>,</xsl:text>
            
            <xsl:call-template name="CsvEscape">
                <xsl:with-param name="value" select="normalize-space(//language/meta/author)"/>
            </xsl:call-template>
            <xsl:text>,</xsl:text>
                
        </xsl:variable>
        
        
        <xsl:for-each select="//language/string">
            
            <xsl:value-of select="$memo" />
            
            <xsl:call-template name="CsvEscape">
                <xsl:with-param name="value" select="key"/>
            </xsl:call-template>
            <xsl:text>,</xsl:text>
            
            <xsl:variable name="key_for_id" select="key" />
            <xsl:call-template name="CsvEscape">
                <xsl:with-param name="value" select="translate($key_for_id, ' *', '__')"/>
            </xsl:call-template>
            <xsl:text>,</xsl:text>
            
            <xsl:variable name="temp" select="value" />
            <xsl:call-template name="CsvEscape">
                <xsl:with-param name="value" select="translate($temp, '&#xa;', '')"/>
            </xsl:call-template>
            <xsl:text>,</xsl:text>
            
            
            <xsl:call-template name="CsvEscape">
                <xsl:with-param name="value" select="normalize-space(hint)"/>
            </xsl:call-template>
            
            
            
                   
            <!-- Add a newline at the end of the record -->     
            <xsl:text>&#xa;</xsl:text>
        </xsl:for-each>
    </xsl:template>
    
    
    <!-- 
        Source : https://stackoverflow.com/questions/365312/xml-to-csv-using-xslt
        https://stackoverflow.com/a/61820507
        
        The template EscapeQuotes replaces all double quotes with 2 double quotes, recursively from the start of the string.   
    -->
        
    <xsl:template name="EscapeQuotes">
        <xsl:param name="value"/>
        <xsl:choose>
            <xsl:when test="contains($value,'&quot;')">
                <xsl:value-of select="substring-before($value,'&quot;')"/>
                <xsl:text>&quot;&quot;</xsl:text>
                <xsl:call-template name="EscapeQuotes">
                    <xsl:with-param name="value" select="substring-after($value,'&quot;')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$value"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
        
    <!--
        Source :  https://stackoverflow.com/questions/365312/xml-to-csv-using-xslt
      https://stackoverflow.com/a/61820507
    
This CsvEscape function is XSLT 1.0 and escapes column values ,, ", and newlines like RFC 4180 https://www.rfc-editor.org/rfc/rfc4180 or Excel. 
It makes use of the fact that you can recursively call XSLT templates:

      The template EscapeQuotes replaces all double quotes with 2 double quotes, recursively from the start of the string.
    
        The template CsvEscape checks if the text contains a comma or double quote, and if so surrounds the whole string with a pair of double quotes and calls EscapeQuotes for the string.
      
    Example usage: xsltproc xmltocsv.xslt file.xml > file.csv
    -->
        
    <xsl:template name="CsvEscape">
        <xsl:param name="value"/>
        <xsl:choose>
            <xsl:when test="contains($value,',')">
                <xsl:text>&quot;</xsl:text>
                <xsl:call-template name="EscapeQuotes">
                    <xsl:with-param name="value" select="$value"/>
                </xsl:call-template>
                <xsl:text>&quot;</xsl:text>
            </xsl:when>
            <xsl:when test="contains($value,'&#xA;')">
                <xsl:text>&quot;</xsl:text>
                <xsl:call-template name="EscapeQuotes">
                    <xsl:with-param name="value" select="$value"/>
                </xsl:call-template>
                <xsl:text>&quot;</xsl:text>
            </xsl:when>
            <xsl:when test="contains($value,'&quot;')">
                <xsl:text>&quot;</xsl:text>
                <xsl:call-template name="EscapeQuotes">
                    <xsl:with-param name="value" select="$value"/>
                </xsl:call-template>
                <xsl:text>&quot;</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$value"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
        
    <!-- 
    
      
    <xsl:template match="/">
      <xsl:text>project,name,language,owner,state,startDate</xsl:text>
      <xsl:text>&#xA;</xsl:text>
      <xsl:for-each select="projects/project">
        <xsl:call-template name="CsvEscape"><xsl:with-param name="value" select="normalize-space(name)"/></xsl:call-template>
        <xsl:text>,</xsl:text>
        <xsl:call-template name="CsvEscape"><xsl:with-param name="value" select="normalize-space(language)"/></xsl:call-template>
        <xsl:text>,</xsl:text>
        <xsl:call-template name="CsvEscape"><xsl:with-param name="value" select="normalize-space(owner)"/></xsl:call-template>
        <xsl:text>,</xsl:text>
        <xsl:call-template name="CsvEscape"><xsl:with-param name="value" select="normalize-space(state)"/></xsl:call-template>
        <xsl:text>,</xsl:text>
        <xsl:call-template name="CsvEscape"><xsl:with-param name="value" select="normalize-space(startDate)"/></xsl:call-template>
        <xsl:text>&#xA;</xsl:text>
      </xsl:for-each>
    </xsl:template>
    
    -->
        
        
        
</xsl:stylesheet>

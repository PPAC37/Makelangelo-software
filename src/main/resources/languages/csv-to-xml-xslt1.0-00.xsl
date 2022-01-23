<?xml version="1.0" encoding="UTF-8"?>

<!--


    Document   : csv-to-xml-xslt1.0-00.xsl
    Created on : 23 janvier 2022, 18:29
    Author     : https://p2p.wrox.com/xslt/40898-transform-csv-file-xml.html
    Description:
       ramarc ramarc is offline
Registered User
 	
Join Date: Apr 2006
Posts: 4
Thanks: 0
Thanked 0 Times in 0 Posts
Default
Here's the basis of an XSLT 1.0 solution. Simply enclose your CSV data within an XML tag using CDATA.
XML CSV data:
<data><![CDATA[ ...your CSV data here... ]]></data>

Stylesheet:
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    exclude-result-prefixes="xsl">
    <xsl:output method="xml" encoding="utf-8" />

    <xsl:variable name="newline" select="'#xa;'" />
    <xsl:variable name="comma" select="','" />

    <xsl:template match="/">
        <data>
            <xsl:apply-templates/>
        </data>
    </xsl:template>

    <xsl:template match="text()">
        <originalCSV>
            <xsl:value-of select="." />
        </originalCSV>
        <xsl:call-template name="write-line" />
    </xsl:template>

    <xsl:template name="write-line">
        <xsl:param name="text" select="." />
        <xsl:variable name="this-row" select="substring-before( concat( $text, $newline ), $newline )" />
        <xsl:variable name="remaining-rows" select="substring-after( $text, $newline )" />
        <xsl:if test="string-length($this-row) &gt; 1">
            <row>
                <xsl:call-template name="write-item">
                    <xsl:with-param name="line" select="$this-row" />
                </xsl:call-template>
            </row>
        </xsl:if>
        <xsl:if test="string-length( $remaining-rows ) &gt; 0">
            <xsl:call-template name="write-line">
                <xsl:with-param name="text" select="$remaining-rows" />
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template name="write-item">
        <xsl:param name="line"/>
        <xsl:variable name="this-item" select="substring-before( concat( $line, $comma ), $comma)" />
        <xsl:variable name="remaining-items" select="substring-after( $line, $comma )" />
        <item>
            <xsl:value-of select="$this-item" />
        </item>
        <xsl:if test="string-length( $remaining-items ) &gt; 0">
            <xsl:call-template name="write-item">
                <xsl:with-param name="line" select="$remaining-items" />
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>

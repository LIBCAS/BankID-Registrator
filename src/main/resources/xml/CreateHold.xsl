<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

    <xsl:output method="xml" encoding="utf-8" indent="yes" standalone="yes"/>

    <xsl:param name="pickup-location" select="''"/>
    <xsl:param name="last-interest-date" select="''"/>
    <xsl:param name="note-1" select="''"/>

    <xsl:template name="hold-request-parameters">
        <pickup-location>
            <xsl:value-of select="$pickup-location"/>
        </pickup-location>
        <last-interest-date>
            <xsl:value-of select="$last-interest-date"/>
        </last-interest-date>
        <note-1>
            <xsl:value-of select="$note-1"/>
        </note-1>
    </xsl:template>

    <xsl:template match="/">
        <xsl:element name="hold-request-parameters">
            <xsl:call-template name="hold-request-parameters"/>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>
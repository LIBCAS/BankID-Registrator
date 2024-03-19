<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

    <xsl:output method="xml" encoding="utf-8" indent="yes" standalone="yes"/>

    <!-- >>> Z303 >>> -->
    <xsl:param name="is-z303-match-id-type" select="'true'"/>
    <xsl:param name="z303-match-id-type" select="'00'"/>
    <xsl:param name="is-z303-match-id" select="'true'"/>
    <xsl:param name="z303-match-id" select="''"/>
    <xsl:param name="is-z303-record-action" select="'true'"/>
    <xsl:param name="z303-record-action" select="'A'"/>
    <xsl:param name="is-z303-id" select="'true'"/>
    <xsl:param name="z303-id" select="''"/>

    <xsl:template name="z303">
        <xsl:if test="starts-with($is-z303-match-id-type, 'true')">
            <xsl:element name="match-id-type">
                <xsl:copy-of select="$z303-match-id-type"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-match-id, 'true')">
            <xsl:element name="match-id">
                <xsl:copy-of select="$z303-match-id"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-record-action, 'true')">
            <xsl:element name="record-action">
                <xsl:copy-of select="$z303-record-action"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-id, 'true')">
            <xsl:element name="z303-id">
                <xsl:copy-of select="$z303-id"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    <!-- <<< Z303 <<< -->

    <!-- >>> Z305 >>> -->
    <xsl:param name="is-z305-record-action" select="'true'"/>
    <xsl:param name="z305-record-action" select="'A'"/>
    <xsl:param name="is-z305-id" select="'true'"/>
    <xsl:param name="z305-id" select="''"/>
    <xsl:param name="is-z305-sub-library" select="'true'"/>
    <xsl:param name="z305-sub-library" select="''"/>
    <xsl:param name="is-z305-open-date" select="'true'"/>
    <xsl:param name="z305-open-date" select="''"/>
    <xsl:param name="is-z305-update-date" select="'true'"/>
    <xsl:param name="z305-update-date" select="''"/>
    <xsl:param name="is-z305-bor-status" select="'true'"/>
    <xsl:param name="z305-bor-status" select="''"/>

    <xsl:template name="z305">
        <xsl:if test="starts-with($is-z305-record-action, 'true')">
            <xsl:element name="record-action">
                <xsl:copy-of select="$z305-record-action"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-id, 'true')">
            <xsl:element name="z305-id">
                <xsl:copy-of select="$z305-id"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-sub-library, 'true')">
            <xsl:element name="z305-sub-library">
                <xsl:copy-of select="$z305-sub-library"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-open-date, 'true')">
            <xsl:element name="z305-open-date">
                <xsl:copy-of select="$z305-open-date"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-update-date, 'true')">
            <xsl:element name="z305-update-date">
                <xsl:copy-of select="$z305-update-date"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-bor-status, 'true')">
            <xsl:element name="z305-bor-status">
                <xsl:copy-of select="$z305-bor-status"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    <!-- <<< Z305 <<< -->

    <!-- >>> p-file-20 >>> -->
    <!-- Z303  -->
    <xsl:param name="is-z303" select="'true'"/>
    <!-- Z305 -->
    <xsl:param name="is-z305" select="'true'"/>

    <xsl:template name="p-file-20">
        <xsl:element name="patron-record">
            <xsl:if test="starts-with($is-z303, 'true')">
                <xsl:element name="z303">
                    <xsl:call-template name="z303"/>
                </xsl:element>
            </xsl:if>
            <xsl:if test="starts-with($is-z305, 'true')">
                <xsl:element name="z305">
                    <xsl:call-template name="z305"/>
                </xsl:element>
            </xsl:if>
        </xsl:element>
    </xsl:template>
    <!-- <<< p-file-20 <<< -->

    <xsl:template match="/">
        <xsl:element name="p-file-20">
            <xsl:call-template name="p-file-20"/>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>
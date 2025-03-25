<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

    <xsl:output method="xml" encoding="utf-8" indent="yes" standalone="yes"/>

    <!--
        Z303
    -->
    <xsl:param name="is-z303-match-id-type" select="'true'"/>
    <xsl:param name="z303-match-id-type" select="'00'"/>
    <xsl:param name="is-z303-match-id" select="'true'"/>
    <xsl:param name="z303-match-id" select="''"/>
    <xsl:param name="is-z303-record-action" select="'true'"/>
    <xsl:param name="z303-record-action" select="'I'"/>
    <xsl:param name="is-z303-id" select="'true'"/>
    <xsl:param name="z303-id" select="''"/>
    <xsl:param name="is-z303-name" select="'true'"/>
    <xsl:param name="z303-name" select="''"/>
    <xsl:param name="is-z303-birth-date" select="'true'"/>
    <xsl:param name="z303-birth-date" select="''"/>
    <xsl:param name="is-z303-con-lng" select="'true'"/>
    <xsl:param name="z303-con-lng" select="''"/>
    <xsl:param name="is-z303-alpha" select="'true'"/>
    <xsl:param name="z303-alpha" select="'L'"/>
    <xsl:param name="is-z303-export-consent" select="'true'"/>
    <xsl:param name="z303-export-consent" select="'N'"/>
    <xsl:param name="is-z303-send-all-letters" select="'true'"/>
    <xsl:param name="z303-send-all-letters" select="'Y'"/>
    <xsl:param name="is-z303-plain-html" select="'true'"/>
    <xsl:param name="z303-plain-html" select="'P'"/>
    <xsl:param name="is-z303-want-sms" select="'true'"/>
    <xsl:param name="z303-want-sms" select="'N'"/>
    <xsl:param name="is-z303-last-name" select="'false'"/>
    <xsl:param name="z303-last-name" select="''"/>
    <xsl:param name="is-z303-first-name" select="'false'"/>
    <xsl:param name="z303-first-name" select="''"/>
    <xsl:param name="is-z303-delinq-1" select="'false'"/>
    <xsl:param name="z303-delinq-1" select="'00'"/>
    <xsl:param name="is-z303-delinq-n-1" select="'false'"/>
    <xsl:param name="z303-delinq-n-1" select="'Registrace přes BankID'"/>

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
        <xsl:if test="starts-with($is-z303-name, 'true')">
            <xsl:element name="z303-name">
                <xsl:copy-of select="$z303-name"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-birth-date, 'true')">
            <xsl:element name="z303-birth-date">
                <xsl:copy-of select="$z303-birth-date"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-con-lng, 'true')">
            <xsl:element name="z303-con-lng">
                <xsl:copy-of select="$z303-con-lng"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-alpha, 'true')">
            <xsl:element name="z303-alpha">
                <xsl:copy-of select="$z303-alpha"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-export-consent, 'true')">
            <xsl:element name="z303-export-consent">
                <xsl:copy-of select="$z303-export-consent"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-send-all-letters, 'true')">
            <xsl:element name="z303-send-all-letters">
                <xsl:copy-of select="$z303-send-all-letters"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-plain-html, 'true')">
            <xsl:element name="z303-plain-html">
                <xsl:copy-of select="$z303-plain-html"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-want-sms, 'true')">
            <xsl:element name="z303-want-sms">
                <xsl:copy-of select="$z303-want-sms"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-last-name, 'true')">
            <xsl:element name="z303-last-name">
                <xsl:copy-of select="$z303-last-name"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-first-name, 'true')">
            <xsl:element name="z303-first-name">
                <xsl:copy-of select="$z303-first-name"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-delinq-1, 'true')">
            <xsl:element name="z303-delinq-1">
                <xsl:copy-of select="$z303-delinq-1"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-delinq-n-1, 'true')">
            <xsl:element name="z303-delinq-n-1">
                <xsl:copy-of select="$z303-delinq-n-1"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <!--
        Z304 / sequence 01
    -->
    <xsl:param name="is-z304-seq01-record-action" select="'true'"/>
    <xsl:param name="z304-seq01-record-action" select="'I'"/>
    <xsl:param name="is-z304-seq01-id" select="'true'"/>
    <xsl:param name="z304-seq01-id" select="''"/>
    <xsl:param name="is-z304-seq01-sequence" select="'true'"/>
    <xsl:param name="z304-seq01-sequence" select="'01'"/>
    <xsl:param name="is-z304-seq01-address-type" select="'true'"/>
    <xsl:param name="z304-seq01-address-type" select="'01'"/>
    <xsl:param name="is-z304-seq01-address-0" select="'true'"/>
    <xsl:param name="z304-seq01-address-0" select="''"/>
    <xsl:param name="is-z304-seq01-address-1" select="'true'"/>
    <xsl:param name="z304-seq01-address-1" select="''"/>
    <xsl:param name="is-z304-seq01-address-2" select="'true'"/>
    <xsl:param name="z304-seq01-address-2" select="''"/>
    <xsl:param name="is-z304-seq01-zip" select="'true'"/>
    <xsl:param name="z304-seq01-zip" select="''"/>
    <xsl:param name="is-z304-seq01-email-address" select="'true'"/>
    <xsl:param name="z304-seq01-email-address" select="''"/>
    <xsl:param name="is-z304-seq01-telephone-2" select="'true'"/>
    <xsl:param name="z304-seq01-telephone-2" select="'ID CZ'"/>
    <xsl:param name="is-z304-seq01-telephone-3" select="'true'"/>
    <xsl:param name="z304-seq01-telephone-3" select="'Občanský průkaz'"/>
    <xsl:param name="is-z304-seq01-telephone-4" select="'true'"/>
    <xsl:param name="z304-seq01-telephone-4" select="''"/>
    <xsl:param name="is-z304-seq01-date-from" select="'true'"/>
    <xsl:param name="z304-seq01-date-from" select="''"/>
    <xsl:param name="is-z304-seq01-date-to" select="'true'"/>
    <xsl:param name="z304-seq01-date-to" select="''"/>

    <xsl:template name="z304-seq01">
        <xsl:if test="starts-with($is-z304-seq01-record-action, 'true')">
            <xsl:element name="record-action">
                <xsl:copy-of select="$z304-seq01-record-action"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq01-id, 'true')">
            <xsl:element name="z304-id">
                <xsl:copy-of select="$z304-seq01-id"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq01-sequence, 'true')">
            <xsl:element name="z304-sequence">
                <xsl:copy-of select="$z304-seq01-sequence"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq01-address-type, 'true')">
            <xsl:element name="z304-address-type">
                <xsl:copy-of select="$z304-seq01-address-type"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq01-address-0, 'true')">
            <xsl:element name="z304-address-0">
                <xsl:copy-of select="$z304-seq01-address-0"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq01-address-1, 'true')">
            <xsl:element name="z304-address-1">
                <xsl:copy-of select="$z304-seq01-address-1"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq01-address-2, 'true')">
            <xsl:element name="z304-address-2">
                <xsl:copy-of select="$z304-seq01-address-2"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq01-zip, 'true')">
            <xsl:element name="z304-zip">
                <xsl:copy-of select="$z304-seq01-zip"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq01-email-address, 'true')">
            <xsl:element name="z304-email-address">
                <xsl:copy-of select="$z304-seq01-email-address"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq01-telephone-2, 'true')">
            <xsl:element name="z304-telephone-2">
                <xsl:copy-of select="$z304-seq01-telephone-2"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq01-telephone-3, 'true')">
            <xsl:element name="z304-telephone-3">
                <xsl:copy-of select="$z304-seq01-telephone-3"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq01-telephone-4, 'true')">
            <xsl:element name="z304-telephone-4">
                <xsl:copy-of select="$z304-seq01-telephone-4"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq01-date-from, 'true')">
            <xsl:element name="z304-date-from">
                <xsl:copy-of select="$z304-seq01-date-from"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq01-date-to, 'true')">
            <xsl:element name="z304-date-to">
                <xsl:copy-of select="$z304-seq01-date-to"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <!--
        Z305 / KNA50 sublibrary
    -->
    <xsl:param name="is-z305-kna50-record-action" select="'true'"/>
    <xsl:param name="z305-kna50-record-action" select="'I'"/>
    <xsl:param name="is-z305-kna50-id" select="'true'"/>
    <xsl:param name="z305-kna50-id" select="''"/>
    <xsl:param name="is-z305-kna50-sub-library" select="'true'"/>
    <xsl:param name="z305-kna50-sub-library" select="'KNA50'"/>
    <xsl:param name="is-z305-kna50-registration-date" select="'true'"/>
    <xsl:param name="z305-kna50-registration-date" select="''"/>
    <xsl:param name="is-z305-kna50-bor-status" select="'true'"/>
    <xsl:param name="z305-kna50-bor-status" select="'16'"/>

    <xsl:template name="z305-kna50">
        <xsl:if test="starts-with($is-z305-kna50-record-action, 'true')">
            <xsl:element name="record-action">
                <xsl:copy-of select="$z305-kna50-record-action"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-kna50-id, 'true')">
            <xsl:element name="z305-id">
                <xsl:copy-of select="$z305-kna50-id"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-kna50-sub-library, 'true')">
            <xsl:element name="z305-sub-library">
                <xsl:copy-of select="$z305-kna50-sub-library"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-kna50-registration-date, 'true')">
            <xsl:element name="z305-registration-date">
                <xsl:copy-of select="$z305-kna50-registration-date"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-kna50-bor-status, 'true')">
            <xsl:element name="z305-bor-status">
                <xsl:copy-of select="$z305-kna50-bor-status"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <!--
        Z305 / KNAV sublibrary
    -->
    <xsl:param name="is-z305-knav-record-action" select="'true'"/>
    <xsl:param name="z305-knav-record-action" select="'I'"/>
    <xsl:param name="is-z305-knav-id" select="'true'"/>
    <xsl:param name="z305-knav-id" select="''"/>
    <xsl:param name="is-z305-knav-sub-library" select="'true'"/>
    <xsl:param name="z305-knav-sub-library" select="'KNAV'"/>
    <xsl:param name="is-z305-knav-registration-date" select="'true'"/>
    <xsl:param name="z305-knav-registration-date" select="''"/>
    <xsl:param name="is-z305-knav-bor-status" select="'true'"/>
    <xsl:param name="z305-knav-bor-status" select="'16'"/>

    <xsl:template name="z305-knav">
        <xsl:if test="starts-with($is-z305-knav-record-action, 'true')">
            <xsl:element name="record-action">
                <xsl:copy-of select="$z305-knav-record-action"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-knav-id, 'true')">
            <xsl:element name="z305-id">
                <xsl:copy-of select="$z305-knav-id"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-knav-sub-library, 'true')">
            <xsl:element name="z305-sub-library">
                <xsl:copy-of select="$z305-knav-sub-library"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-knav-registration-date, 'true')">
            <xsl:element name="z305-registration-date">
                <xsl:copy-of select="$z305-knav-registration-date"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-knav-bor-status, 'true')">
            <xsl:element name="z305-bor-status">
                <xsl:copy-of select="$z305-knav-bor-status"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <!--
        Z305 / KNAVD sublibrary
    -->
    <xsl:param name="is-z305-knavd-record-action" select="'true'"/>
    <xsl:param name="z305-knavd-record-action" select="'I'"/>
    <xsl:param name="is-z305-knavd-id" select="'true'"/>
    <xsl:param name="z305-knavd-id" select="''"/>
    <xsl:param name="is-z305-knavd-sub-library" select="'true'"/>
    <xsl:param name="z305-knavd-sub-library" select="'KNAVD'"/>
    <xsl:param name="is-z305-knavd-registration-date" select="'true'"/>
    <xsl:param name="z305-knavd-registration-date" select="''"/>
    <xsl:param name="is-z305-knavd-bor-status" select="'true'"/>
    <xsl:param name="z305-knavd-bor-status" select="'16'"/>

    <xsl:template name="z305-knavd">
        <xsl:if test="starts-with($is-z305-knavd-record-action, 'true')">
            <xsl:element name="record-action">
                <xsl:copy-of select="$z305-knavd-record-action"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-knavd-id, 'true')">
            <xsl:element name="z305-id">
                <xsl:copy-of select="$z305-knavd-id"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-knavd-sub-library, 'true')">
            <xsl:element name="z305-sub-library">
                <xsl:copy-of select="$z305-knavd-sub-library"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-knavd-registration-date, 'true')">
            <xsl:element name="z305-registration-date">
                <xsl:copy-of select="$z305-knavd-registration-date"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-knavd-bor-status, 'true')">
            <xsl:element name="z305-bor-status">
                <xsl:copy-of select="$z305-knavd-bor-status"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <!--
        Z305 / KNAVP sublibrary
    -->
    <xsl:param name="is-z305-knavp-record-action" select="'true'"/>
    <xsl:param name="z305-knavp-record-action" select="'I'"/>
    <xsl:param name="is-z305-knavp-id" select="'true'"/>
    <xsl:param name="z305-knavp-id" select="''"/>
    <xsl:param name="is-z305-knavp-sub-library" select="'true'"/>
    <xsl:param name="z305-knavp-sub-library" select="'KNAVP'"/>
    <xsl:param name="is-z305-knavp-registration-date" select="'true'"/>
    <xsl:param name="z305-knavp-registration-date" select="''"/>
    <xsl:param name="is-z305-knavp-bor-status" select="'true'"/>
    <xsl:param name="z305-knavp-bor-status" select="'16'"/>

    <xsl:template name="z305-knavp">
        <xsl:if test="starts-with($is-z305-knavp-record-action, 'true')">
            <xsl:element name="record-action">
                <xsl:copy-of select="$z305-knavp-record-action"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-knavp-id, 'true')">
            <xsl:element name="z305-id">
                <xsl:copy-of select="$z305-knavp-id"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-knavp-sub-library, 'true')">
            <xsl:element name="z305-sub-library">
                <xsl:copy-of select="$z305-knavp-sub-library"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-knavp-registration-date, 'true')">
            <xsl:element name="z305-registration-date">
                <xsl:copy-of select="$z305-knavp-registration-date"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z305-knavp-bor-status, 'true')">
            <xsl:element name="z305-bor-status">
                <xsl:copy-of select="$z305-knavp-bor-status"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <!--
        Z308 / id
    -->
    <xsl:param name="is-z308-key-type-00-record-action" select="'true'"/>
    <xsl:param name="z308-key-type-00-record-action" select="'I'"/>
    <xsl:param name="is-z308-key-type-00-id" select="'false'"/>
    <xsl:param name="z308-key-type-00-id" select="''"/>
    <xsl:param name="is-z308-key-type-00-key-type" select="'true'"/>
    <xsl:param name="z308-key-type-00-key-type" select="'00'"/>
    <xsl:param name="is-z308-key-type-00-key-data" select="'true'"/>
    <xsl:param name="z308-key-type-00-key-data" select="''"/>
    <xsl:param name="is-z308-key-type-00-verification-type" select="'true'"/>
    <xsl:param name="z308-key-type-00-verification-type" select="'00'"/>
    <xsl:param name="is-z308-key-type-00-verification" select="'false'"/>
    <xsl:param name="z308-key-type-00-verification" select="''"/>
    <xsl:param name="is-z308-key-type-00-status" select="'true'"/>
    <xsl:param name="z308-key-type-00-status" select="'AC'"/>
    <xsl:param name="is-z308-key-type-00-encryption" select="'true'"/>
    <xsl:param name="z308-key-type-00-encryption" select="'N'"/>

    <xsl:template name="z308-key-type-00">
        <xsl:if test="starts-with($is-z308-key-type-00-record-action, 'true')">
            <xsl:element name="record-action">
                <xsl:copy-of select="$z308-key-type-00-record-action"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-00-id, 'true')">
            <xsl:element name="z308-id">
                <xsl:copy-of select="$z308-key-type-00-id"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-00-key-type, 'true')">
            <xsl:element name="z308-key-type">
                <xsl:copy-of select="$z308-key-type-00-key-type"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-00-key-data, 'true')">
            <xsl:element name="z308-key-data">
                <xsl:copy-of select="$z308-key-type-00-key-data"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-00-verification-type, 'true')">
            <xsl:element name="z308-verification-type">
                <xsl:copy-of select="$z308-key-type-00-verification-type"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-00-verification, 'true')">
            <xsl:element name="z308-verification">
                <xsl:copy-of select="$z308-key-type-00-verification"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-00-status, 'true')">
            <xsl:element name="z308-status">
                <xsl:copy-of select="$z308-key-type-00-status"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-00-encryption, 'true')">
            <xsl:element name="z308-encryption">
                <xsl:copy-of select="$z308-key-type-00-encryption"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <!--
        Z308 / barcode
    -->
    <xsl:param name="is-z308-key-type-01-record-action" select="'true'"/>
    <xsl:param name="z308-key-type-01-record-action" select="'I'"/>
    <xsl:param name="is-z308-key-type-01-id" select="'false'"/>
    <xsl:param name="z308-key-type-01-id" select="''"/>
    <xsl:param name="is-z308-key-type-01-key-type" select="'true'"/>
    <xsl:param name="z308-key-type-01-key-type" select="'01'"/>
    <xsl:param name="is-z308-key-type-01-key-data" select="'true'"/>
    <xsl:param name="z308-key-type-01-key-data" select="''"/>
    <xsl:param name="is-z308-key-type-01-verification-type" select="'true'"/>
    <xsl:param name="z308-key-type-01-verification-type" select="'00'"/>
    <xsl:param name="is-z308-key-type-01-verification" select="'false'"/>
    <xsl:param name="z308-key-type-01-verification" select="''"/>
    <xsl:param name="is-z308-key-type-01-status" select="'true'"/>
    <xsl:param name="z308-key-type-01-status" select="'AC'"/>
    <xsl:param name="is-z308-key-type-01-encryption" select="'true'"/>
    <xsl:param name="z308-key-type-01-encryption" select="'N'"/>

    <xsl:template name="z308-key-type-01">
        <xsl:if test="starts-with($is-z308-key-type-01-record-action, 'true')">
            <xsl:element name="record-action">
                <xsl:copy-of select="$z308-key-type-01-record-action"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-01-id, 'true')">
            <xsl:element name="z308-id">
                <xsl:copy-of select="$z308-key-type-01-id"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-01-key-type, 'true')">
            <xsl:element name="z308-key-type">
                <xsl:copy-of select="$z308-key-type-01-key-type"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-01-key-data, 'true')">
            <xsl:element name="z308-key-data">
                <xsl:copy-of select="$z308-key-type-01-key-data"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-01-verification-type, 'true')">
            <xsl:element name="z308-verification-type">
                <xsl:copy-of select="$z308-key-type-01-verification-type"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-01-verification, 'true')">
            <xsl:element name="z308-verification">
                <xsl:copy-of select="$z308-key-type-01-verification"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-01-status, 'true')">
            <xsl:element name="z308-status">
                <xsl:copy-of select="$z308-key-type-01-status"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-01-encryption, 'true')">
            <xsl:element name="z308-encryption">
                <xsl:copy-of select="$z308-key-type-01-encryption"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <!--
        Z308 / bankid
    -->
    <xsl:param name="is-z308-key-type-07-record-action" select="'true'"/>
    <xsl:param name="z308-key-type-07-record-action" select="'I'"/>
    <xsl:param name="is-z308-key-type-07-id" select="'false'"/>
    <xsl:param name="z308-key-type-07-id" select="''"/>
    <xsl:param name="is-z308-key-type-07-key-type" select="'true'"/>
    <xsl:param name="z308-key-type-07-key-type" select="'07'"/>
    <xsl:param name="is-z308-key-type-07-key-data" select="'true'"/>
    <xsl:param name="z308-key-type-07-key-data" select="''"/>
    <xsl:param name="is-z308-key-type-07-verification-type" select="'true'"/>
    <xsl:param name="z308-key-type-07-verification-type" select="'00'"/>
    <xsl:param name="is-z308-key-type-07-verification" select="'false'"/>
    <xsl:param name="z308-key-type-07-verification" select="''"/>
    <xsl:param name="is-z308-key-type-07-status" select="'true'"/>
    <xsl:param name="z308-key-type-07-status" select="'AC'"/>
    <xsl:param name="is-z308-key-type-07-encryption" select="'true'"/>
    <xsl:param name="z308-key-type-07-encryption" select="'N'"/>

    <xsl:template name="z308-key-type-07">
        <xsl:if test="starts-with($is-z308-key-type-07-record-action, 'true')">
            <xsl:element name="record-action">
                <xsl:copy-of select="$z308-key-type-07-record-action"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-07-id, 'true')">
            <xsl:element name="z308-id">
                <xsl:copy-of select="$z308-key-type-07-id"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-07-key-type, 'true')">
            <xsl:element name="z308-key-type">
                <xsl:copy-of select="$z308-key-type-07-key-type"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-07-key-data, 'true')">
            <xsl:element name="z308-key-data">
                <xsl:copy-of select="$z308-key-type-07-key-data"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-07-verification-type, 'true')">
            <xsl:element name="z308-verification-type">
                <xsl:copy-of select="$z308-key-type-07-verification-type"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-07-verification, 'true')">
            <xsl:element name="z308-verification">
                <xsl:copy-of select="$z308-key-type-07-verification"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-07-status, 'true')">
            <xsl:element name="z308-status">
                <xsl:copy-of select="$z308-key-type-07-status"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-07-encryption, 'true')">
            <xsl:element name="z308-encryption">
                <xsl:copy-of select="$z308-key-type-07-encryption"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <!--
        Z308 / internal verification id
    -->
    <xsl:param name="is-z308-key-type-08-record-action" select="'true'"/>
    <xsl:param name="z308-key-type-08-record-action" select="'I'"/>
    <xsl:param name="is-z308-key-type-08-id" select="'false'"/>
    <xsl:param name="z308-key-type-08-id" select="''"/>
    <xsl:param name="is-z308-key-type-08-key-type" select="'true'"/>
    <xsl:param name="z308-key-type-08-key-type" select="'08'"/>
    <xsl:param name="is-z308-key-type-08-key-data" select="'true'"/>
    <xsl:param name="z308-key-type-08-key-data" select="''"/>
    <xsl:param name="is-z308-key-type-08-verification-type" select="'true'"/>
    <xsl:param name="z308-key-type-08-verification-type" select="'00'"/>
    <xsl:param name="is-z308-key-type-08-verification" select="'false'"/>
    <xsl:param name="z308-key-type-08-verification" select="''"/>
    <xsl:param name="is-z308-key-type-08-status" select="'true'"/>
    <xsl:param name="z308-key-type-08-status" select="'AC'"/>
    <xsl:param name="is-z308-key-type-08-encryption" select="'true'"/>
    <xsl:param name="z308-key-type-08-encryption" select="'N'"/>

    <xsl:template name="z308-key-type-08">
        <xsl:if test="starts-with($is-z308-key-type-08-record-action, 'true')">
            <xsl:element name="record-action">
                <xsl:copy-of select="$z308-key-type-08-record-action"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-08-id, 'true')">
            <xsl:element name="z308-id">
                <xsl:copy-of select="$z308-key-type-08-id"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-08-key-type, 'true')">
            <xsl:element name="z308-key-type">
                <xsl:copy-of select="$z308-key-type-08-key-type"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-08-key-data, 'true')">
            <xsl:element name="z308-key-data">
                <xsl:copy-of select="$z308-key-type-08-key-data"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-08-verification-type, 'true')">
            <xsl:element name="z308-verification-type">
                <xsl:copy-of select="$z308-key-type-08-verification-type"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-08-verification, 'true')">
            <xsl:element name="z308-verification">
                <xsl:copy-of select="$z308-key-type-08-verification"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-08-status, 'true')">
            <xsl:element name="z308-status">
                <xsl:copy-of select="$z308-key-type-08-status"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-08-encryption, 'true')">
            <xsl:element name="z308-encryption">
                <xsl:copy-of select="$z308-key-type-08-encryption"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <!--
        p-file-20
    -->
    <!-- Z303  -->
    <xsl:param name="is-z303" select="'true'"/>
    <!-- Z304 / seq01 -->
    <xsl:param name="is-z304-seq01" select="'true'"/>
    <!-- KNA50 -->
    <xsl:param name="is-z305-kna50" select="'true'"/>
    <!-- KNAV -->
    <xsl:param name="is-z305-knav" select="'true'"/>
    <!-- KNAVD -->
    <xsl:param name="is-z305-knavd" select="'true'"/>
    <!-- KNAVP -->
    <xsl:param name="is-z305-knavp" select="'true'"/>
    <!-- id -->
    <xsl:param name="is-z308-key-type-00" select="'true'"/>
    <!-- barcode -->
    <xsl:param name="is-z308-key-type-01" select="'true'"/>
    <!-- bankid / sub -->
    <xsl:param name="is-z308-key-type-07" select="'true'"/>
    <!-- internal verification id / last-name + first-name + birth-date -->
    <xsl:param name="is-z308-key-type-08" select="'true'"/>

    <xsl:template name="p-file-20">
        <xsl:element name="patron-record">
            <xsl:if test="starts-with($is-z303, 'true')">
                <xsl:element name="z303">
                    <xsl:call-template name="z303"/>
                </xsl:element>
            </xsl:if>
            <xsl:if test="starts-with($is-z304-seq01, 'true')">
                <xsl:element name="z304">
                    <xsl:call-template name="z304-seq01"/>
                </xsl:element>
            </xsl:if>
            <xsl:if test="starts-with($is-z305-kna50, 'true')">
                <xsl:element name="z305">
                    <xsl:call-template name="z305-kna50"/>
                </xsl:element>
            </xsl:if>
            <xsl:if test="starts-with($is-z305-knav, 'true')">
                <xsl:element name="z305">
                    <xsl:call-template name="z305-knav"/>
                </xsl:element>
            </xsl:if>
            <xsl:if test="starts-with($is-z305-knavd, 'true')">
                <xsl:element name="z305">
                    <xsl:call-template name="z305-knavd"/>
                </xsl:element>
            </xsl:if>
            <xsl:if test="starts-with($is-z305-knavp, 'true')">
                <xsl:element name="z305">
                    <xsl:call-template name="z305-knavp"/>
                </xsl:element>
            </xsl:if>
            <xsl:if test="starts-with($is-z308-key-type-00, 'true')">
                <xsl:element name="z308">
                    <xsl:call-template name="z308-key-type-00"/>
                </xsl:element>
            </xsl:if>
            <xsl:if test="starts-with($is-z308-key-type-01, 'true')">
                <xsl:element name="z308">
                    <xsl:call-template name="z308-key-type-01"/>
                </xsl:element>
            </xsl:if>
            <xsl:if test="starts-with($is-z308-key-type-07, 'true')">
                <xsl:element name="z308">
                    <xsl:call-template name="z308-key-type-07"/>
                </xsl:element>
            </xsl:if>
            <xsl:if test="starts-with($is-z308-key-type-08, 'true')">
                <xsl:element name="z308">
                    <xsl:call-template name="z308-key-type-08"/>
                </xsl:element>
            </xsl:if>
        </xsl:element>
    </xsl:template>

    <xsl:template match="/">
        <xsl:element name="p-file-20">
            <xsl:call-template name="p-file-20"/>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>

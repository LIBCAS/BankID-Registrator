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
    <xsl:param name="z303-record-action" select="'I'"/>
    <xsl:param name="is-z303-id" select="'true'"/>
    <xsl:param name="z303-id" select="''"/>
    <xsl:param name="is-z303-proxy-for-id" select="'true'"/>
    <xsl:param name="z303-proxy-for-id" select="''"/>
    <xsl:param name="is-z303-primary-id" select="'true'"/>
    <xsl:param name="z303-primary-id" select="''"/>
    <xsl:param name="is-z303-name-key" select="'true'"/>
    <xsl:param name="z303-name-key" select="''"/>
    <xsl:param name="is-z303-user-type" select="'true'"/>
    <xsl:param name="z303-user-type" select="''"/>
    <xsl:param name="is-z303-user-library" select="'true'"/>
    <xsl:param name="z303-user-library" select="'KNA50'"/>
    <xsl:param name="is-z303-home-library" select="'true'"/>
    <xsl:param name="z303-home-library" select="'KNAV'"/>
    <xsl:param name="is-z303-open-date" select="'true'"/>
    <xsl:param name="z303-open-date" select="''"/>
    <xsl:param name="is-z303-update-date" select="'true'"/>
    <xsl:param name="z303-update-date" select="''"/>
    <xsl:param name="is-z303-con-lng" select="'true'"/>
    <xsl:param name="z303-con-lng" select="''"/>
    <xsl:param name="is-z303-alpha" select="'true'"/>
    <xsl:param name="z303-alpha" select="'L'"/>
    <xsl:param name="is-z303-name" select="'true'"/>
    <xsl:param name="z303-name" select="''"/>
    <xsl:param name="is-z303-birth-date" select="'true'"/>
    <xsl:param name="z303-birth-date" select="''"/>
    <xsl:param name="is-z303-export-consent" select="'true'"/>
    <xsl:param name="z303-export-consent" select="''"/>
    <xsl:param name="is-z303-send-all-letters" select="'true'"/>
    <xsl:param name="z303-send-all-letters" select="'Y'"/>
    <xsl:param name="is-z303-plain-html" select="'true'"/>
    <xsl:param name="z303-plain-html" select="'P'"/>
    <xsl:param name="is-z303-want-sms" select="'true'"/>
    <xsl:param name="z303-want-sms" select="'N'"/>
    <xsl:param name="is-z303-gender" select="'true'"/>
    <xsl:param name="z303-gender" select="''"/>
    <xsl:param name="is-z303-birthplace" select="'true'"/>
    <xsl:param name="z303-birthplace" select="''"/>
    <xsl:param name="is-z303-last-name" select="'true'"/>
    <xsl:param name="z303-last-name" select="''"/>
    <xsl:param name="is-z303-first-name" select="'true'"/>
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
        <xsl:if test="starts-with($is-z303-proxy-for-id, 'true')">
            <xsl:element name="z303-proxy-for-id">
                <xsl:copy-of select="$z303-proxy-for-id"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-primary-id, 'true')">
            <xsl:element name="z303-primary-id">
                <xsl:copy-of select="$z303-primary-id"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-name-key, 'true')">
            <xsl:element name="z303-name-key">
                <xsl:copy-of select="$z303-name-key"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-user-type, 'true')">
            <xsl:element name="z303-user-type">
                <xsl:copy-of select="$z303-user-type"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-user-library, 'true')">
            <xsl:element name="z303-user-library">
                <xsl:copy-of select="$z303-user-library"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-home-library, 'true')">
            <xsl:element name="z303-home-library">
                <xsl:copy-of select="$z303-home-library"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-open-date, 'true')">
            <xsl:element name="z303-open-date">
                <xsl:copy-of select="$z303-open-date"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-update-date, 'true')">
            <xsl:element name="z303-update-date">
                <xsl:copy-of select="$z303-update-date"/>
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
        <xsl:if test="starts-with($is-z303-gender, 'true')">
            <xsl:element name="z303-gender">
                <xsl:copy-of select="$z303-gender"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z303-birthplace, 'true')">
            <xsl:element name="z303-birthplace">
                <xsl:copy-of select="$z303-birthplace"/>
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
    <!-- <<< Z303 <<< -->

    <!-- >>> Z304 - sequence 01 >>> -->
    <xsl:param name="is-z304-seq01-record-action" select="'true'"/>
    <xsl:param name="z304-seq01-record-action" select="'I'"/>
    <xsl:param name="is-z304-seq01-id" select="'true'"/>
    <xsl:param name="z304-seq01-id" select="''"/>
    <xsl:param name="is-z304-seq01-sequence" select="'true'"/>
    <xsl:param name="z304-seq01-sequence" select="'01'"/>
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
    <xsl:param name="is-z304-seq01-date-from" select="'true'"/>
    <xsl:param name="z304-seq01-date-from" select="''"/>
    <xsl:param name="is-z304-seq01-address-type" select="'true'"/>
    <xsl:param name="z304-seq01-address-type" select="'01'"/>
    <xsl:param name="is-z304-seq01-sms-number" select="'true'"/>
    <xsl:param name="z304-seq01-sms-number" select="''"/>
    <xsl:param name="is-z304-seq01-cat-name" select="'true'"/>
    <xsl:param name="z304-seq01-cat-name" select="'VYPTJ'"/>
    <xsl:param name="is-z304-seq01-telephone-2" select="'false'"/>
    <xsl:param name="z304-seq01-telephone-2" select="'ID CZ'"/>
    <xsl:param name="is-z304-seq01-telephone-3" select="'false'"/>
    <xsl:param name="z304-seq01-telephone-3" select="'Občanský průkaz'"/>
    <xsl:param name="is-z304-seq01-telephone-4" select="'false'"/>
    <xsl:param name="z304-seq01-telephone-4" select="''"/>

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
        <xsl:if test="starts-with($is-z304-seq01-date-from, 'true')">
            <xsl:element name="z304-date-from">
                <xsl:copy-of select="$z304-seq01-date-from"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq01-address-type, 'true')">
            <xsl:element name="z304-address-type">
                <xsl:copy-of select="$z304-seq01-address-type"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq01-sms-number, 'true')">
            <xsl:element name="z304-sms-number">
                <xsl:copy-of select="$z304-seq01-sms-number"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq01-cat-name, 'true')">
            <xsl:element name="z304-cat-name">
                <xsl:copy-of select="$z304-seq01-cat-name"/>
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
    </xsl:template>
    <!-- <<< Z304 - sequence 01 <<< -->

    <!-- >>> Z304 - sequence 02 >>> -->
    <xsl:param name="is-z304-seq02-record-action" select="'true'"/>
    <xsl:param name="z304-seq02-record-action" select="'I'"/>
    <xsl:param name="is-z304-seq02-id" select="'true'"/>
    <xsl:param name="z304-seq02-id" select="''"/>
    <xsl:param name="is-z304-seq02-sequence" select="'true'"/>
    <xsl:param name="z304-seq02-sequence" select="'02'"/>
    <xsl:param name="is-z304-seq02-address-0" select="'true'"/>
    <xsl:param name="z304-seq02-address-0" select="''"/>
    <xsl:param name="is-z304-seq02-address-1" select="'true'"/>
    <xsl:param name="z304-seq02-address-1" select="''"/>
    <xsl:param name="is-z304-seq02-address-2" select="'true'"/>
    <xsl:param name="z304-seq02-address-2" select="''"/>
    <xsl:param name="is-z304-seq02-zip" select="'true'"/>
    <xsl:param name="z304-seq02-zip" select="''"/>
    <xsl:param name="is-z304-seq02-email-address" select="'true'"/>
    <xsl:param name="z304-seq02-email-address" select="''"/>
    <xsl:param name="is-z304-seq02-date-from" select="'true'"/>
    <xsl:param name="z304-seq02-date-from" select="''"/>
    <xsl:param name="is-z304-seq02-address-type" select="'true'"/>
    <xsl:param name="z304-seq02-address-type" select="'02'"/>
    <xsl:param name="is-z304-seq02-sms-number" select="'true'"/>
    <xsl:param name="z304-seq02-sms-number" select="''"/>

    <xsl:template name="z304-seq02">
        <xsl:if test="starts-with($is-z304-seq02-record-action, 'true')">
            <xsl:element name="record-action">
                <xsl:copy-of select="$z304-seq02-record-action"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq02-id, 'true')">
            <xsl:element name="z304-id">
                <xsl:copy-of select="$z304-seq02-id"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq02-sequence, 'true')">
            <xsl:element name="z304-sequence">
                <xsl:copy-of select="$z304-seq02-sequence"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq02-address-0, 'true')">
            <xsl:element name="z304-address-0">
                <xsl:copy-of select="$z304-seq02-address-0"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq02-address-1, 'true')">
            <xsl:element name="z304-address-1">
                <xsl:copy-of select="$z304-seq02-address-1"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq02-address-2, 'true')">
            <xsl:element name="z304-address-2">
                <xsl:copy-of select="$z304-seq02-address-2"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq02-zip, 'true')">
            <xsl:element name="z304-zip">
                <xsl:copy-of select="$z304-seq02-zip"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq02-email-address, 'true')">
            <xsl:element name="z304-email-address">
                <xsl:copy-of select="$z304-seq02-email-address"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq02-date-from, 'true')">
            <xsl:element name="z304-date-from">
                <xsl:copy-of select="$z304-seq02-date-from"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq02-address-type, 'true')">
            <xsl:element name="z304-address-type">
                <xsl:copy-of select="$z304-seq02-address-type"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z304-seq02-sms-number, 'true')">
            <xsl:element name="z304-sms-number">
                <xsl:copy-of select="$z304-seq02-sms-number"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    <!-- <<< Z304 - sequence 02 <<< -->

    <!-- >>> Z305 >>> -->
    <xsl:param name="is-z305-record-action" select="'true'"/>
    <xsl:param name="z305-record-action" select="'I'"/>
    <xsl:param name="is-z305-id" select="'true'"/>
    <xsl:param name="z305-id" select="''"/>
    <xsl:param name="is-z305-sub-library" select="'true'"/>
    <xsl:param name="z305-sub-library" select="'KNAV'"/>
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

    <!-- >>> Z308 - ID >>> -->
    <xsl:param name="is-z308-key-type-00-record-action" select="'true'"/>
    <xsl:param name="z308-key-type-00-record-action" select="'I'"/>
    <xsl:param name="is-z308-key-type-00-key-type" select="'true'"/>
    <xsl:param name="z308-key-type-00-key-type" select="'00'"/>
    <xsl:param name="is-z308-key-type-00-key-data" select="'true'"/>
    <xsl:param name="z308-key-type-00-key-data" select="''"/>
    <xsl:param name="is-z308-key-type-00-verification" select="'true'"/>
    <xsl:param name="z308-key-type-00-verification" select="''"/>
    <xsl:param name="is-z308-key-type-00-verification-type" select="'true'"/>
    <xsl:param name="z308-key-type-00-verification-type" select="'00'"/>
    <xsl:param name="is-z308-key-type-00-id" select="'true'"/>
    <xsl:param name="z308-key-type-00-id" select="''"/>
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
        <xsl:if test="starts-with($is-z308-key-type-00-verification, 'true')">
            <xsl:element name="z308-verification">
                <xsl:copy-of select="$z308-key-type-00-verification"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-00-verification-type, 'true')">
            <xsl:element name="z308-verification-type">
                <xsl:copy-of select="$z308-key-type-00-verification-type"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-00-id, 'true')">
            <xsl:element name="z308-id">
                <xsl:copy-of select="$z308-key-type-00-id"/>
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
    <!-- <<< Z308 - ID <<< -->

    <!-- <<< Z308 - barcode <<< -->
    <xsl:param name="is-z308-key-type-01-record-action" select="'true'"/>
    <xsl:param name="z308-key-type-01-record-action" select="'I'"/>
    <xsl:param name="is-z308-key-type-01-key-type" select="'true'"/>
    <xsl:param name="z308-key-type-01-key-type" select="'01'"/>
    <xsl:param name="is-z308-key-type-01-key-data" select="'true'"/>
    <xsl:param name="z308-key-type-01-key-data" select="''"/>
    <xsl:param name="is-z308-key-type-01-verification" select="'true'"/>
    <xsl:param name="z308-key-type-01-verification" select="''"/>
    <xsl:param name="is-z308-key-type-01-verification-type" select="'true'"/>
    <xsl:param name="z308-key-type-01-verification-type" select="'00'"/>
    <xsl:param name="is-z308-key-type-01-id" select="'true'"/>
    <xsl:param name="z308-key-type-01-id" select="''"/>
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
        <xsl:if test="starts-with($is-z308-key-type-01-verification, 'true')">
            <xsl:element name="z308-verification">
                <xsl:copy-of select="$z308-key-type-01-verification"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-01-verification-type, 'true')">
            <xsl:element name="z308-verification-type">
                <xsl:copy-of select="$z308-key-type-01-verification-type"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-01-id, 'true')">
            <xsl:element name="z308-id">
                <xsl:copy-of select="$z308-key-type-01-id"/>
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
    <!-- <<< Z308 - barcode <<< -->

    <!-- <<< Z308 - RFID <<< -->
    <xsl:param name="is-z308-key-type-03-record-action" select="'true'"/>
    <xsl:param name="z308-key-type-03-record-action" select="'I'"/>
    <xsl:param name="is-z308-key-type-03-key-type" select="'true'"/>
    <xsl:param name="z308-key-type-03-key-type" select="'03'"/>
    <xsl:param name="is-z308-key-type-03-key-data" select="'true'"/>
    <xsl:param name="z308-key-type-03-key-data" select="''"/>
    <xsl:param name="is-z308-key-type-03-user-library" select="'true'"/>
    <xsl:param name="z308-key-type-03-user-library" select="'KNA50'"/>
    <xsl:param name="is-z308-key-type-03-verification-type" select="'true'"/>
    <xsl:param name="z308-key-type-03-verification-type" select="'00'"/>
    <xsl:param name="is-z308-key-type-03-id" select="'true'"/>
    <xsl:param name="z308-key-type-03-id" select="''"/>
    <xsl:param name="is-z308-key-type-03-status" select="'true'"/>
    <xsl:param name="z308-key-type-03-status" select="'AC'"/>
    <xsl:param name="is-z308-key-type-03-encryption" select="'true'"/>
    <xsl:param name="z308-key-type-03-encryption" select="'N'"/>

    <xsl:template name="z308-key-type-03">
        <xsl:if test="starts-with($is-z308-key-type-03-record-action, 'true')">
            <xsl:element name="record-action">
                <xsl:copy-of select="$z308-key-type-03-record-action"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-03-key-type, 'true')">
            <xsl:element name="z308-key-type">
                <xsl:copy-of select="$z308-key-type-03-key-type"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-03-key-data, 'true')">
            <xsl:element name="z308-key-data">
                <xsl:copy-of select="$z308-key-type-03-key-data"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-03-user-library, 'true')">
            <xsl:element name="z308-user-library">
                <xsl:copy-of select="$z308-key-type-03-user-library"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-03-verification-type, 'true')">
            <xsl:element name="z308-verification-type">
                <xsl:copy-of select="$z308-key-type-03-verification-type"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-03-id, 'true')">
            <xsl:element name="z308-id">
                <xsl:copy-of select="$z308-key-type-03-id"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-03-status, 'true')">
            <xsl:element name="z308-status">
                <xsl:copy-of select="$z308-key-type-03-status"/>
            </xsl:element>
        </xsl:if>
        <xsl:if test="starts-with($is-z308-key-type-03-encryption, 'true')">
            <xsl:element name="z308-encryption">
                <xsl:copy-of select="$z308-key-type-03-encryption"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    <!-- <<< Z308 - RFID <<< -->

    <!-- >>> p-file-20 >>> -->
    <!-- Z303  -->
    <xsl:param name="is-z303" select="'true'"/>
    <!-- Z304 - seq01 -->
    <xsl:param name="is-z304-seq01" select="'true'"/>
    <!-- Z304 - seq02 -->
    <xsl:param name="is-z304-seq02" select="'true'"/>
    <!-- Z305 - kna50 -->
    <xsl:param name="is-z305" select="'true'"/>
    <!-- Z308 - id -->
    <xsl:param name="is-z308-key-type-00" select="'true'"/>
    <!-- Z308 - barcode -->
    <xsl:param name="is-z308-key-type-01" select="'true'"/>
    <!-- Z308 - RFID -->
    <xsl:param name="is-z308-key-type-03" select="'false'"/>

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
            <xsl:if test="starts-with($is-z304-seq02, 'true')">
                <xsl:element name="z304">
                    <xsl:call-template name="z304-seq02"/>
                </xsl:element>
            </xsl:if>
            <xsl:if test="starts-with($is-z305, 'true')">
                <xsl:element name="z305">
                    <xsl:call-template name="z305"/>
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
            <xsl:if test="starts-with($is-z308-key-type-03, 'true')">
                <xsl:element name="z308">
                    <xsl:call-template name="z308-key-type-03"/>
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
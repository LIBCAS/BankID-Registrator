<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

    <xsl:output method="xml" encoding="utf-8" indent="yes" standalone="yes"/>

    <!-- >>> Z30 >>> -->
    <xsl:param name="z30-doc-number" select="''"/>
    <xsl:param name="z30-item-sequence" select="'1.0'"/>
    <xsl:param name="z30-barcode" select="''"/>
    <xsl:param name="z30-sub-library" select="'KNAV'"/>
    <xsl:param name="z30-material" select="'BOOK'"/>
    <xsl:param name="z30-item-status" select="''"/>
    <xsl:param name="z30-open-date" select="''"/>
    <xsl:param name="z30-update-date" select="''"/>
    <xsl:param name="z30-cataloger" select="'OIT'"/>
    <xsl:param name="z30-date-last-return" select="'0'"/>
    <xsl:param name="z30-hour-last-return" select="'0'"/>
    <xsl:param name="z30-ip-last-return" select="''"/>
    <xsl:param name="z30-no-loans" select="'000'"/>
    <xsl:param name="z30-alpha" select="'L'"/>
    <xsl:param name="z30-collection" select="''"/>
    <xsl:param name="z30-call-no-type" select="''"/>
    <xsl:param name="z30-call-no" select="'studovna'"/>
    <xsl:param name="z30-call-no-key" select="'01'"/>
    <xsl:param name="z30-call-no-2-type" select="''"/>
    <xsl:param name="z30-call-no-2" select="''"/>
    <xsl:param name="z30-call-no-2-key" select="''"/>
    <xsl:param name="z30-description" select="'Registrace'"/>
    <xsl:param name="z30-note-opac" select="''"/>
    <xsl:param name="z30-note-circulation" select="''"/>
    <xsl:param name="z30-note-internal" select="''"/>
    <xsl:param name="z30-order-number" select="''"/>
    <xsl:param name="z30-inventory-number" select="''"/>
    <xsl:param name="z30-inventory-number-date" select="''"/>
    <xsl:param name="z30-last-shelf-report-date" select="'00000000'"/>
    <xsl:param name="z30-price" select="''"/>
    <xsl:param name="z30-shelf-report-number" select="''"/>
    <xsl:param name="z30-on-shelf-date" select="'00000000'"/>
    <xsl:param name="z30-on-shelf-seq" select="'000000'"/>
    <xsl:param name="z30-doc-number-2" select="'000000000'"/>
    <xsl:param name="z30-schedule-sequence-2" select="'00000'"/>
    <xsl:param name="z30-copy-sequence-2" select="'00000'"/>
    <xsl:param name="z30-vendor-code" select="''"/>
    <xsl:param name="z30-invoice-number" select="''"/>
    <xsl:param name="z30-line-number" select="'00000'"/>
    <xsl:param name="z30-pages" select="''"/>
    <xsl:param name="z30-issue-date" select="''"/>
    <xsl:param name="z30-expected-arrival-date" select="''"/>
    <xsl:param name="z30-arrival-date" select="''"/>
    <xsl:param name="z30-item-statistic" select="''"/>
    <xsl:param name="z30-item-process-status" select="''"/>
    <xsl:param name="z30-copy-id" select="''"/>
    <xsl:param name="z30-hol-doc-number" select="'000000000'"/>
    <xsl:param name="z30-temp-location" select="'No'"/>
    <xsl:param name="z30-enumeration-a" select="''"/>
    <xsl:param name="z30-enumeration-b" select="''"/>
    <xsl:param name="z30-enumeration-c" select="''"/>
    <xsl:param name="z30-enumeration-d" select="''"/>
    <xsl:param name="z30-enumeration-e" select="''"/>
    <xsl:param name="z30-enumeration-f" select="''"/>
    <xsl:param name="z30-enumeration-g" select="''"/>
    <xsl:param name="z30-enumeration-h" select="''"/>
    <xsl:param name="z30-chronological-i" select="''"/>
    <xsl:param name="z30-chronological-j" select="''"/>
    <xsl:param name="z30-chronological-k" select="''"/>
    <xsl:param name="z30-chronological-l" select="''"/>
    <xsl:param name="z30-chronological-m" select="''"/>
    <xsl:param name="z30-supp-index-o" select="''"/>
    <xsl:param name="z30-85x-type" select="''"/>
    <xsl:param name="z30-depository-id" select="''"/>
    <xsl:param name="z30-inking-number" select="'000000000'"/>
    <xsl:param name="z30-gap-indicator" select="''"/>
    <xsl:param name="z30-maintenance-count" select="'000'"/>
    <xsl:param name="z30-process-status-date" select="''"/>

    <xsl:template name="z30">
        <z30-doc-number>
            <xsl:value-of select="$z30-doc-number"/>
        </z30-doc-number>
        <z30-item-sequence>
            <xsl:value-of select="$z30-item-sequence"/>
        </z30-item-sequence>
        <z30-barcode>
            <xsl:value-of select="$z30-barcode"/>
        </z30-barcode>
        <z30-sub-library>
            <xsl:value-of select="$z30-sub-library"/>
        </z30-sub-library>
        <z30-material>
            <xsl:value-of select="$z30-material"/>
        </z30-material>
        <z30-item-status>
            <xsl:value-of select="$z30-item-status"/>
        </z30-item-status>
        <z30-open-date>
            <xsl:value-of select="$z30-open-date"/>
        </z30-open-date>
        <z30-update-date>
            <xsl:value-of select="$z30-update-date"/>
        </z30-update-date>
        <z30-cataloger>
            <xsl:value-of select="$z30-cataloger"/>
        </z30-cataloger>
        <z30-date-last-return>
            <xsl:value-of select="$z30-date-last-return"/>
        </z30-date-last-return>
        <z30-hour-last-return>
            <xsl:value-of select="$z30-hour-last-return"/>
        </z30-hour-last-return>
        <z30-ip-last-return>
            <xsl:value-of select="$z30-ip-last-return"/>
        </z30-ip-last-return>
        <z30-no-loans>
            <xsl:value-of select="$z30-no-loans"/>
        </z30-no-loans>
        <z30-alpha>
            <xsl:value-of select="$z30-alpha"/>
        </z30-alpha>
        <z30-collection>
            <xsl:value-of select="$z30-collection"/>
        </z30-collection>
        <z30-call-no-type>
            <xsl:value-of select="$z30-call-no-type"/>
        </z30-call-no-type>
        <z30-call-no>
            <xsl:value-of select="$z30-call-no"/>
        </z30-call-no>
        <z30-call-no-key>
            <xsl:value-of select="$z30-call-no-key"/>
        </z30-call-no-key>
        <z30-call-no-2-type>
            <xsl:value-of select="$z30-call-no-2-type"/>
        </z30-call-no-2-type>
        <z30-call-no-2>
            <xsl:value-of select="$z30-call-no-2"/>
        </z30-call-no-2>
        <z30-call-no-2-key>
            <xsl:value-of select="$z30-call-no-2-key"/>
        </z30-call-no-2-key>
        <z30-description>
            <xsl:value-of select="$z30-description"/>
        </z30-description>
        <z30-note-opac>
            <xsl:value-of select="$z30-note-opac"/>
        </z30-note-opac>
        <z30-note-circulation>
            <xsl:value-of select="$z30-note-circulation"/>
        </z30-note-circulation>
        <z30-note-internal>
            <xsl:value-of select="$z30-note-internal"/>
        </z30-note-internal>
        <z30-order-number>
            <xsl:value-of select="$z30-order-number"/>
        </z30-order-number>
        <z30-inventory-number>
            <xsl:value-of select="$z30-inventory-number"/>
        </z30-inventory-number>
        <z30-inventory-number-date>
            <xsl:value-of select="$z30-inventory-number-date"/>
        </z30-inventory-number-date>
        <z30-last-shelf-report-date>
            <xsl:value-of select="$z30-last-shelf-report-date"/>
        </z30-last-shelf-report-date>
        <z30-price>
            <xsl:value-of select="$z30-price"/>
        </z30-price>
        <z30-shelf-report-number>
            <xsl:value-of select="$z30-shelf-report-number"/>
        </z30-shelf-report-number>
        <z30-on-shelf-date>
            <xsl:value-of select="$z30-on-shelf-date"/>
        </z30-on-shelf-date>
        <z30-on-shelf-seq>
            <xsl:value-of select="$z30-on-shelf-seq"/>
        </z30-on-shelf-seq>
        <z30-doc-number-2>
            <xsl:value-of select="$z30-doc-number-2"/>
        </z30-doc-number-2>
        <z30-schedule-sequence-2>
            <xsl:value-of select="$z30-schedule-sequence-2"/>
        </z30-schedule-sequence-2>
        <z30-copy-sequence-2>
            <xsl:value-of select="$z30-copy-sequence-2"/>
        </z30-copy-sequence-2>
        <z30-vendor-code>
            <xsl:value-of select="$z30-vendor-code"/>
        </z30-vendor-code>
        <z30-invoice-number>
            <xsl:value-of select="$z30-invoice-number"/>
        </z30-invoice-number>
        <z30-line-number>
            <xsl:value-of select="$z30-line-number"/>
        </z30-line-number>
        <z30-pages>
            <xsl:value-of select="$z30-pages"/>
        </z30-pages>
        <z30-issue-date>
            <xsl:value-of select="$z30-issue-date"/>
        </z30-issue-date>
        <z30-expected-arrival-date>
            <xsl:value-of select="$z30-expected-arrival-date"/>
        </z30-expected-arrival-date>
        <z30-arrival-date>
            <xsl:value-of select="$z30-arrival-date"/>
        </z30-arrival-date>
        <z30-item-statistic>
            <xsl:value-of select="$z30-item-statistic"/>
        </z30-item-statistic>
        <z30-item-process-status>
            <xsl:value-of select="$z30-item-process-status"/>
        </z30-item-process-status>
        <z30-copy-id>
            <xsl:value-of select="$z30-copy-id"/>
        </z30-copy-id>
        <z30-hol-doc-number>
            <xsl:value-of select="$z30-hol-doc-number"/>
        </z30-hol-doc-number>
        <z30-temp-location>
            <xsl:value-of select="$z30-temp-location"/>
        </z30-temp-location>
        <z30-enumeration-a>
            <xsl:value-of select="$z30-enumeration-a"/>
        </z30-enumeration-a>
        <z30-enumeration-b>
            <xsl:value-of select="$z30-enumeration-b"/>
        </z30-enumeration-b>
        <z30-enumeration-c>
            <xsl:value-of select="$z30-enumeration-c"/>
        </z30-enumeration-c>
        <z30-enumeration-d>
            <xsl:value-of select="$z30-enumeration-d"/>
        </z30-enumeration-d>
        <z30-enumeration-e>
            <xsl:value-of select="$z30-enumeration-e"/>
        </z30-enumeration-e>
        <z30-enumeration-f>
            <xsl:value-of select="$z30-enumeration-f"/>
        </z30-enumeration-f>
        <z30-enumeration-g>
            <xsl:value-of select="$z30-enumeration-g"/>
        </z30-enumeration-g>
        <z30-enumeration-h>
            <xsl:value-of select="$z30-enumeration-h"/>
        </z30-enumeration-h>
        <z30-chronological-i>
            <xsl:value-of select="$z30-chronological-i"/>
        </z30-chronological-i>
        <z30-chronological-j>
            <xsl:value-of select="$z30-chronological-j"/>
        </z30-chronological-j>
        <z30-chronological-k>
            <xsl:value-of select="$z30-chronological-k"/>
        </z30-chronological-k>
        <z30-chronological-l>
            <xsl:value-of select="$z30-chronological-l"/>
        </z30-chronological-l>
        <z30-chronological-m>
            <xsl:value-of select="$z30-chronological-m"/>
        </z30-chronological-m>
        <z30-supp-index-o>
            <xsl:value-of select="$z30-supp-index-o"/>
        </z30-supp-index-o>
        <z30-85x-type>
            <xsl:value-of select="$z30-85x-type"/>
        </z30-85x-type>
        <z30-depository-id>
            <xsl:value-of select="$z30-depository-id"/>
        </z30-depository-id>
        <z30-inking-number>
            <xsl:value-of select="$z30-inking-number"/>
        </z30-inking-number>
        <z30-gap-indicator>
            <xsl:value-of select="$z30-gap-indicator"/>
        </z30-gap-indicator>
        <z30-maintenance-count>
            <xsl:value-of select="$z30-maintenance-count"/>
        </z30-maintenance-count>
        <z30-process-status-date>
            <xsl:value-of select="$z30-process-status-date"/>
        </z30-process-status-date>
    </xsl:template>
    <!-- <<< Z30 <<< -->

    <xsl:template match="/">
        <xsl:element name="z30">
            <xsl:call-template name="z30"/>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>
<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <xsl:param name="oldPassword"/>
    <xsl:param name="newPassword"/>
    
    <xsl:template match="/">
        <get-pat-pswd>
            <password_parameters>
                <old-password>
                    <xsl:value-of select="$oldPassword"/>
                </old-password>
                <new-password>
                    <xsl:value-of select="$newPassword"/>
                </new-password>
            </password_parameters>
        </get-pat-pswd>
    </xsl:template>
</xsl:stylesheet>
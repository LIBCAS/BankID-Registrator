/*
 * Copyright (C) 2022 Academy of Sciences Library
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.configurations.MainConfiguration;
import cz.cas.lib.bankid_registrator.entities.entity.Address;
import cz.cas.lib.bankid_registrator.entities.entity.IDCard;
import cz.cas.lib.bankid_registrator.product.Connect;
import cz.cas.lib.bankid_registrator.product.Identify;
import cz.cas.lib.bankid_registrator.util.TimestampToDate;
import java.io.StringWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 *
 * @author iok
 */
@Service
public class AlephService extends AlephServiceAbstract {

    @Autowired
    MainConfiguration mainConfig;

    public AlephService() {
        super();
        init();
    }

    /**
     * 
     * @param userInfo
     * @param userProfile
     * @return 
     */
    @Override
    public String CreatePatronXML(Connect userInfo, Identify userProfile) {

        Assert.notNull(userInfo, "\"userInfo\" is required");
        Assert.notNull(userProfile, "\"userProfile\" is required");

        String fullName;
        StringBuilder strTmp = new StringBuilder(0);
        String now_yyyyMMdd = TimestampToDate.getTimestampToDate("yyyyMMdd");
        String now_yyyy = TimestampToDate.getTimestampToDate("yyyy");
        String xmlString = "<?xml version=\"1.0\"?>";

        Source stylesheetSource = new StreamSource();
        stylesheetSource.setSystemId("classpath:/xml/AlephNewPatron.xsl");
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer(stylesheetSource);
            // z303
            transformer.setParameter("z303-id", this.mainConfig.getId_prefix().concat("XXXXX"));
            if (userInfo.getFamily_name() == null || userInfo.getFamily_name().trim().isEmpty()) {
                if (userProfile.getFamily_name() == null || userProfile.getFamily_name().trim().isEmpty()) {
                    getLogger().error("Error: missing attribute [family_name]");
                    return "error:missing_attribute:[family_name]";
                } else {
                    strTmp.append(userProfile.getFamily_name().concat(" "));
                    transformer.setParameter("z303-last-name", userProfile.getFamily_name());
                }
            } else {
                strTmp.append(userInfo.getFamily_name().concat(" "));
                transformer.setParameter("z303-last-name", userInfo.getFamily_name());
            }
            if (userInfo.getGiven_name() == null || userInfo.getGiven_name().trim().isEmpty()) {
                if (userProfile.getGiven_name() == null || userProfile.getGiven_name().trim().isEmpty()) {
                    getLogger().error("Error: missing attribute [given_name]");
                    return "error:missing_attribute:[given_name]";
                } else {
                    strTmp.append(userProfile.getGiven_name().concat(" "));
                    transformer.setParameter("z303-first-name", userProfile.getGiven_name());
                }
            } else {
                strTmp.append(userInfo.getGiven_name().concat(" "));
                transformer.setParameter("z303-first-name", userInfo.getGiven_name());
            }
            if (userInfo.getMiddle_name() != null && !userInfo.getMiddle_name().trim().isEmpty()) {
                strTmp.append(userInfo.getMiddle_name());
            } else {
                if (userProfile.getMiddle_name() != null && !userProfile.getMiddle_name().trim().isEmpty()) {
                    strTmp.append(userProfile.getMiddle_name());
                }
            }
            transformer.setParameter("z303-name", strTmp.toString().trim());
            fullName = transformer.getParameter("z303-name").toString();
            if (userInfo.getLocale() != null && !userInfo.getLocale().trim().isEmpty()) {
                switch (userInfo.getLocale().trim()) {
                    case "cs_CZ": transformer.setParameter("z303-con-lng", "CZE");
                        break;
                    default: {
                            getLogger().warn("Warning: unknown value of attribute [locale] / {}", userInfo.getLocale());
                            transformer.setParameter("z303-con-lng", userInfo.getLocale());
                        }
                        break;
                }
            } else {
                getLogger().error("Error: missing attribute [locale]");
                return "error:missing_attribute:[locale]";
            }
            if (userInfo.getBirthdate() == null || userInfo.getBirthdate().trim().isEmpty()) {
                if (userProfile.getBirthdate() == null || userProfile.getBirthdate().trim().isEmpty()) {
                    getLogger().error("Error: missing attribute [birthdate]");
                    return "error:missing_attribute:[birthdate]";
                } else {
                    transformer.setParameter("z303-birth-date", userProfile.getBirthdate().replaceAll("(-){1,}", ""));
                }
            } else {
                transformer.setParameter("z303-birth-date", userInfo.getBirthdate().replaceAll("(-){1,}", ""));
            }
            // z304
            transformer.setParameter("z304-seq01-id", this.mainConfig.getId_prefix().concat("XXXXX"));
            if (userProfile.getAddresses() == null) {
                getLogger().error("Error: missing attribute [address]");
                return "error:missing_attribute:[address]";
            }
            Boolean permanentResidence = Boolean.FALSE;
            for (Address address : userProfile.getAddresses()) {
                switch (address.getType()) {
                    case PERMANENT_RESIDENCE: {
                            permanentResidence = Boolean.TRUE;
                            transformer.setParameter("z304-seq01-address-0", fullName);
                            strTmp.setLength(0);
                            strTmp.append(address.getStreet().trim().concat(" "));
                            strTmp.append(address.getEvidencenumber());
                            transformer.setParameter("z304-seq01-address-1", strTmp.toString());
                            if (address.getCityarea().trim().isEmpty()) {
                                getLogger().error("Error: missing attribute [cityarea]");
                                if (address.getCity().trim().isEmpty()) {
                                    getLogger().error("Error: missing attribute [city]");
                                    // TODO resolve error?
                                } else {
                                    transformer.setParameter("z304-seq01-address-2", address.getCity());
                                }
                            } else {
                                transformer.setParameter("z304-seq01-address-2", address.getCityarea());
                            }
                            if (address.getZipcode().trim().isEmpty()) {
                                getLogger().error("Error: missing attribute [zipcode]");
                                return "error:missing_attribute:[zipcode]";
                            } else {
                                transformer.setParameter("z304-seq01-zip", address.getZipcode());
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
            if (!permanentResidence) {
                getLogger().error("Error: missing attribute [PERMANENT_RESIDENCE]");
                return "error:missing_attribute:[PERMANENT_RESIDENCE]";
            }
            if (userInfo.getEmail() == null || userInfo.getEmail().trim().isEmpty()) {
                if (userProfile.getEmail() == null || userProfile.getEmail().trim().isEmpty()) {
                    getLogger().error("Error: missing attribute [email]");
                    return "error:missing_attribute:[email]";
                } else {
                    transformer.setParameter("z304-seq01-email-address", userProfile.getEmail());
                }
            } else {
                transformer.setParameter("z304-seq01-email-address", userInfo.getEmail());
            }
            if (userProfile.getIdcards() == null) {
                getLogger().error("Error: missing attribute [idcard]");
                return "error:missing_attribute:[idcard]";
            }
            Boolean idCZ = Boolean.FALSE;
            for (IDCard idCard : userProfile.getIdcards()) {
                switch (idCard.getType()) {
                    case ID: if (idCard.getCountry().trim().matches("(CZ){1}")) {
                            idCZ = Boolean.TRUE;
                            strTmp.setLength(0);
                            strTmp.append(idCard.getType().toString().concat(" "));
                            strTmp.append(idCard.getCountry());
                            transformer.setParameter("z304-seq01-telephone-2", strTmp.toString());
                            transformer.setParameter("z304-seq01-telephone-4", idCard.getNumber());
                        }
                        break;
                    default:
                        break;
                }
            }
            if (!idCZ) {
                getLogger().error("Error: missing attribute [ID CZ]");
                return "error:missing_attribute:[ID CZ]";
            }
            transformer.setParameter("z304-seq01-date-from", now_yyyyMMdd);
            if (!this.mainConfig.getLength_of_registration().matches("[0-9]{1,}")) {
                this.mainConfig.setLength_of_registration("30");
            }
            int future_date = Integer.valueOf(now_yyyy) + Integer.valueOf(this.mainConfig.getLength_of_registration());
            transformer.setParameter("z304-seq01-date-to", String.valueOf(future_date).concat("1231"));
            // z305 / KNA50, KNAV, KNAVD, KNAVP
            transformer.setParameter("z305-kna50-id", this.mainConfig.getId_prefix().concat("XXXXX"));
            transformer.setParameter("z305-kna50-registration-date", now_yyyyMMdd);
            transformer.setParameter("z305-knav-id", this.mainConfig.getId_prefix().concat("XXXXX"));
            transformer.setParameter("z305-knav-registration-date", now_yyyyMMdd);
            transformer.setParameter("z305-knavd-id", this.mainConfig.getId_prefix().concat("XXXXX"));
            transformer.setParameter("z305-knavd-registration-date", now_yyyyMMdd);
            transformer.setParameter("z305-knavp-id", this.mainConfig.getId_prefix().concat("XXXXX"));
            transformer.setParameter("z305-knavp-registration-date", now_yyyyMMdd);
            // z308 / id
            transformer.setParameter("z308-key-type-00-id", this.mainConfig.getId_prefix().concat("XXXXX"));
            transformer.setParameter("z308-key-type-00-key-data", this.mainConfig.getId_prefix().concat("XXXXX"));
            // z308 / barcode
            transformer.setParameter("z308-key-type-01-id", this.mainConfig.getId_prefix().concat("XXXXX"));
            transformer.setParameter("z308-key-type-01-key-data", this.mainConfig.getBarcode_prefix().concat("XXXXX"));
            // z308 / bankid
            transformer.setParameter("z308-key-type-07-id", this.mainConfig.getId_prefix().concat("XXXXX"));
            if (userInfo.getSub() == null || userInfo.getSub().trim().isEmpty()) {
                if (userProfile.getSub() == null || userProfile.getSub().trim().isEmpty()) {
                    getLogger().error("Error: missing attribute [sub]");
                    return "error:missing_attribute:[sub]";
                } else {
                    transformer.setParameter("z308-key-type-07-key-data", userProfile.getSub());
                }
            } else {
                transformer.setParameter("z308-key-type-07-key-data", userInfo.getSub());
            }
            // z308 / internal verification id
            transformer.setParameter("z308-key-type-08-id", this.mainConfig.getId_prefix().concat("XXXXX"));
            transformer.setParameter("z308-key-type-08-key-data",
                    fullName.replaceAll("\\s{1,}", "").concat(userInfo.getBirthdate().replaceAll("(-){1,}", "")));

            StringWriter stringWriter = new StringWriter();
            Result result = new StreamResult(stringWriter);
            transformer.transform(stylesheetSource, result);
            if (this.mainConfig.getRewrite_aleph_batch_xml_header()) {
                xmlString = stringWriter.getBuffer().toString().replaceFirst("\\<\\?(xml){1}\\s{1,}.*\\?\\>", xmlString);
            } else {
                xmlString = stringWriter.getBuffer().toString();
            }
        } catch (TransformerException ex) {
            getLogger().error(MainService.class.getName(), ex);
        }

        return xmlString;
    }

}

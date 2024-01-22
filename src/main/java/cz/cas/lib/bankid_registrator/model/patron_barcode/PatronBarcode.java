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
package cz.cas.lib.bankid_registrator.model.patron_barcode;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;

/**
 *
 * @author iok
 */
@Entity
@Table(name="patron_barcode")
public class PatronBarcode extends PatronAbstract implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;

    @Column(name="sub")
    @NotEmpty
    private String sub;

    @Column(name="barcode")
    @NotEmpty
    private String barcode;

    public PatronBarcode() {
        super();
        init();
    }

    /**
     * 
     * @return 
     */
    @Override
    public int hashCode() {
       int hash = 3;
       hash = 97 * hash + Objects.hashCode(this.id);

       return hash;
    }

    /**
     * 
     * @param obj
     * @return 
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return Boolean.TRUE;
        }
        if (obj == null) {
            return Boolean.FALSE;
        }
        if (getClass() != obj.getClass()) {
            return Boolean.FALSE;
        }
        final PatronBarcode other = (PatronBarcode) obj;

        return Objects.equals(this.id, other.sub);
    }

    /**
     * 
     * @param id 
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 
     * @return 
     */
    public Long getId() {
        return id;
    }

    /**
     * 
     * @param sub 
     */
    public void setSub(String sub) {
       this.sub = sub; 
    }

    /**
     * 
     * @return 
     */
    public String getSub() {
        return this.sub;
    }

    /**
     * 
     * @param barcode 
     */
    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    /**
     * 
     * @return 
     */
    public String getBarcode() {
        return this.barcode;
    }

    /**
     * 
     * @return 
     */
    @Override
    public String toString() {

        StringBuilder strTmp = new StringBuilder(0);

        strTmp.append("PatronBarcode {");
        strTmp.append("id=\"".concat(String.valueOf(this.id)).concat("\", "));
        strTmp.append("sub=\"".concat(this.sub).concat("\", "));
        strTmp.append("barcode=\"".concat(this.barcode).concat("\""));
        strTmp.append("}");

        return strTmp.toString();
    }
}

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
package cz.cas.lib.bankid_registrator.dao.oracle;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(transactionManager = "oracleTransactionManager")
public class OracleRepository
{
    @PersistenceContext(unitName = "oracleEntityManager")
    private EntityManager entityManager;

     /**
     * Gets the number of matches with the given name and birth date.
     * @param name
     * @param birthDate
     * @return Number of matches.
     */
    public int getPatronRowsCount(String name, String birthDate)
    {
        String sql = "SELECT COUNT(*) AS count " +
                     "FROM KNA50.Z303 A, KNA50.Z305 B, KNA50.Z308 C " +
                     "WHERE A.Z303_REC_KEY = SUBSTR(B.Z305_REC_KEY, 1, 12) " +
                     "AND A.Z303_REC_KEY = C.Z308_ID " +
                     "AND A.Z303_BIRTH_DATE = :birthDate " +
                     "AND A.Z303_NAME = :name " +
                     "AND C.Z308_REC_KEY LIKE '00KNAV%'";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("name", name);
        query.setParameter("birthDate", birthDate);
        return ((Number) query.getSingleResult()).intValue();
    }

    /**
     * Checks if a RFID is already in use by any patron except the given one (if any).
     * @param rfid
     * @param patronId
     * @return Number of matches.
     */
    public int getRFIDRowsCount(String rfid, @Nullable String patronId)
    {
        String sanitizedRfid = rfid.replace("%", "\\%").replace("_", "\\_");
        String sanitizedPatronId = (patronId != null) ? patronId.replace("%", "\\%").replace("_", "\\_") : null;
    
        String sql = "SELECT COUNT(*) AS count " +
                     "FROM KNA50.Z308 " +
                     "WHERE Z308_REC_KEY LIKE :rfid ESCAPE '\\'";
    
        if (sanitizedPatronId != null) {
            sql += " AND Z308_ID NOT LIKE :patronId ESCAPE '\\'";
        }
    
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("rfid", "03" + sanitizedRfid + " %");
    
        if (sanitizedPatronId != null) {
            query.setParameter("patronId", sanitizedPatronId + " %");
        }
    
        return ((Number) query.getSingleResult()).intValue();
    }

    /**
     * Gets the maximum number from the Z303_REC_KEY column of rows
     * where Z303_REC_KEY starts with 'KNBD' followed by numeric characters.
     * @return Maximum number.
     */
    public Long getMaxBankIdZ303RecKey()
    {
        String sql = "SELECT MAX(TO_NUMBER(TRIM(SUBSTR(Z303_REC_KEY, 5)))) " +
                     "FROM KNA50.Z303 " +
                     "WHERE REGEXP_LIKE(TRIM(Z303_REC_KEY), '^KNBD[0-9]+$')";

        Query query = entityManager.createNativeQuery(sql);
        return ((Number) query.getSingleResult()).longValue();
    }

    /**
     * Deletes a record of 07 type from the z308 table for a specific patron.
     *
     * @param patronId The ID of the record to delete.
     *
     * @return The number of rows affected by the DELETE operation.
     */
    public int deleteZ308RecordType07(String patronId)
    {
        String sql = "DELETE FROM KNA50.Z308 WHERE Z308_ID LIKE :patronId AND Z308_REC_KEY LIKE '07%'";
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("patronId", patronId + " %");

        return ((Number) query.executeUpdate()).intValue();
    }
}

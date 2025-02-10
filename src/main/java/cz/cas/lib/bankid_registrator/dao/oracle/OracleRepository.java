/**
 * Repository for the Aleph Oracle database.
 */
package cz.cas.lib.bankid_registrator.dao.oracle;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
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
     * Search for an Aleph patron based on the given name and birth date.
     * @param name The patron's name.
     * @param birthDate The patron's birth date.
     * @return An Optional containing the Aleph patron's ID if found, or empty if not.
     */
    public Optional<String> getPatronIdByNameAndBirth(String name, String birthDate) {
        String sql = "SELECT A.Z303_REC_KEY AS patron_id " +
                    "FROM KNA50.Z303 A, KNA50.Z305 B, KNA50.Z308 C " +
                    "WHERE A.Z303_REC_KEY = SUBSTR(B.Z305_REC_KEY, 1, 12) " +
                    "AND A.Z303_REC_KEY = C.Z308_ID " +
                    "AND A.Z303_BIRTH_DATE = :birthDate " +
                    "AND A.Z303_NAME = :name " +
                    "AND (C.Z308_REC_KEY LIKE '00KNAV%' OR C.Z308_REC_KEY LIKE '00KNBD%') " +
                    "AND ROWNUM <= 1";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("name", name);
        query.setParameter("birthDate", birthDate);

        try {
            String patronId = ((String) query.getSingleResult()).trim();
            return Optional.of(patronId);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

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
     * Checks if the given email is already in use by any patron except the given one (if any).
     * @param email The email address to check.
     * @param patronId The patron ID to exclude from the check.
     * @return true if the email exists, false otherwise.
     */
    public boolean isExistingPatronEmail(String email, @Nullable String patronId)
    {
        String sanitizedPatronId = (patronId != null) ? patronId.replace("%", "\\%").replace("_", "\\_") : null;

        String sql = "SELECT COUNT(*) AS count " +
                    "FROM KNA50.Z304 " +
                    "WHERE LOWER(Z304_EMAIL_ADDRESS) = LOWER(:email)";

        if (sanitizedPatronId != null) {
            sql += " AND Z304_REC_KEY NOT LIKE :patronId ESCAPE '\\'";
        }        

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("email", email);

        if (sanitizedPatronId != null) {
            query.setParameter("patronId", sanitizedPatronId + " %");
        }
        
        int count = ((Number) query.getSingleResult()).intValue();
        return count > 0;
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
        Object result = query.getSingleResult();

        return (result != null) ? ((Number) result).longValue() : 0L;
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

    /**
     * Retrieves data of multiple Aleph patrons at once
     * @param patronIds
     * @return
     */
    public List<Object[]> getBulkPatronsData(List<String> patronIds)
    {
        String feeDescr = "Bank iD";

        String sql = "SELECT " +
                        "TRIM(SUBSTR(Z303.Z303_REC_KEY, 1, INSTR(Z303.Z303_REC_KEY, ' ') - 1)) AS patron_id, " +
                        "Z31.Z31_STATUS AS payment_status, " +
                        "Z303.Z303_NAME AS fullname, " +
                        "Z304.Z304_EMAIL_ADDRESS AS email, " +
                        "Z304.Z304_SMS_NUMBER AS phone, " +
                        "Z31.Z31_PAYMENT_CATALOGER AS cataloger, " +
                        "Z31.Z31_UPD_TIME_STAMP AS modified_at " +
                    "FROM KNA50.Z31 Z31 " +
                        "JOIN KNA50.Z304 Z304 ON SUBSTR(Z31.Z31_REC_KEY, 1, INSTR(Z31.Z31_REC_KEY, ' ') - 1) = SUBSTR(Z304.Z304_REC_KEY, 1, INSTR(Z304.Z304_REC_KEY, ' ') - 1) " +
                        "JOIN KNA50.Z303 Z303 ON SUBSTR(Z31.Z31_REC_KEY, 1, INSTR(Z31.Z31_REC_KEY, ' ') - 1) = SUBSTR(Z303.Z303_REC_KEY, 1, INSTR(Z303.Z303_REC_KEY, ' ') - 1) " +
                    "WHERE Z31.Z31_DESCRIPTION = :feeDescr " +
                        "AND Z304.Z304_ADDRESS_TYPE = 1 " +
                        "AND SUBSTR(Z304.Z304_REC_KEY, 1, INSTR(Z304.Z304_REC_KEY, ' ') - 1) IN  :patronIds " +
                        "AND Z31.Z31_DATE_X = (" +
                            "SELECT MAX(Z31_SUB.Z31_DATE_X) " +
                            "FROM KNA50.Z31 Z31_SUB " +
                            "WHERE SUBSTR(Z31_SUB.Z31_REC_KEY, 1, INSTR(Z31_SUB.Z31_REC_KEY, ' ') - 1) = SUBSTR(Z31.Z31_REC_KEY, 1, INSTR(Z31.Z31_REC_KEY, ' ') - 1)" +
                        ")";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("feeDescr", feeDescr);
        query.setParameter("patronIds", patronIds);

        @SuppressWarnings("unchecked")
        List<Object[]> result = query.getResultList();

        return result;
    }
}

/**
 * Repository for the Aleph Oracle database.
 */
package cz.cas.lib.bankid_registrator.dao.oracle;

import cz.cas.lib.bankid_registrator.configurations.AlephServiceConfig;
import cz.cas.lib.bankid_registrator.configurations.MainConfiguration;
import cz.cas.lib.bankid_registrator.exceptions.AmbiguousPatronMatchException;
import cz.cas.lib.bankid_registrator.util.PatronNameMatcher;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(transactionManager = "oracleTransactionManager")
public class OracleRepository
{
    @PersistenceContext(unitName = "oracleEntityManager")
    private EntityManager entityManager;

    @Autowired
    private AlephServiceConfig alephServiceConfig;

    @Autowired
    private MainConfiguration mainConfiguration;

    private static final Logger logger = LoggerFactory.getLogger(OracleRepository.class);

    /**
     * Oracle limits IN clauses to 1000 elements (ORA-01795).
     * This constant defines the maximum batch size for IN clause parameters.
     */
    private static final int ORACLE_IN_CLAUSE_LIMIT = 1000;

    /** Resolves a unique patron; multiple distinct matches require staff assistance. */
    public Optional<String> getPatronIdByNameAndBirth(String name, String birthDate)
    {
        List<String> matches = getMatchingPatronIds(name, birthDate);
        if (matches.size() > 1) {
            throw new AmbiguousPatronMatchException();
        }
        return matches.stream().findFirst();
    }

    /** Counts distinct patrons using the same matching rules as identity resolution. */
    public int getPatronRowsCount(String name, String birthDate)
    {
        return getMatchingPatronIds(name, birthDate).size();
    }

    private List<String> getMatchingPatronIds(String name, String birthDate)
    {
        if (PatronNameMatcher.normalize(name).isEmpty()
                || birthDate == null || !birthDate.matches("[0-9]{8}")) {
            throw new IllegalArgumentException("Name and birth date are required for patron matching");
        }
        String[] prefixes = alephServiceConfig.getPatronidPrefixes();
        String likeConditions = Arrays.stream(prefixes)
            .map(prefix -> "C.Z308_REC_KEY LIKE '00" + prefix + "%'")
            .collect(Collectors.joining(" OR "));

        // Filter by exact birth date in Oracle; compare names consistently in Java.
        // EXISTS avoids multiplying candidates by their library/identifier rows.
        String sql = "SELECT A.Z303_REC_KEY, A.Z303_NAME FROM KNA50.Z303 A " +
            "WHERE A.Z303_BIRTH_DATE = :birthDate " +
            "AND EXISTS (SELECT 1 FROM KNA50.Z305 B " +
            "WHERE A.Z303_REC_KEY = SUBSTR(B.Z305_REC_KEY, 1, 12)) " +
            "AND EXISTS (SELECT 1 FROM KNA50.Z308 C " +
            "WHERE A.Z303_REC_KEY = C.Z308_ID AND (" + likeConditions + "))";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("birthDate", birthDate);
        @SuppressWarnings("unchecked")
        List<Object[]> candidates = query.getResultList();
        String normalizedName = PatronNameMatcher.normalize(name);
        return candidates.stream()
            .filter(row -> normalizedName.equals(PatronNameMatcher.normalize((String) row[1])))
            .map(row -> ((String) row[0]).trim())
            .distinct()
            .collect(Collectors.toList());
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
     * @param limitPatronPrefixes Array of patron ID prefixes to limit the query to.
     * @return true if the email exists, false otherwise.
     */
    public boolean isExistingPatronEmail(String email, @Nullable String patronId, @Nullable String[] limitPatronPrefixes)
    {
        String sanitizedPatronId = (patronId != null) ? patronId.replace("%", "\\%").replace("_", "\\_") : null;

        String sql = "SELECT COUNT(*) AS count " +
                    "FROM KNA50.Z304 " +
                    "WHERE LOWER(Z304_EMAIL_ADDRESS) = LOWER(:email)";

        if (sanitizedPatronId != null) {
            sql += " AND Z304_REC_KEY NOT LIKE :patronId ESCAPE '\\'";
        }

        if (limitPatronPrefixes != null && limitPatronPrefixes.length > 0) {
            sql += " AND (" + 
                Arrays.stream(limitPatronPrefixes)
                    .map(prefix -> "Z304_REC_KEY LIKE :prefix_" + prefix.replaceAll("[^a-zA-Z0-9]", ""))
                    .collect(Collectors.joining(" OR ")) +
               ")";
        }

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("email", email);

        if (sanitizedPatronId != null) {
            query.setParameter("patronId", sanitizedPatronId + " %");
        }

        if (limitPatronPrefixes != null && limitPatronPrefixes.length > 0) {
            for (String prefix : limitPatronPrefixes) {
                String safePrefix = prefix.replaceAll("[^a-zA-Z0-9]", "");
                query.setParameter("prefix_" + safePrefix, prefix + "%");
            }
        }

        int count = ((Number) query.getSingleResult()).intValue();

        logger.info("Check email occurrence: email={}, patronId={}, limitPatronPrefixes={}, count={}", email, patronId, limitPatronPrefixes, count);

        return count > 0;
    }

    /**
     * Gets the maximum number from the Z303_REC_KEY column of rows
     * where Z303_REC_KEY starts with the configured BankID Aleph patron ID prefix followed by numeric characters.
     * @return Maximum number.
     */
    public Long getMaxBankIdZ303RecKey()
    {
        String idPrefix = mainConfiguration.getId_prefix();
        int prefixLength = idPrefix.length() + 1; // +1 for SUBSTR position (1-indexed)

        String sql = "SELECT MAX(TO_NUMBER(TRIM(SUBSTR(Z303_REC_KEY, " + prefixLength + ")))) " +
                     "FROM KNA50.Z303 " +
                     "WHERE REGEXP_LIKE(TRIM(Z303_REC_KEY), '^" + idPrefix + "[0-9]+$')";

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
     * Retrieves data of multiple Aleph patrons at once.
     * Automatically batches the patron IDs to avoid Oracle's 1000-element IN clause limit (ORA-01795).
     * @param patronIds
     * @return
     */
    public List<Object[]> getBulkPatronsData(List<String> patronIds)
    {
        if (patronIds == null || patronIds.isEmpty()) {
            return Collections.emptyList();
        }

        String feeDescr = "Bank iD";

        // The fixed-width record key includes the fee creation sequence. DATE_X alone
        // cannot distinguish a paid registration from a later renewal on the same day.

        String sql = 
            "SELECT " + 
            "    TRIM(SUBSTR(Z303.Z303_REC_KEY, 1, INSTR(Z303.Z303_REC_KEY, ' ') - 1)) AS patron_id, " + 
            "    COALESCE(Z31.Z31_STATUS, 'EXEMPT') AS payment_status, " + 
            "    Z303.Z303_NAME AS fullname, " + 
            "    Z304.Z304_EMAIL_ADDRESS AS email, " + 
            "    Z304.Z304_SMS_NUMBER AS phone, " + 
            "    Z31.Z31_PAYMENT_CATALOGER AS cataloger, " + 
            "    Z31.Z31_UPD_TIME_STAMP AS modified_at " + 
            "FROM KNA50.Z303 Z303 " + 
            "JOIN KNA50.Z304 Z304 ON " + 
            "    SUBSTR(Z303.Z303_REC_KEY, 1, INSTR(Z303.Z303_REC_KEY, ' ') - 1) = SUBSTR(Z304.Z304_REC_KEY, 1, INSTR(Z304.Z304_REC_KEY, ' ') - 1) " + 
            "LEFT JOIN ( " + 
            "    SELECT " + 
            "        Z31.*, " + 
            "        ROW_NUMBER() OVER (PARTITION BY SUBSTR(Z31.Z31_REC_KEY, 1, INSTR(Z31.Z31_REC_KEY, ' ') - 1) ORDER BY Z31.Z31_DATE_X DESC, Z31.Z31_REC_KEY DESC) AS rn " +
            "    FROM KNA50.Z31 Z31 " + 
            "    WHERE Z31.Z31_DESCRIPTION = :feeDescr " + 
            ") Z31 ON " + 
            "    SUBSTR(Z303.Z303_REC_KEY, 1, INSTR(Z303.Z303_REC_KEY, ' ') - 1) = SUBSTR(Z31.Z31_REC_KEY, 1, INSTR(Z31.Z31_REC_KEY, ' ') - 1) " + 
            "    AND Z31.rn = 1 " + 
            "WHERE Z304.Z304_ADDRESS_TYPE = 1 " + 
            "AND SUBSTR(Z303.Z303_REC_KEY, 1, INSTR(Z303.Z303_REC_KEY, ' ') - 1) IN (:patronIds)";

        List<Object[]> result = new java.util.ArrayList<>();

        for (int i = 0; i < patronIds.size(); i += ORACLE_IN_CLAUSE_LIMIT) {
            List<String> batch = patronIds.subList(i, Math.min(i + ORACLE_IN_CLAUSE_LIMIT, patronIds.size()));

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("feeDescr", feeDescr);
            query.setParameter("patronIds", batch);

            @SuppressWarnings("unchecked")
            List<Object[]> batchResult = query.getResultList();
            result.addAll(batchResult);
        }

        return result;
    }
}

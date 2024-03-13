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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;

/**
 *
 * @author iok
 */
@Repository
public class OracleRepository
{
    @PersistenceContext(unitName = "oracleEntityManager")
    private EntityManager entityManager;

     /**
     * Gets the number of matches with the given name and birth date.
     * @param name
     * @param birthDate
     * @return
     */
    public int getPatronRowsCount(String name, String birthDate) {
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
}

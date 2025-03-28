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
package cz.cas.lib.bankid_registrator.dao.mariadb;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import cz.cas.lib.bankid_registrator.model.patron.PatronBarcode;

/**
 *
 * @author iok
 */
public interface PatronBarcodeRepository extends JpaRepository<PatronBarcode, Long> {
    @Query(value = "SELECT MAX(p.id) FROM patron_barcode p", nativeQuery = true)
    Long findMaxId();

    @Query(value = "SELECT MAX(p.barcode_aleph) FROM patron_barcode p", nativeQuery = true)
    Long findMaxBarcode();
}

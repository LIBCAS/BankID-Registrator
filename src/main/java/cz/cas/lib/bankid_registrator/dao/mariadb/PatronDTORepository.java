package cz.cas.lib.bankid_registrator.dao.mariadb;

import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatronDTORepository extends JpaRepository<PatronDTO, Long> {
}
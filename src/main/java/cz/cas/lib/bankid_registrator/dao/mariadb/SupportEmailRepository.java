package cz.cas.lib.bankid_registrator.dao.mariadb;

import cz.cas.lib.bankid_registrator.model.app_settings.SupportEmail;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportEmailRepository extends JpaRepository<SupportEmail, Long>
{
    List<SupportEmail> findAllByOrderBySortOrderAscIdAsc();
    Optional<SupportEmail> findByPrimaryTrue();
}

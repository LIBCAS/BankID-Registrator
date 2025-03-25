package cz.cas.lib.bankid_registrator.dao.mariadb;

import cz.cas.lib.bankid_registrator.model.identity.IdentityActivity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityActivityRepository extends JpaRepository<IdentityActivity, Long>
{
    List<IdentityActivity> findByIdentityIdOrderByCreatedAtAsc(Long identityId);
}
package cz.cas.lib.bankid_registrator.dao.mariadb;

import cz.cas.lib.bankid_registrator.model.identity.IdentityActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityActivityRepository extends JpaRepository<IdentityActivity, Long> {
}
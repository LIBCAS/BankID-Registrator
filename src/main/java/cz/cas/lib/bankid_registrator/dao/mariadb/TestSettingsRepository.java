package cz.cas.lib.bankid_registrator.dao.mariadb;

import cz.cas.lib.bankid_registrator.model.test_settings.TestSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestSettingsRepository extends JpaRepository<TestSettings, Long>
{
}

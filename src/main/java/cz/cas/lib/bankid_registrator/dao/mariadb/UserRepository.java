package cz.cas.lib.bankid_registrator.dao.mariadb;

import org.springframework.data.jpa.repository.JpaRepository;
import cz.cas.lib.bankid_registrator.model.user.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
}
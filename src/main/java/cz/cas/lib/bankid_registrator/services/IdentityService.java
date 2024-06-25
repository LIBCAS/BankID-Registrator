package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.dao.mariadb.IdentityRepository;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class IdentityService
{
    @Autowired
    private IdentityRepository identityRepository;

    @Transactional
    public void emptyTable() {
        this.identityRepository.deleteAll();
    }

    public void save(Identity identity) {
        this.identityRepository.save(identity);
    }

    public Optional<Identity> findById(Long id) {
        return this.identityRepository.findById(id);
    }

    public Optional<Identity> findByBankId(String bankId) {
        return this.identityRepository.findByBankId(bankId);
    }

    /**
     * Get an Aleph-linked identity by its bank_id column. 
     * Aleph-linked identity is an identity which has an Aleph patron linked to it.
     */
    public Optional<Identity> findAlephLinkedByBankId(String bankId) {
        return this.identityRepository.findAlephLinkedByBankId(bankId);
    }

    /**
     * Get identities which have media files attached (i.e. CAS employees)
     * @return list of identities
     */
    public List<Identity> getIdentitiesWithMedia() {
        return this.identityRepository.getIdentitiesWithMedia();
    }

    /**
     * Get the maximum id of all identities
     * @return maximum id
     */
    public Long getMaxId() {
        return this.identityRepository.getMaxId();
    }
}
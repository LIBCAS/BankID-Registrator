package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.dao.mariadb.IdentityRepository;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService extends ServiceAbstract
{
    private final IdentityRepository identityRepository;

    public IdentityService(IdentityRepository identityRepository) {
        super(null);
        this.identityRepository = identityRepository;
    }

    @Transactional
    public void emptyTable() {
        this.identityRepository.deleteAll();
    }

    public void save(Identity identity) {
        this.identityRepository.save(identity);
    }

    public void delete(Identity identity) {
        this.identityRepository.delete(identity);
    }

    public Optional<Identity> findById(Long id) {
        return this.identityRepository.findById(id);
    }

    public Optional<Identity> findByBankId(String bankId) {
        return this.identityRepository.findByBankId(bankId);
    }

    public Optional<Identity> findByAlephId(String alephId) {
        return this.identityRepository.findByAlephId(alephId);
    }

    public Optional<Identity> findByAlephBarcode(String alephBarcode) {
        return this.identityRepository.findByAlephBarcode(alephBarcode);
    }

    /**
     * Get an Aleph ID (Aleph Patron's ID) by the identity's id
     * @param identityId
     * @return
     */
    public String findAlephIdById(Long identityId) {
        Optional<Identity> identity = findById(identityId);
        return identity.map(Identity::getAlephId).orElse(null);
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

    /**
     * Get all Aleph IDs
     * @return
     */
    public String[] getAllAlephIds() {
        List<String> alephIds = this.identityRepository.findAllAlephIds();
        return alephIds.toArray(new String[0]);
    }

    /**
     * Find identities by search criteria with pagination
     * @param pageable
     * @param searchAlephIdOrBarcode
     * @param filterCasEmployee
     * @param filterCheckedByAdmin
     * @param filterSoftDeleted
     * @return
     */
    public Page<Identity> findIdentities(Pageable pageable, String searchAlephIdOrBarcode, Boolean filterCasEmployee, Boolean filterCheckedByAdmin, Boolean filterSoftDeleted) {
        return identityRepository.findIdentities(pageable, searchAlephIdOrBarcode, filterCasEmployee, filterCheckedByAdmin, filterSoftDeleted);
    }

    /**
     * Find all identities by search criteria without pagination
     * @param searchAlephIdOrBarcode
     * @param filterCasEmployee
     * @param filterCheckedByAdmin
     * @param filterSoftDeleted
     * @param sort
     * @return List of all matching identities
     */
    public List<Identity> findAllIdentities(String searchAlephIdOrBarcode, Boolean filterCasEmployee, Boolean filterCheckedByAdmin, Boolean filterSoftDeleted, Sort sort) {
        return identityRepository.findAllIdentities(searchAlephIdOrBarcode, filterCasEmployee, filterCheckedByAdmin, filterSoftDeleted, sort);
    }
}

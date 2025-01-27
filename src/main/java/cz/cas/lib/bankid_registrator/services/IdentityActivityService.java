package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.dao.mariadb.IdentityActivityRepository;
import cz.cas.lib.bankid_registrator.entities.activity.ActivityEvent;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.identity.IdentityActivity;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class IdentityActivityService
{
    @Autowired
    private IdentityActivityRepository identityActivityRepository;

    @Transactional
    public void emptyTable() {
        this.identityActivityRepository.deleteAll();
    }

    public void logBankIdVerificationInitiation(Identity identity) {
        this.identityActivityRepository.save(new IdentityActivity(identity, ActivityEvent.BANKID_VERIFICATION_INITIATION));
    }

    public void logBankIdVerificationSuccess(Identity identity) {
        this.identityActivityRepository.save(new IdentityActivity(identity, ActivityEvent.BANKID_VERIFICATION_SUCCESS));
    }

    public void logBankIdVerificationFailure(Identity identity) {
        this.identityActivityRepository.save(new IdentityActivity(identity, ActivityEvent.BANKID_VERIFICATION_FAILURE));
    }

    public void logNewRegistrationInitiation(Identity identity) {
        this.identityActivityRepository.save(new IdentityActivity(identity, ActivityEvent.NEW_REGISTRATION_INITIATION));
    }

    public void logNewRegistrationSubmission(Identity identity) {
        this.identityActivityRepository.save(new IdentityActivity(identity, ActivityEvent.NEW_REGISTRATION_SUBMISSION));
    }

    public void logNewRegistrationSuccess(Identity identity) {
        this.identityActivityRepository.save(new IdentityActivity(identity, ActivityEvent.NEW_REGISTRATION_SUCCESS));
    }

    public void logNewRegistrationFailure(Identity identity) {
        this.identityActivityRepository.save(new IdentityActivity(identity, ActivityEvent.NEW_REGISTRATION_FAILURE));
    }

    public void logNewRegistrationEmailSent(Identity identity) {
        this.identityActivityRepository.save(new IdentityActivity(identity, ActivityEvent.NEW_REGISTRATION_EMAIL_SENT));
    }

    public void logMembershipRenewalInitiation(Identity identity) {
        this.identityActivityRepository.save(new IdentityActivity(identity, ActivityEvent.MEMBERSHIP_RENEWAL_INITIATION));
    }

    public void logMembershipRenewalSubmission(Identity identity) {
        this.identityActivityRepository.save(new IdentityActivity(identity, ActivityEvent.MEMBERSHIP_RENEWAL_SUBMISSION));
    }

    public void logMembershipRenewalSuccess(Identity identity) {
        this.identityActivityRepository.save(new IdentityActivity(identity, ActivityEvent.MEMBERSHIP_RENEWAL_SUCCESS));
    }

    public void logMembershipRenewalFailure(Identity identity) {
        this.identityActivityRepository.save(new IdentityActivity(identity, ActivityEvent.MEMBERSHIP_RENEWAL_FAILURE));
    }

    public void logMembershipRenewalEmailSent(Identity identity) {
        this.identityActivityRepository.save(new IdentityActivity(identity, ActivityEvent.MEMBERSHIP_RENEWAL_EMAIL_SENT));
    }

    public void logIdentityDeleted(Identity identity) {
        this.identityActivityRepository.save(new IdentityActivity(identity, ActivityEvent.IDENTITY_DELETED));
    }

    public void logIdentityRestored(Identity identity) {
        this.identityActivityRepository.save(new IdentityActivity(identity, ActivityEvent.IDENTITY_RESTORED));
    }

    public void logIdentityMarkedAsDeletedInAleph(Identity identity) {
        this.identityActivityRepository.save(new IdentityActivity(identity, ActivityEvent.IDENTITY_MARKED_AS_DELETED_IN_ALEPH));
    }

    public void logAppExit(Identity identity) {
        this.identityActivityRepository.save(new IdentityActivity(identity, ActivityEvent.APP_EXIT));
    }

    /**
     * Get all activities of an identity ordered by creation date
     * @param identityId
     * @return
     */
    public List<IdentityActivity> findByIdentityId(Long identityId) {
        return identityActivityRepository.findByIdentityIdOrderByCreatedAtAsc(identityId);
    }
}

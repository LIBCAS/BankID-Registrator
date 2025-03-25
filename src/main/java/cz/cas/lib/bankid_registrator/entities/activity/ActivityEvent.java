package cz.cas.lib.bankid_registrator.entities.activity;

/**
 * Enum representing an activity event
 */
public enum ActivityEvent {
    BANKID_VERIFICATION_INITIATION,
    BANKID_VERIFICATION_SUCCESS,
    BANKID_VERIFICATION_FAILURE,
    NEW_REGISTRATION_INITIATION,
    NEW_REGISTRATION_SUBMISSION,
    NEW_REGISTRATION_SUCCESS,
    NEW_REGISTRATION_FAILURE,
    NEW_REGISTRATION_EMAIL_SENT,
    MEMBERSHIP_RENEWAL_INITIATION,
    MEMBERSHIP_RENEWAL_SUBMISSION,
    MEMBERSHIP_RENEWAL_SUCCESS,
    MEMBERSHIP_RENEWAL_FAILURE,
    MEMBERSHIP_RENEWAL_EMAIL_SENT,
    IDENTITY_DELETED,   // soft-delete
    IDENTITY_RESTORED,  // restore soft-deleted
    IDENTITY_MARKED_AS_DELETED_IN_ALEPH,
    APP_EXIT
}

package cz.cas.lib.bankid_registrator.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionSubmissionGuardTest
{
    @Test
    void submissionCanOnlyBeClaimedOnceUntilReleased() {
        MockHttpSession session = new MockHttpSession();

        assertTrue(SessionSubmissionGuard.claim(session, "registration:123"));
        assertFalse(SessionSubmissionGuard.claim(session, "registration:123"));

        SessionSubmissionGuard.release(session, "registration:123");

        assertTrue(SessionSubmissionGuard.claim(session, "registration:123"));
    }

    @Test
    void differentBusinessActionsHaveIndependentClaims() {
        MockHttpSession session = new MockHttpSession();

        assertTrue(SessionSubmissionGuard.claim(session, "registration:123"));
        assertTrue(SessionSubmissionGuard.claim(session, "renewal:123"));
    }
}

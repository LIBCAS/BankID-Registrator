package cz.cas.lib.bankid_registrator.util;

import javax.servlet.http.HttpSession;

/**
 * Atomically claims state-changing form submissions within one browser session.
 */
public final class SessionSubmissionGuard
{
    private static final String ATTRIBUTE_PREFIX = SessionSubmissionGuard.class.getName() + ".";

    private SessionSubmissionGuard() {
    }

    public static boolean claim(HttpSession session, String submissionKey) {
        String attributeName = ATTRIBUTE_PREFIX + submissionKey;

        synchronized (session) {
            if (Boolean.TRUE.equals(session.getAttribute(attributeName))) {
                return false;
            }

            session.setAttribute(attributeName, true);
            return true;
        }
    }

    public static void release(HttpSession session, String submissionKey) {
        synchronized (session) {
            session.removeAttribute(ATTRIBUTE_PREFIX + submissionKey);
        }
    }
}

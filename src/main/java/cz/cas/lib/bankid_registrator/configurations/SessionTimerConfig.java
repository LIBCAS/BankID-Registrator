package cz.cas.lib.bankid_registrator.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the client-side session inactivity timer.
 * 
 * - inactivityTimeoutSeconds: Time in seconds of user inactivity before auto-logout (default: 300 = 5 minutes)
 * - hardCapSeconds: Absolute maximum session duration in seconds since BankID login (default: 2700 = 45 minutes)
 * - warningBeforeSeconds: Show warning modal this many seconds before inactivity timeout (default: 60 = 1 minute)
 * - keepAliveIntervalSeconds: Interval in seconds between keep-alive pings on user activity (default: 30)
 */
@Configuration
@ConfigurationProperties(prefix = "session-timer")
@Validated
public class SessionTimerConfig
{
    private int inactivityTimeoutSeconds = 300;     // Default is 5 minutes
    private int hardCapSeconds = 2700;              // Default is 45 minutes
    private int warningBeforeSeconds = 60;          // Default is 1 minute before timeout
    private int keepAliveIntervalSeconds = 30;      // Default is 30 seconds

    public int getInactivityTimeoutSeconds() {
        return inactivityTimeoutSeconds;
    }

    public void setInactivityTimeoutSeconds(int inactivityTimeoutSeconds) {
        this.inactivityTimeoutSeconds = inactivityTimeoutSeconds;
    }

    public int getHardCapSeconds() {
        return hardCapSeconds;
    }

    public void setHardCapSeconds(int hardCapSeconds) {
        this.hardCapSeconds = hardCapSeconds;
    }

    public int getWarningBeforeSeconds() {
        return warningBeforeSeconds;
    }

    public void setWarningBeforeSeconds(int warningBeforeSeconds) {
        this.warningBeforeSeconds = warningBeforeSeconds;
    }

    public int getKeepAliveIntervalSeconds() {
        return keepAliveIntervalSeconds;
    }

    public void setKeepAliveIntervalSeconds(int keepAliveIntervalSeconds) {
        this.keepAliveIntervalSeconds = keepAliveIntervalSeconds;
    }
}

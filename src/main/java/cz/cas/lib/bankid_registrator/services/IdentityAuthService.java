package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.exceptions.IdentityAuthException;
import cz.cas.lib.bankid_registrator.valueobjs.AccessTokenContainer;
import cz.cas.lib.bankid_registrator.valueobjs.TokenContainer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/**
 * Handle identity (Bank iD related) authentication
 */
@Service
public class IdentityAuthService extends ServiceAbstract
{
    private final AccessTokenContainer accessTokenContainer;
    private final MainService mainService;

    public IdentityAuthService(
        MessageSource messageSource,
        AccessTokenContainer accessTokenContainer,
        MainService mainService
    ) {
        super(messageSource);
        this.accessTokenContainer = accessTokenContainer;
        this.mainService = mainService;
    }

    /**
     * Check if the current identity is logged in
     * @param request
     * @return boolean
     */
    public boolean isLoggedin(HttpServletRequest request)
    {
        HttpSession session = this.getCurrentSession(request);

        if (session == null) {
            return false;
        }

        String accessToken = (String) session.getAttribute("accessToken");

        return accessToken != null && this.mainService.isTokenValid(accessToken) && this.isMatchingUserBrowser(request, session);
    }

    /**
     * Log in the Bank iD verified identity
     * @param request
     * @param code authorization code provided by the Bank iD after successful verification
     * @throws IdentityAuthException
     * @return void
     */
    public void login(HttpServletRequest request, String code) throws IdentityAuthException
    {
        try {
            HttpSession session = this.getCurrentSession(request);
            TokenContainer tokenContainer = this.mainService.getTokenExchange(code);
            String accessToken = tokenContainer.getAccessToken();
            String idToken = tokenContainer.getIdToken();
            String userIp = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            this.accessTokenContainer.setAccessToken(code, accessToken);
    
            session.setAttribute("code", code);
            session.setAttribute("accessToken", accessToken);
            session.setAttribute("idToken", idToken);
            session.setAttribute("userIp", userIp);
            session.setAttribute("userAgent", userAgent);
        } catch (Exception e) {
            throw new IdentityAuthException("error.identity.auth.login", e);
        }
    }

    /**
     * Log out the Bank iD verified identity
     * @param request
     * @return void
     */
    public void logout(HttpServletRequest request)
    {
        HttpSession session = this.getCurrentSession(request);

        if (session != null) {
            String code = (String) session.getAttribute("code");
            String idToken = (String) session.getAttribute("idToken");

            if (code != null) {
                this.accessTokenContainer.getCodeTokenMap().remove(code);
            }

            if (idToken != null) {
                this.mainService.logout(idToken);
            }
    
            session.invalidate();
        }
    }

    /**
     * Get the session
     * @return HttpSession
     */
    private HttpSession getCurrentSession(HttpServletRequest request)
    {
        return request.getSession(true);
    }

    /**
     * Check if the current user's browser matches the one that was used for the Bank iD verification
     * @param request
     * @param session
     * @return boolean
     */
    private boolean isMatchingUserBrowser(HttpServletRequest request, HttpSession session)
    {
        String sessionUserIp = (String) session.getAttribute("userIp");
        String sessionUserAgent = (String) session.getAttribute("userAgent");
        String currentIp = request.getRemoteAddr();
        String currentUserAgent = request.getHeader("User-Agent");

        return sessionUserIp.equals(currentIp) && sessionUserAgent.equals(currentUserAgent);
    }
}

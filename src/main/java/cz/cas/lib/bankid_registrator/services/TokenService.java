package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.configurations.TokenServiceConfig;
import cz.cas.lib.bankid_registrator.dao.mariadb.TokenBlacklistedRepository;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.token.TokenBlacklisted;
import cz.cas.lib.bankid_registrator.model.user.User;
import cz.cas.lib.bankid_registrator.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TokenService {

    private final JwtUtil jwtUtil;
    private final TokenServiceConfig tokenServiceConfig;
    private final TokenBlacklistedRepository tokenBlacklistedRepository;

    public TokenService(JwtUtil jwtUtil, TokenServiceConfig tokenServiceConfig, TokenBlacklistedRepository tokenBlacklistedRepository) {
        this.jwtUtil = jwtUtil;
        this.tokenServiceConfig = tokenServiceConfig;
        this.tokenBlacklistedRepository = tokenBlacklistedRepository;
    }

    public String createIdentityToken(Identity identity) {
        return jwtUtil.generateToken(identity.getId().toString(), this.tokenServiceConfig.getIdentityTokenExpiration());
    }

    public boolean isIdentityTokenValid(String token) {
        return jwtUtil.isTokenValid(token) && !this.tokenBlacklistedRepository.existsByToken(token);
    }

    public String extractIdentityIdFromToken(String token) {
        return jwtUtil.extractSubject(token);
    }

    public String createUserToken(User user) {
        return jwtUtil.generateToken(user.getId().toString(), this.tokenServiceConfig.getUserTokenExpiration());
    }

    public boolean isUserTokenValid(String token) {
        return jwtUtil.isTokenValid(token) && !this.tokenBlacklistedRepository.existsByToken(token);
    }

    public String extractUserIdFromToken(String token) {
        return jwtUtil.extractSubject(token);
    }

    public String createApiToken(String clientId) {
        return jwtUtil.generateToken(clientId, this.tokenServiceConfig.getApiTokenExpiration());
    }

    public boolean isApiTokenValid(String token) {
        return jwtUtil.isTokenValid(token) && !this.tokenBlacklistedRepository.existsByToken(token);
    }

    public String extractClientIdFromApiToken(String token) {
        return jwtUtil.extractSubject(token);
    }

    public void invalidateToken(String token) {
        TokenBlacklisted tokenBlacklisted = new TokenBlacklisted(token, LocalDateTime.now());
        this.tokenBlacklistedRepository.save(tokenBlacklisted);
    }
}

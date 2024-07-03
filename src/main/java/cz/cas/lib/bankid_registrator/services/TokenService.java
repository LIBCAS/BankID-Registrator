package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.user.User;
import cz.cas.lib.bankid_registrator.util.JwtUtil;
import org.springframework.stereotype.Service;

@Service
public class TokenService
{
    private static final long TOKEN_EXPIRATION_MILLIS = 24 * 60 * 60 * 1000;

    private final JwtUtil jwtUtil;

    public TokenService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public String createIdentityToken(Identity identity) {
        return jwtUtil.generateToken(identity.getId().toString(), TOKEN_EXPIRATION_MILLIS);
    }

    public String createUserToken(User user) {
        return jwtUtil.generateToken(user.getId().toString(), TOKEN_EXPIRATION_MILLIS);
    }

    public boolean isIdentityTokenValid(String token) {
        return jwtUtil.isTokenValid(token);
    }

    public boolean isUserTokenValid(String token) {
        return jwtUtil.isTokenValid(token);
    }

    public String extractIdentityIdFromToken(String token) {
        return jwtUtil.extractSubject(token);
    }

    public String extractUserIdFromToken(String token) {
        return jwtUtil.extractSubject(token);
    }
}

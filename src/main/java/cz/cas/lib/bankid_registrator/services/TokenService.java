package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.dao.mariadb.IdentityTokenRepository;
import cz.cas.lib.bankid_registrator.dao.mariadb.UserTokenRepository;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.token.IdentityToken;
import cz.cas.lib.bankid_registrator.model.token.UserToken;
import cz.cas.lib.bankid_registrator.model.user.User;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TokenService
{
    @Autowired
    private IdentityTokenRepository identityTokenRepository;

    @Autowired
    private UserTokenRepository userTokenRepository;

    public boolean identityTokenExists(String token) {
        return identityTokenRepository.findByToken(token).isPresent();
    }

    public boolean userTokenExists(String token) {
        return userTokenRepository.findByToken(token).isPresent();
    }

    public boolean isIdentityTokenValid(String token) {
        Optional<IdentityToken> identityToken = identityTokenRepository.findByToken(token);
        return identityToken.isPresent() && identityToken.get().getExpiryDate().isAfter(LocalDateTime.now());
    }

    public boolean isUserTokenValid(String token) {
        Optional<UserToken> userToken = userTokenRepository.findByToken(token);
        return userToken.isPresent() && userToken.get().getExpiryDate().isAfter(LocalDateTime.now());
    }

    public String createIdentityToken(Identity identity) {
        String token;

        do {
            token = UUID.randomUUID().toString();
        } while (this.identityTokenExists(token));

        LocalDateTime expiryDate = LocalDateTime.now().plusHours(24);

        IdentityToken identityToken = new IdentityToken();
        identityToken.setToken(token);
        identityToken.setIdentity(identity);
        identityToken.setExpiryDate(expiryDate);

        identityTokenRepository.save(identityToken);

        return token;
    }

    public String createUserToken(User user) {
        String token;

        do {
            token = UUID.randomUUID().toString();
        } while (this.identityTokenExists(token));

        LocalDateTime expiryDate = LocalDateTime.now().plusHours(24);

        UserToken userToken = new UserToken();
        userToken.setToken(token);
        userToken.setUser(user);
        userToken.setExpiryDate(expiryDate);

        userTokenRepository.save(userToken);

        return token;
    }

    public void deleteUserToken(String token) {
        userTokenRepository.findByToken(token).ifPresent(userTokenRepository::delete);
    }

    public void deleteIdentityToken(String token) {
        identityTokenRepository.findByToken(token).ifPresent(identityTokenRepository::delete);
    }
}

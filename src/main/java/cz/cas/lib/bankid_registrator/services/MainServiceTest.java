package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.product.Connect;
import cz.cas.lib.bankid_registrator.product.Identify;
import cz.cas.lib.bankid_registrator.util.StringUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class MainServiceTest extends MainService
{
    private static final Map<String, int[]> ageRangeMap = new HashMap<>();

    /**
     * Initialize age range map for different sub (fake Bank ID identities)
     */
    static {
        ageRangeMap.put("1ec7c063-d600-4961-8ea5-7a407dcc8525", new int[]{18, 30}); // JanN
        ageRangeMap.put("cce5817d-0aa7-4340-99c9-07332c032d97", new int[]{80, 90}); // Nedbal1
        ageRangeMap.put("default", new int[]{18, 70});
    }

    @Override
    public Connect getUserInfo(String accessToken) {
        Connect userInfo = super.getUserInfo(accessToken);
        if (userInfo != null) {
            userInfo = new Connect(
                userInfo.getName(),
                userInfo.getGiven_name(),
                userInfo.getFamily_name(),
                userInfo.getMiddle_name(),
                userInfo.getNickname(),
                userInfo.getPreferred_username(),
                this.generateEmailTest(userInfo.getEmail(), accessToken),
                userInfo.getEmail_verified(),
                userInfo.getGender(),
                this.generateBirthdateTest(userInfo.getSub(), accessToken),
                userInfo.getZoneinfo(),
                userInfo.getLocale(),
                userInfo.getPhone_number(),
                userInfo.getPhone_number_verified(),
                userInfo.getUpdated_at(),
                this.generateSubTest(accessToken),
                userInfo.getTxn(),
                userInfo.getVerified_claims()
            );
        }
        return userInfo;
    }

    @Override
    public Identify getProfile(String accessToken) {
        Identify userProfile = super.getProfile(accessToken);
        if (userProfile != null) {
            userProfile = new Identify(
                userProfile.getTitle_prefix(),
                userProfile.getTitle_suffix(),
                userProfile.getGiven_name(),
                userProfile.getFamily_name(),
                userProfile.getMiddle_name(),
                userProfile.getPhone_number(),
                this.generateEmailTest(userProfile.getEmail(), accessToken),
                userProfile.getAddresses(),
                this.generateBirthdateTest(userProfile.getSub(), accessToken),
                userProfile.getAge(),
                userProfile.getDate_of_death(),
                userProfile.getGender(),
                userProfile.getBirthnumber(),
                userProfile.getBirthcountry(),
                userProfile.getBirthplace(),
                userProfile.getPrimary_nationality(),
                userProfile.getNationalities(),
                userProfile.getMaritalstatus(),
                userProfile.getIdcards(),
                userProfile.getPaymentAccounts(),
                userProfile.getPaymentAccountsDetails(),
                userProfile.getLimited_legal_capacity(),
                userProfile.getMajority(),
                userProfile.getPep(),
                userProfile.getUpdated_at(),
                this.generateSubTest(accessToken),
                userProfile.getTxn(),
                userProfile.getVerified_claims()
            );
        }
        return userProfile;
    }

    /**
     * Generate random email based on accessToken
     * @param originalEmail
     * @param accessToken
     * @return
     */
    private String generateEmailTest(String originalEmail, String accessToken) {
        String seed = getHash(accessToken);
        Random random = new Random(seed.hashCode());
        return originalEmail.substring(0, originalEmail.indexOf('@')) + "+" + StringUtils.generateRandomAlphanumeric(5, random) + originalEmail.substring(originalEmail.indexOf('@'));
    }

    /**
     * Generate random birthdate based on age range
     * @param sub
     * @param accessToken
     * @return
     */
    private String generateBirthdateTest(String sub, String accessToken) {
        String seed = getHash(accessToken);
        Random random = new Random(seed.hashCode());

        int[] ageRange = ageRangeMap.getOrDefault(sub, ageRangeMap.get("default"));
        int minAge = ageRange[0];
        int maxAge = ageRange[1];

        int randomAge = minAge + random.nextInt(maxAge - minAge + 1);
        LocalDate birthdate = LocalDate.now().minusYears(randomAge);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        return birthdate.format(formatter);
    }

    /**
     * Generate random sub
     * @param accessToken
     * @return
     */
    private String generateSubTest(String accessToken) {
        String seed = getHash(accessToken);
        Random random = new Random(seed.hashCode());

        return new UUID(random.nextLong(), random.nextLong()).toString();
    }

    /**
     * Generate hash from accessToken
     * @param accessToken
     * @return
     */
    private String getHash(String accessToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(accessToken.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating hash", e);
        }
    }
}
package cz.cas.lib.bankid_registrator.util;

import java.util.Random;
import org.apache.commons.lang3.RandomStringUtils;

public class RandomStringUtil
{
    /**
     * Generate random alphanumeric string of given length
     * @param length
     * @return
     */
    public static String generateRandomAlphanumeric(int length) {
        return RandomStringUtils.randomAlphanumeric(length);
    }

    /**
     * Generate random alphanumeric string of given length using given random generator
     * @param length
     * @param random
     * @return
     */
    public static String generateRandomAlphanumeric(int length, Random random) {
        char[] possibleCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(possibleCharacters[random.nextInt(possibleCharacters.length)]);
        }
        return sb.toString();
    }
}
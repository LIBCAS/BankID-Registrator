package cz.cas.lib.bankid_registrator.util;

import java.util.Random;
import org.apache.commons.lang3.RandomStringUtils;

public class StringUtils
{
    /**
     * Generates a random alphanumeric string of given length
     * @param length
     * @return
     */
    public static String generateRandomAlphanumeric(int length) {
        return RandomStringUtils.randomAlphanumeric(length);
    }

    /**
     * Generates a random alphanumeric string of given length using given random generator
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

    /**
     * Checks if a trimmed string is null or empty
     * @param str
     * @return
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Trims all strings in an array and checks if all are either null or empty
     * @param strArr
     * @return
     */
    public static boolean isEmpty(String... strArr) {
        for (String str : strArr) {
            if (!isEmpty(str)) {
                return false;
            }
        }
        return true;
    }
}
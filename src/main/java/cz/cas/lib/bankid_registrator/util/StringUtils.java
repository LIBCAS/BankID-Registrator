package cz.cas.lib.bankid_registrator.util;

import java.security.SecureRandom;
import java.util.Random;
import org.apache.commons.lang3.RandomStringUtils;

public class StringUtils
{
    private static final String SUPPORT_TICKET_ID_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int SUPPORT_TICKET_ID_LENGTH = 8;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
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

    /**
     * Generate a short customer-facing support ticket identifier.
     * Ambiguous characters such as I/1 and O/0 are intentionally excluded.
     */
    public static String generateSupportTicketId() {
        StringBuilder sb = new StringBuilder(SUPPORT_TICKET_ID_LENGTH);
        for (int i = 0; i < SUPPORT_TICKET_ID_LENGTH; i++) {
            sb.append(SUPPORT_TICKET_ID_CHARS.charAt(SECURE_RANDOM.nextInt(SUPPORT_TICKET_ID_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Formats a string so that it is capitalized if the input string was all uppercase.
     * @param str The string to format.
     * @return The formatted string.
     */
    public static String capitalizeIfUppercase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        if (str.equals(str.toUpperCase())) {
            return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
        }

        return str;
    }
}

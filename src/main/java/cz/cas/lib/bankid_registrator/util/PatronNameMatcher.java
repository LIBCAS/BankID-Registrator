package cz.cas.lib.bankid_registrator.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Comparison-only normalization; never used to rewrite a patron's name. */
public final class PatronNameMatcher {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private PatronNameMatcher() {}

    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        String spaced = WHITESPACE.matcher(name).replaceAll(" ").trim();
        return Normalizer.normalize(spaced.toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
    }
}

package cz.cas.lib.bankid_registrator.util;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Web utilities
 */
public class WebUtils
{

    /**
     * Get the current URL with URL parameters
     * @param request
     * @return
     */
    public static String getCurrentUrl(HttpServletRequest request) {
        return getCurrentUrl(request, new String[]{});
    }

    /**
     * Get the current URL with URL parameters, excluding specified parameters
     * @param request
     * @param excludeParams
     * @return
     */
    public static String getCurrentUrl(HttpServletRequest request, String... excludeParams) {
        Set<String> excludeSet = Arrays.stream(excludeParams).collect(Collectors.toSet());
        StringBuffer requestURL = request.getRequestURL();
        String queryString = request.getQueryString();

        if (queryString == null) {
            return requestURL.toString();
        } else {
            String filteredQueryString = Arrays.stream(queryString.split("&"))
                    .filter(param -> !excludeSet.contains(param.split("=")[0]))
                    .collect(Collectors.joining("&"));

            if (filteredQueryString.isEmpty()) {
                return requestURL.toString();
            } else {
                return requestURL.append('?').append(filteredQueryString).toString();
            }
        }
    }
}
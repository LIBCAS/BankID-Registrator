package cz.cas.lib.bankid_registrator.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
    /**
     * Converts a String date from the Aleph format 'yyyyMMdd' to the Thymeleaf 'yyyy-MM-dd' format.
     * 
     * @param dateStr the date String to convert.
     * @return the converted date String or null if parsing fails.
     */
    public static String convertAlephDate(String dateStr) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
        
        try {
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Converts a String date from the Thymeleaf 'yyyy-MM-dd' format to the Aleph 'yyyyMMdd' format.
     * 
     * @param dateStr the date String to convert.
     * @return the converted date String or null if parsing fails.
     */
    public static String convertThymeleafDate(String dateStr) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyyMMdd");
        
        try {
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}

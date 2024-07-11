package cz.cas.lib.bankid_registrator.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtils
{
    /**
     * Adds a specific number of days to a given date.
     * 
     * @param inputDate The date to which days will be added, as a String.
     * @param daysToAdd The number of days to add.
     * @param inputFormat The format of the input date string.
     * @param outputFormat The format for the output date string.
     */
    public static String addDaysToDateString(String inputDate, int daysToAdd, String inputFormat, String outputFormat) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(inputFormat);
        LocalDate date = LocalDate.parse(inputDate, inputFormatter);
        LocalDate newDate = date.plusDays(daysToAdd);
        return newDate.format(DateTimeFormatter.ofPattern(outputFormat));
    }

    /**
     * Adds a specific number of days to a given date.
     * 
     * @param date The date to which days will be added.
     * @param daysToAdd The number of days to add.
     * @param format The format of the date string.
     * @return The new date as a String after adding the specified number of days.
     */
    private static String addDaysToDate(LocalDate date, int daysToAdd, String format) {
        LocalDate newDate = date.plusDays(daysToAdd);
        return newDate.format(DateTimeFormatter.ofPattern(format));
    }

    /**
     * Adds a specific number of days to today's date.
     * 
     * @param daysToAdd The number of days to add to today.
     * @param format The format for the output date string.
     * @return The new date as a String after adding the specified number of days to today.
     */
    public static String addDaysToToday(int daysToAdd, String format) {
        return addDaysToDate(LocalDate.now(), daysToAdd, format);
    }

    /**
     * Adds a specific number of days to a specific date.
     * 
     * @param dateString The date to which days will be added, as a String.
     * @param daysToAdd The number of days to add.
     * @param inputFormat The format of the input date string.
     * @param outputFormat The format for the output date string.
     * @return The new date as a String after adding the specified number of days, or null if parsing fails.
     */
    public static String addDaysToSpecificDate(String dateString, int daysToAdd, String inputFormat, String outputFormat) {
        try {
            LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern(inputFormat));
            return addDaysToDate(date, daysToAdd, outputFormat);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Checks if a date in a given format is today
     * @param dateString
     * @param format
     * @return
     */
    public static boolean isDateToday(String dateString, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        LocalDate date = LocalDate.parse(dateString, formatter);
        return date.isEqual(LocalDate.now());
    }

    /**
     * Checks if a date in a given format is before the current date.
     * 
     * @param dateString The date string to check.
     * @param format The format of the date string.
     * @return true if the date is before today, false otherwise.
     */
    public static boolean isDateExpired(String dateString, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        LocalDate date = LocalDate.parse(dateString, formatter);
        return date.isBefore(LocalDate.now());
    }

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
     * Converts a String date formats
     * @param dateStr the date String to convert.
     * @return
     */
    public static String convertDateFormat(String dateStr, String inputFormat, String outputFormat) {
        SimpleDateFormat inputFormatFinal = new SimpleDateFormat(inputFormat);
        SimpleDateFormat outputFormatFinal = new SimpleDateFormat(outputFormat);

        try {
            Date date = inputFormatFinal.parse(dateStr);
            return outputFormatFinal.format(date);
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

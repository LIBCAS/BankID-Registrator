package cz.cas.lib.bankid_registrator.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateUtils
{
    private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);

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
     * Adds a specific number of years to a given date.
     * 
     * @param inputDate The date to which years will be added, as a String.
     * @param yearsToAdd The number of years to add.
     * @param inputFormat The format of the input date string.
     * @param outputFormat The format for the output date string.
     * @return The new date as a String after adding the specified number of years.
     */
    public static String addYearsToDateString(String inputDate, int yearsToAdd, String inputFormat, String outputFormat) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(inputFormat);
        LocalDate date = LocalDate.parse(inputDate, inputFormatter);
        LocalDate newDate = date.plusYears(yearsToAdd);
        return newDate.format(DateTimeFormatter.ofPattern(outputFormat));
    }

    /**
     * Adds a specific number of years to today's date.
     * 
     * @param yearsToAdd The number of years to add to today.
     * @param format The format for the output date string.
     * @return The new date as a String after adding the specified number of years to today.
     */
    public static String addYearsToToday(int yearsToAdd, String format) {
        return addYearsToDateString(LocalDate.now().format(DateTimeFormatter.ofPattern(format)), yearsToAdd, format, format);
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
     * @return true if the date is before today (expired), false otherwise.
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
     * @param inputFormat - format of the `dateStr`, for example: "yyyy-MM-dd"
     * @param outputFormat - desired format, for example: "dd-MM-yyyy"
     * @return the converted date String or null if parsing fails.
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

    /**
     * Converts a MySQL datetime-formatted string (which was converted for Spring) into a human-friendly string.
     * It converts the format 'dd.MM.yy H:mm' to 'dd/MM/yyyy HH:mm'.
     * @param originalDateTime
     * @return
     */
    public static String convertDateTimeFormat(String originalDateTime)
    {
        String inputFormat;
        if (originalDateTime.contains("AM") || originalDateTime.contains("PM")) {
            inputFormat = "M/dd/yy, h:mm a";
        } else if (originalDateTime.contains("T")) {
            inputFormat = "yyyy-MM-dd'T'HH:mm:ss";
        } else {
            inputFormat = "dd.MM.yy H:mm";
        }

        return DateUtils.convertDateFormat(originalDateTime, inputFormat, "dd/MM/yyyy HH:mm");
    }

    /**
     * Checks if the given date is less than or equal to one month from today.
     * @param dateString The date to check, as a String.
     * @param format The format of the input date string.
     * @return true if the date is less than or equal to one month from today, false otherwise.
     */
    public static boolean isLessThanOrEqualToOneMonthFromToday(String dateString, String format)
    {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            LocalDate givenDate = LocalDate.parse(dateString, formatter);
            LocalDate today = LocalDate.now();
            LocalDate oneMonthFromToday = today.plusMonths(1);

            return !givenDate.isAfter(oneMonthFromToday);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets the current date and time in the specified format.
     * 
     * @param format The format for the date and time string.
     * @return The current date and time as a String.
     */
    public static String getDateTime(String format) {
        return new SimpleDateFormat(format).format(new Date());
    }

    /**
     * Gets the last date of the current month in the specified format.
     * 
     * @param format The format for the date string.
     * @return The last date of the current month as a String.
     */
    public static String getLastDateOfCurrentMonth(String format) {
        return LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).format(DateTimeFormatter.ofPattern(format));
    }

    /**
     * Checks if a given date string adheres to a specific date format.
     *
     * @param dateStr The date string to check.
     * @param format The expected date format.
     * @return true if the date string matches the format, false otherwise.
     */
    public static boolean isValidDateFormat(String dateStr, String format) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            LocalDate.parse(dateStr, formatter);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calculate person's age on a given date (if specified) based on their birth date
     * 
     * @param birthDate - person's birth date
     * @param birthDateFormat - person's birth date format, default is 'yyyyMMdd'
     * @param onDate - calculation date, default is today
     * @param onDateFormat - calculation date format, default is 'yyyyMMdd'
     * @return person's age in years
     */
    public static int calculateAge(String birthDate, String birthDateFormat, String onDate, String onDateFormat) {
        try {
            String defaultFormat = "yyyyMMdd";

            DateTimeFormatter birthFormatter = DateTimeFormatter.ofPattern(
                birthDateFormat != null && !birthDateFormat.isEmpty() ? birthDateFormat : defaultFormat
            );
            DateTimeFormatter onFormatter = DateTimeFormatter.ofPattern(
                onDateFormat != null && !onDateFormat.isEmpty() ? onDateFormat : defaultFormat
            );

            LocalDate birthLocalDate = LocalDate.parse(birthDate, birthFormatter);
            LocalDate calculationDate = (onDate != null && !onDate.isEmpty()) ? LocalDate.parse(onDate, onFormatter) : LocalDate.now();

            return calculationDate.getYear() - birthLocalDate.getYear() - (calculationDate.getDayOfYear() < birthLocalDate.getDayOfYear() ? 1 : 0);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}

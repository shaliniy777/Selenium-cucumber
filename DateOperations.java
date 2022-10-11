/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * The type Date operations.
 */
public class DateOperations {

  private String defaultTimezone;

  /**
   * Instantiates a new Date operations.
   */
  public DateOperations() {
    defaultTimezone = Config.get("system.timezone");
  }

  /**
   * Calculate date string.
   *
   * @param secondsToAdd      the seconds to add
   * @param minutesToAdd      the minutes to add
   * @param hoursToAdd        the hours to add
   * @param daysToAdd         the days to add
   * @param monthsToAdd       the months to add
   * @param yearsToAdd        the years to add
   * @param dayOfMonth        the day of month
   * @param returnDateFormat  the return date format
   * @param initialDate       the initial date
   * @param initialDateFormat the initial date format
   * @param timezoneStr       the timezone str
   * @return the string
   * @throws ParseException the parse exception
   */
  public String calculateDate(String secondsToAdd, String minutesToAdd, String hoursToAdd, String daysToAdd,
      String monthsToAdd, String yearsToAdd,
      String dayOfMonth, String returnDateFormat, String initialDate, String initialDateFormat, String timezoneStr)
      throws ParseException {

    timezoneStr = timezoneStr != null ? timezoneStr : defaultTimezone;
    initialDateFormat = initialDateFormat != null ? initialDateFormat : "yyyy-MM-dd";

    TimeZone timeZone = TimeZone.getTimeZone(timezoneStr);
    Calendar calendar = Calendar.getInstance();
    Date date;
    SimpleDateFormat dateFormat;
    if (initialDate == null) {
      date = new Date();
    } else {
      dateFormat = new SimpleDateFormat(initialDateFormat);
      date = dateFormat.parse(initialDate);
    }
    calendar.setTimeZone(timeZone);
    calendar.setTime(date);

    if (secondsToAdd != null) {
      calendar.add(Calendar.SECOND, Integer.parseInt(secondsToAdd));
    }
    if (minutesToAdd != null) {
      calendar.add(Calendar.MINUTE, Integer.parseInt(minutesToAdd));
    }
    if (daysToAdd != null) {
      calendar.add(Calendar.DATE, Integer.parseInt(daysToAdd));
    }
    if (monthsToAdd != null) {
      calendar.add(Calendar.MONTH, Integer.parseInt(monthsToAdd));
    }
    if (yearsToAdd != null) {
      calendar.add(Calendar.YEAR, Integer.parseInt(yearsToAdd));
    }
    if (dayOfMonth != null) {
      calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dayOfMonth));
    }
    if (hoursToAdd != null) {
      calendar.add(Calendar.HOUR_OF_DAY, Integer.parseInt(hoursToAdd));
    }

    dateFormat = new SimpleDateFormat(returnDateFormat);
    dateFormat.setTimeZone(timeZone);

    return dateFormat.format(calendar.getTime());
  }

  /**
   * Calculate date string.
   *
   * @param initialDate      the initial date
   * @param returnDateFormat the return date format
   * @return the string
   * @throws ParseException the parse exception
   */
  public String calculateDate(String initialDate, String returnDateFormat) throws ParseException {
    return calculateDate(null, null, null, null, null, null,
                         null, returnDateFormat, initialDate, null, null);
  }

  /**
   * Add minutes to date string.
   *
   * @param minutesToAdd     the minutes to add
   * @param returnDateFormat the return date format
   * @param initialDate      the initial date
   * @return the string
   * @throws ParseException the parse exception
   */
  public String addMinutesToDate(String minutesToAdd, String returnDateFormat, String initialDate)
      throws ParseException {
    return calculateDate(null, minutesToAdd, null, null, null, null,
                         null, returnDateFormat, initialDate, null, null);
  }

  /**
   * Add hours to date string.
   *
   * @param hoursToAdd       the hours to add
   * @param returnDateFormat the return date format
   * @param initialDate      the initial date
   * @return the string
   * @throws ParseException the parse exception
   */
  public String addHoursToDate(String hoursToAdd, String returnDateFormat, String initialDate) throws ParseException {
    return calculateDate(null, null, hoursToAdd, null, null, null,
                         null, returnDateFormat, initialDate, null, null);
  }

  /**
   * Add days to date string.
   *
   * @param daysToAdd        the days to add
   * @param returnDateFormat the return date format
   * @param initialDate      the initial date
   * @return the string
   * @throws ParseException the parse exception
   */
  public String addDaysToDate(String daysToAdd, String returnDateFormat, String initialDate) throws ParseException {
    return calculateDate(null, null, null, daysToAdd, null, null,
                         null, returnDateFormat, initialDate, null, null);
  }

  /**
   * Add months to date string.
   *
   * @param monthsToAdd      the months to add
   * @param returnDateFormat the return date format
   * @param initalDate       the inital date
   * @return the string
   * @throws ParseException the parse exception
   */
  public String addMonthsToDate(String monthsToAdd, String returnDateFormat, String initalDate) throws ParseException {
    return calculateDate(null, null, null, null, monthsToAdd, null,
                         null, returnDateFormat, initalDate, null, null);
  }

  /**
   * Add years to date string.
   *
   * @param yearsToAdd       the years to add
   * @param returnDateFormat the return date format
   * @param initialDate      the initial date
   * @return the string
   * @throws ParseException the parse exception
   */
  public String addYearsToDate(String yearsToAdd, String returnDateFormat, String initialDate) throws ParseException {
    return calculateDate(null, null, null, null, null, yearsToAdd,
                         null, returnDateFormat, initialDate, null, null);
  }

}
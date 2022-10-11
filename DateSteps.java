/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.steps;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.experian.automation.cucumber.configuration.ConfigurationProperties;
import com.experian.automation.helpers.DateOperations;
import com.experian.automation.helpers.Variables;
import com.experian.automation.helpers.databases.DBOperationsFactory;
import com.experian.automation.logger.Logger;
import com.experian.automation.steps.exceptions.DateStepException;
import com.experian.automation.transformers.VariablesTransformer;
import io.cucumber.java.en.And;
import java.security.InvalidParameterException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.testng.TestException;

/**
 * The type Date steps.
 */
@ConfigurationProperties(
    properties = {
        "system.timezone"
    }
)
public class DateSteps {

  private static final String SAVE_VAR_MESSAGE = "Saved variable - %s = %s";
  private static final String DATE_FORMAT_LITERAL = "dateFormat";

  private final Logger logger = Logger.getLogger(this.getClass());
  private final String stepDataDateFormat = "dd/MM/yyyy HH:mm:ss.SSS";

  /**
   * Instantiates a new Date steps.
   *
   * @throws Exception the exception
   */
  public DateSteps() throws Exception {
    // Blank Constructor
  }

  /**
   * Save date format.
   *
   * @param dateFormatVarName the date format var name
   * @param dateFormatString  the date format string
   * @throws Throwable the throwable
   */
  @And("^I save date format (.*) with value (.*)$")
  public void saveDateFormat(String dateFormatVarName, String dateFormatString) throws Throwable {
    Variables.set(dateFormatVarName, dateFormatString);
  }

  /**
   * Sets date variable.
   *
   * @param dateVarName the date var name
   * @param props       the props
   * @throws Throwable the throwable
   */
  @And("^I set date variable (.*) with properties:$")
  public void setDateVariable(String dateVarName, Map<String, String> props) throws Throwable {
    props = VariablesTransformer.transformMap(props);

    DateOperations dateOperations = new DateOperations();
    String dateStr = dateOperations.calculateDate(
        props.getOrDefault("secondsToAdd", null),
        props.getOrDefault("minutesToAdd", null),
        props.getOrDefault("hoursToAdd", null),
        props.getOrDefault("daysToAdd", null),
        props.getOrDefault("monthsToAdd", null),
        props.getOrDefault("yearsToAdd", null),
        props.getOrDefault("dayOfMonth", null),
        props.getOrDefault("returnDateFormat", stepDataDateFormat),
        props.getOrDefault("initialDate", null),
        props.getOrDefault("initialDateFormat", null),
        props.getOrDefault("timezone", null)
    );

    Variables.set(dateVarName, dateStr);
    logger.info(String.format("Set %s = %s", dateVarName, Variables.get(dateVarName)));
  }

  /**
   * Reformat date string.
   *
   * @param date               the date
   * @param originalDateFormat the original date format
   * @param format             the format
   * @param variableName       the variable name
   * @throws Throwable the throwable
   */
  @And("^I change the format of date (.*?) from (.*) to (.*) and save it to variable (.*)$")
  public void reformatDateString(String date, String originalDateFormat, String format, String variableName)
      throws Throwable {

    date = VariablesTransformer.transformSingleValue(date);
    originalDateFormat = VariablesTransformer.transformSingleValue(originalDateFormat);
    format = VariablesTransformer.transformSingleValue(format);

    DateFormat originalFormat = new SimpleDateFormat(originalDateFormat);
    DateFormat targetFormat = new SimpleDateFormat(format);
    Date orDate = originalFormat.parse(date);
    String dateValue = targetFormat.format(orDate);

    Variables.set(variableName, dateValue);
    logger.info(String.format(SAVE_VAR_MESSAGE, variableName, Variables.get(variableName)));
  }

  /**
   * Reformat date string.
   *
   * @param date         is string representation of the date to be formatted.
   * @param language     is the criteria to define the locale and the date formatting.
   * @param variableName is the name of the variable the formatted date will be saved to.
   */
  /*
   * Usage Example(s):
   *      I format date ${today} by locale for language de and save it to variable todayGermanFormat
   * Example values:
   *      today        = a variable name referring to 06/12/2021 10:38:45.464 date time value.
   *      language     = de, en, bg, cs, etc.
   *      variableName = a string value by choice.
   */
  @And("^I format date (.*) by locale for language (.*) and save it to variable (.*)$")
  public void reformatDate(String date, String language, String variableName) {
    Locale locale = Locale.forLanguageTag(language);
    String defaultDateFormat = "dd/MM/yyyy"; // NOI18N

    date = VariablesTransformer.transformSingleValue(date);

    DateFormat targetFormat;
    DateFormat originalFormat;
    String formattedDate;
    Date nonFormattedDate;

    if (language.equalsIgnoreCase("en")) { // NOI18N
      targetFormat = new SimpleDateFormat(defaultDateFormat);
      nonFormattedDate = parseDate(date, targetFormat);
    } else {
      targetFormat = DateFormat.getDateInstance(SimpleDateFormat.SHORT, locale);
      originalFormat = new SimpleDateFormat(defaultDateFormat);
      nonFormattedDate = parseDate(date, originalFormat);
    }

    formattedDate = targetFormat.format(nonFormattedDate);

    Variables.set(variableName, formattedDate);
    logger.info(String.format(SAVE_VAR_MESSAGE, variableName, Variables.get(variableName)));
  }

  private Date parseDate(String date, DateFormat targetFormat) {
    try {
      return targetFormat.parse(date);
    } catch (ParseException exception) {
      throw new DateStepException("There was an error while parsing the date.", exception); // NOI18N
    }
  }

  /**
   * Sets date string.
   *
   * @param date         the date
   * @param format       the format
   * @param variableName the variable name
   * @throws Throwable the throwable
   */
  @And("^I format date (.*?) using (.*) and save it to variable (.*)$")
  public void setDateString(String date, String format, String variableName) throws Throwable {

    date = VariablesTransformer.transformSingleValue(date);
    format = VariablesTransformer.transformSingleValue(format);

    reformatDateString(date, stepDataDateFormat, format, variableName);
  }

  /**
   * Format date to timestamp.
   *
   * @param originalDate the original date
   * @param format       the format
   * @param variableName the variable name
   * @throws Throwable the throwable
   */
  /* Usage Example(s):
   *  And I convert 15/12/2077 with format dd/MM/yyyy to timestamp and save the result as variable dateVar
   *  And I convert 15/12/2077 15:30:00 with format dd/MM/yyyy hh:mm:ss to timestamp and save the result as variable dateVar
   */
  @And("^I convert (.*) with format (.*) to timestamp and save the result as variable (.*)$")
  public void formatDateToTimestamp(String originalDate, String format, String variableName) throws Throwable {

    originalDate = VariablesTransformer.transformSingleValue(originalDate);
    format = VariablesTransformer.transformSingleValue(format);
    variableName = VariablesTransformer.transformSingleValue(variableName);

    Date formattedDate = new SimpleDateFormat(format).parse(originalDate);
    String dateValue = String.valueOf(formattedDate.getTime());

    Variables.set(variableName, dateValue);
    logger.info(String.format(SAVE_VAR_MESSAGE, variableName, Variables.get(variableName)));
  }

  /**
   * Days diff.
   *
   * @param dateFormatVarName the date format var name
   * @param diffType          the diff type
   * @param hasPassed         the has passed
   * @param dataTable         the data table
   * @throws Throwable the throwable
   */
  /* Usage Example(s):
   *   And I save as variable diffHours the number of hours since:
   *     | dateVariable | ${today} 13:00:00         |
   *     | dateFormat   | ${dateFormatter} hh:mm:ss |
   *
   *   And I save as variable initDate1 the number of days since:
   *     | days   | 01   |
   *     | months | 01   |
   *     | year   | 2014 |
   *
   */
  @And("^I save as variable (.*) the number of (days|hours)( passed)? since:$")
  public void daysDiff(String dateFormatVarName, String diffType, String hasPassed, Map<String, String> dataTable)
      throws Throwable {
    Date firstDate;
    dataTable = VariablesTransformer.transformMap(dataTable);

    if (dataTable.containsKey("dateVariable")) {
      DateFormat format = new SimpleDateFormat(dataTable.get(DATE_FORMAT_LITERAL));
      firstDate = format.parse(dataTable.get("dateVariable"));
    } else {
      Calendar cal = Calendar.getInstance();
      cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dataTable.get("days")));
      cal.set(Calendar.MONTH, Integer.parseInt(dataTable.get("months")) - 1);
      cal.set(Calendar.YEAR, Integer.parseInt(dataTable.get("year")));
      cal.set(Calendar.MILLISECOND, 0);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.HOUR, 0);
      firstDate = cal.getTime();
    }

    Date secondDate = new Date();
    long daysDiffMile = 1000 * 60 * 60;

    if (diffType.equals("days")) {
      daysDiffMile *= 24;
    }

    long subtractedDates = StringUtils.isEmpty(hasPassed) ? firstDate.getTime() - secondDate.getTime()
        : secondDate.getTime() - firstDate.getTime();
    long diff = Math.round(subtractedDates / daysDiffMile);
    logger.info(String.format("Saving [%s = %s]", dateFormatVarName, diff));
    Variables.set(dateFormatVarName, Long.toString(diff));
  }

  /**
   * Add current timezone postfix.
   *
   * @param variable  the variable
   * @param inputDate the input date
   * @throws Throwable the throwable
   */
  @And("^I save (.*) variable with added current time zone postfix for date (.*)$")
  public void addCurrentTimezonePostfix(String variable, String inputDate) throws Throwable {
    SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    String pattern = "(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})\\+\\d{4}";
    Date aDate;

    if (inputDate.matches(pattern)) {
      aDate = df2.parse(inputDate);
    } else {
      aDate = df1.parse(inputDate);
    }

    String convertedDate = df2.format(aDate);

    String convertedDateTimePart = convertedDate.substring(0, 19);
    String convertedDateTimezonePart = convertedDate.substring(19);

    String timezonePostfix;

    if ("+0000".equals(convertedDateTimezonePart)) {
      timezonePostfix = "Z";
    } else {
      timezonePostfix = convertedDateTimezonePart.substring(0, 3) + ":" + convertedDateTimezonePart.substring(3);
    }

    String var = convertedDateTimePart + timezonePostfix;
    Variables.set(variable, var);
  }

  /**
   * Adjust timezone.
   *
   * @param variable  the variable
   * @param inputDate the input date
   * @throws Throwable the throwable
   */
  @And("^I save correct timezone (.*) for datetime string (.*)$")
  public void adjustTimezone(String variable, String inputDate) throws Throwable {
    addCurrentTimezonePostfix(variable, inputDate);
  }


  /**
   * Shift date.
   *
   * @param datetime            the datetime
   * @param initialTimezone     the initial timezone
   * @param initialFormat       the initial format
   * @param destinationTimezone the destination timezone
   * @param destinationFormat   the destination format
   * @param variableName        the variable name
   * @throws Throwable the throwable
   */
/*
  Example usages:
    I shift datetime string 2019-08-28 09:53:08.151 from timezone Europe/London with format yyyy-MM-dd HH:mm:ss.SSS to timezone Europe/Moscow and save it to variable dtMoscow
    I shift datetime string 2019-01-01 11:02:33 from timezone Asia/Katmandu to timezone UTC and save it to variable todayUTC
    I shift datetime string ${var} from timezone America/New_York with format yyyy-MM-dd HH:mm:ss.S to timezone Atlantic/Reykjavik with format yyyy-MM-dd'T'HH:mm:ss.SSS and save it to variable dtReykjavik
   */
  @And("^I shift datetime string (.*) from timezone (.*?)(?: with format (.*))? to timezone (.*?)(?: with format (.*))? and save it to variable (.*)$")
  public void shiftDate(String datetime, String initialTimezone, String initialFormat, String destinationTimezone,
      String destinationFormat, String variableName) throws Throwable {

    datetime = VariablesTransformer.transformSingleValue(datetime);
    initialTimezone = VariablesTransformer.transformSingleValue(initialTimezone);
    initialFormat = VariablesTransformer.transformSingleValue(initialFormat);
    destinationTimezone = VariablesTransformer.transformSingleValue(destinationTimezone);
    destinationFormat = VariablesTransformer.transformSingleValue(destinationFormat);

    final String defaultPattern = "yyyy-MM-dd HH:mm:ss";

    if (initialFormat == null) {
      initialFormat = defaultPattern;
    }

    if (destinationFormat == null) {
      destinationFormat = initialFormat;
    }

    DateTimeFormatter initialDateTimeFormatter = DateTimeFormatter.ofPattern(initialFormat).withZone(
        ZoneId.of(initialTimezone));
    ZonedDateTime initialDateTimeZone = ZonedDateTime.parse(datetime, initialDateTimeFormatter);

    DateTimeFormatter destinationDateTimeFormatter = DateTimeFormatter.ofPattern(destinationFormat);
    ZonedDateTime destinationTz = initialDateTimeZone.withZoneSameInstant(ZoneId.of(destinationTimezone));

    String resultDateTimeZone = destinationDateTimeFormatter.format(destinationTz);

    Variables.set(variableName, resultDateTimeZone);
    logger.info(String.format(SAVE_VAR_MESSAGE, variableName, Variables.get(variableName)));

  }

  /**
   * Refresh value of date.
   *
   * @param dateVariable the date variable
   * @param props        the props
   * @throws Throwable the throwable
   */
/*
  Can be used to get a date with altered values if needed - base of date is today.
  Example usages:
      Getting an updated today date
          And I save date string to variable today using:
            |dateFormat | dd/MM/yyyy|
      Getting a modified today date
          And I save date string to variable today using:
            |dateFormat | dd/MM/yyyy|
            |daysToAdd  | 1         |
   Required parameters: dateFormat with valid pattern
   */
  @And("^I save date string to variable (.*) using:$")
  public void refreshValueOfDate(String dateVariable, Map<String, String> props) throws Throwable {
    if (!props.keySet().contains(DATE_FORMAT_LITERAL)) {
      throw new InvalidParameterException("Parameter dateFormat is required");
    }
    props = VariablesTransformer.transformMap(props);

    setDateVariable(dateVariable, props);
    setDateString(Variables.get(dateVariable), props.get(DATE_FORMAT_LITERAL), dateVariable);
  }

  /**
   * Sets date string to db format.
   *
   * @param date         the date
   * @param format       the format
   * @param variableName the variable name
   * @throws Throwable the throwable
   */
/*
      Example usage:
          And I format date ${dateToValidate} with ${dateFormatter} to DB date and save it to variable dbRecDateConverted
   */
  @And("^I format date (.*?) with (.*) to DB date and save it to variable (.*)$")
  public void setDateStringToDbFormat(String date, String format, String variableName) throws Throwable {

    date = VariablesTransformer.transformSingleValue(date);
    format = VariablesTransformer.transformSingleValue(format);

    DBOperationsFactory dbOperationsFactory = DBOperationsFactory.getDbOperationsFactoryInstance();
    reformatDateString(date, format, dbOperationsFactory.operationsFactory().getDefaultDateFormat(), variableName);
  }

  /**
   * Format timestamp to date.
   *
   * @param timestamp    the timestamp
   * @param format       the date format
   * @param variableName the variable name
   */
/*
      Example usage:
          And I format timestamp 1.582623897268E9 using yyyy-MM-dd HH:mm and save it to variable formattedDate
   */
  @And("^I format timestamp (.*) using (.*) and save it to variable (.*)$")
  public void formatTimestampToDate(String timestamp, String format, String variableName) {
    timestamp = VariablesTransformer.transformSingleValue(timestamp);
    long epoch = Double.valueOf(timestamp).longValue();
    logger.info(String.format("timestamp: %s epoch: %s", timestamp, epoch));
    Instant instant = Instant.ofEpochSecond(epoch);
    ZonedDateTime destinationTz = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
    String resultDateTimeZone = DateTimeFormatter.ofPattern(format).format(destinationTz);
    Variables.set(variableName, resultDateTimeZone);
    logger.info(String.format("variableName: %s = %s", variableName, Variables.get(variableName)));
  }

  /**
   * Usage example:
   * And I convert date ${sysdateplus1d} with date format ${dateFormat} to epoch nanoseconds and save it to variable sysdateplus1d
   * <p>
   * Convert date to epoch seconds/milliseconds/nanoseconds string
   *
   * @param date         Date to be converted
   * @param dateFormat   Date format of the Date to be converted
   * @param unit         Unit of the epoch date string. Leave empty will default to seconds. (|milli|nano)
   * @param variableName Variable name to save the epoch seconds/milliseconds/nanoseconds string
   */
  @And("^I convert date (.*) with date format (.*) to epoch (|milli|nano)seconds and save it to variable (.*)$")
  public void convertDateToEpoch(String date, String dateFormat, String unit, String variableName) {
    date = VariablesTransformer.transformSingleValue(date);
    dateFormat = VariablesTransformer.transformSingleValue(dateFormat);
    unit = VariablesTransformer.transformSingleValue(unit);

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateFormat.trim());
    ZonedDateTime zdt;
    try {
      zdt = ZonedDateTime.parse(date, dtf);
    } catch (DateTimeParseException ex) {
      if (ex.getMessage().contains("Unable to obtain ZonedDateTime")) { // NOI18N
        ZoneId zoneId = ZoneId.systemDefault();
        logger.warn(
            String.format("Timezone not provided in date format, using system default zone: %s", // NOI18N
                          zoneId));
        zdt = ZonedDateTime.parse(date, dtf.withZone(zoneId));
      } else {
        throw ex;
      }
    }

    String epochStr;
    Instant epochInstant = zdt.toInstant();
    switch (unit) {
      case "":
        epochStr = Long.toString(epochInstant.getEpochSecond());
        break;
      case "milli": // NOI18N
        epochStr = Long.toString(epochInstant.toEpochMilli());
        break;
      case "nano": // NOI18N
        String epochNano = Integer.toString(epochInstant.getNano());
        while (epochNano.length() < 9) {
          epochNano = "0".concat(epochNano); // NOI18N
        }
        epochStr = Long.toString(epochInstant.getEpochSecond()).concat(epochNano);
        break;
      default:
        throw new TestException(String.format("Epoch unit not support. Got: %s", unit)); // NOI18N
    }
    logger.info(String.format("timestamp: %s epoch: %s", date, epochStr)); // NOI18N

    Variables.set(variableName, epochStr);
    logger.info(String.format("variableName: %s = %s", variableName, Variables.get(variableName))); // NOI18N
  }

  /**
   * Compare timestamps
   *
   * @param firstTimestamp  First timestamp to compare
   * @param compareType     How we are going to compare the variable <, = or >
   * @param secondTimestamp Second timestamp to compare
   *                        Usage example(s):
   *                        And I verify that timestamp timestampAfterRefresh is greater than timestampBeforeRefresh
   */
  @And("^I verify that timestamp (.*) is (greater than|equal to|lesser than) (.*)$")
  public void compareTimeStamp(String firstTimestamp, String compareType, String secondTimestamp) {
    firstTimestamp = VariablesTransformer.transformSingleValue(firstTimestamp);
    secondTimestamp = VariablesTransformer.transformSingleValue(secondTimestamp);
    long initialTimestamp = Long.parseLong(firstTimestamp);
    long finalTimestamp = Long.parseLong(secondTimestamp);
    if (compareType.equals("greater than")) {//NOI18N
      assertTrue(initialTimestamp > finalTimestamp, "Date comparison failed as timestamp 1: "
          + initialTimestamp + " is not greater than timestamp 2: " + finalTimestamp);// NOI18N
    } else if (compareType.equals("equal to")) {//NOI18N
      assertTrue(initialTimestamp == finalTimestamp, "Date comparison failed as timestamp 2: "
          + finalTimestamp + " is not equal to timestamp 1: " + initialTimestamp);// NOI18N
    } else if (compareType.equals("lesser than")) {//NOI18N
      assertTrue(initialTimestamp < finalTimestamp, "Date comparison failed as timestamp 1: "
          + initialTimestamp + " is not lesser than timestamp 2: " + finalTimestamp);// NOI18N
    }
  }
}
/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import java.util.Calendar;
import java.util.Date;

public class CronOperations {

  /**
   * Adds additional time in seconds to the current time and transforms in in cron expression
   * @param date the desired delay in seconds
   * @return cron expression with a desired delay
   */

  public static String getCronExpression(Date date){
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    final String cronExpressionValue = calendar.get(Calendar.SECOND) + " " + calendar.get(Calendar.MINUTE) + " * ? * * *";
    return cronExpressionValue;
  }
}
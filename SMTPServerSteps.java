/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.steps;

import com.dumbster.smtp.SmtpMessage;
import com.experian.automation.helpers.SMTPOperations;
import com.experian.automation.helpers.Variables;
import com.experian.automation.logger.Logger;
import com.experian.automation.transformers.VariablesTransformer;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringEscapeUtils;
import org.testng.asserts.SoftAssert;

/**
 * The type Smtp server steps.
 */
public class SMTPServerSteps {

  private final Logger logger = Logger.getLogger(this.getClass());

  /**
   * Instantiates a new Smtp server steps.
   */
  public SMTPServerSteps() {
    // Blank Constructor
  }

  /**
   * Operate smtp server.
   *
   * @param command    the command
   * @param portNumber the port number
   * @throws Throwable the throwable
   */
  /*
   * Example Usages:
   *      And I start SMTP server
   *      And I start SMTP server on port: 587
   *      And I stop SMTP server
   */
  @And("^I (start|stop) SMTP server(?: on port: (.*))?$")
  public void operateSMTPServer(String command, String portNumber) throws Throwable {
    SMTPOperations smtpOperations = new SMTPOperations();
    if (command.equals("start")) {
      if (portNumber != null) {
        smtpOperations.startSMTPServer(Integer.parseInt(portNumber));
      } else {
        smtpOperations.startSMTPServer();
      }
    } else if (command.equals("stop")) {
      smtpOperations.stopSMTPServer();
    }
  }

  /**
   * Verify multiple received email values.
   *
   * @param table the table
   * @throws Exception the exception
   */
  /*
   * Usage example :
   *   I verify received emails headers and contents:
   *   |To           |From            |Subject |Email contents include Regex Pattern|
   *   |Pooja@exp.com|test@appollo.org|Info    |^This is actual message$            |
   *   |Pooja@exp.com|test@appollo.org|Info1   |^This.*                             |
   *
   *   or
   *   I verify received emails headers and contents:
   *   |To           |From            |Subject |
   *   |Pooja@exp.com|test@appollo.org|Info    |
   *   |Pooja@exp.com|test@appollo.org|Info1   |
   */
  @And("^I verify received emails headers and contents:?$")
  public void verifyMultipleReceivedEmailValues(DataTable table) throws Exception {

    List<Map<String, String>> dataTable = table.asMaps(String.class, String.class);

    SMTPOperations smtpo = new SMTPOperations();
    Iterator it = smtpo.getReceivedEmails(dataTable.size());

    SoftAssert softAssert = new SoftAssert();

    for (Map<String, String> pair : dataTable) {
      pair = VariablesTransformer.transformMap(pair);
      SmtpMessage email = (SmtpMessage) it.next();

      softAssert.assertEquals(email.getHeaderValue("To"), pair.get("To"));
      softAssert.assertEquals(email.getHeaderValue("From"), pair.get("From"));
      softAssert.assertEquals(email.getHeaderValue("Subject"), pair.get("Subject"));

      String expectedBodyRegex = pair.get("Email contents include Regex Pattern");
      String actualBody = StringEscapeUtils.escapeJava(email.getBody().trim());
      if (expectedBodyRegex != null) {
        softAssert.assertTrue(Pattern.matches(expectedBodyRegex, actualBody),
                   String.format("Email contents mismatch. Expected regex: %s. Got: %s.", expectedBodyRegex, // NOI18N
                                 actualBody));
      }
    }
    softAssert.assertAll();
  }
  /*
   * Usage example :
   *   I save content matching regex (?<=-{32}).*(?=-{32}) of the received email to variable oneTimePassword
   */
  @And("^I save content matching regex (.*) of the received email to variable (.*)$")
  public void returnMailBodyMatching(String regex, String variable) throws Exception {

    SMTPOperations smtpo = new SMTPOperations();
    Iterator it = smtpo.getReceivedEmails(1);
    SmtpMessage email = (SmtpMessage) it.next();

    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(email.getBody());
    m.find();
    Variables.set(variable, m.group());
  }
}
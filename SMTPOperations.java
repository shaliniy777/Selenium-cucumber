/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import com.dumbster.smtp.SimpleSmtpServer;
import java.util.Iterator;
import com.experian.automation.logger.Logger;

/**
 * The type Smtp operations.
 */
public class SMTPOperations {

  /**
   * The Config.
   */
  public Config config;
  private final Logger logger = Logger.getLogger(this.getClass());
  private static SimpleSmtpServer simpleSmtpServer;

  /**
   * Instantiates a new Smtp operations.
   */
  public SMTPOperations() {
    // Blank Constructor
  }

  /**
   * Start smtp server.
   *
   * @throws Exception the exception
   */
  public void startSMTPServer() throws Exception {
    this.startSMTPServer(SimpleSmtpServer.DEFAULT_SMTP_PORT);
  }

  /**
   * Start smtp server.
   *
   * @param portNumber the port number
   * @throws Exception the exception
   */
  public void startSMTPServer(int portNumber) throws Exception {
    NetworkOperations netOps = new NetworkOperations(portNumber);
    if (netOps.checkPortAvailability(0, true)) {
      throw new Exception("Port: " + portNumber + " not available. Failed to start SMTP server.");
    }
    startServer(portNumber);
  }

  private static void startServer(int portNumber) {
    SMTPOperations.simpleSmtpServer = SimpleSmtpServer.start(portNumber);
  }

  /**
   * Stop smtp server.
   *
   * @throws Exception the exception
   */
  public void stopSMTPServer() throws Exception {
    SMTPOperations.simpleSmtpServer.stop();
  }

  /**
   * Gets received emails.
   *
   * @param numberOfEmails the number of emails
   * @return the received emails
   * @throws Exception the exception
   */
  public Iterator getReceivedEmails(int numberOfEmails) throws Exception {
    new RetryExecutor().retry(60).delay(2000).execute((() -> {
      if (SMTPOperations.simpleSmtpServer.getReceivedEmailSize() != numberOfEmails) {
        throw new Exception("Expected number of emails: " + numberOfEmails + ", Received number of emails: "
                                + SMTPOperations.simpleSmtpServer.getReceivedEmailSize());
      }
    }));
    logger.info(String.format("Expected number of emails: %s, Received number of emails: %s",
                              numberOfEmails, SMTPOperations.simpleSmtpServer.getReceivedEmailSize()));
    return SMTPOperations.simpleSmtpServer.getReceivedEmail();
  }

  /**
   * Gets received email.
   *
   * @return the received email
   * @throws Exception the exception
   */
  public Iterator getReceivedEmail() throws Exception {
    return this.getReceivedEmails(1);
  }
}
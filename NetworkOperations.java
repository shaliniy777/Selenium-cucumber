/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import com.experian.automation.logger.Logger;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * The type Network operations.
 */
public class NetworkOperations {

  private String host;
  private Integer port;
  private String status;
  private static final Integer DEFAULT_TIMEOUT = 60000;
  private final Logger logger = Logger.getLogger(this.getClass());

  /**
   * Instantiates a new Network operations.
   */
  public NetworkOperations() {
    this.host = "localhost";
    this.status = "";
  }

  /**
   * Instantiates a new Network operations.
   *
   * @param host the host
   */
  public NetworkOperations(String host) {
    this.host = host;
    this.status = "";
  }

  /**
   * Instantiates a new Network operations.
   *
   * @param port the port
   */
  public NetworkOperations(Integer port) {
    this.host = "localhost";
    this.port = port;
    this.status = "";
  }

  /**
   * Instantiates a new Network operations.
   *
   * @param host the host
   * @param port the port
   */
  public NetworkOperations(String host, Integer port) {
    this.host = host;
    this.port = port;
    this.status = "";
  }

  /**
   * Gets host.
   *
   * @return the host
   */
  public String getHost() {
    return host;
  }

  /**
   * Sets host.
   *
   * @param host the host
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Gets port.
   *
   * @return the port
   */
  public Integer getPort() {
    return port;
  }

  /**
   * Sets port.
   *
   * @param port the port
   */
  public void setPort(Integer port) {
    this.port = port;
  }

  /**
   * Is available boolean.
   *
   * @return the boolean
   */
  public boolean isAvailable() {
    try (Socket s = new Socket(host, port)){
      s.setReuseAddress(true);
      try (InputStream is = s.getInputStream()){
        return true;
      }
    } catch (Exception e) {
      status = e.getMessage();
    }

    return false;
  }

  /**
   * Check port availability boolean.
   *
   * @param timeoutMillis    the timeout millis
   * @param becomesAvailable the becomes available
   * @return the boolean
   * @throws Exception the exception
   */
  public Boolean checkPortAvailability(int timeoutMillis, Boolean becomesAvailable) throws Exception {
    Boolean isAvailable = false;
    String availableMessage;

    if (becomesAvailable) {
      availableMessage = "to be available";
    } else {
      availableMessage = "not to be available";
    }

    long timeoutTime = System.currentTimeMillis() + timeoutMillis;
    logger.info(String.format("Checking port: %s %s.", port, availableMessage));

    try {
      do {
        isAvailable = isAvailable();
        Thread.sleep(500);
      } while ((isAvailable != becomesAvailable) && (System.currentTimeMillis() < timeoutTime));
    } catch (InterruptedException e) {
      status = e.getMessage();
    }

    return (isAvailable == becomesAvailable);
  }

  /**
   * Check port availability boolean.
   *
   * @param isAvailable the is available
   * @return the boolean
   * @throws Exception the exception
   */
  public Boolean checkPortAvailability(Boolean isAvailable) throws Exception {
    return checkPortAvailability(DEFAULT_TIMEOUT, isAvailable);
  }

  /**
   * Gets local machine ip.
   *
   * @return the local machine ip
   * @throws UnknownHostException the unknown host exception
   */
  public String getLocalMachineIP() throws UnknownHostException {
    return InetAddress.getLocalHost().getHostAddress();
  }

  /**
   * Gets the fully qualified domain name (FQDN) for this machine.
   * @return FQDN of machine
   * @throws UnknownHostException
   */
  public String getCanonicalHostName() throws UnknownHostException {
    return InetAddress.getLocalHost().getCanonicalHostName();
  }
}

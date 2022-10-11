/*
 * Copyright (c) Experian, 2021. All rights reserved.
 */
package com.experian.automation.helpers;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;

/**
 * Contains methods related to BrowserMobProxy
 */
public class ProxyOperations {

  private static BrowserMobProxy proxy; // NOI18N

  /**
   * Constructor of the class
   */
  private ProxyOperations() {
    //class constructor
  }

  /**
   * Initiate proxy server as object
   */
  private static void initProxyServer() {
    if (proxy == null) {
      proxy = new BrowserMobProxyServer();
      proxy.setTrustAllServers(true);
    }
  }

  /**
   * Start proxy server
   */
  public static void startProxyServer() {
    initProxyServer();
    if (!proxy.isStarted()) {
      proxy.start(0);
      setProxyURLInVariables();
    }
  }

  /**
   * Stop proxy server
   */
  public static void stopProxyServer() {
    if (proxy != null && proxy.isStarted()) {
      proxy.abort();
    }
    //The proxy server do not support to be restarted, so we need to instanciate it again if we need it later on
    // https://github.com/lightbody/browsermob-proxy/issues/563
    proxy=null;
  }

  /**
   * Iitiates the proxy server and returns the BrowserMobProxy object
   * @return  the initiated BrowserMobProxy object
   */
  public static BrowserMobProxy getProxy() {
    initProxyServer();
    return proxy;
  }

  /**
   * Initiates proxy server and returns its url
   * @return  proxy url
   */
  public static String getProxyURL() {
    return ClientUtil.createSeleniumProxy(ProxyOperations.getProxy()).getHttpProxy();
  }

  /**
   * Sets the proxy url as variable with name
   * webdriver.proxy.http (as in config.properties)
   */
  public static void setProxyURLInVariables() {
    Variables.set("webdriver.proxy.http", getProxyURL()); // NOI18N
  }

  /**
   * Adds proxy header by provided name and value
   *
   * @param name  header name, e.g. Authorization
   * @param value header value, e.g. Bearer xxx (a valid token)
   */
  public static void addProxyHeader(String name, String value) {
    proxy.addHeader(name, value);
  }

  /**
   * Remove all headers
   */
  public static void removeAllHeaders() {
    proxy.removeAllHeaders();
  }

  /**
   * Remove specific header by name
   * @param name  header name to be removed
   */
  public static void removeHeader(String name) {
    proxy.removeHeader(name);
  }
}
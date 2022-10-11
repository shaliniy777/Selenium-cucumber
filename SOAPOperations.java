/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import com.experian.automation.logger.Logger;
import com.experian.automation.transformers.VariablesTransformer;
import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.http.options.Option;
import com.mashape.unirest.http.options.Options;
import com.mashape.unirest.request.HttpRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.testng.TestException;

/**
 * The type Soap operations.
 */
public class SOAPOperations extends APIOperations {

  /**
   * The constant RESPONSE_VARIABLE.
   */
  public final static String RESPONSE_VARIABLE = "SOAPResponse";
  /**
   * The constant RESPONSE_ROOT_NODE.
   */
  public final static String RESPONSE_ROOT_NODE = "SOAPResponseRootNode";

  /**
   * The constant CONTENT_TYPE_NAME.
   */
  public final static String CONTENT_TYPE_NAME = "Content-Type";
  /**
   * The constant CONTENT_TYPE_VALUE.
   */
  public final static String CONTENT_TYPE_VALUE = "text/xml";

  private final Logger logger = Logger.getLogger(this.getClass());

  /**
   * Instantiates a new Soap operations.
   *
   */
  public SOAPOperations() {
    Unirest.setDefaultHeader(CONTENT_TYPE_NAME, CONTENT_TYPE_VALUE);
    Unirest.setDefaultHeader("Accept", "application/xml;charset=utf-8");
  }

  /**
   * Instantiates a new Soap operations.
   *
   * @param url the url
   */
  public SOAPOperations(String url) {
    this();
    this.url = url;
  }

  /**
   * Instantiates a new Soap operations.
   *
   * @param baseUrl the base url
   * @param uri     the uri
   */
  public SOAPOperations(String baseUrl, String uri) {
    this();
    url = VariablesTransformer.transformSingleValue(baseUrl + uri);
  }

  /**
   * Execute async.
   *
   * @param userName           the user name
   * @param password           the password
   * @param message            the message
   * @param responseRootNode   the response root node
   * @param expectedStatusCode the expected status code
   * @throws SOAPException the exception
   */
  public void executeAsync(String userName, String password, SOAPMessage message, String responseRootNode,
      int expectedStatusCode) throws SOAPException {

    setAuthentication(message, userName, password);

    HttpRequest request = createRequest(HttpMethod.POST, new HashMap<>(),
                                        XMLOperations.documentToString(
                                            message.getSOAPPart().getEnvelope().getOwnerDocument()));

    request.asStringAsync(new Callback<String>() {

      public void failed(UnirestException e) {
        logger.warn("The asynchronous request has failed");
      }

      public void completed(HttpResponse<String> response) {
        logger.info("The asynchronous request has been executed successfully");
        checkResponse(response, expectedStatusCode);
        if (expectedStatusCode == HttpStatus.SC_OK) {
          Variables.set(RESPONSE_ROOT_NODE, responseRootNode);
        } else {
          //if there is some error remove response root node step data
          Variables.set(RESPONSE_ROOT_NODE, "");
        }
      }

      public void cancelled() {
        logger.warn("The asynchronous request has been cancelled");
      }

    });
  }

  /**
   * Execute string.
   *
   * @param message            the message
   * @param expectedStatusCode the expected status code
   * @param responseRootNode   the response root node
   * @return the string
   * @throws SOAPException the exception
   * @throws UnirestException the exception
   */
  public String execute(SOAPMessage message, int expectedStatusCode, String responseRootNode)
      throws SOAPException, UnirestException {

    HttpRequest request = createRequest(HttpMethod.POST, new HashMap<>(),
                                        XMLOperations.documentToString(
                                            message.getSOAPPart().getEnvelope().getOwnerDocument()));

    addCookiesToRequest(request);

    clearCookies();

    HttpResponse<String> response = request.asString();

    checkResponse(response, expectedStatusCode);

    saveResponseCookies(response);

    if (expectedStatusCode == HttpStatus.SC_OK) {
      Variables.set(RESPONSE_ROOT_NODE, responseRootNode);
    } else {
      //if there is some error remove response root node step data
      Variables.set(RESPONSE_ROOT_NODE, "");
    }

    return Variables.get(RESPONSE_VARIABLE);
  }

  /**
   * Execute string.
   *
   * @param userName           the user name
   * @param password           the password
   * @param message            the message
   * @param expectedStatusCode the expected status code
   * @param responseRootNode   the response root node
   * @return the string
   * @throws SOAPException the exception
   * @throws UnirestException the exception
   */
  public String execute(String userName, String password, SOAPMessage message, int expectedStatusCode,
      String responseRootNode) throws SOAPException, UnirestException {
    setAuthentication(message, userName, password);
    return execute(message, expectedStatusCode, responseRootNode);
  }

  /**
   * Execute string.
   *
   * @param message            the message
   * @param expectedStatusCode the expected status code
   * @return the string
   * @throws UnirestException the exception
   * @throws SOAPException the exception
   */
  public String execute(SOAPMessage message, int expectedStatusCode) throws UnirestException, SOAPException {
    return execute(message, expectedStatusCode, "");
  }

  /**
   * Execute string.
   *
   * @param userName           the user name
   * @param password           the password
   * @param message            the message
   * @param expectedStatusCode the expected status code
   * @return the string
   * @throws SOAPException the exception
   * @throws UnirestException the exception
   */
  public String execute(String userName, String password, SOAPMessage message, int expectedStatusCode)
      throws UnirestException, SOAPException {
    return execute(userName, password, message, expectedStatusCode, "");
  }

  /**
   * Execute string.
   *
   * @param message            the message
   * @param expectedStatusCode the expected status code
   * @return the string
   * @throws SOAPException the exception
   * @throws UnirestException the exception
   * @throws IOException the exception
   */
  public String execute(String message, int expectedStatusCode) throws SOAPException, UnirestException, IOException {
    return execute(createSoapMessage(message), expectedStatusCode, "");
  }

  /**
   * Execute string.
   *
   * @param userName           the user name
   * @param password           the password
   * @param message            the message
   * @param expectedStatusCode the expected status code
   * @return the string
   * @throws IOException the exception
   * @throws SOAPException the exception
   * @throws UnirestException the exception
   */
  public String execute(String userName, String password, String message, int expectedStatusCode)
      throws IOException, SOAPException, UnirestException {
    return execute(userName, password, createSoapMessage(message), expectedStatusCode, "");
  }

  /**
   * Sets authentication.
   *
   * @param soapMessage the soap message
   * @param userName    the user name
   * @param password    the password
   * @throws SOAPException the soap exception
   */
  public void setAuthentication(SOAPMessage soapMessage, String userName, String password) throws SOAPException {

    SOAPPart soapPart = soapMessage.getSOAPPart();
    // SOAP Envelope
    SOAPEnvelope envelope = soapPart.getEnvelope();
    if (envelope.getHeader() != null) {
      envelope.getHeader().detachNode();
    }
    SOAPHeader header = envelope.addHeader();
    SOAPElement security =
        header.addChildElement("Security", "wsse",
                               "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");
    SOAPElement usernameToken =
        security.addChildElement("UsernameToken", "wsse");

    usernameToken.setAttribute("wsu:Id", "UsernameToken-1");
    usernameToken.setAttribute("xmlns:wsu","http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");

    SOAPElement usernameEl =
        usernameToken.addChildElement("Username", "wsse");
    usernameEl.addTextNode(userName);

    SOAPElement passwordEl =
        usernameToken.addChildElement("Password", "wsse");
    passwordEl.setAttribute("Type",
                            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText");
    passwordEl.addTextNode(password);

    String encodedNonce = String.valueOf(new SecureRandom().nextLong());
    encodedNonce = new String(Base64.encodeBase64(encodedNonce.getBytes()));
    SOAPElement nonceElement = usernameToken.addChildElement("Nonce", "wsse");
    nonceElement.setAttribute("EncodingType",
                              "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary");
    nonceElement.addTextNode(encodedNonce);

    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    df.setTimeZone(tz);
    String xmlFormattedNow = df.format(new Date());
    SOAPElement createdElement = usernameToken.addChildElement("Created", "wsu");
    createdElement.addTextNode(xmlFormattedNow);

    soapMessage.saveChanges();
  }

  /**
   * Check response.
   *
   * @param response           the response
   * @param expectedStatusCode the expected status code
   */
  public void checkResponse(HttpResponse<String> response, int expectedStatusCode) {

    if (response.getStatus() != expectedStatusCode) {
      throw new TestException(
          "Actual response status code " + response.getStatus() + " is not equal to expected " + expectedStatusCode);
    }

    String responseBody = response.getBody();
    Variables.set(RESPONSE_VARIABLE, XMLOperations.removeXmlNamespaces(responseBody));
  }

  private SOAPMessage createSoapMessage(String message) throws SOAPException, IOException {

    HashMap headers = (HashMap) Options.getOption(Option.DEFAULT_HEADERS);
    MimeHeaders mimeHeaders = new MimeHeaders();
    mimeHeaders.addHeader(CONTENT_TYPE_NAME, (String) headers.get(CONTENT_TYPE_NAME));

    InputStream stream = new ByteArrayInputStream(message.getBytes());
    SOAPMessage soapMessage = MessageFactory.newInstance(SOAPConstants.DYNAMIC_SOAP_PROTOCOL).createMessage(mimeHeaders,
                                                                                                            stream);

    return soapMessage;
  }
}
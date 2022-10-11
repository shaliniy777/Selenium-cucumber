/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.steps;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import com.experian.automation.cucumber.configuration.ConfigurationProperties;
import com.experian.automation.cucumber.driver.CustomChromeDriver;
import com.experian.automation.cucumber.driver.CustomEdgeDriver;
import com.experian.automation.cucumber.driver.CustomFirefoxDriver;
import com.experian.automation.cucumber.driver.CustomIEDriver;
import com.experian.automation.cucumber.driver.CustomRemoteWebDriver;
import com.experian.automation.harnesses.WebHarness;
import com.experian.automation.helpers.Config;
import com.experian.automation.helpers.Variables;
import com.experian.automation.helpers.saas.TokenUtils;
import com.experian.automation.logger.Logger;
import com.experian.automation.screens.AlertScreen;
import com.experian.automation.transformers.VariablesTransformer;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.testng.util.Strings;

/**
 * The type Web steps.
 */
@ConfigurationProperties(
    properties = {
        "browser",
        "webdriver.ie.driver",
        "selenium.hub.url"
    }
)
public class WebSteps {

  private static final String CHROME_NAME = "chrome";
  private static final String FIREFOX_NAME = "firefox";
  private static final String EDGE_NAME = "edge";

  private static final String BROWSER_PROP = "browser";
  private static final String SELENIUM_HUB_PROP = "selenium.hub.url";

  private static final String BROWSER_W_INDEX_MESSAGE = "Browser with index '";

  /**
   * The Web harness.
   */
  protected final WebHarness webHarness;

  private final Logger logger = Logger.getLogger(this.getClass());
  private String defaultBrowser;
  private String substituteBrowser;

  /**
   * Before scenario divert ie.
   */
  @Before("@divert-ie")
  public void beforeScenarioDivertIE() {
    if (Strings.isNotNullAndNotEmpty(defaultBrowser) && defaultBrowser.equals("ie")) {
      defaultBrowser = null;
    }
  }

  /**
   * Before scenario divert chrome.
   */
  @Before("@divert-chrome")
  public void beforeScenarioDivertChrome() {
    if (Strings.isNotNullAndNotEmpty(defaultBrowser) && defaultBrowser.equals(CHROME_NAME)) {
      defaultBrowser = null;
    }
  }

  /**
   * Before scenario divert fireforx.
   */
  @Before("@divert-firefox")
  public void beforeScenarioDivertFireforx() {
    if (Strings.isNotNullAndNotEmpty(defaultBrowser) && defaultBrowser.equals(FIREFOX_NAME)) {
      defaultBrowser = null;
    }
  }

  /**
   * Before scenario set browser ie.
   */
  @Before("@ie")
  public void beforeScenarioSetBrowserIE() {
    substituteBrowser = "ie";
  }

  /**
   * Before scenario set browser chrome.
   */
  @Before("@chrome")
  public void beforeScenarioSetBrowserChrome() {
    substituteBrowser = CHROME_NAME;
  }

  /**
   * Before scenario set browser firefox.
   */
  @Before("@firefox")
  public void beforeScenarioSetBrowserFirefox() {
    substituteBrowser = FIREFOX_NAME;
  }

   /**
   * Before scenario set browser edge.
   */
  @Before("@edge")
  public void beforeScenarioSetBrowserEdge() {
    substituteBrowser = EDGE_NAME;
  }

  /**
   * Instantiates a new Web steps.
   *
   * @param webHarness the web harness
   */
  public WebSteps(WebHarness webHarness) {
    this.webHarness = webHarness;
    defaultBrowser = Config.get(BROWSER_PROP);
  }


  /**
   * Start browser.
   * To start browser using http proxy (chrome and firefox only), add "webdriver.proxy.http" in your config.properties e.g. webdriver.proxy.http=http://proxy:9595
   *
   * @param browserInstance the browser instance
   * @param incognito       the incognito
   * @param language        the language
   * @throws Throwable the throwable
   */
  @And("^I start (the|another) browser( in incognito mode)?(?: and in (.*) language)?$")
  public void startBrowser(String browserInstance, String incognito, String language) throws Throwable {
    language = VariablesTransformer.transformSingleValue(language);

    String browser = defaultBrowser == null ? substituteBrowser : defaultBrowser;
    assertNotNull(browser,
                  "Browser is not set. Substitute browser is not chosen after diverting the default one.");

    browser = Variables.getOrDefault(BROWSER_PROP, browser);

    // Set as variable, because it might be used in steps to change the logic based on browser type
    Variables.set(BROWSER_PROP, browser);

    AbstractDriverOptions browserOpts;
    Boolean localBrowser = Config.get(SELENIUM_HUB_PROP).isEmpty();

    switch (browser.toLowerCase()) {

      case FIREFOX_NAME:
        browserOpts = new FirefoxOptions();

        if (localBrowser) {
          webHarness.driver = new CustomFirefoxDriver(incognito, language);
        }

        break;

      case CHROME_NAME:
        browserOpts=new ChromeOptions();

        if (localBrowser) {
          webHarness.driver = new CustomChromeDriver(incognito, language);
        }

        break;

      case EDGE_NAME:
        browserOpts = new EdgeOptions();

        if (localBrowser) {
          webHarness.driver = new CustomEdgeDriver(incognito, language);
        }

        break;

      case "ie":
      case "internet explorer":
        browserOpts = new InternetExplorerOptions();

        if (localBrowser) {
          webHarness.driver = new CustomIEDriver(incognito, language);
        }

        break;
      default:
        throw new IllegalArgumentException("Unsupported browser " + browser.toLowerCase());

    }

    // Selenium grid will be used
    if (!localBrowser) {
      URL hubURL = new URL(Config.get(SELENIUM_HUB_PROP));
      webHarness.driver = new CustomRemoteWebDriver(browser, hubURL, browserOpts);
    }

    Integer browserIndex = 1;
    if (browserInstance.equals("another")) {
      browserIndex = Collections.max(webHarness.driversMap.keySet()) + 1;
    }

    //Add the driver to the map
    if (!webHarness.driversMap.containsKey(browserIndex)) {
      webHarness.driver.manage().window().maximize();
      webHarness.driversMap.put(browserIndex, webHarness.driver);
    } else {
      throw new RuntimeException(BROWSER_W_INDEX_MESSAGE + browserIndex + "' is already started");
    }

  }


  /**
   * Start browser.
   *
   * @param browserInstance the browser instance
   * @throws Throwable the throwable
   */
  public void startBrowser(String browserInstance) throws Throwable {
    startBrowser(browserInstance, null, null);
  }


  /**
   * Stop all browsers.
   *
   * @throws Throwable the throwable
   */
  @And("^I stop (?:the|all) browser(?:s)?$")
  public void stopAllBrowsers() throws Throwable {
    for (WebDriver driver : webHarness.driversMap.values()) {
      driver.quit();
    }
    webHarness.driversMap.clear();
  }

  /**
   * Stop specific browser.
   *
   * @param browserIndex the browser index
   * @throws Throwable the throwable
   */
  @And("^I stop (\\d+)(?:st|nd|rd|th) browser$")
  public void stopSpecificBrowser(Integer browserIndex) throws Throwable {
    if (webHarness.driversMap.containsKey(browserIndex)) {
      webHarness.driversMap.get(browserIndex).quit();
      webHarness.driversMap.remove(browserIndex);
    } else {
      throw new RuntimeException(
          BROWSER_W_INDEX_MESSAGE + browserIndex + "' is not started and cannot be stopped");
    }
  }

  /**
   * Switch browser.
   *
   * @param browserIndex the browser index
   * @throws Throwable the throwable
   */
  @And("^I switch to (\\d+)(?:st|nd|rd|th) browser$")
  public void switchBrowser(Integer browserIndex) throws Throwable {
    if (webHarness.driversMap.containsKey(browserIndex)) {
      webHarness.driver = webHarness.driversMap.get(browserIndex);
    } else {
      throw new RuntimeException(
          BROWSER_W_INDEX_MESSAGE + browserIndex + "' is not started and cannot be switched to");
    }
  }

  /**
   * Verify alert with message.
   *
   * @param expectedAlertMessage the expected alert message
   * @throws Throwable the throwable
   */
  @And("^I verify that alert with message (.*) is shown$")
  public void verifyAlertWithMessage(String expectedAlertMessage) throws Throwable {

    expectedAlertMessage = VariablesTransformer.transformSingleValue(expectedAlertMessage);

    String formattedAlertMessage = expectedAlertMessage.trim();

    AlertScreen screen = new AlertScreen(webHarness);
    String actualAlertMessage = screen.getAlertText().trim();

    if (formattedAlertMessage.startsWith("regex%") && formattedAlertMessage.endsWith("%")) {
      //Get the regex
      String alertMessageRegEx = formattedAlertMessage
          .substring(formattedAlertMessage.indexOf('%') + 1,
                     formattedAlertMessage.lastIndexOf('%'));
      assertTrue(actualAlertMessage.matches(alertMessageRegEx),
                 "Alert message '" + actualAlertMessage + "' matches '" + alertMessageRegEx + "'");
    } else {
      assertEquals(actualAlertMessage, formattedAlertMessage, "Alert message");
    }
    screen.acceptAlert();
  }

  /**
   * Refresh page and verify alert with message.
   *
   * @param expectedAlertMessage the expected alert message
   * @throws Throwable the throwable
   */
  @And("^I refresh page and verify that alert with message (.*) is shown$")
  public void refreshPageAndVerifyAlertWithMessage(String expectedAlertMessage) throws Throwable {

    AlertScreen screen = new AlertScreen(webHarness);
    screen.refreshScreen();

    verifyAlertWithMessage(expectedAlertMessage);
  }

  /**
   * Open browser window.
   *
   * @throws Throwable the throwable
   */
  @And("^I open another browser window$")
  public void openBrowserWindow() throws Throwable {
    ((JavascriptExecutor) webHarness.driver).executeScript("window.open();");

    ArrayList<String> windowHandles = new ArrayList<>(webHarness.driver.getWindowHandles());
    webHarness.driver.switchTo().window(windowHandles.get(windowHandles.size() - 1));
  }

  /**
   * Switch to browser window.
   *
   * @param windowNumber the window number
   * @throws Throwable the throwable
   */
  @And("^I switch to the (\\d+)(?:st|nd|rd|th) browser window$")
  public void switchToBrowserWindow(int windowNumber) throws Throwable {
    ArrayList<String> windowHandles = new ArrayList<>(webHarness.driver.getWindowHandles());
    if (windowHandles.size() >= windowNumber) {
      webHarness.driver.switchTo().window(windowHandles.get(windowNumber - 1));
    } else {
      throw new RuntimeException("Window #" + windowNumber + " does not exist");
    }
  }

  /**
   * Set Request header with the ModHeader extension
   * header name: Authorization
   * header value: Bearer + token
   */
  @And("I set ModHeader chrome extension with authorization header")
  public void setModHeader() {
    String token =  TokenUtils.getAuthorizationBearerToken();
    ((JavascriptExecutor)webHarness.driver).executeScript(
         "localStorage.setItem('profiles', JSON.stringify([{                " +  // NOI18N
            "  title: 'Selenium', hideComment: true, appendMode: '',           " +  // NOI18N
            "  headers: [                                                      " +  // NOI18N
            "   {enabled: true, name: 'Authorization', value: '" + token + "', comment: ''} " + // NOI18N
            "  ],                                                              " +  // NOI18N
            "  respHeaders: [],                                                " +  // NOI18N
            "  filters: []                                                     " +  // NOI18N
            "}]));                                                             " ); // NOI18N
  }

}
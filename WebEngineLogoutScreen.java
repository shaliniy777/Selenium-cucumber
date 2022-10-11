/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.screens.powercurve.webengine;

import com.experian.automation.harnesses.WebHarness;
import com.experian.automation.screens.Screen;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * The type Web engine logout screen.
 */
public class WebEngineLogoutScreen extends Screen {

  /**
   * The Home button on navigation bar.
   */
  @FindBy(xpath = "//div[@class='exit']/a[text()='Home']")
  public WebElement homeButtonOnNavigationBar;

  /**
   * The Return to home link.
   */
  @FindBy(xpath = "//div[@class='info-panel']/a[text()='Home']")
  public WebElement returnToHomeLink;

  /**
   * The Page title.
   */
  @FindBy(xpath = "//div[@class='pop-banner']//h1")
  public WebElement pageTitle;

  @Override
  public void clickElement(WebElement element) {
    String strJavaScript = "arguments[0].click();";
    ((JavascriptExecutor) webHarness.driver).executeScript(strJavaScript, element);
  }

  /**
   * Instantiates a new Web engine logout screen.
   *
   * @param webHarness the web harness
   */
  public WebEngineLogoutScreen(WebHarness webHarness) {
    super(webHarness);
  }
}

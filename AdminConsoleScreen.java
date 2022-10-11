/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.screens.powercurve.webengine;

import com.experian.automation.harnesses.WebHarness;
import com.experian.automation.helpers.Config;
import com.experian.automation.screens.Screen;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;
import org.openqa.selenium.Alert;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Main AdminConsole screen class
 */
public class AdminConsoleScreen extends Screen {

  /**
   * Deployment tab menu option
   */
  @FindBy(linkText = "Deployment Console")
  public WebElement deploymentTab;

  /**
   * Upload file field
   */
  @FindBy(xpath = "//input[@name='deployField']")
  public WebElement fileUploadButton;

  /**
   * Deployment button
   */
  @FindBy(xpath = "//input[@value='Deploy WRA']")
  public WebElement deployWRAbutton;

  /**
   * Undeploy button
   */
  @FindBy(xpath = "//input[@value='Undeploy WRA']")
  public WebElement undeployWRAbutton;

  /**
   * Start application button
   */
  @FindBy(xpath = "//input[@value='Start Application']")
  public WebElement startApplicationButton;

  /**
   * Stop application button
   */
  @FindBy(xpath = "//input[@value='Stop Application']")
  public WebElement stopApplicationButton;

  /**
   * Deployment Console tab - deployment loading icon
   */
  @FindBy(xpath = "//span[@class='wicket-ajax-indicator']")
  public WebElement deploymentLoadingIcon;

  /**
   * Logout link
   */
  @FindBy(xpath = "//div[@id='logoutLink']")
  public WebElement logoutLink;

  /**
   * Admin Console Tabs
   */
  @FindBy(xpath = "//div[@id='tabs']//li")
  public List<WebElement> adminConsoleTabs;

  /**
   * Security tab - OpenAM section - Update button
   */
  @FindBy(xpath = "//input[@id='ide']")
  public WebElement securityOpenAMUpdate;

  /**
   * Instantiates a new Admin console screen.
   *
   * @param webHarness the web harness
   */
  public AdminConsoleScreen(WebHarness webHarness) {
    super(webHarness);
  }

  /**
   * Upload file.
   *
   * @param filePath the file path
   */
  public void uploadFile(String filePath) {

    waitForElement(fileUploadButton);
    if (SystemUtils.IS_OS_WINDOWS) {
      filePath = FilenameUtils.normalize(FilenameUtils.separatorsToWindows(filePath));
    } else {
      filePath = FilenameUtils.normalize(FilenameUtils.separatorsToUnix(filePath));
    }
    fileUploadButton.sendKeys(filePath);
    clickElement(deployWRAbutton);
    waitForElementToBeClickable(undeployWRAbutton);
    waitForElementToBeClickable(startApplicationButton);
  }
  /**
   * Click the specified tab on Admin Console
   *
   * @param tabName name of the tab to navigate to in Admin Console
   */
  public void selectTab(String tabName) {
    waitForElements(adminConsoleTabs);
    clickElement(getElementByText(adminConsoleTabs, tabName));
  }
}

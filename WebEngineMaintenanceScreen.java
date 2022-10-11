/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.screens.powercurve.webengine;

import com.experian.automation.harnesses.WebHarness;
import com.experian.automation.screens.Screen;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * The type Web engine maintenance screen.
 */
public class WebEngineMaintenanceScreen extends Screen {

  /**
   * The Security policy edit button.
   */
  @FindBy(id = "edit_spo")
  public WebElement securityPolicyEditButton;

  /**
   * The Activity timeout field.
   */
  @FindBy(id = "activity_timeout")
  public WebElement activityTimeoutField;

  /**
   * The Login inactivity timeout field.
   */
  @FindBy(id = "login_inactivity_timeout")
  public WebElement loginInactivityTimeoutField;

  /**
   * The Profiles list.
   */
  @FindBy(xpath = "//div[contains(@class,'content-active')]//li[contains(@class,'ui-draggable')]//a")
  List<WebElement> profilesList;

  /**
   * The Current profile edit button.
   */
  @FindBy(xpath = "//div[contains(@style,'display: block') and contains(@id,'edit')]//a[contains(text(),'Edit')]")
  public WebElement currentProfileEditButton;

  /**
   * The Groups button.
   */
  @FindBy(xpath = "//a[@class='groupHeader']")
  public WebElement groupsButton;

  /**
   * The Security profiles button.
   */
  @FindBy(xpath = "//a[@class='secuProfileHeader']")
  public WebElement securityProfilesButton;

  /**
   * The Security policy button.
   */
  @FindBy(xpath = "//a[@class='secuPolicyHeader']")
  public WebElement securityPolicyButton;

  /**
   * The Global security policy button.
   */
  @FindBy(xpath = "//a[contains(text(),'Global security policy')]")
  public WebElement globalSecurityPolicyButton;

  /**
   * The Security policy save button.
   */
  @FindBy(xpath = "//button[text()='Save']")
  public WebElement securityPolicySaveButton;


  /**
   * Find expression for unpredictable front overlay throughout the maintenance module
   */
  public By overlayFindXpr = By.xpath("//div[contains(@class,'overlay')]");

  /**
   * Instantiates a new Web engine maintenance screen.
   *
   * @param webHarness the web harness
   */
  public WebEngineMaintenanceScreen(WebHarness webHarness) {
    super(webHarness);
    waitForScreen(groupsButton);
  }

  /**
   * Select profile.
   *
   * @param profile the profile
   */
  public void selectProfile(String profile) {
    By prflLocator = By.xpath(
        "//div[contains(@class,'content-active')]//li[contains(@class,'ui-draggable')]//a[text()='" + profile + "']");
    WebElement prflEl = waitForElementPresence(prflLocator);
    clickElement(prflEl);
  }

  /**
   * Waits for element to disappear only if it initially exists. Does not pre-initialize the element.
   *
   * @param findXpr the find expression for the element
   */
  public void waitForElementAbsence(By findXpr) {
    if (webHarness.driver.findElements(findXpr).size() != 0) {
      try {
        waitForElementToDisappear(webHarness.driver.findElement(findXpr));
      } catch (NoSuchElementException e) {
        e.printStackTrace();
      }
    }
  }
}

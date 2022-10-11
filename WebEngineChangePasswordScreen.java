/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.screens.powercurve.webengine;

import com.experian.automation.harnesses.WebHarness;
import com.experian.automation.screens.Screen;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * The type Web engine change password screen.
 */
public class WebEngineChangePasswordScreen extends Screen {

  /**
   * The Previous passowrd field.
   */
  @FindBy(id = "prev-password")
  public WebElement previousPassowrdField;

  /**
   * The First new password field.
   */
  @FindBy(id = "new-password1")
  public WebElement firstNewPasswordField;

  /**
   * The Second new passowrd field.
   */
  @FindBy(id = "new-password2")
  public WebElement secondNewPassowrdField;

  /**
   * The Enabled change password button.
   */
  @FindBy(xpath = "//input[@id='submitButton' and not(@disabled)]")
  public WebElement enabledChangePasswordButton;

  /**
   * The Confirm password change button.
   */
  @FindBy(xpath = "//input[@value='Yes']")
  public WebElement confirmPasswordChangeButton;

  /**
   * The Decline password change button.
   */
  @FindBy(xpath = "//input[@value='No']")
  public WebElement declinePasswordChangeButton;

  /**
   * The Cancel button.
   */
  @FindBy(xpath = "//*[@id='logoutLink']")
  public WebElement cancelButton;

  /**
   * Instantiates a new Web engine change password screen.
   *
   * @param webHarness the web harness
   */
  public WebEngineChangePasswordScreen(WebHarness webHarness) {
    super(webHarness);
    waitForScreen(previousPassowrdField);
  }

  /**
   * Change password.
   *
   * @param oldPassword the old password
   * @param newPassword the new password
   */
  public void changePassword(String oldPassword, String newPassword) {
    typeWithClear(previousPassowrdField, oldPassword);
    typeWithClear(firstNewPasswordField, newPassword);
    typeWithClear(secondNewPassowrdField, newPassword);
    waitForElement(enabledChangePasswordButton);
    waitForElementToBeClickable(enabledChangePasswordButton);
    enabledChangePasswordButton.sendKeys(Keys.ENTER);
    waitForElement(confirmPasswordChangeButton);
    confirmPasswordChangeButton.click();
    waitForURLChange();
  }

  /**
   * Cancel change of password.
   *
   * @param oldPassword the old password
   * @param newPassword the new password
   */
  public void cancelChangeOfPassword(String oldPassword, String newPassword) {
    typeWithClear(previousPassowrdField, oldPassword);
    typeWithClear(firstNewPasswordField, newPassword);
    typeWithClear(secondNewPassowrdField, newPassword);
    waitForElement(enabledChangePasswordButton);
    waitForElementToBeClickable(enabledChangePasswordButton);
    enabledChangePasswordButton.sendKeys(Keys.ENTER);
    waitForElement(declinePasswordChangeButton);
    declinePasswordChangeButton.click();
    refreshScreen();
  }
}

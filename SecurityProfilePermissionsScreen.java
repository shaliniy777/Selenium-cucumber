/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.screens.powercurve.webengine;

import com.experian.automation.harnesses.WebHarness;
import com.experian.automation.screens.Screen;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * The type Security profile permissions screen.
 */
public class SecurityProfilePermissionsScreen extends Screen {
  public static final String PERMISSION_ALLOW = "allow";  // NOI18N
  public static final String PERMISSION_DENY = "deny";    // NOI18N
  private static final String ATTRIBUTE_CLASS = "class";  // NOI18N

  /**
   * The Current editor.
   */
  @FindBy(xpath = "//div[not(contains(@style,'display: none')) and @role='dialog']")
  public WebElement currentEditor;

  /**
   * The Item names list.
   */
  @FindBy(xpath = "(//table[@id='spTable']//tbody//tr//td[1])")
  List<WebElement> itemNamesList;

  /**
   * The Item create permissions list.
   */
  @FindBy(xpath = "(//table[@id='spTable']//tbody//tr//td[2])")
  List<WebElement> itemCreatePermissionsList;

  /**
   * The Ext store grp input.
   */
  @FindBy(id = "grp_ldap_input")
  public WebElement extStoreGrpInput;

  /**
   * The Grp name input.
   */
  @FindBy(id = "grp_name_input")
  public WebElement grpNameInput;

  /**
   * The Ok button.
   */
  @FindBy(xpath = "//button[text()='Ok']")
  public WebElement okButton;

  /**
   * The Save button.
   */
  @FindBy(xpath = "//button[text()='Save']")
  public WebElement saveButton;

  /**
   * Instantiates a new Security profile permissions screen.
   *
   * @param webHarness the web harness
   */
  public SecurityProfilePermissionsScreen(WebHarness webHarness) {
    super(webHarness);
    waitForScreen(currentEditor);
  }

  /**
   * Allow create permissionsfor item.
   *
   * @param itemName the item name
   */
  public void allowCreatePermissionsforItem(String itemName) {
    Integer itemIndex = findItemIndex(itemName);
    WebElement desiredPermissions = itemCreatePermissionsList.get(itemIndex);
    if (!desiredPermissions.getAttribute(ATTRIBUTE_CLASS).equals(PERMISSION_ALLOW)) {
      desiredPermissions.click();
    }
  }

  /**
   * Set allow/deny for a combination of workflow and worklist
   *
   * @param permType as worklist (eg: In Progress)
   * @param itemName as workflow (eg: Search and View)
   * @param action as allow or deny
   */
  public void setSelectedProfilePermissions(String permType, String itemName, String action) {
    // Get the list of header in the table. This will result in list of worklist
    List<WebElement> tableHeaders = waitForElementsPresence(
        By.xpath("(//table[@id='spTable']//thead//th)")
    );

    List<String> headersText = new ArrayList<>();
    for (WebElement el : tableHeaders) {
      headersText.add(el.getText().trim());
    }

    // Get the index of worklist for the given permType
    int permissionIndex = headersText.indexOf(permType) + 1;

    List<WebElement> itemAllPermissionsList = waitForElementsPresence(
        By.xpath("(//table[@id='spTable']//tbody//tr//td["+ permissionIndex +"])")
    );

    Integer itemIndex = findItemIndex(itemName);
    WebElement desiredPermissions = itemAllPermissionsList.get(itemIndex);

    // Set allow/deny by clicking on the icon
    if ( ((action.equals(PERMISSION_ALLOW)) && (!desiredPermissions.getAttribute(ATTRIBUTE_CLASS).equals(PERMISSION_ALLOW))) ||
        ( (action.equals(PERMISSION_DENY)) && (!desiredPermissions.getAttribute(ATTRIBUTE_CLASS).equals(PERMISSION_DENY)) ) ) {
      desiredPermissions.click();
    }
  }

  private Integer findItemIndex(String itemName) {

    for (WebElement el :
        itemNamesList) {
      if (el.getText().trim().equals(itemName)) {
        return itemNamesList.indexOf(el);
      }
    }
    throw new RuntimeException("Item with name " + itemName + " does not exist.");
  }

  /**
   * Get the permission for a specific workflow and worklist in the security profile permissions table
   *
   * @param permType as worklist (eg: In Progress)
   * @param itemName as workflow (eg: Search and View)
   * @return allow or deny according to what is configured in the cell
   */
  public String getSelectedProfilePermission(String permType, String itemName) {
    // Get the list of header in the table. This will result in list of worklist
    List<WebElement> tableHeaders = waitForElementsPresence(
            By.xpath("(//table[@id='spTable']//thead//th)")
    );

    List<String> headersText = new ArrayList<>();
    for (WebElement el : tableHeaders) {
      headersText.add(el.getText().trim());
    }

    // Get the index of worklist for the given permType
    int permissionIndex = headersText.indexOf(permType) + 1;

    List<WebElement> itemAllPermissionsList = waitForElementsPresence(
            By.xpath("(//table[@id='spTable']//tbody//tr//td["+ permissionIndex +"])")
    );

    Integer itemIndex = findItemIndex(itemName);
    WebElement element = itemAllPermissionsList.get(itemIndex);

    String permission = element.getAttribute(ATTRIBUTE_CLASS);
    if(StringUtils.isEmpty(permission)) {
      permission = PERMISSION_ALLOW;
    }
    return permission;
  }
}

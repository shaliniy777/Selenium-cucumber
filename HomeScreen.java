package com.experian.automation.skeleton.screens.google;

import com.experian.automation.harnesses.WebHarness;
import com.experian.automation.screens.Screen;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class HomeScreen extends Screen {

  public String url = "http://www.google.com";

  @FindBy(xpath = "//input[@type='text' and @title='Search']")
  public WebElement searchText;

  @FindBy(xpath = "//input[@value='Google Search']")
  public WebElement searchBtn;

  public HomeScreen(WebHarness webHarness) {
    super(webHarness);
  }

  public void goToURL() {
    // Load Google home page
    goToURL(this.url);
    // Wait for search button to be presented
    waitForScreen(this.searchBtn);
  }
}

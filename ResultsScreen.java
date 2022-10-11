package com.experian.automation.skeleton.screens.google;

import com.experian.automation.harnesses.WebHarness;
import com.experian.automation.screens.Screen;
import java.util.List;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class ResultsScreen extends Screen {

  @FindBy(xpath = "//div[@class='rc']/h3/a")
  public List<WebElement> resultTitles;

  @FindBy(xpath = "//div[@class='rc']//cite")
  public List<WebElement> resultURLs;

  public ResultsScreen(WebHarness webHarness) {
    super(webHarness);
    // Wait for search results
    waitForElements(this.resultTitles);
  }
}

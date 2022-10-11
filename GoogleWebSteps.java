package com.experian.automation.skeleton.steps;

import com.experian.automation.harnesses.WebHarness;
import com.experian.automation.skeleton.screens.google.HomeScreen;
import com.experian.automation.skeleton.screens.google.ResultsScreen;
import cucumber.api.java.en.When;
import java.io.IOException;
import java.util.Iterator;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.openqa.selenium.WebElement;
import org.testng.Assert;

public class GoogleWebSteps {

  private final WebHarness webHarness;

  public GoogleWebSteps(WebHarness webHarness) throws IOException, ConfigurationException {
    this.webHarness = webHarness;
  }

  /*
  *
  * And I search for {search string}
  *
  */
  @When("^I search for (.*)$")
  public void search(String searchText) {
    HomeScreen screen = new HomeScreen(webHarness);
    // Open page and wait to load
    screen.goToURL();
    // Type search text
    screen.type(screen.searchText, searchText);
    // Click on search button
    screen.clickElement(screen.searchBtn);
  }

  /*
  *
  * And I verify that there is a search result with url http://www.abc.com/
  *
  */
  @When("^I verify that there is a search result with url (.*)$")
  public void searchResult(String url) {
    ResultsScreen screen = new ResultsScreen(webHarness);

    Boolean found = false;
    Iterator<WebElement> iterator = screen.resultURLs.iterator();
    while (iterator.hasNext() && !found) {
      found = iterator.next().getText().equals(url);
    }

    Assert.assertTrue(found, String.format("Cannot find %s url in seach resutls", url));
  }

}


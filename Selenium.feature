Feature: Web

  Scenario: Search in Google
    Given I start the browser
    When I search for google.com
    Then I verify that there is a search result with url https://www.google.com/
    And I stop all browsers
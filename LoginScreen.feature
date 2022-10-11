Feature: Login to Cembra solution
@single
  Scenario: Open a page and login
    Given I start the browser
    When I navigate to Cembra homepage in "http://ukc9vm0009332.uk.experian.local:8080/WebEngine"
    And I login to Cembra solution with user "lukasz" and password "Testing123!"
    And I hover over menu "Apply"
    And I click on menu "Apply"
    And I click on menu "New Application"
    And I fill in Requested Product Details section
    And I fill in Party Details section
    And I fill in MDM section
    And I fill in Pending Application Search Page
    And I fill in Previous Application Search Page
    And I fill in Basic Applicant Details Page
    And I fill in Applicant and Address Details Page
    And I fill in Employment and Affordability Details Page
    And I fill in Partner Included Page
    And I fill in Partner Party Start Page
    And I fill in Response MDM Partner Page
    And I fill in Pending Application Search Page
    And I fill in Previous Application Search Page
    And I fill in Partner Applicant Details Page
    And I fill in Partner Affordability Details Page
      Then I verify Employment and Affordability Kremo Output




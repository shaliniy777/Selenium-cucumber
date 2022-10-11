Feature: REST

  Scenario: Get a book title and author
    Given I set the base webservice url to http://openlibrary.org/api
    When I send a REST GET request to /books?bibkeys=ISBN:0451526538&jscmd=data&format=json and receive status code HTTP 200
    Then I verify that the JSON response has fields:
      | $.ISBN:0451526538.title           | The adventures of Tom Sawyer |
      | $.ISBN:0451526538.authors[0].name | Mark Twain                   |

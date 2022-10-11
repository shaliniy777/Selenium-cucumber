Feature: SOAP

  Scenario: Temperature converter - SOAP 1.1
    Given I set the base webservice url to http://www.webservicex.net
    And I prepare SOAP request body:
      """
      <?xml version="1.0" encoding="utf-8"?>
      <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
        <soap:Body>
          <ConvertTemp xmlns="http://www.webserviceX.NET/">
            <Temperature>12</Temperature>
            <FromUnit>degreeCelsius</FromUnit>
            <ToUnit>degreeFahrenheit</ToUnit>
          </ConvertTemp>
        </soap:Body>
      </soap:Envelope>
      """
    And I add the following headers to the SOAP request:
      | Content-Type | text/xml; charset=utf-8                |
      | SOAPAction   | http://www.webserviceX.NET/ConvertTemp |
    When I send a SOAP request to /ConvertTemperature.asmx and receive status code HTTP 200
    Then I verify that the XML response has fields:
      | /Envelope/Body/ConvertTempResponse/ConvertTempResult | 53.6 |

  Scenario: Temperature converter - SOAP 1.2
    Given I set the base webservice url to http://www.webservicex.net
    And I prepare SOAP request body:
      """
      <?xml version="1.0" encoding="utf-8"?>
      <soap12:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
        <soap12:Body>
          <ConvertTemp xmlns="http://www.webserviceX.NET/">
            <Temperature>12</Temperature>
            <FromUnit>degreeCelsius</FromUnit>
            <ToUnit>degreeFahrenheit</ToUnit>
          </ConvertTemp>
        </soap12:Body>
      </soap12:Envelope>
      """
    And I add the following headers to the SOAP request:
      | Content-Type | application/soap+xml; charset=utf-8    |
      | SOAPAction   | http://www.webserviceX.NET/ConvertTemp |
    When I send a SOAP request to /ConvertTemperature.asmx and receive status code HTTP 200
    Then I verify that the XML response has fields:
      | /Envelope/Body/ConvertTempResponse/ConvertTempResult | 53.6 |
/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.awt.Image;
import java.awt.image.PixelGrabber;

import io.cucumber.datatable.DataTable;

import io.cucumber.java.en.And;

import org.testng.AssertJUnit;

import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.RegularExpressionValueMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.json.JSONObject;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;

import com.experian.automation.helpers.Config;
import com.experian.automation.helpers.FSOperations;
import com.experian.automation.helpers.ImagesOperations;
import com.experian.automation.helpers.JSONOperations;
import com.experian.automation.helpers.PDFImageExtractor;
import com.experian.automation.helpers.Variables;

import com.experian.automation.transformers.VariablesTransformer;

import com.experian.automation.logger.Logger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * The type File comparison steps.
 */
public class FileComparisonSteps {

  private static final String IS_DIFFERENT_MESSAGE = " is different.";
  private static final String TMP_DIR_VAR = "temp.dir";
  private static final String FILE_NOT_FOUND_MESSAGE = "File not found: ";
  private static final String JSON_FILE_SUFFIX = ".json"; // NOI18N


  private final Logger logger = Logger.getLogger(this.getClass());

  /**
   * Instantiates a new File comparison steps.
   */
  public FileComparisonSteps() {
    // Blank Constructor
  }

  /**
   * Compare pdf file.
   *
   * @param actualFilePath   the actual file path
   * @param expectedFilePath the expected file path
   * @throws Throwable the throwable
   */
  @And("^I compare PDF file (.*) with expected file (.*)$")
  public void comparePDFFile(String actualFilePath, String expectedFilePath) throws Throwable {

    actualFilePath = VariablesTransformer.transformSingleValue(actualFilePath);
    expectedFilePath = VariablesTransformer.transformSingleValue(expectedFilePath);

    PDDocument actualPDF = PDDocument.load(new File(actualFilePath));
    PDDocument expectedPDF = PDDocument.load(new File(expectedFilePath));

    try {
      assertEquals(actualPDF.getNumberOfPages(), expectedPDF.getNumberOfPages(), "PDF number of pages is different.");

      int pageNumber = 1;
      PDFImageExtractor imgActual = new PDFImageExtractor();
      PDFImageExtractor imgExpected = new PDFImageExtractor();
      while (pageNumber <= actualPDF.getNumberOfPages()) {

        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(pageNumber);
        stripper.setEndPage(pageNumber);
        String actText = stripper.getText(actualPDF);
        String expText = stripper.getText(expectedPDF);

        assertEquals(actText, expText,
                     "PDF text on page " + pageNumber + IS_DIFFERENT_MESSAGE);

        PDPage pageActual = actualPDF.getPage(pageNumber - 1);
        PDPage pageExpected = expectedPDF.getPage(pageNumber - 1);
        imgActual.processPage(pageActual);
        imgExpected.processPage(pageExpected);
        pageNumber++;
      }

      List actualPageImageList = imgActual.getAllImages();
      List expectedPageImageList = imgExpected.getAllImages();

      assertEquals(actualPageImageList.size(), expectedPageImageList.size(),
                   "PDF number of images on page " + pageNumber + IS_DIFFERENT_MESSAGE);

      int imageNumber = 0;

      while (imageNumber < actualPageImageList.size()) {

        Boolean imagesEquals = false;

        PixelGrabber actualGrabber = new PixelGrabber((Image) actualPageImageList.get(imageNumber), 0, 0, -1, -1,
                                                      false);
        PixelGrabber expectedGrabber = new PixelGrabber((Image) expectedPageImageList.get(imageNumber), 0, 0, -1, -1,
                                                        false);

        actualGrabber.grabPixels();
        Object e = actualGrabber.getPixels();
        expectedGrabber.grabPixels();
        Object expectedPixelsObject = expectedGrabber.getPixels();
        if ((actualGrabber.getStatus() & 128) == 0 && (expectedGrabber.getStatus() & 128) == 0
            && actualGrabber.getWidth() == expectedGrabber.getWidth()
            && actualGrabber.getHeight() == expectedGrabber.getHeight()) {
          if (e instanceof byte[] && expectedPixelsObject instanceof byte[]) {
            imagesEquals = Arrays.equals((byte[]) ((byte[]) e), (byte[]) ((byte[]) expectedPixelsObject));
          } else if (e instanceof int[] && expectedPixelsObject instanceof int[]) {
            imagesEquals = Arrays.equals((int[]) ((int[]) e), (int[]) ((int[]) expectedPixelsObject));
          }
        }

        assertTrue(imagesEquals, "PDF image on " + imageNumber + " on page " + pageNumber + IS_DIFFERENT_MESSAGE);

        imageNumber++;
      }


    } finally {
      actualPDF.close();
      expectedPDF.close();
    }
  }

  /**
   * Compare json file excluding fields.
   *
   * @param filePath   the file path
   * @param jsonString the json string
   * @param properties the properties
   * @throws Throwable the throwable
   */
/*
  Usage example 1:
  And I compare JSON file ${features.path}/ApiHistoryService/data/expected-response.json to string ${RESPONSE} excluding:
      | $._embedded.models.[*].uuid                   |                                                |
      | $._embedded.models.[0]._links.self.href       | /api/entity/model/account                      |
      | $._embedded.models.[0]._links.collection.href | /api/entity/model                              |
  Usage example 2 (replaced with random string):
  And I compare JSON file ${features.path}/ApiHistoryService/data/expected-response.json to string ${RESPONSE} excluding:
      | $._embedded.models.[*].uuid                   | do_not_compare                                              |
      | $._embedded.models.[0]._links.self.href       | do_not_compare/api/entity/model/account                     |
  */
  @And("^I compare JSON file (.*) with response (.*) excluding:$")
  public void compareJSONFileExcludingFields(String filePath, String jsonString, Map<String, String> properties)
      throws Throwable {
    filePath = VariablesTransformer.transformSingleValue(filePath);
    jsonString = VariablesTransformer.transformSingleValue(jsonString);
    properties = VariablesTransformer.transformMap(properties);
    filePath = FilenameUtils.separatorsToUnix(filePath);

    FileOperationsSteps fileOperationsSteps = new FileOperationsSteps();
    fileOperationsSteps.createTemporaryFile("jsonResponseTemp", JSON_FILE_SUFFIX, "true", "jsonFileLocation"); // NOI18N
    String jsonFileLocation = Variables.get("jsonFileLocation");
    fileOperationsSteps.createFile(jsonFileLocation, jsonString);

    fileOperationsSteps.createTemporaryFile("jsonTemplateCopyTemp", JSON_FILE_SUFFIX, "true", "jsonTemplateCopyLocation"); // NOI18N
    String jsonTemplateCopyLocation = Variables.get("jsonTemplateCopyLocation");
    fileOperationsSteps.copyFile("file", filePath, jsonTemplateCopyLocation);

    JSONOperations jsonOperations = new JSONOperations();
    jsonOperations.modifyJsonFile(jsonTemplateCopyLocation, properties);
    jsonOperations.modifyJsonFile(jsonFileLocation, properties);

    JsonNode actual = new ObjectMapper().readTree(new File(jsonFileLocation));
    JsonNode expected = new ObjectMapper().readTree(new File(jsonTemplateCopyLocation));

    JSONAssert.assertEquals(expected.toString(), actual.toString(), JSONCompareMode.NON_EXTENSIBLE);
  }

  /**
   * Usage example :
   *       Then I compare JSON file ${features.path}/client-gateway/data/client-gateway-log-level.json with response { "bpe": {"bps13-xxx-tpqh4": "DEBUG"}, "webEngine": {}, "da": {"bps13-xxx-tpqh4": "25"}, "compiler": {}, "deployment": { "deployment-mgmt-1-tpqh4": "DEBUG" } } with compare mode LENIENT and regex:
   *       | JsonPath   | Regex Pattern                                |
   *       | deployment | \{("${deployment.pod.regex}":"DEBUG"(,*))+\} |
   *       | bpe        | \{("${bps.pod.regex}":"DEBUG"(,*))+\}        |
   *       | da         | \{("${bps.pod.regex}":"25"(,*))+\}           |
   *       | webEngine  | \{\}                                         |
   *       | compiler   | \{\}                                         |
   *
   * Compare JSON file with JSON string response
   *
   * @param goldenFile JSON file to be compare against
   * @param jsonString  Actual JSON response in string
   * @param compareMode Compare mode (STRICT|LENIENT|NON_EXTENSIBLE|STRICT_ORDER)
   * @param dataTable   JSONPath vs Regex to be compare against
   * @throws Throwable from FileOperationsSteps
   */
  @And("^I compare JSON file (.*) with response (.*) with compare mode (STRICT|LENIENT|NON_EXTENSIBLE|STRICT_ORDER) and regex:$")
  public void compareJSONFileWithRegex(String goldenFile, String jsonString, JSONCompareMode compareMode,
      List<List<String>> dataTable)
      throws Throwable {
    goldenFile = VariablesTransformer.transformSingleValue(goldenFile);
    jsonString = VariablesTransformer.transformSingleValue(jsonString);
    goldenFile = FilenameUtils.separatorsToUnix(goldenFile);

    FileOperationsSteps fileOperationsSteps = new FileOperationsSteps();
    String jsonFilepathVar = "jsonFilepathVar"; // NOI18N
    fileOperationsSteps.createTemporaryFile("jsonResponseTempfile", JSON_FILE_SUFFIX, "true", jsonFilepathVar); // NOI18N
    String actualJsonFilepath = Variables.get(jsonFilepathVar);  // NOI18N
    fileOperationsSteps.createFile(actualJsonFilepath, jsonString);

    compareJsonFileObjects(goldenFile, actualJsonFilepath, compareMode, dataTable);
  }

  /**
   * Compare json file to string.
   *
   * @param filePath   the file path
   * @param jsonString the json string
   * @throws Throwable the throwable
   *
   * Example:
   * And I compare JSON file ${features.path}/ApiHistoryService/data/expected-response.json to string ${RESPONSE}
   */
  @And("^I compare JSON file (.*) to string (.*)$")
  public void compareJSONFileToString(String filePath, String jsonString) throws Throwable {

    filePath = VariablesTransformer.transformSingleValue(filePath);
    jsonString = VariablesTransformer.transformSingleValue(jsonString);

    filePath = FilenameUtils.separatorsToUnix(filePath);
    File jsonFile = new File(filePath);

    assertTrue(new JSONOperations().compare(jsonString, jsonFile),
               "JSON File matches the provided string");
  }

  /**
   * Compare.
   *
   * @param actual   the actual
   * @param expected the expected
   * @throws Throwable the throwable
   */
  @And("^I compare file (.*) with file (.*)$")
  public void compare(String actual, String expected) throws Throwable {

    FSOperations.compareFiles(actual, expected, Boolean.FALSE);
  }

  /**
   * Compare without EOL.
   *
   * @param actual   the actual
   * @param expected the expected
   * @throws Throwable the throwable
   */
  @And("^I compare files (.*) with (.*) ignoring EOL$")
  public void compareWithoutEOL(String actual, String expected) throws Throwable {

    FSOperations.compareFiles(actual, expected, Boolean.TRUE);
  }

  /**
   * Compare xml data.
   *
   * @param dataTable the data table
   * @throws IOException the throwable
   * @throws SAXException the throwable
   */
  @And("^I compare xml files:$")
  public void compareXMLData(List<List<String>> dataTable) throws IOException, SAXException {

    for (int i = 1; i < dataTable.size(); i++) {
      String expectedXMLKey = dataTable.get(i).get(0);
      String actualXMLKey = dataTable.get(i).get(1);

      FSOperations.compareXMLContent(expectedXMLKey,actualXMLKey,Boolean.FALSE);
    }
  }

  /**
   * Compare xml files.
   *
   * @param dataTable the data table
   * @throws IOException the throwable
   * @throws SAXException the throwable
   */
  /*
  Usage example :
  And I compare the xml files with path:
      | Expected XML File Path                                | Actual XML File Path                           |
      | ${features.path}/connectivity/data/goldenFile.xml     | ${features.path}/connectivity/OutputFile.xml   |
      | ${features.path}/connectivity/data/expectedOutput.xml | ${features.path}/connectivity/actualOutput.xml |
  */
  @And("^I compare the xml files with path:$")
  public void compareXMLFiles(DataTable dataTable) throws IOException, SAXException {

    List<Map<String, String>> table = dataTable.asMaps(String.class, String.class);
    for (Map<String, String> pair : table) {
      String expectedXMLKey = pair.get("Expected XML File Path"); // NOI18N
      String actualXMLKey = pair.get("Actual XML File Path"); // NOI18N

      FSOperations.compareXMLContent(expectedXMLKey,actualXMLKey,Boolean.TRUE);
    }
  }

  /**
   * Usage example 1: And I compare C:/Users/Administrator/Desktop/CopyDoc_1.pdf file to
   * C:/Users/Administrator/Desktop/CarolingiaFontFile.pdf file Usage example 2 (with difference percentage parameter):
   * And I compare C:/Users/Administrator/Desktop/CopyDoc_1.pdf file to C:/Users/Administrator/Desktop/CarolingiaFontFile.pdf
   * file with allowed difference percentage of 0.000000001
   *
   * @param source     the source
   * @param newFile    the new file
   * @param percentage the percentage
   * @throws Throwable the throwable
   */
  @And("^I compare (.*) file to (.*) file(?: with allowed difference percentage of (.*))?$")
  public void compareFilesWithBufferedImage(String source, String newFile, String percentage) throws Throwable {

    source = VariablesTransformer.transformSingleValue(source);
    newFile = VariablesTransformer.transformSingleValue(newFile);
    percentage = VariablesTransformer.transformSingleValue(percentage);

    String pngFileName = "/images";

    ImagesOperations fileCompare = new ImagesOperations();

    if (StringUtils.isEmpty(percentage)) {
      percentage = "1";
    }

    fileCompare.convertFileToImage(source, Config.get(TMP_DIR_VAR) + pngFileName + ".png");
    fileCompare.convertFileToImage(newFile, Config.get(TMP_DIR_VAR) + pngFileName + "second.png");

    boolean result = fileCompare.compareSimilarImages(Config.get(TMP_DIR_VAR) + pngFileName + ".png",
                                                      Config.get(TMP_DIR_VAR) + pngFileName + "second.png",
                                                      Double.parseDouble(percentage));

    assertTrue(result, "The files are not identical");
  }

  /**
   * Compare json file objects.
   *
   * @param goldenFile  the golden file
   * @param resultFile  the result file
   * @param compareMode the compare mode
   * @param dataTable   the data table
   * @throws Throwable the throwable
   */
/*
  Usage example :
  And I compare Golden Json file "${features.path}/da/DAadaptorsToiHDataInterfaceJSON/data/test.json" against expected Result Json file "${da.executor.path}/data-output/test_output.json" with compare mode "NON_EXTENSIBLE" excluding:
      | JsonPath                                  |  Regex Pattern                                                     |
      | DAJSONDocument.OCONTROL.EDITIONDATE.value | ([12]\d{3}-(0[1-9]\|1[0-2])-(0[1-9]\|[12]\d\|3[01]))               |
      | DAJSONDocument.OCONTROL.ERRORCOUNT.value  | //d                                                                |
  */
  @And("^I compare golden Json file \"(.*)\" against expected result Json file \"(.*)\" with compare mode \"(.*)\" excluding:$")
  public void compareJsonFileObjects(String goldenFile, String resultFile, JSONCompareMode compareMode,
      List<List<String>> dataTable) throws Throwable {

    goldenFile = VariablesTransformer.transformSingleValue(goldenFile);
    resultFile = VariablesTransformer.transformSingleValue(resultFile);
    dataTable = VariablesTransformer.transformTable(dataTable);

    JSONOperations jsonOperations = new JSONOperations();
    AssertJUnit.assertTrue(FILE_NOT_FOUND_MESSAGE + goldenFile, Files.exists(Paths.get(goldenFile)));
    AssertJUnit.assertTrue(FILE_NOT_FOUND_MESSAGE + resultFile, Files.exists(Paths.get(resultFile)));

    JSONObject expectedJsonObject = jsonOperations.convertFileToJsonObject(goldenFile, StandardCharsets.UTF_8);
    JSONObject actualJsonObject = jsonOperations.convertFileToJsonObject(resultFile, StandardCharsets.UTF_8);

    // resolve variable used inside the golden file
    String actualJsonObjectStr = VariablesTransformer.transformSingleValue(expectedJsonObject.toString());
    expectedJsonObject = new JSONObject(actualJsonObjectStr);

    dataTable = dataTable.subList(1, dataTable.size());
    Customization[] customizations = new Customization[dataTable.size()];
    for (int i = 0; i < dataTable.size(); i++) {
      customizations[i] = new Customization(dataTable.get(i).get(0),
                                            new RegularExpressionValueMatcher<>(dataTable.get(i).get(1)));
    }
    JSONCompareResult result = jsonOperations.compareCustomizedJSON(expectedJsonObject, actualJsonObject,
                                                                    compareMode,
                                                                    customizations);
    logger.info(String.format("Compare Json Files using: %s JSONCompareMode.", compareMode.name()));
    AssertJUnit.assertTrue(result.getMessage(), !result.failed());
  }

  /**
   * Compare xml file objects.
   *
   * @param goldenFile the golden file
   * @param resultFile the result file
   * @param dataTable  the data table
   * @throws Throwable the throwable
   */
/*
 Usage example :
 And I compare Golden XML file "${features.path}/da/CLASSINGScriptingFunction/data/C40879_golden.xml" against expected Result XML file "${da.executor.path}/data-output/C40879_output.xml" excluding:
   | Absolute Xpath                                                      | Regex Pattern                                         |
   | /DAXMLDocument[1]/OCONTROL[1]/EDITIONDATE[1]/data_type[1]/text()[1] | date                                                  |
   | /DAXMLDocument[1]/OCONTROL[1]/EDITIONDATE[1]/value[1]/text()[1]     | ([12]\d{3}-(0[1-9]\|1[0-2])-(0[1-9]\|[12]\d\|3[01]))  |
 */
  @And("^I compare golden XML file \"(.*)\" against expected result XML file \"(.*)\" excluding:$")
  public void compareXMLFileObjects(String goldenFile, String resultFile, List<List<String>> dataTable)
      throws Throwable {

    goldenFile = VariablesTransformer.transformSingleValue(goldenFile);
    resultFile = VariablesTransformer.transformSingleValue(resultFile);
    dataTable = VariablesTransformer.transformTable(dataTable);

    AssertJUnit.assertTrue(FILE_NOT_FOUND_MESSAGE + goldenFile, Files.exists(Paths.get(goldenFile)));
    AssertJUnit.assertTrue(FILE_NOT_FOUND_MESSAGE + resultFile, Files.exists(Paths.get(resultFile)));

    org.w3c.dom.Document goldenDoc = XMLUnit.buildControlDocument(
        new InputSource(new InputStreamReader(new FileInputStream(goldenFile), StandardCharsets.UTF_8)));
    org.w3c.dom.Document resultDoc = XMLUnit.buildTestDocument(
        new InputSource(new InputStreamReader(new FileInputStream(resultFile), StandardCharsets.UTF_8)));

    XMLUnit.setIgnoreComments(true);
    XMLUnit.setIgnoreWhitespace(true);
    XMLUnit.setIgnoreAttributeOrder(true);
    XMLUnit.setNormalizeWhitespace(true);
    XMLUnit.setNormalize(true);

    DetailedDiff diff = new DetailedDiff(XMLUnit.compareXML(goldenDoc, resultDoc));
    List<Difference> newDiff = new ArrayList<>();

    for (Object object : diff.getAllDifferences()) {
      Difference difference = (Difference) object;
      String xpathLocation = difference.getControlNodeDetail().getXpathLocation();

      Iterator<List<String>> it = dataTable.iterator();
      it.next();
      while (it.hasNext()) {
        List<String> list = it.next();
        if (list.get(0).equals(xpathLocation)) {
          XpathEngine engine = XMLUnit.newXpathEngine();
          String result = engine.evaluate(xpathLocation, resultDoc);
          if (!result.matches(list.get(1))) {
            newDiff.add(difference);
            logger.error("Expected " + difference.getDescription() + " '" +
                             difference.getControlNodeDetail().getValue() + "' but was '" + list.get(1) +
                             "' - comparing " + xpathLocation + " to "
                             + difference.getTestNodeDetail().getXpathLocation());
          }
          break;
        }
        if (!it.hasNext()) {
          newDiff.add(difference);
          logger.error(difference.toString());
        }
      }
    }
    AssertJUnit.assertTrue(newDiff.size() + " numbers of differences between XML files found.", newDiff.size() == 0);

  }

  /**
   *  This method is to replace a string in the second file and then compares it with the first file
   *
   * @param firstFile the first file location
   * @param secondFile the second file location
   * @param matchString match the string that going to be replace
   * @param replaceString replace string with this
   * @throws IOException when reading files
   */
  /*
  Usage example :
  And I compare files "${firstFile}" with "${secondFile}" by replace string TEMP_EDA_EDASYS to EDA_EDASYS
  And I compare files "${temp.dir}/dbpdm/DBPDM - Oracle Eda Sys.sql" with "${temp.dir}/dbpdm/DBPDM - Oracle Eda Sys.sql.tempschema" by replace string TEMP_EDA_EDASYS to EDA_EDASYS
   */
  @And("^I compare files \"(.*)\" with \"(.*)\"(?: by replace string (.*) to (.*))?$")
  public void compareFileWithReplaceString(String firstFile, String secondFile, String matchString, String replaceString)
      throws IOException {

    firstFile = VariablesTransformer.transformSingleValue(firstFile);
    secondFile = VariablesTransformer.transformSingleValue(secondFile);
    String matchStringTransformed = VariablesTransformer.transformSingleValue(matchString);
    String replaceStringTransformed = VariablesTransformer.transformSingleValue(replaceString);

    assertTrue(Paths.get(firstFile).toFile().exists(), FILE_NOT_FOUND_MESSAGE + firstFile);
    assertTrue(Paths.get(secondFile).toFile().exists(), FILE_NOT_FOUND_MESSAGE + secondFile);

    List<String> actualFile = Files.readAllLines(Paths.get(firstFile));
    List<String> tempFile = Files.readAllLines(Paths.get(secondFile));
    tempFile.replaceAll(updateResult -> updateResult.replaceAll(matchStringTransformed, replaceStringTransformed));

    assertEquals(actualFile, tempFile);
  }
}

/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.steps;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import com.experian.automation.helpers.ArchiversOperations;
import com.experian.automation.helpers.CSVCellInput;
import com.experian.automation.helpers.FSOperations;
import com.experian.automation.helpers.JSONOperations;
import com.experian.automation.helpers.RemoteFile;
import com.experian.automation.helpers.TextFileOperations;
import com.experian.automation.helpers.Variables;
import com.experian.automation.helpers.XMLOperations;
import com.experian.automation.logger.Logger;
import com.experian.automation.transformers.RemoteFileTransformer;
import com.experian.automation.transformers.VariablesTransformer;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.lingala.zip4j.ZipFile;
import net.minidev.json.JSONArray;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.json.JSONObject;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;
import org.testng.TestException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The type File operations steps.
 */
public class FileOperationsSteps {

  private static final String LOCALHOST_NAME = "localhost";
  private static final String LOGBACK_LOCATION = "/logback.xml";
  private static final String FILE_MESSAGE = "File ";

  private static final String LEVEL_PROP = "level";
  private static final String CONSOLE_APPENDER_PROP = "includeConsoleAppender";
  private static final String CHANGE_SET_APPENDER_PROP = "includeChangeSetJdbcAppender";
  private static final String CHANGE_APPENDER_PROP = "includeChangeJdbcAppender";
  private static final String DIFF_APPENDER_PROP = "includeDifferenceJdbcAppender";
  private static final String SIMPLE_ACTIVITY_HOOK_PROP = "includeSimpleActivityHook";
  private static final String SIMPLE_ACTIVITY_HOOK_LOCATION_PROP = "simpleActivityHookLogLocation";
  private static final long TIMEOUT_MS = 10 * 1000L;

  private static final String DOCUMENTBUILDERFACTORY = "javax.xml.parsers.DocumentBuilderFactory";
  private static final String DOCUMENTBUILDERFACTORYIMPL = "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl";

  private final Logger logger = Logger.getLogger(this.getClass());

  /**
   * Instantiates a new File operations steps.
   */
  public FileOperationsSteps() {
    // Blank Constructor
  }

  /**
   * Delete file.
   *
   * @param path the path
   * @throws Throwable the throwable
   */
/*
  And I delete file ${scripts.path}/qa/ContactAddressModelSuite/resources/deleteFile.txt
  And I delete file ${design.host}@${scripts.path}/qa/ContactAddressModelSuite/resources/deleteFile.txt
  */
  @And("^I delete file (.*)$")
  public void deleteFile(String path) throws Throwable {
    RemoteFile file = RemoteFileTransformer.transformFilePath(path);

    FSOperations fsOperations = new FSOperations(file.getHost(), LOCALHOST_NAME);

    fsOperations.delete(new File(file.getPath()));
  }

  /**
   * Count files and dirs in directory.
   *
   * @param fileCount the file count
   * @param dirCount  the dir count
   * @param dirPath   the dir path
   * @throws Throwable the throwable
   */
/*
  And I save number of files as variable filesVar and number of directories as dirsVar found in ${printDir}
  */
  @And("^I save number of files as variable (.*) and number of directories as (.*) found in (.*)$")
  public void countFilesAndDirsInDirectory(String fileCount, String dirCount, String dirPath) throws Throwable {

    dirPath = VariablesTransformer.transformSingleValue(dirPath);

    dirPath = FilenameUtils.separatorsToUnix(dirPath);
    File dir = new File(dirPath);
    int dCount = 0;

    for (File directory : dir.listFiles()) {
      if (directory.isDirectory()) {
        dCount++;
      }
    }
    int fCount = dir.list().length - dCount;

    Variables.set(dirCount, dirCount);
    Variables.set(fileCount, Integer.toString(fCount));
  }

  /**
   * Verify file amount in directory.
   *
   * @param type                the type
   * @param dirPath             the dir path
   * @param expectedCountString the expected count string
   * @throws Throwable the throwable
   */
/*
  And verify that the amount of files found in ${exampleDir} is 15
   */
  @And("I verify that the amount of (files|folders) found in (.*) is (.*)$")
  public void verifyFileAmountInDirectory(String type, String dirPath, String expectedCountString) throws Throwable {

    dirPath = VariablesTransformer.transformSingleValue(dirPath);
    expectedCountString = VariablesTransformer.transformSingleValue(expectedCountString);

    dirPath = FilenameUtils.separatorsToUnix(dirPath);

    Integer expectedCount = Integer.valueOf(expectedCountString);

    Integer actualCount = 0;

    File dir = new File(dirPath);

    if (type.equals("files")) {
      for (File directory : dir.listFiles()) {
        if (directory.isFile()) {
          actualCount++;
        }
      }
    } else {
      for (File directory : dir.listFiles()) {
        if (directory.isDirectory()) {
          actualCount++;
        }
      }
    }
    Assert.assertEquals(expectedCount, actualCount,
                        "Expected " + type + " count in " + dirPath + " is " + expectedCount + ", but actual count is "
                            + actualCount);
  }


  /**
   * Copy file.
   *
   * @param type   the type
   * @param source the source
   * @param target the target
   * @throws Throwable the throwable
   */
/*
  And I copy file ${scripts.path}/qa/TestDocumentFreeTXTExpImp.txt to ${scripts.path}/qa/TestD.txt
  And I copy file ${runtime.host}@${scripts.path}/qa/TestDocumentFreeTXTExpImp.txt to ${design.host}@${scripts.path}/qa/TestD.txt
  */
  @And("^I copy (file|directory) (.*) to (.*)$")
  public void copyFile(String type, String source, String target) throws Throwable {

    RemoteFile sourceFile = RemoteFileTransformer.transformFilePath(source);
    RemoteFile targetFile = RemoteFileTransformer.transformFilePath(target);
    String sourcePath = sourceFile.getPath();
    String targetPath = targetFile.getPath();

    FSOperations fso = new FSOperations(sourceFile.getHost(), targetFile.getHost());
    if (type.equals("file")) {
      assertTrue(fso.copyFile(sourcePath, targetPath));
    } else {
      assertTrue(fso.copyDirectory(sourcePath, targetPath));
    }
  }

  /**
   * Read file.
   *
   * @param filePath the file path
   * @param regex    the regex
   * @param var      the var
   * @throws Throwable the throwable
   */
/*
  Reads a file into a variable.
  And I save file ${scripts.path}/qa/TestDocumentFreeTXTExpImp.txt content as variable txtTemplate
  And I save file ${design.host}@${scripts.path}/qa/TestDocumentFreeTXTExpImp.txt content as variable txtTemplate
  And I save file ${scripts.path}/qa/TestDocumentFreeTXTExpImp.txt content matching regex .*[a-z]+ as variable txtTemplate
  */
  @And("^I save file (.*) content(?: matching regex (.*))? as variable (.*)$")
  public void readFile(String filePath, String regex, String var)
      throws Throwable {
    RemoteFile file = RemoteFileTransformer.transformFilePath(filePath);

    File tempFile = File.createTempFile("tempFile", FilenameUtils.getExtension(file.getPath()));
    String tempFilePath = tempFile.getAbsolutePath();
    tempFile.deleteOnExit();
    new FSOperations(file.getHost(), LOCALHOST_NAME).copyFile(file.getPath(), tempFilePath);

    tempFilePath = FilenameUtils.separatorsToUnix(tempFilePath);
    File sourceFile = new File(tempFilePath);
    String extractedValue = FileUtils.readFileToString(sourceFile, StandardCharsets.UTF_8);

    if (StringUtils.isNotEmpty(regex)) {
      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(extractedValue);

      if (matcher.find()) {
        extractedValue = matcher.group();
      } else {
        fail("Could not find text matching regex " + regex + " in file " + filePath);
      }
    }

    Variables.set(var, extractedValue);
  }

  /**
   * Create temporary file.
   *
   * @param prefix           the prefix
   * @param suffix           the suffix
   * @param saveAbsolutePath the save absolute path
   * @param fileVar          the file var
   * @throws Throwable the throwable
   */
/*
  Constructs a temporary file, one that is marked as deleteOnExit.
   And I create temporary file importDocument with extension .txt and save (full path of) the file to import.file variable
  */
  @And("^I create temporary file (.*) with extension (.*) and save( absolute path of)? the file to (.*) variable$")
  public void createTemporaryFile(String prefix, String suffix, String saveAbsolutePath, String fileVar)
      throws Throwable {
    File file = File.createTempFile(prefix, suffix);
    file.deleteOnExit();

    if (saveAbsolutePath != null) {
      Variables.set(fileVar, file.getAbsolutePath());
    } else {
      Variables.set(fileVar, FileUtils.readFileToString(file, Charset.defaultCharset()));
    }
  }

  /**
   * Make directory.
   *
   * @param dir the dir
   * @throws Throwable the throwable
   */
/*
  And I create directory ${scripts.path}/qa/flat
  */
  @And("^I create directory (.*)$")
  public void makeDirectory(String dir) throws Throwable {

    dir = VariablesTransformer.transformSingleValue(dir);

    FSOperations fso = new FSOperations();

    assertTrue(fso.makeDirectory(dir));
  }

  /**
   * Clean directory.
   *
   * @param dir the dir
   * @throws Throwable the throwable
   */
/*
  And I clean directory ${scripts.path}/qa/flat
  */
  @And("^I clean directory (.*)$")
  public void cleanDirectory(String dir) throws Throwable {

    dir = VariablesTransformer.transformSingleValue(dir);

    FSOperations fso = new FSOperations();
    assertTrue(fso.deleteDirectoryContents(dir), "Files deleted");
  }

  /**
   * Zip directory.
   *
   * @param srcFolderPath the src folder path
   * @param dstZipPath    the dst zip path
   * @throws Throwable the throwable
   */
  /*
   * Usage example(s):
   *  I zip directory ${temp.dir}/api-zip/ to C:/example/Collections Composition.pcc-api.zip
   */
  @And("^I zip directory (.*) to (.*)$")
  public void zipDirectory(String srcFolderPath, String dstZipPath) throws Throwable {

    srcFolderPath = VariablesTransformer.transformSingleValue(srcFolderPath);
    dstZipPath = VariablesTransformer.transformSingleValue(dstZipPath);

    ArchiversOperations.zip(srcFolderPath, dstZipPath);
  }

  /**
   * Unzip archive.
   *
   * @param zipFilePath   the zip file path
   * @param dstFolderPath the dst folder path
   * @throws Throwable the throwable
   */
  /*
   * Usage example(s):
   *  I unzip ${api.deploy.path}/Collections Composition.pcc-api.zip to ${temp.dir}/api-zip/ directory
   */
  @And("^I unzip (.*) to (.*) directory$")
  public void unzipArchive(String zipFilePath, String dstFolderPath) throws Throwable {

    zipFilePath = VariablesTransformer.transformSingleValue(zipFilePath);
    dstFolderPath = VariablesTransformer.transformSingleValue(dstFolderPath);

    new ArchiversOperations().unzip(zipFilePath, dstFolderPath);
  }

  /**
   * Unzip fms archive.
   *
   * @param zipFilePath   the zip file path
   * @param destFolder the dst folder path
   * @throws IOException       the exception
   */
  /*
   * Usage example(s):
   *  I unzip file ${api.deploy.path}/Collections Composition.pcc-api.zip in ${temp.dir}/api-zip/ directory
   */
  @And("^I unzip file (.*) in (.*) directory$")
  public void unzipFmsArchive(String zipFilePath, String destFolder) throws IOException {

    zipFilePath = VariablesTransformer.transformSingleValue(zipFilePath);
    destFolder = VariablesTransformer.transformSingleValue(destFolder);

    try (ZipFile zipFile = new ZipFile(zipFilePath)) {
      zipFile.extractAll(destFolder);
    }
  }

  /**
   * Create file.
   *
   * @param filePath the file path
   * @param content  the content
   * @throws Throwable the throwable
   */
/*
  And I create file C:\example.txt with content:
  """
  example content
  """
   */
  @And("^I create file (.*) with content:$")
  public void createFile(String filePath, String content) throws Throwable {

    filePath = VariablesTransformer.transformSingleValue(filePath);
    content = VariablesTransformer.transformSingleValue(content, false);

    File file = new File(filePath);
    file.deleteOnExit();
    FileUtils.writeByteArrayToFile(file, content.trim().getBytes());
  }

  /**
   * Create file with size.
   *
   * @param filePath the file path
   * @param size     the size
   * @param unit     the unit
   * @throws Throwable the throwable
   */
  @And("^I create file (.*) with size (\\d+) (kB|MB|GB)$")
  public void createFileWithSize(String filePath, int size, String unit) throws Throwable {

    filePath = VariablesTransformer.transformSingleValue(filePath);

    File file = new File(filePath);
    try (RandomAccessFile rafile = new RandomAccessFile(file, "rw")) {
      switch (unit) {
        case "kB":
          rafile.setLength(1024 * size);
          break;
        case "MB":
          rafile.setLength(1024 * 1024 * size);
          break;
        case "GB":
          rafile.setLength(1024l * 1024 * 1024 * size);
          break;
        default:
          throw new IllegalArgumentException("Unsupported unit " + unit);
      }
    }
  }

  /**
   * Update prop file.
   *
   * @param targetPath the target path
   * @param properties the properties
   * @throws Throwable the throwable
   */
/*
  And I update properties file C:\examplePropFile.properties :
  | property name          | property value        |
  */
  @And("^I update properties file (.*) :?$")
  public void updatePropFile(String targetPath, Map<String, String> properties)
      throws Throwable {

    targetPath = VariablesTransformer.transformSingleValue(targetPath);
    properties = VariablesTransformer.transformMap(properties);

    Properties prop = new Properties();

    try (InputStreamReader input = new InputStreamReader(
            new FileInputStream(FilenameUtils.normalize(targetPath, Boolean.TRUE)), StandardCharsets.UTF_8)) {
      prop.load(input);
    }

    for (String key : properties.keySet()) {
      prop.setProperty(key, properties.get(key));
    }

    try (OutputStreamWriter output = new OutputStreamWriter(new FileOutputStream(targetPath), StandardCharsets.UTF_8)) {
      prop.store(output, null);
    }
  }

  /**
   * Update yml file.
   *
   * @param targetPath the target path
   * @param properties the properties
   * @throws Throwable the throwable
   */
/*
    And I update yml properties file ${api.path}/config/application.yml :
    | experian.collections.deployment.path  | C:/collections-api-0.0.1-SNAPSHOT/Deploy |
    | server.port                           | 11080 |
    | logging                               | test  |
  */
  @And("^I update yml file (.*) :?$")
  public void updateYmlFile(String targetPath, Map<String, String> properties)
      throws Throwable {

    targetPath = VariablesTransformer.transformSingleValue(targetPath);
    properties = VariablesTransformer.transformMap(properties);
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(Feature.WRITE_DOC_START_MARKER));
    JsonNode tree = mapper.readTree(new File(targetPath));

    for (String key : properties.keySet()) {
      List<String> nodeList = Arrays.asList(key.split("\\."));

      if (nodeList.size() == 1) {
        ((ObjectNode) tree).put(nodeList.get(0), properties.get(key));
      } else {
        ObjectNode firstNode = (ObjectNode) tree.get(nodeList.get(0));

        for (int i = 1; i < nodeList.size() - 1; i++) {
          if (firstNode.get(nodeList.get(i)) == null) {
            firstNode = (ObjectNode) firstNode.putObject(nodeList.get(i));
          } else {
            firstNode = (ObjectNode) firstNode.get(nodeList.get(i));
          }
        }

        firstNode.put(nodeList.get(nodeList.size() - 1), properties.get(key));
      }
    }

    ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
    writer.writeValue(new File(targetPath), tree);
  }

  /**
   * Update json file.
   *
   * @param targetPath the target path
   * @param properties the properties
   * @throws Throwable the throwable
   */
/*
  And I update json file ${temp.dir}/example.json :
      | $['componentsDataConfig']['home-search']['components'][0]['grid']['rows'][0]['cells'][0]['values'][0]['value'] | Account Data View.Balance |
      | $['componentsDataConfig']['home-search']['components'][0]['id'] | Parameters |
      | $['helloIntervalInMS'] | 10000 |
      | $['disableAngularAnimations'] | false |
      | $['routes']['node'] | { "type": 10000 } |
  */
  @And("^I update json file (.*) :?$")
  public void updateJsonFile(String targetPath, Map<String, String> properties) throws Throwable {

    targetPath = VariablesTransformer.transformSingleValue(targetPath);
    properties = VariablesTransformer.transformMap(properties);
    JSONOperations jsonOperations = new JSONOperations();
    jsonOperations.modifyJsonFile(targetPath, properties);
  }

  /**
   * Example usage:
   *  And I save value of jsonpath $.data.Application-DV.['MissingInformationFlag'][0] from json file ${expectedJsonFile} as variable expectedMissingInformationFlag
   *
   * Save value of Jsonpath from JSON file to variable
   * @param jsonPath Json path of the value to be saved.
   * @param jsonFilepath  Json file path of the json path.
   * @param variableName  Variable name to save the json value.
   * @throws Exception  from JSONOperations
   */
  @And("^I save value of jsonpath (.*) from json file (.*) as variable (.*)?$")
  public void saveValueFromJsonFileAsVariable(String jsonPath, String jsonFilepath, String variableName)
      throws Exception {

    jsonPath = VariablesTransformer.transformSingleValue(jsonPath);
    jsonFilepath = VariablesTransformer.transformSingleValue(jsonFilepath);

    JSONOperations jsonOperations = new JSONOperations();
    Variables.set(variableName, jsonOperations.getValueFromJsonFile(jsonFilepath, jsonPath));

  }

  /**
   * Read prop value.
   *
   * @param propertyKey  the property key
   * @param targetPath   the target path
   * @param variableName the variable name
   * @throws IOException the exception that can be thrown if FileInputStream is not initiated properly
   */
/*
  I read property db.vendor from file dbConfigPath and save the value as dbVendor variable
  */
  @And("^I read property (.*) from file (.*) and save the value as (.*) variable$")
  public void readPropValue(String propertyKey, String targetPath, String variableName) throws IOException {

    targetPath = VariablesTransformer.transformSingleValue(targetPath);
    propertyKey = VariablesTransformer.transformSingleValue(propertyKey);

    Properties prop = new Properties();

    try (InputStreamReader input = new InputStreamReader(
            new FileInputStream(FilenameUtils.normalize(targetPath, Boolean.TRUE)), StandardCharsets.UTF_8)) {
      prop.load(input);
    }
    String propValue = prop.getProperty(propertyKey);
    Variables.set(variableName, propValue);
  }

  /**
   * Verify error in log file.
   *
   * @param state        the state
   * @param expectedText the expected text
   * @param filePath     the file path
   * @throws Throwable the throwable
   */
  @And("^I verify the (presence|absence) of text (.*) in file with path (.*)$")
  public void verifyErrorInLogFile(String state, String expectedText, String filePath) throws Throwable {

    expectedText = VariablesTransformer.transformSingleValue(expectedText);
    filePath = VariablesTransformer.transformSingleValue(filePath);

    File file = new File(filePath);

    String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    content = content.replace("\r\n", "").replace("\n", "");
    if (state.equals("presence")) {
      assertTrue(content.contains(expectedText),
                 "Expected error - '" + expectedText + "' is NOT present in the file: " + filePath);
    } else {
      assertFalse(content.contains(expectedText),
                  "Expected error - '" + expectedText + "' IS present in the file: " + filePath);
    }
  }

  /**
   * Verify regex in file.
   *
   * @param verifyType the verify type
   * @param regex      the regex
   * @param filePath   the file path
   * @throws Throwable the throwable
   */
  @And("^I verify the (presence|absence) of regex (.*) in file with path (.*)$")
  public void verifyRegexInFile(String verifyType, String regex, String filePath) throws Throwable {

    regex = VariablesTransformer.transformSingleValue(regex);
    filePath = VariablesTransformer.transformSingleValue(filePath);

    int fileLookupTimeout = 2000;
    boolean checkLogExist = new FSOperations().waitForFile(filePath, fileLookupTimeout);

    if (checkLogExist) {
      long count = new TextFileOperations().filterByRegex(filePath, regex);

      if (verifyType.equals("presence")) {
        assertTrue(count > 0L, "Expected regex not found: [" + regex + "]");
      } else {
        assertTrue(count == 0L, "Expected 0 but found " + count + " occurrence of regex: [" + regex + "]");
      }
    } else {
      throw new Exception("The file : [" + filePath + "] does not exist");
    }
  }

  /**
   * Find file regex.
   *
   * @param regex    the regex
   * @param filePath the file path
   * @param variable the variable
   * @throws Throwable the throwable
   */
/*
  And I save full path of file oua_exported_data.*.csv located in ${temp.dir}/ to variable exportFile
  */
  @And("^I save full path of file (.*) located in (.*) to variable (.*)")
  public void findFileRegex(String regex, String filePath, String variable) throws Throwable {

    regex = VariablesTransformer.transformSingleValue(regex);
    filePath = VariablesTransformer.transformSingleValue(filePath);

    FSOperations fsop = new FSOperations();
    Variables.set(variable, filePath + FileSystems.getDefault().getSeparator() + fsop.getFilenameRegex(filePath, regex));
  }

  /**
   * Create log file.
   *
   * @param location    the location
   * @param logLocation the log location
   * @param properties  the properties
   * @throws Throwable the throwable
   */
/*
  And I create logback file in C:\exampleDir directory with log location C:\example.log with parameters:
  | property name             | property value |
  | includeConsoleAppender    | false          |
  | includeSimpleActivityHook | true           |
  */
  @And("^I create logback file in (.*) directory with log location (.*) and parameters:$")
  public void createLogFile(String location, String logLocation, Map<String, String> properties) throws Throwable {

    location = VariablesTransformer.transformSingleValue(location);
    logLocation = VariablesTransformer.transformSingleValue(logLocation);
    properties = VariablesTransformer.transformMap(properties);
    String level;
    List<String> levels = new ArrayList<>(Arrays.asList("TRACE", "DEBUG", "INFO", "ERROR", "WARNING"));
    if (properties.keySet().contains(LEVEL_PROP)) {
      level = properties.get(LEVEL_PROP);
      assertTrue(levels.contains(level), "Level value - " + level + " is not correct!");
    } else {
      level = "INFO";
    }
    Boolean includeConsoleAppender =
        StringUtils.isEmpty(properties.get(CONSOLE_APPENDER_PROP)) || Boolean.parseBoolean(
            properties.get(CONSOLE_APPENDER_PROP));
    Boolean includeChangeSetJdbcAppender =
        !StringUtils.isEmpty(properties.get(CHANGE_SET_APPENDER_PROP)) || Boolean.parseBoolean(
            properties.get(CHANGE_SET_APPENDER_PROP));
    Boolean includeChangeJdbcAppender =
        !StringUtils.isEmpty(properties.get(CHANGE_APPENDER_PROP)) || Boolean.parseBoolean(
            properties.get(CHANGE_APPENDER_PROP));
    Boolean includeDifferenceJdbcAppender =
        !StringUtils.isEmpty(properties.get(DIFF_APPENDER_PROP)) || Boolean.parseBoolean(
            properties.get(DIFF_APPENDER_PROP));
    Boolean includeSimpleActivityHook =
        !StringUtils.isEmpty(properties.get(SIMPLE_ACTIVITY_HOOK_PROP)) || Boolean.parseBoolean(
            properties.get(SIMPLE_ACTIVITY_HOOK_PROP));

    String logbackLocation = location + LOGBACK_LOCATION;
    File file = new File(logbackLocation);
    FileOutputStream featureFileStream = new FileOutputStream(file);
    JtwigTemplate template = JtwigTemplate.classpathTemplate("steps/logback/logback.twig");
    JtwigModel model = JtwigModel.newModel();

    XMLUnit.setIgnoreWhitespace(true);
    XMLUnit.setIgnoreAttributeOrder(true);

    model.with(SIMPLE_ACTIVITY_HOOK_PROP, includeSimpleActivityHook);
    model.with(CONSOLE_APPENDER_PROP, includeConsoleAppender);
    model.with(CHANGE_SET_APPENDER_PROP, includeChangeSetJdbcAppender);
    model.with(CHANGE_APPENDER_PROP, includeChangeJdbcAppender);
    model.with(DIFF_APPENDER_PROP, includeDifferenceJdbcAppender);
    model.with(LEVEL_PROP, level);
    model.with("logLocation", logLocation);

    if (Variables.has(SIMPLE_ACTIVITY_HOOK_LOCATION_PROP)) {
      String simpleActivityHookLogLocation = Variables.get(SIMPLE_ACTIVITY_HOOK_LOCATION_PROP).toString();
      model.with(SIMPLE_ACTIVITY_HOOK_LOCATION_PROP, simpleActivityHookLogLocation);
    }
    template.render(model, featureFileStream);
    featureFileStream.close();

    Variables.set(CONSOLE_APPENDER_PROP, includeConsoleAppender.toString());
  }

  /**
   * Add package.
   *
   * @param location  the location
   * @param dataTable the data table
   * @throws Throwable the throwable
   */
/*
  And I add package logging definitions in file  C:\exampleDir with properties :
  | package                                                 | level | appenders           |
  | com.talgentra.tallyman.collections.util.SessionListener | DEBUG | RollingFileAppender |
  | com.talgentra.tallyman.domain.facade.DefaultLoginFacade | DEBUG | RollingFileAppender |
  */
  @And("^I add package logging definitions in file (.*) with properties:$")
  public void addPackage(String location, List<List<String>> dataTable) throws Throwable {

    location = VariablesTransformer.transformSingleValue(location);

    String includeConsoleAppender = Variables.get(CONSOLE_APPENDER_PROP);
    String tempLocation = location + "/temp.xml";
    File file = new File(tempLocation);
    FileOutputStream featureFileStream = new FileOutputStream(file);
    JtwigTemplate template = JtwigTemplate.classpathTemplate("steps/logback/addLogger.twig");
    JtwigModel model = JtwigModel.newModel();
    XMLUnit.setIgnoreWhitespace(true);
    XMLUnit.setIgnoreAttributeOrder(true);
    model.with("list", dataTable);
    model.with(CONSOLE_APPENDER_PROP, includeConsoleAppender);
    template.render(model, featureFileStream);
    featureFileStream.close();

    System.setProperty(DOCUMENTBUILDERFACTORY, DOCUMENTBUILDERFACTORYIMPL);
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    docFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    docFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    // protect against Denial of Service attack and remote file access
    docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    Document tempDoc = docBuilder.parse(tempLocation);
    DocumentBuilderFactory docFactoryP = DocumentBuilderFactory.newInstance();
    docFactoryP.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    docFactoryP.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    // protect against Denial of Service attack and remote file access
    docFactoryP.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    DocumentBuilder docBuilderP = docFactoryP.newDocumentBuilder();
    Document parentDoc = docBuilderP.parse(location + LOGBACK_LOCATION);

    Node parentLogger = parentDoc.getElementsByTagName("logger").item(0);
    NodeList logInTempDoc = tempDoc.getElementsByTagName("logger");
    for (int i = 0; i < logInTempDoc.getLength(); i++) {
      Node impLogger = logInTempDoc.item(i);
      Node importedLogger = parentDoc.importNode(impLogger, true);
      Element elem = (Element) importedLogger;
      parentLogger.getParentNode().insertBefore(elem, parentLogger);
    }
    System.setProperty("javax.xml.transform.TransformerFactory","com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    // protect against remote file access
    transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    DOMSource source = new DOMSource(parentDoc);
    StreamResult result = new StreamResult(new File(location + LOGBACK_LOCATION).getAbsolutePath());
    transformer.transform(source, result);

    deleteFile(tempLocation);
  }

  /**
   * Verify file existence.
   *
   * @param status   the status
   * @param location the location
   * @param files    the files
   * @throws Throwable the throwable
   */
/*
      Example usage:
          And I verify the following files exist in directory "${operations.path}/properties/distributors":
            | distributionDefinitionList_example.xml |
            | distributorConfiguration.xsd           |
  */
  @And("^I verify the following files (exist| do not exist) in directory \"(.*)\":$")
  public void verifyFileExistence(String status, String location, List<String> files) throws Throwable {

    location = VariablesTransformer.transformSingleValue(location);
    files = VariablesTransformer.transformList(files);
    Boolean expectedExistence = status.equals("exist");
    for (String fileToCheck : files) {
      String filePath = location + "/" + fileToCheck.trim();
      File currentFile = new File(FilenameUtils.normalize(filePath, Boolean.TRUE));
      if (expectedExistence) {
        assertTrue(currentFile.exists(), FILE_MESSAGE + filePath + " does not exist");
      } else {
        assertFalse(currentFile.exists(), FILE_MESSAGE + filePath + " exists");
      }
    }

  }

  /**
   * Directory exists.
   *
   * @param type           the type
   * @param dirPath        the dir path
   * @param variableToSave the variable to save
   */
  @And("^I check if (directory|file) (.*) exists and save the result as variable (.*)$")
  public void directoryExists(String type, String dirPath, String variableToSave) {

    dirPath = VariablesTransformer.transformSingleValue(dirPath);
    dirPath = FilenameUtils.separatorsToUnix(dirPath);
    Boolean exists = new File(dirPath).exists();
    Variables.set(variableToSave, exists.toString());
  }

  /**
   * Extract xm lcontent.
   *
   * @param filePath        the file path
   * @param xpathExpression the xpath expression
   * @param variableToSave  the variable to save
   * @throws Throwable the throwable
   */
  @And("^I extract XML content from file (.*) using xpath (.*) and save it to variable (.*)$")
  public void extractXMLcontent(String filePath, String xpathExpression, String variableToSave) throws Throwable {

    filePath = VariablesTransformer.transformSingleValue(filePath);

    System.setProperty(DOCUMENTBUILDERFACTORY, DOCUMENTBUILDERFACTORYIMPL);
    DocumentBuilderFactory docBuilder = DocumentBuilderFactory.newInstance();
    docBuilder.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    docBuilder.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    // protect against Denial of Service attack and remote file access
    docBuilder.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    Document document = docBuilder.newDocumentBuilder().parse(filePath);
    XPath xPath = XPathFactory.newInstance().newXPath();
    NodeList nodeList = (NodeList) xPath.compile(xpathExpression).evaluate(document, XPathConstants.NODESET);

    Document newXMLDocument = docBuilder.newDocumentBuilder().newDocument();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = newXMLDocument.importNode(nodeList.item(i), true);
      newXMLDocument.appendChild(node);
    }
    String xmlString = XMLOperations.documentToString(newXMLDocument);
    Variables.set(variableToSave, xmlString);
  }

  /**
   * Insert xm lcontent.
   *
   * @param targetPath      the target path
   * @param action          the action
   * @param xpathExpression the xpath expression
   * @param content         the content
   * @throws Throwable the throwable
   */
  @And("^I modify XML file (.*) by (inserting before|appending to|replacing) node with xpath (.*) the following XML content (.*)$")
  public void insertXMLcontent(String targetPath, String action, String xpathExpression, String content)
      throws Throwable {

    content = VariablesTransformer.transformSingleValue(content);

    RemoteFile targetFile = RemoteFileTransformer.transformFilePath(targetPath);

    String fileHost = targetFile.getHost();
    String filePath = targetFile.getPath();
    //Create temporary xml file on runtime machine which will be manipulated
    File tempXML = File.createTempFile("temporaryXML", ".xml");
    String tempXMLPath = tempXML.getAbsolutePath();
    tempXML.deleteOnExit();
    new FSOperations(fileHost, LOCALHOST_NAME).copyFile(filePath, tempXMLPath);

    System.setProperty(DOCUMENTBUILDERFACTORY, DOCUMENTBUILDERFACTORYIMPL);
    DocumentBuilderFactory docBuilder = DocumentBuilderFactory.newInstance();
    docBuilder.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    docBuilder.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    // protect against Denial of Service attack and remote file access
    docBuilder.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

    Document targetDocument = docBuilder.newDocumentBuilder().parse(tempXMLPath);

    XPath xPath = XPathFactory.newInstance().newXPath();
    Node targetNode = (Node) xPath.compile(xpathExpression).evaluate(targetDocument, XPathConstants.NODE);

    Document documentToInsert = XMLOperations.convertStringToDocument(content);

    Node nodeToInsert = targetDocument.importNode(documentToInsert.getDocumentElement(), true);
    if (action.equals("appending to")) {
      targetNode.appendChild(nodeToInsert);
    } else if (action.equals("replacing")) {
      targetNode.getParentNode().replaceChild(nodeToInsert, targetNode);
    } else {
      targetDocument.getDocumentElement().insertBefore(nodeToInsert, targetNode);
    }
    System.setProperty("javax.xml.transform.TransformerFactory","com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    // protect against remote file access
    transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

    StreamResult output = new StreamResult(new File(tempXMLPath).getAbsolutePath());
    DOMSource input = new DOMSource(targetDocument);
    transformer.transform(input, output);
    new FSOperations(LOCALHOST_NAME, fileHost).copyFile(tempXMLPath, filePath);
  }

  /**
   * Update text file.
   *
   * @param targetPath the target path
   * @param action     the action
   * @param regex      the regex
   * @param newContent the new content
   * @throws Throwable the throwable
   */
/*
  And I modify text file ${myTestPath}\testfile.conf by replacing text matching regex (?<=\ndefault_options=")[^"]+ with the following content:
    """
    ${mytestVar} this is fun
    """
  */
  @And("^I modify text file (.*) by (inserting before|appending to|replacing) text matching regex (.*) with the following content:?$")
  public void updateTextFile(String targetPath, String action, String regex, String newContent) throws Throwable {

    newContent = VariablesTransformer.transformSingleValue(newContent);
    RemoteFile targetFile = RemoteFileTransformer.transformFilePath(targetPath);

    String fileHost = targetFile.getHost();
    String filePath = targetFile.getPath();

    //Create temporary .txt file on localhost
    File tempTextFile = File.createTempFile("temporaryTXT", ".txt");
    String tempTextFilePath = tempTextFile.getAbsolutePath();
    tempTextFile.deleteOnExit();
    new FSOperations(fileHost, LOCALHOST_NAME).copyFile(filePath, tempTextFilePath);

    String fileContent = FileUtils.readFileToString(new File(tempTextFilePath), StandardCharsets.UTF_8);

    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(fileContent);

    String newFileContent;
    if (matcher.find()) {
      if (action.equals("inserting before")) {
        newFileContent = new StringBuilder(fileContent).insert(matcher.start(), newContent).toString();
      } else if (action.equals("appending to")) {
        newFileContent = new StringBuilder(fileContent).insert(matcher.end(), newContent).toString();
      } else {
        newFileContent = new StringBuilder(fileContent).replace(matcher.start(), matcher.end(), newContent).toString();
      }
      FileUtils.writeStringToFile(new File(tempTextFilePath), newFileContent, StandardCharsets.UTF_8);
      new FSOperations(LOCALHOST_NAME, fileHost).copyFile(tempTextFilePath, filePath);
    } else {
      fail("Could not find text matching regex " + regex + " in file " + targetPath);
    }
  }

  /**
   * Update text file regex.
   *
   * @param targetPath the target path
   * @param fileRegex  the file regex
   * @param action     the action
   * @param regex      the regex
   * @param newContent the new content
   * @throws Throwable the throwable
   */
/*
  And I modify file located in ${myTestPath} with name matching regex testfile.* by replacing text matching regex (?<=\ndefault_options=")[^"]+ with the following content:
    """
    ${mytestVar} this is fun
    """
  */
  @And("^I modify file located in (.*) with name matching regex (.*) by (inserting before|appending to|replacing) text matching regex (.*) with the following content:?$")
  public void updateTextFileRegex(String targetPath, String fileRegex, String action, String regex, String newContent)
      throws Throwable {
    targetPath = VariablesTransformer.transformSingleValue(targetPath);
    String foundFileName = new FSOperations().getFilenameRegex(targetPath, fileRegex);
    updateTextFile(targetPath + '/' + foundFileName, action, regex, newContent);
  }

  @And("^I modify all the files located in (.*) with name matching regex (.*) by (inserting before|appending to|replacing) text matching regex (.*) with the following content:?$")
  public void updateTextFilesRegex(String targetPath, String fileRegex, String action, String regex, String newContent)
      throws Throwable {
    targetPath = VariablesTransformer.transformSingleValue(targetPath);
    List<String> foundFileList = new FSOperations().getFileListRegex(targetPath, fileRegex);
    for (String fileName : foundFileList) {
      updateTextFile(targetPath + '/' + fileName, action, regex, newContent);
    }
  }

  /**
   * Wait for file to exist with timeout.
   *
   * @param filePath the file path
   * @param timeout  the timeout
   * @throws Throwable the throwable
   */
  @And("^I wait for file (.*) to exist with timeout (.*) milliseconds$")
  public void waitForFileToExistWithTimeout(String filePath, int timeout) throws Throwable {

    filePath = VariablesTransformer.transformSingleValue(filePath);
	filePath = FilenameUtils.separatorsToSystem(filePath);

    FSOperations fsOperations = new FSOperations();
    Boolean fileExists = fsOperations.waitForFile(filePath, timeout);
    assertTrue(fileExists, FILE_MESSAGE + filePath + " does not exist after " + timeout + " seconds.");
  }

  /*
  Then I verify with regex the values of the properties from file ${api.path}/config/encryption/com.eda.crypto.cfg.properties :
      | encrypted.spring.liquibase.change-log | ^ENC\\(.+\\)$ |

  *with regex is optional parameter!
  */

  /**
   * Property values check.
   *
   * @param regex      the regex
   * @param targetPath the target path
   * @param properties the properties
   * @throws IOException the io exception
   */
  @And("^I verify( with regex)? the values of the properties from file (.*) :$")
  public void propertyValuesCheck(String regex, String targetPath, Map<String, String> properties) throws IOException {

    targetPath = VariablesTransformer.transformSingleValue(targetPath);
    properties = VariablesTransformer.transformMap(properties);

    Properties propertiesForCheck = new Properties();
    try (InputStreamReader input = new InputStreamReader(
        new FileInputStream(FilenameUtils.normalize(targetPath, Boolean.TRUE)), StandardCharsets.UTF_8)) {
      propertiesForCheck.load(input);
    }

    List<String> differentThanExpectedValues = new ArrayList<>();

    for (Map.Entry<String, String> property : properties.entrySet()) {
      String propForCheck = propertiesForCheck.getProperty(property.getKey());
      if (StringUtils.isEmpty(regex)) {
        if (!(propForCheck.equals(property.getValue()))) {
          differentThanExpectedValues.add(property.getKey());
        }
      } else {
        if (!(propForCheck.matches(property.getValue()))) {
          differentThanExpectedValues.add(property.getKey());
        }
      }
    }
    Assert.assertTrue(differentThanExpectedValues.isEmpty(),
                      "The following properties are with different values than expected - "
                          + differentThanExpectedValues.toString());
  }

  /**
   * @param filepath   placeholder for the file that needs to be manipulated
   * @param csvUpdates placeholder for List of CSVCellInput which we can go through and update file with each input for
   *                   row and col
   */
  @And("^I modify CSV file (.*) with the following content:$")
  public void iChangeFileCsvContentWith(String filepath, DataTable csvUpdates) throws IOException {
    RemoteFile targetFile = RemoteFileTransformer.transformFilePath(filepath);
    String targetPath = targetFile.getPath();

    List<CSVCellInput> inputsTest = csvUpdates.asList(CSVCellInput.class);

    // Loop to make all entry changes for defined cells in test
    for (CSVCellInput csvInput : inputsTest) {
      this.editCellInCSV(targetPath, csvInput.getValue(), csvInput.getRow(), csvInput.getCol());
    }
  }

  /**
   * Reads the content of a CSV file and creates JSON object from header-row value map using rowNumber
   * parameter to define which row to be converted, then saves the JSON object result as variable.
   *
   * @param filePath    CSV file that should be processed. Header values are used as json keys.
   * @param rowNumber   Optional parameter. Row number to be used for json values. If missing the whole CSV file is converted to JSON object.
   * @param variableName  Variable where the JSON object is saved
   * @throws IOException  Exception
   */
  @And("^I convert CSV file (.*?)(?: line (\\d+) )?to JSON and save it as variable (.*)$")
  public void convertCSVtoJSON(String filePath, Integer rowNumber, String variableName) throws IOException {

    filePath = VariablesTransformer.transformSingleValue(filePath);
    File csvFile = new File(filePath);

    try (CSVReader csvReader = new CSVReader(new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8))) {
      List<String[]> csvContent = csvReader.readAll();

      if (rowNumber != null) {
        Map<String, String> jsonData = mapCsvHeaderToValue(csvContent, rowNumber-1);
        Variables.set(variableName, new JSONObject(jsonData).toString());
      } else {
        List<Map<String,String>> jsonObject = new ArrayList<>();
        for (int row = 1; row <= csvContent.size() - 1; row++) {
          jsonObject.add(mapCsvHeaderToValue(csvContent, row) );
        }
        Variables.set(variableName, JSONArray.toJSONString(jsonObject));
      }
    }
  }

  /**
   * Delete from json file.
   *
   * @param targetPath the target path to the json file
   * @param jsonPathsToDelete the object from json file that we want to delete
   * @throws IOException the io exception
   */
/*
  And I delete from json file ${temp.dir}/example.json :
      | $['Activities'].['GenericActivityUserUpdates'] |
      | $['Captions'].['Contact Search'] | 'Advanced Search33' |
*/
  @And("^I delete from json file (.*) :?$")
  public void removeFromJson(String targetPath,  List<String> jsonPathsToDelete) throws IOException {

    targetPath = VariablesTransformer.transformSingleValue(targetPath);
    jsonPathsToDelete = VariablesTransformer.transformList(jsonPathsToDelete);
    JSONOperations jsonOperation = new JSONOperations();
    jsonOperation.removeFromJsonFile(targetPath, jsonPathsToDelete);
  }

  /**
   * Edit CSV content by replacing value. For example on row 4, col 12 with dateVar so that file's content could be
   * unique each time
   *
   * @param targetPath path of targeted file that needs to be updated
   * @param value      value we want to put inside specific cell in file
   * @param row        number of row of cell for update from file (starting 0)
   * @param col        number of column of cell for update (starting 0)
   */
  private void editCellInCSV(String targetPath, String value, int row, int col) throws IOException {
    //new CSVReader(new InputStreamReader(new FileInputStream(targetPath), StandardCharsets.UTF_8))
    try (CSVReader reader = new CSVReader(
        new InputStreamReader(new FileInputStream(targetPath), StandardCharsets.UTF_8))) {
      List<String[]> values = reader.readAll();
      values.get(row)[col] = value;

      try (CSVWriter writer = new CSVWriter(
          new OutputStreamWriter(new FileOutputStream(targetPath), StandardCharsets.UTF_8),
          ',',
          CSVWriter.NO_QUOTE_CHARACTER,
          CSVWriter.NO_ESCAPE_CHARACTER,
          System.getProperty("line.separator"))) {
        writer.writeAll(values);
      }
    }
  }

  /**
   * Creates a map between CSV header and a row value
   *
   * @param csvContent    content of CSV file
   * @param rowToProcess  row number from the CSV file
   * @return header-value map
   */
  private Map<String, String> mapCsvHeaderToValue (List<String[]> csvContent, int rowToProcess) {
    Map<String, String> headerValueMap = new LinkedHashMap<>();
    String[] headersValues = csvContent.get(0);
    String[] rowValues = csvContent.get(rowToProcess);

    for (int i = 0; i < headersValues.length; i++) {
      headerValueMap.put(headersValues[i], rowValues[i]);
    }

    return headerValueMap;
  }

  /*
   * Usage example(s):
   * I rename folder ${spa.deploy.path}/assets/i18n/lang/de to ${spa.deploy.path}/assets/i18n/lang/ded
   */
  /**
   * Rename file or folder
   *
   * @param targetPath  the target path to the file/folder that should be renamed
   * @param newName  new name of the file/folder
   */
  @And("^I rename (?:file|folder) (.*) to (.*)$")
  public void renameFileOrFolder(String targetPath, String newName) {
    targetPath = VariablesTransformer.transformSingleValue(targetPath);
    newName = VariablesTransformer.transformSingleValue(newName);
    File sourceFile = new File(targetPath);
    File destinationFile = new File(newName);
    if (!sourceFile.renameTo(destinationFile)) {
      throw  new TestException(("File or Folder cannot be renamed"));
    }
  }

  /**
   * Extract attribute from XML file
   *
   * Usage example(s):
   * I extract attributes with path "/daBundle/@name" from all the files located in ${temp.dir}/pccApi/da-service/ with name matching regex [a-zA-Z0-9_]*.xml and save them to daScriptNames
   * @param attributesPath path to attribute
   * @param targetPath path to XML file
   * @param fileRegex regex
   * @param daScriptVariables already extracted value
   * @throws SAXException the exception
   * @throws ParserConfigurationException the ParserConfigurationException
   * @throws XPathExpressionException the XpathExpressionException
   * @throws IOException the io exception
   */
  @And("^I extract attributes with path (.*) from all the files located in (.*) with name matching regex (.*) and save them to (.*)$")
  public void extractAttribute(String attributesPath, String targetPath, String fileRegex, String daScriptVariables)
      throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
    targetPath = VariablesTransformer.transformSingleValue(targetPath);
    List<String> foundFileList = new FSOperations().getFileListRegex(targetPath, fileRegex);

    Set<String> daScriptVariableParse = new HashSet<>();

    for (String fileName : foundFileList) {
      List<String> daBundle = XMLOperations.extractXMLattributes(targetPath + fileName, attributesPath);
      if(daBundle.isEmpty()) {
        throw new TestException("Element with attribute " + attributesPath + " not found");//NOI18N
      }
      daScriptVariableParse.add(daBundle.get(0));
    }
    Variables.set(daScriptVariables, org.apache.commons.lang3.StringUtils.join(daScriptVariableParse, ','));
  }

  /**
   *  Compare if comma separated values inside a String variable contains all values from a list
   *
   *  Usage example(s):
   *  I verify variable daScriptNames contains unique values:
   *  | DerivedDataScriptTwo |
   *  | DerivedDataScriptOne |
   *
   * @param csvVariableName extracted value the name of the variable that contains comma separated values
   * @param compareValues names to be compared with
   */
  @And("^I verify variable (.*) contains unique values:$")
  public void compareCsvValuesToList(String csvVariableName, List<String> compareValues) {
    String csvString = Variables.get(csvVariableName);
    assertTrue(Arrays.asList(csvString.split(",")).containsAll(compareValues));
  }

  /**
   *  Verify contents of file1 are present in file2
   *
   *  Usage example(s):
   *    And I verify lines from file file1.csv are present in file file2.csv
   *
   * @param file1 the file whose contents will be checked if present in file2
   * @param file2 the file whose contents are expected to contain the contents from file1
   *
   * @throws IOException IOException thrown if unable to read file from the provided file path
   */
  @And("^I verify lines from file (.*) are present in file (.*)$")
  public void verifyPresenceOfLinesInFile(String file1, String file2) throws IOException {

    file1 = VariablesTransformer.transformSingleValue(file1);
    file2 = VariablesTransformer.transformSingleValue(file2);

    List<String> file1Contents = Files.readAllLines(Paths.get(file1));
    List<String> file2Contents = Files.readAllLines(Paths.get(file2));

    for (String line : file1Contents) {
      assertTrue(
          file2Contents.contains(line),
          String.format("File %s: does not contain %s.%nFile content: %s%n", file2, line, file2Contents) //NOI18N
      );
    }
  }

  /**
   *  Verify number of lines in a file
   *
   *  Usage example(s):
   *    And I verify there are 5 lines in file file1.csv
   *
   * @param numOfLines expected number of lines in the file
   * @param filePath the path to the file
   *
   * @throws IOException IOException thrown if unable to read file from the provided file path
   */
  @And("^I verify there are (.*) lines in file (.*)$")
  public void verifyNumberOfLinesInFile(String numOfLines, String filePath) throws IOException {

    numOfLines = VariablesTransformer.transformSingleValue(numOfLines);
    filePath = VariablesTransformer.transformSingleValue(filePath);
    long expectedNumOfLines = Long.parseLong(numOfLines);

    try(Stream<String> fileStream = Files.lines(Paths.get(filePath))) {
      long actualNumOfLines = fileStream.count();
      assertEquals(
          expectedNumOfLines,
          actualNumOfLines,
          String.format("Verify actual number of lines (%d) are equal to expected number of lines (%d)", actualNumOfLines, expectedNumOfLines) //NOI18N
      );
    }
  }

  /**
   *  Check XML file contains attribute
   * Usage example(s):
   * And I verify that XML file ${temp.dir}/pccApi/data-model/data-model.xml contains:
   * | /entityComposition/entity/@name='REL'              |
   * | /entityComposition/entity/@name='Interface Data'   |
   * | /entityComposition/entity/@name='DDSOne_LDS'       |
   * | /entityComposition/entity/@name='DDSTwo_LDS'       |
   * | /entityComposition/dataView/@name='Account Person' |
   *
   * @param filePath path to XML file
   * @param xpathExpressions path to attribute inside XML file
   * @throws SAXException the exception
   * @throws ParserConfigurationException the parcerConfigurationException
   * @throws XPathExpressionException the XpathExpressionException
   * @throws IOException the io exception
   */
  @And("^I verify that XML file (.*) contains attributes:$")
  public void containsAttributes (String filePath, List<String> xpathExpressions)
      throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
    SoftAssert softAssert = new SoftAssert();
    Document xmlDocument = XMLOperations.getXmlDocument(filePath);
    for (String xpathExpression : xpathExpressions) {
      softAssert.assertTrue(XMLOperations.containsXpathInXML(xmlDocument, xpathExpression));
    }
    softAssert.assertAll();
  }

 /**
   *  Wait and verify text in a file for certain seconds
   *
   *  Usage example(s):
   *    And I wait for text Business Process Composition [ErosAlphaV6.bpc.zip] deployment completed in file ${bpe.path}/Log/BPE.log for 10 seconds
   *
   * @param expectedText expected text to verify in the file
   * @param fullFilePath the path to the file
   * @param seconds number of seconds to wait for the text to appear
   *
   * @throws IOException IOException thrown if unable to read file from the provided file path
   */
  @And("^I wait for text (.*) in file (.*) for (\\d+) seconds$")
  public void waitForText(String expectedText, String fullFilePath, int seconds) throws Exception {
    fullFilePath = VariablesTransformer.transformSingleValue(fullFilePath);

    FSOperations fsOperations = new FSOperations();
    int fileTimeOut = 60 * 1000;
    fsOperations.waitForFile(fullFilePath, fileTimeOut);
    File file = new File(fullFilePath);
    String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    long timeoutTime = System.currentTimeMillis() + seconds * 1000;
    boolean textFound = false;
    do {
      if (content.contains(expectedText)) {
        textFound = true;
      }
    } while (textFound && System.currentTimeMillis() > timeoutTime);
    if (!textFound) {
      Thread.sleep(TIMEOUT_MS);
      waitForText(expectedText, fullFilePath, seconds);
    }
  }

  /**
   * Check if the actual file size fulfil the expected one
   *
   * @param filePath        the full path to the file
   * @param levelOfEquality how we are going to compare the file <, = or >
   * @param fileSize        expected file size
   * @param sizePrefix      the unit for measuring
   * @throws IllegalAccessException if the file is not found
   */
  @And("^I check that file size of (.*) is (greater than|equal|less than) (\\d+) (kilo|mega|giga)?bytes")
  public void checkFileSize(String filePath, String levelOfEquality, long fileSize, String sizePrefix)
      throws IllegalAccessException {
    filePath = VariablesTransformer.transformSingleValue(filePath, false);
    filePath = FilenameUtils.separatorsToSystem(filePath);
    File fileForExamination = new File(filePath);
    if(!fileForExamination.exists()){
      throw new IllegalAccessException("File does not exists"); //NOI18N
    }
    long expectedFileSize;
    switch (sizePrefix) {
      case "kilo": //NOI18N
        expectedFileSize = fileSize * 1024;
        break;
      case "mega": //NOI18N
        expectedFileSize = fileSize * 1024 * 1024;
        break;
      case "giga": //NOI18N
        expectedFileSize = fileSize * 1024 * 1024 * 1024;
        break;
      default:
        expectedFileSize = fileSize;
        break;
    }

    long actualFileSize = fileForExamination.length();
    String errorMessage = "File size does not meet the criteria for:"; //NOI18N
    switch (levelOfEquality) {
      case "greater than": //NOI18N
        assertTrue(actualFileSize > expectedFileSize,errorMessage+levelOfEquality);
        break;
      case "equal": //NOI18N
        assertEquals(expectedFileSize, actualFileSize,errorMessage+levelOfEquality);
        break;
      case "less than": //NOI18N
        assertTrue(actualFileSize < expectedFileSize,errorMessage+levelOfEquality);
        break;
      default:
        throw new IllegalArgumentException("Unknown comparison"); //NOI18N
    }
  }
}

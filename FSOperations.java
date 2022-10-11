/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.experian.automation.logger.Logger;
import com.experian.automation.transformers.VariablesTransformer;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.BooleanUtils;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.XMLUnit;
import org.xml.sax.SAXException;


/**
 * The type Fs operations.
 */
public class FSOperations {

  private final Logger logger = Logger.getLogger(this.getClass());
  private STAFExecutor stafExecutor = new STAFExecutor();

  private static final String LOCALHOST_NAME = "localhost";
  private static final String FILE_MESSAGE = "File ";

  /**
   * The Src host.
   */
  protected String srcHost;
  /**
   * The Dst host.
   */
  protected String dstHost;

  /**
   * Instantiates a new Fs operations.
   */
  public FSOperations() {
    srcHost = LOCALHOST_NAME;
    dstHost = LOCALHOST_NAME;
  }

  /**
   * Instantiates a new Fs operations.
   *
   * @param srcHost the src host
   * @param dstHost the dst host
   */
  public FSOperations(String srcHost, String dstHost) {
    this.srcHost = srcHost;
    this.dstHost = dstHost;
  }

  /**
   * Rename file boolean.
   *
   * @param srcFile the src file
   * @param dstFile the dst file
   * @return the boolean
   */
  public boolean renameFile(String srcFile, String dstFile) {
    boolean renamed = false;
    File oldFile = new File(FilenameUtils.separatorsToUnix(srcFile));
    File newFile = new File(FilenameUtils.separatorsToUnix(dstFile));
    if (oldFile.exists() && oldFile.isFile()) {
      if (!newFile.exists()) {
        if (oldFile.renameTo(newFile)) {
          logger.info(String.format("%s%s successfully renamed to %s", FILE_MESSAGE, srcFile.toString(), dstFile));
          renamed = true;
        } else {
          logger.info("The file can't be renamed.");
        }
      } else {
        logger.info(String.format("%s%s already exists !", FILE_MESSAGE, newFile.toString()));
      }
    } else {
      logger.info(String.format("%s%s doesnt exist !", FILE_MESSAGE, srcFile.toString()));
    }
    return renamed;
  }

  /**
   * Copy file to directory boolean.
   *
   * @param srcFile the src file
   * @param dstFile the dst file
   * @return the boolean
   * @throws Exception the exception
   */
  public boolean copyFileToDirectory(File srcFile, File dstFile) throws Exception {
    return copyFileToDirectory(srcFile.getAbsolutePath(), dstFile.getAbsolutePath());
  }

  /**
   * Copy file to directory boolean.
   *
   * @param srcPath the src path
   * @param dstPath the dst path
   * @return the boolean
   * @throws Exception the exception
   */
  public boolean copyFileToDirectory(String srcPath, String dstPath) throws Exception {
    srcPath = FilenameUtils.separatorsToUnix(srcPath);
    dstPath = FilenameUtils.separatorsToUnix(dstPath);

    if (isLocalOperation()) {
      FileUtils.copyFileToDirectory(new File(srcPath), new File(dstPath));
    } else {
      if (!exists(dstPath, dstHost)) {
        makeDirectory(dstPath, dstHost);
      }
      String stafCommand = String.format("COPY FILE %s TODIRECTORY %s TOMACHINE %s",
                                         srcPath, dstPath, dstHost);

      return stafExecutor.execute(srcHost, "FS", stafCommand) == 0;
    }

    return true;
  }

  /**
   * Copy file boolean.
   *
   * @param srcFile the src file
   * @param dstFile the dst file
   * @return the boolean
   * @throws Exception the exception
   */
  public boolean copyFile(File srcFile, File dstFile) throws Exception {
    return copyFile(srcFile.getAbsolutePath(), dstFile.getAbsolutePath());
  }

  /**
   * Copy file boolean.
   *
   * @param srcPath the src path
   * @param dstPath the dst path
   * @return the boolean
   * @throws Exception the exception
   */
  public boolean copyFile(String srcPath, String dstPath) throws Exception {
    srcPath = FilenameUtils.separatorsToUnix(srcPath);
    dstPath = FilenameUtils.separatorsToUnix(dstPath);

    if (isLocalOperation()) {
      FileUtils.copyFile(new File(srcPath), new File(dstPath));
    } else {
      String dstParentDir = new File(dstPath).getParent();
      if (!exists(dstParentDir, dstHost)) {
        makeDirectory(dstParentDir, dstHost);
      }
      String stafCommand = String.format("COPY FILE %s TOFILE %s TOMACHINE %s",
                                         srcPath, dstPath, dstHost);

      return stafExecutor.execute(srcHost, "FS", stafCommand) == 0;
    }

    return true;
  }

  /**
   * Copy directory boolean.
   *
   * @param srcFile the src file
   * @param dstFile the dst file
   * @return the boolean
   * @throws Exception the exception
   */
  public boolean copyDirectory(File srcFile, File dstFile) throws Exception {
    return copyDirectory(srcFile.getAbsolutePath(), dstFile.getAbsolutePath());
  }

  /**
   * Copy directory boolean.
   *
   * @param srcPath the src path
   * @param dstPath the dst path
   * @return the boolean
   * @throws Exception the exception
   */
  public boolean copyDirectory(String srcPath, String dstPath) throws Exception {
    srcPath = FilenameUtils.separatorsToUnix(srcPath);
    dstPath = FilenameUtils.separatorsToUnix(dstPath);

    if (isLocalOperation()) {
      FileUtils.copyDirectory(new File(srcPath), new File(dstPath));
    } else {
      String stafCommand = String.format("COPY DIRECTORY %s TODIRECTORY %s TOMACHINE %s RECURSE",
                                         srcPath, dstPath, dstHost);

      return stafExecutor.execute(srcHost, "FS", stafCommand) == 0;
    }
    return true;
  }

  /**
   * Delete directory contents boolean.
   *
   * @param directory the directory
   * @return the boolean
   * @throws Exception the exception
   */
  public boolean deleteDirectoryContents(File directory) throws Exception {
    return deleteDirectoryContents(directory.getAbsolutePath());
  }

  /**
   * Delete directory contents boolean.
   *
   * @param directoryPath the directory path
   * @return the boolean
   * @throws Exception the exception
   */
  public boolean deleteDirectoryContents(String directoryPath) throws Exception {
    return deleteDirectoryContents(directoryPath, true, true);
  }

  /**
   * Delete directory contents boolean.
   *
   * @param directoryPath     the directory path
   * @param deleteDirectories the delete directories
   * @param deleteFiles       the delete files
   * @return the boolean
   * @throws Exception the exception
   */
  public boolean deleteDirectoryContents(String directoryPath, boolean deleteDirectories, boolean deleteFiles)
      throws Exception {
    if (isLocalOperation()) {
      return deleteLocalDirContents(directoryPath, deleteDirectories, deleteFiles);
    }

    String stafCommand = String.format("DELETE ENTRY %s CONFIRM RECURSE CHILDREN", directoryPath);

    if (deleteDirectories && !deleteFiles) {
      stafCommand = String.format("LIST DIRECTORY %s TYPE D", directoryPath);

      if (stafExecutor.execute(srcHost, "FS", stafCommand) != 0) {
        return false;
      } else {
        LinkedList<String> direcoriesList = stafExecutor.getResultList();

        for (String directory : direcoriesList) {
          stafCommand = String.format("DELETE ENTRY %s CONFIRM RECURSE CHILDREN", directoryPath + "/" + directory);
          if (stafExecutor.execute(srcHost, "FS", stafCommand) != 0) {
            return false;
          }
        }

        return true;
      }
    } else if (!deleteDirectories && deleteFiles) {
      stafCommand = String.format("DELETE ENTRY %s CONFIRM CHILDREN TYPE F", directoryPath);
    }

    return stafExecutor.execute(srcHost, "FS", stafCommand) == 0;

  }

  /**
   * Delete local directory contents boolean.
   *
   * @param directoryPath       the directory path
   * @param deleteDirectories   the delete directories
   * @param deleteFiles         the delete files
   * @return
   * @throws IOException
   */
  private boolean deleteLocalDirContents(String directoryPath, boolean deleteDirectories, boolean deleteFiles)
      throws IOException {
    boolean isDeleted = false;
    File directory = new File(directoryPath);

    if (directory.exists() && directory.isDirectory()) {
      isDeleted = true;
      for (File fileOrDir : directory.listFiles()) {
        if ((deleteDirectories && fileOrDir.isDirectory()) ||
            (deleteFiles && fileOrDir.isFile())) {
          FileUtils.forceDelete(fileOrDir);

          isDeleted = isDeleted && !fileOrDir.exists();
        }
      }
    }
    return isDeleted;
  }

  /**
   * Delete boolean.
   *
   * @param file the file
   * @return the boolean
   * @throws Exception the exception
   */
  public boolean delete(File file) throws Exception {
    return delete(file.getAbsolutePath());
  }

  /**
   * Delete boolean.
   *
   * @param path the path
   * @return the boolean
   * @throws Exception the exception
   */
  public boolean delete(String path) throws Exception {
    path = FilenameUtils.separatorsToUnix(path);

    if (isLocalOperation()) {
      File file = new File(path);

      if (file.exists()) {
        FileUtils.forceDelete(file);
      }
      return !file.exists();
    } else {
      String stafCommand = String.format("DELETE ENTRY %s CONFIRM RECURSE", path);

      return stafExecutor.execute(srcHost, "FS", stafCommand) == 0;
    }
  }

  private boolean isLocalOperation() {
    return srcHost.equals(dstHost) && srcHost.equals(LOCALHOST_NAME);
  }

  /**
   * Make directory boolean.
   *
   * @param dstFile the dst file
   * @return the boolean
   * @throws Exception the exception
   */
  public boolean makeDirectory(File dstFile) throws Exception {
    return makeDirectory(dstFile.getAbsolutePath());
  }

  /**
   * Make directory boolean.
   *
   * @param path the path
   * @return the boolean
   * @throws Exception the exception
   */
  public boolean makeDirectory(String path) throws Exception {
    return makeDirectory(path, srcHost);
  }

  /**
   * Make directory boolean.
   *
   * @param path the path
   * @param host the host
   * @return the boolean
   * @throws Exception the exception
   */
  public boolean makeDirectory(String path, String host) throws Exception {
    path = FilenameUtils.separatorsToUnix(path);

    if (isLocalOperation()) {
      File dstDir = new File(path);
      FileUtils.forceMkdir(dstDir);
    } else {
      String stafCommand = String.format("CREATE DIRECTORY %s FULLPATH", path);

      return stafExecutor.execute(host, "FS", stafCommand) == 0;
    }

    return true;
  }

  /**
   * Exists boolean.
   *
   * @param path the path
   * @return the boolean
   * @throws Exception the exception
   */
  public boolean exists(String path) throws Exception {
    return exists(path, srcHost);
  }

  /**
   * Exists boolean.
   *
   * @param path the path
   * @param host the host
   * @return the boolean
   * @throws Exception the exception
   */
  public boolean exists(String path, String host) throws Exception {
    path = FilenameUtils.separatorsToUnix(path);

    if (isLocalOperation()) {
      return new File(path).exists();
    } else {
      String stafCommand = String.format("QUERY ENTRY %s", path);

      return stafExecutor.execute(host, "FS", stafCommand) == 0;
    }
  }

  /**
   * Gets home path.
   *
   * @return the home path
   * @throws Exception the exception
   */
  public String getHomePath() throws Exception {
    if (isLocalOperation()) {
      return FileUtils.getUserDirectory().getAbsolutePath();
    } else {
      String stafCommand = "resolve string {Staf/Env/USERPROFILE}";
      stafExecutor.execute(srcHost, "var", stafCommand);
      return stafExecutor.getResult().replace("\\", "/");
    }
  }

  /**
   * Wait for file boolean.
   *
   * @param filePath      the file path
   * @param timeoutMillis the timeout millis
   * @return the boolean
   * @throws Exception the exception
   */
  public boolean waitForFile(String filePath, int timeoutMillis) throws Exception {
    return waitForFileExpectedState(filePath,timeoutMillis,true);
  }

  /**
   * Wait for file to be in an expected state. Boolean.
   *
   * @param filePath      the file path
   * @param timeoutMillis the timeout millis
   * @param isExpected    the expected state of the file - to be present (true) or missing (false)
   * @return the boolean
   *         true - if the file is in the expected state
   *         false - otherwise
   * @throws InterruptedException the exception
   */
  public boolean waitForFileExpectedState(String filePath, int timeoutMillis, Boolean isExpected) throws InterruptedException{
    long timeout = timeoutMillis + System.currentTimeMillis();
    File file = new File(filePath);
    Boolean isAvailable = file.exists();
    while (!isAvailable.equals(isExpected) && System.currentTimeMillis() < timeout) {
      Thread.sleep(500);
      isAvailable = file.exists();
    }
    try {
      assertEquals(isAvailable,isExpected);
      return true;
    } catch (AssertionError ex) {
      String expectedState = isExpected ? "present" : "missing";
      logger.error(String.format("The procedure timed out while waiting for file %s to be %s.%n Full error: %s", filePath, expectedState, ex));
      return false;
    }
  }

  /**
   * Returns whether file with name that matches regex exists until a set timeout
   *
   * @param filePath      The parent directory of the file
   * @param regexFileName The regex to match file name
   * @param timeoutMillis Check timeout duration Example: C:/Temp/Lib/(.*)ibm(.*).jar
   * @return the boolean
   * @throws Exception the exception
   */
  public boolean waitForFileRegex(String filePath, String regexFileName, int timeoutMillis) throws Exception {
    long timeout = timeoutMillis + System.currentTimeMillis();
    boolean available = false;
    File directory = new File(filePath);

    while (System.currentTimeMillis() < timeout) {
      available = directory.listFiles((FileFilter) new RegexFileFilter(regexFileName)).length > 0;
      if (available) {
        return true;
      }
      Thread.sleep(500);
    }
    return false;
  }

  /**
   * Searches with regex for the full file name and returns it
   *
   * @param filePath      The parent directory of the file
   * @param regexFileName The regex of of the file Example: C:/Temp/Lib/(.*)ibm(.*).jar
   * @return the filename regex
   * @throws Throwable the throwable
   */
  public String getFilenameRegex(String filePath, String regexFileName)
      throws Throwable {
    File directory = new File(filePath);
    return FileUtils.listFiles(directory, new RegexFileFilter(regexFileName), null).iterator().next().getName();
  }

  /**
   * Searches with regex for full file name and returns a LinkedList with the results
   *
   * @param filePath      The parent directory of the file
   * @param regexFileName The regex of of the file Example: C:/Temp/Lib/(.*)ibm(.*).jar
   * @return the filenameList regex
   * @throws Throwable the throwable
   */
  public List<String> getFileListRegex(String filePath, String regexFileName) {
    File directory = new File(filePath);
    return FileUtils.listFiles(directory, new RegexFileFilter(regexFileName), null).stream()
        .map(File::getName)
        .collect(Collectors.toList());
  }

  /**
   * Read value from property configuration file
   * <p>
   * Example: (file, "myservice.port")
   *
   * @param filePath - Path to config file
   * @param property - Name of property to read
   * @return String - value
   * @throws IOException the IO exception thrown if the file is not loaded correctly
   */
  public String getValueFromPropertiesFile(String filePath, String property) throws IOException {
    Properties systemConfigFile = new Properties();
    FileInputStream inputStream = new FileInputStream(filePath);
    try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
      systemConfigFile.load(inputStreamReader);
    }
    return systemConfigFile.getProperty(property);
  }

  /**
   * Searches with regex for the file name within the directory and returns all match file as array
   * <p>
   * Example: ("C:/Temp/log", "*.log")
   *
   * @param directory - Folder path to get the file that match the wilcard
   * @param wildcard  - regex or pattern of the files
   * @return File Array - list of files that match the wilcard inside the directory
   */
  public static File[] getFilesByWildcard(String directory, String wildcard) {
    File dir = new File(directory);
    FileFilter fileFilter = new WildcardFileFilter(wildcard);
    return dir.listFiles(fileFilter);
  }

  /**
   * Compare files With OR Without EOL
   *
   * @param actual - Output file
   * @param expected - Golden file
   * @boolean ignoreEOL - Exclude EndOfLine in file comparison
   * @throws Exception the exception
   */
  public static void compareFiles(String actual, String expected, Boolean ignoreEOL) throws Exception {

    actual = VariablesTransformer.transformSingleValue(actual);
    expected = VariablesTransformer.transformSingleValue(expected);

    actual = FilenameUtils.separatorsToUnix(actual);
    expected = FilenameUtils.separatorsToUnix(expected);

    File actualFile = new File(actual);
    File expectedFile = new File(expected);

    String actualContent = FileUtils.readFileToString(new File(actual), StandardCharsets.UTF_8);
    String expectedContent = FileUtils.readFileToString(new File(expected), StandardCharsets.UTF_8);

    if (BooleanUtils.isTrue(ignoreEOL)) {
      assertTrue(FileUtils.contentEqualsIgnoreEOL(actualFile, expectedFile, String.valueOf(Charset.defaultCharset())),
                 "The files are not identical between file " + actualFile + " and file " + expectedFile);
    } else {
      assertEquals(actualContent, expectedContent, "Files have different content.");
    }
  }

  /**
   * Compare XML content
   *
   * @param expectedXML - Golden file
   * @param actualXML - Actual file
   * @param isFileInput - read xml file to string
   * @throws IOException the exception
   * @throws SAXException the exception
   */
  public static void compareXMLContent(String expectedXML, String actualXML, Boolean isFileInput)
      throws IOException, SAXException {

    XMLUnit.setIgnoreComments(true);
    XMLUnit.setIgnoreWhitespace(true);
    XMLUnit.setIgnoreAttributeOrder(true);
    XMLUnit.setNormalizeWhitespace(true);
    XMLUnit.setNormalize(true);

    expectedXML = VariablesTransformer.transformSingleValue(expectedXML);
    actualXML = VariablesTransformer.transformSingleValue(actualXML);

    String expectedXMLContent;
    String actualXMLContent;

    if (isFileInput){
      expectedXMLContent = FileUtils.readFileToString(new File(expectedXML), StandardCharsets.UTF_8); // NOI18N
      actualXMLContent = FileUtils.readFileToString(new File(actualXML), StandardCharsets.UTF_8); // NOI18N
    } else {
      expectedXMLContent = expectedXML;
      actualXMLContent = actualXML;
    }

    DetailedDiff diff = new DetailedDiff(XMLUnit.compareXML(expectedXMLContent, actualXMLContent));
    assertTrue(diff.identical() || diff.similar(),
               "Differences between " + expectedXMLContent + " and " + actualXMLContent + "  found: " // NOI18N
                   + diff.toString()); // NOI18N
  }
}

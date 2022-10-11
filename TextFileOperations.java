/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * The type Text file operations.
 */
public class TextFileOperations {

  /**
   * The method finds a specific pattern in some line inside a text file and replaces the line with a given input.
   *
   * @param file      The full path to the XML file (e.g C:/Temp/file.xml)
   * @param findRegex Regular expression which will be used for locating the string to be replaced
   * @param replace   Replacing string
   * @throws IOException the io exception
   */
  public void replaceStringInFile(String file, String findRegex, String replace) throws IOException {
    File fileToEdit = new File(file);
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileToEdit), StandardCharsets.UTF_8))){

      String line = "", oldtext = "";
      while ((line = reader.readLine()) != null) {
        oldtext += line + System.lineSeparator();
      }
      String newtext = oldtext.replaceAll(findRegex, replace);

      try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileToEdit), StandardCharsets.UTF_8))) {
        writer.write(newtext);
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  /**
   * Search for number of occurrence through a regex pattern in a text file
   *
   * @param file  The full path to the text file (e.g C:/Temp/file.xml)
   * @param regex Regular expression which will be used for locating the string to be found
   * @return Number of occurrence which matches the pattern
   * @throws IOException the io exception
   */
  public long filterByRegex(String file, String regex) throws IOException {
    Pattern pattern = Pattern.compile(regex);
    Path path = Paths.get(file);
    long found;

    try (BufferedReader reader = Files.newBufferedReader(path)) {
      found = reader
          .lines()
          .filter(pattern.asPredicate())
          .count();
    }

    return found;
  }

  /**
   * Search for number of occurrence through a regex pattern in a text file with retry and delay
   *
   * @param file  The full path to the XML file (e.g C:/Temp/file.xml)
   * @param regex Regular expression which will be used for locating the string to be found
   * @param retry number of loop or re-run
   * @param delay interval gap for each retry
   * @return Number of occurrence which matches the pattern
   * @throws Exception no pattern match
   */
  public long filterByRegex(String file, String regex, int retry, long delay) throws Exception {

    return new RetryExecutor().delay(delay).retry(retry).execute(() -> {

      long count = filterByRegex(file, regex);

      if (count == 0L) {
        throw new Exception("Pattern not matched by regex: [" + regex + "]");
      }

      return count;
    });
  }
}

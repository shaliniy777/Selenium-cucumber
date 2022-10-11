/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The type Variables.
 */
public class Variables {

  private static HashMap<Long, Properties> variables = new HashMap<Long, Properties>();

  /**
   * Types of comparator functions. Only one function at a time is supported
   */
  private enum ComparatorFunctions {
    /**
     * Ignore the case for a substring in a bigger string
     * Example: httpHeader <=> httpIGNORE_CASE(h)eader
     * httpHeAder <=> httpIGNORE_CASE(h)eIGNORE_CASE(a)der
     */
    IGNORE_CASE,
    /**
     * Ignore a substring in a bigger string.
     * Example: worker_lkj345jkl_data_object <=> worker_IGNORE_CHARS(*)_data_object
     */
    IGNORE_CHARS
  }

  private static class FunctionComparator {

    private static final String FUNCTION_OPENER = "\\("; // NOI18N
    private static final String FUNCTION_CLOSER = "\\)"; // NOI18N
    private static final String REGEX_GROUP_ANY_STRING_LAZY = "(.*?)"; // NOI18N

    /**
     * Compare text containing functions. This method can be used when you want to compare 2 strings that contain
     * dynamic sections. If the dynamic sections are identified, the user can apply a processing function to the section
     * so that a match can be found. The processing will be applied on the section in both input parameters so that they
     * can be equalized and return a match
     *
     * @param textWithFunctions text containing functions. The function logic will be applied on the part of the text
     *                          that the function is wrapping
     * @param comparedTo        string that textWithFunctions will be compared to after the functions are run
     * @return boolean flag indicating whether the 2 texts are matching
     */
    public boolean compare(String textWithFunctions, String comparedTo) {
      Pattern ignoreCasePattern = Pattern.compile(
          ComparatorFunctions.IGNORE_CASE.name() + FUNCTION_OPENER + REGEX_GROUP_ANY_STRING_LAZY + FUNCTION_CLOSER);
      Matcher ignoreCaseMatcher = ignoreCasePattern.matcher(textWithFunctions);

      Pattern ignoreCharsPattern = Pattern.compile(
          ComparatorFunctions.IGNORE_CHARS.name() + FUNCTION_OPENER + REGEX_GROUP_ANY_STRING_LAZY + FUNCTION_CLOSER);
      Matcher ignoreCharsMatcher = ignoreCharsPattern.matcher(textWithFunctions);

      // Only one function type is supported at a time
      if (ignoreCaseMatcher.find()) {
        return compareIgnoreCase(textWithFunctions, comparedTo);
      } else if (ignoreCharsMatcher.find()) {
        return compareIgnoreChars(textWithFunctions, comparedTo);
      }

      return textWithFunctions.equals(comparedTo);
    }

    private boolean compareIgnoreCase(String textWithFunctions, String comparedTo) {
      // wrapping all the IGNORE_CASE sections with (?i) regex flag to make them case insensitive
      return compareWithReplace(ComparatorFunctions.IGNORE_CASE, "(?i)$1(?-i)", textWithFunctions, // NOI18N
                                comparedTo);
    }

    private boolean compareIgnoreChars(String textWithFunctions, String comparedTo) {
      return compareWithReplace(ComparatorFunctions.IGNORE_CHARS, "(.*)", textWithFunctions, comparedTo); // NOI18N
    }

    private boolean compareWithReplace(ComparatorFunctions function, String regexReplace, String textWithFunctions,
        String comparedTo) {
      Pattern p = Pattern.compile(
          function.name() + FUNCTION_OPENER + REGEX_GROUP_ANY_STRING_LAZY + FUNCTION_CLOSER);
      Matcher m = p.matcher(textWithFunctions);

      textWithFunctions = m.replaceAll(regexReplace);

      return comparedTo.matches(textWithFunctions);
    }
  }

  /**
   * Get string.
   *
   * @param name the name. Can contain processing functions, check ComparatorFunctions and FunctionComparator
   * @return the string
   */
  public static synchronized final String get(String name) {
    Properties properties = variables.get(getThreadId());
    String propertyKey = (String) properties.keySet().stream()
        .filter(p -> new FunctionComparator().compare(normalizeName(name), p.toString()))
        .findFirst()
        .orElse(normalizeName(name));

    return properties.getProperty(propertyKey);
  }

  /**
   * Gets or default.
   *
   * @param name         the name
   * @param defaultValue the default value
   * @return the or default
   */
  public static synchronized final String getOrDefault(String name, String defaultValue) {
    String value = get(name);

    return value == null || value.isEmpty() ? defaultValue : value;
  }

  /**
   * Gets all.
   *
   * @return the all
   */
  public static synchronized final Properties getAll() {
    return variables.get(getThreadId());
  }

  /**
   * Set.
   *
   * @param name  the name
   * @param value the value
   */
  public static synchronized final void set(String name, String value) {
    variables.get(getThreadId()).setProperty(normalizeName(name), value);
  }

  /**
   * Set.
   *
   * @param properties the properties
   */
  public static synchronized final void set(Properties properties) {
    for (String name : properties.stringPropertyNames()) {
      set(name, properties.getProperty(name));
    }
  }

  /**
   * Set.
   *
   * @param map the map
   */
  public static synchronized final void set(HashMap<String, String> map) {
    for (String name : map.keySet()) {
      set(name, map.get(name));
    }
  }

  /**
   * Has boolean.
   *
   * @param name the name
   * @return the boolean
   */
  public static synchronized final boolean has(String name) {
    return variables.get(getThreadId()).containsKey(normalizeName(name));
  }

  /**
   * Clear all.
   */
  public static synchronized final void clearAll() {
    variables.put(getThreadId(), new Properties());
  }

  private static Long getThreadId() {
    return Thread.currentThread().getId();
  }

  private static String normalizeName(String key) {
    return key.replaceAll("\\p{Z}", "_");
  }
}

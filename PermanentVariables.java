/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;

/**
 * <p>PermanentVariables uses serialization to store a Map object with String value offline. Permanent Variable scope
 * is per VM instance.
 * </p>
 *
 * <p>A prefix 'permanent:' is used to identify or isolate the existing
 * <code>com.experian.automation.helpers.Variables</code> usage.
 * </p>
 *
 * <p>Examples of setting a value: <code>PermanentVariables.set("permanent:feature.id", "com.feature.123");</code></p>
 * <p>Examples of getting a value: <code>String fid = PermanentVariables.get("permanent:feature.id");</code></p>
 * <p>Examples of usage from transformer: <code>${permanent:feature.id}</code></p>
 */
public class PermanentVariables {

  private static PermanentVariables pv = null;
  private Map<String, String> pair = new HashMap<>();
  private final String PATH = System.getProperty("java.io.tmpdir") + "/cucumber-permanent-variables.properties";

  private PermanentVariables() throws IOException {
    if (Files.exists(Paths.get(PATH))) {
      deserialize();
    } else {
      pair = new Hashtable<>();
    }
  }

  private void removeKey(String key) throws IOException {
    pair.remove(key);
    serialize();
  }

  private void setValue(String key, String value) throws Exception {

    if (key.startsWith("permanent:")) {
      pair.put(key, value);
      serialize();
    } else {
      throw new Exception("Only accepting prefix 'permanent:'");
    }
  }

  private String getValue(String key) {

    return pair.get(key);
  }

  private boolean hasKey(String key) {
    return pair.containsKey(key);
  }

  private void cleanAll() throws IOException {
    pair.clear();
    serialize();
  }

  private void deserialize() throws IOException {
    Properties properties = new Properties();
    FileInputStream inputStream = new FileInputStream(PATH);
    try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
      properties.load(inputStreamReader);
    }
    Stream<Entry<Object, Object>> stream = properties.entrySet().stream();
    Map<String, String> mapOfProperties = stream.collect(Collectors.toMap(
        e -> String.valueOf(e.getKey()),
        e -> String.valueOf(e.getValue())));

    pair.putAll(mapOfProperties);
  }

  private void serialize() throws IOException {
    FileUtils.forceMkdirParent(new File(PATH));
    Properties prop = new Properties();

    prop.putAll(pair);
    try (FileWriter writer = new FileWriter(PATH)) {
      prop.store(writer, "store to permanent-variables.properties");
    }
  }

  private static PermanentVariables getInstance() throws IOException {
    if (pv == null) {
      return new PermanentVariables();
    } else {
      return pv;
    }
  }

  /**
   * Get string.
   *
   * @param name the name
   * @return the string
   */
  public static synchronized String get(String name) {
    try {
      return getInstance().getValue(normalizeName(name));
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Gets or default.
   *
   * @param name         the name
   * @param defaultValue the default value
   * @return the or default
   */
  public static synchronized String getOrDefault(String name, String defaultValue) {
    String value = get(name);

    return value == null || value.isEmpty() ? defaultValue : value;
  }

  /**
   * Gets all.
   *
   * @return the all
   */
  public static synchronized Properties getAll() {
    Properties p = new Properties();
    try {
      getInstance().pair.forEach(p::setProperty);
    } catch (Exception e) {
      return null;
    }
    return p;
  }

  /**
   * Set.
   *
   * @param name  the name
   * @param value the value
   * @throws Exception the exception
   */
  public static synchronized void set(String name, String value) throws Exception {
    getInstance().setValue(normalizeName(name), value);
  }

  /**
   * Set.
   *
   * @param properties the properties
   */
  public static synchronized void set(Properties properties) {
    properties.forEach((k, v) -> {
      try {
        getInstance().setValue((String) k, (String) v);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  /**
   * Set.
   *
   * @param map the map
   */
  public static synchronized void set(HashMap<String, String> map) {
    map.forEach((k, v) -> {
      try {
        getInstance().setValue(k, v);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  /**
   * Has boolean.
   *
   * @param name the name
   * @return the boolean
   */
  public static synchronized boolean has(String name) {
    try {
      return getInstance().hasKey(name);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Remove.
   *
   * @param key the key
   * @throws Exception the exception
   */
  public static synchronized void remove(String key) throws Exception {
    getInstance().removeKey(key);
  }

  /**
   * Clear all.
   *
   * @throws Exception the exception
   */
  public static synchronized void clearAll() throws Exception {
    getInstance().cleanAll();
  }

  private static String normalizeName(String key) {
    return key.replaceAll("\\p{Z}", "_");
  }
}

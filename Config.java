/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import com.experian.automation.config.ApplicationConfigLookup;
import com.experian.automation.config.JasyptDecryptLookup;
import com.experian.automation.config.UnixPathLookup;
import com.experian.automation.config.WindowPathLookup;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Properties;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.interpol.ConfigurationInterpolator;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.io.FilenameUtils;

/**
 * The type Config.
 */
public class Config {

  private static PropertiesConfiguration data;

  /**
   * Instantiates a new Config.
   */
  public Config() {
    load();
  }

  /**
   * Get string.
   *
   * @param property the property
   * @return the string
   */
  public static String get(String property) {
    load();

    if (data.containsKey(property)) {
      return data.getString(property);
    } else {
      return null;
    }
  }

  /**
   * Gets or default.
   *
   * @param name         the name
   * @param defaultValue the default value
   * @return the value or default
   */
  public static String getOrDefault(String name, String defaultValue) {
    String value = get(name);

    return value == null || value.isEmpty() ? defaultValue : value;
  }

  /**
   * Gets as unix path.
   *
   * @param property the property
   * @return the as unix path
   */
  public static String getAsUnixPath(String property) {
    load();
    return FilenameUtils.separatorsToUnix(get(property));
  }

  /**
   * Gets as windows path.
   *
   * @param property the property
   * @return the as windows path
   */
  public static String getAsWindowsPath(String property) {
    load();
    return FilenameUtils.separatorsToWindows(get(property));
  }

  /**
   * Gets as system path.
   *
   * @param property the property
   * @return the as system path
   */
  public static String getAsSystemPath(String property) {
    load();
    return FilenameUtils.separatorsToSystem(get(property));
  }

  /**
   * Gets properties configuration.
   *
   * @return the properties configuration
   */
  public static PropertiesConfiguration getPropertiesConfiguration() {
    load();
    return data;
  }

  /**
   * Gets properties.
   *
   * @return the properties
   */
  public static Properties getProperties() {
    load();
    Properties properties = new Properties();
    Iterator<String> keys = data.getKeys();

    while (keys.hasNext()) {
      String key = keys.next();
      String value = data.getString(key);
      properties.setProperty(key, value);
    }

    return properties;
  }

  private static void load() {

    if (data == null) {

      String configFileName = "config.properties";
      String basePath = Config.class.getResource("/config/").getPath();

      data = new PropertiesConfiguration();

      ConfigurationInterpolator interpolator = data.getInterpolator();
      interpolator.registerLookup("unixpath", new UnixPathLookup());
      interpolator.registerLookup("winpath", new WindowPathLookup());
      interpolator.registerLookup("appconfig", new ApplicationConfigLookup());
      interpolator.registerLookup("jasyptdecrypt",new JasyptDecryptLookup());
      interpolator.setEnableSubstitutionInVariables(true);
      data.setInterpolator(interpolator);

      try {
        FileHandler handler = new FileHandler(data);
        handler.setBasePath(basePath);
        handler.setEncoding(StandardCharsets.UTF_8.name());
        handler.setFileName(configFileName);
        handler.load();

      } catch (ConfigurationException ex) {
        throw new RuntimeException(ex);
      }
    }

  }
}

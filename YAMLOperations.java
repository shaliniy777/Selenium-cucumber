/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.minidev.json.JSONValue;

/**
 * The type Yaml operations.
 */
public class YAMLOperations {

  /**
   * Convert to json string.
   *
   * @param yamlString the yaml string
   * @return the string
   */
  public String convertToJson(String yamlString) {
    Yaml yaml = new Yaml();
    Object obj = yaml.load(yamlString);

    return JSONValue.toJSONString(obj);
  }

  /**
   * Gets value from yaml file.
   *
   * @param filePath the file path
   * @param yamlPath the yaml path
   * @return the value from yaml file
   * @throws IOException the io exception
   */
  public String getValueFromYAMLFile(String filePath, String yamlPath) throws IOException {
    String yamlString = new String(Files.readAllBytes(Paths.get(filePath)));
    String jsonString = convertToJson(yamlString);
    JSONOperations jsonOperations = new JSONOperations();

    return jsonOperations.evaluateJSONPath(jsonString, yamlPath);
  }
}
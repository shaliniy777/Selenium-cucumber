/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

/**
 * The type Jtwig template operations.
 */
public class JtwigTemplateOperations {

  /**
   * Instantiates a new Jtwig template operations.
   */
  public JtwigTemplateOperations() {
    //Blank Constructor
  }

  private String renderTemplate(String templateFilePath, Map<String, Object> modelMap, String encoding,
      Boolean classPathTemplate) throws Throwable {
    InputStream inputStream;

    if (classPathTemplate) {
      inputStream = this.getClass().getClassLoader().getResourceAsStream(
          templateFilePath);
    } else {
      inputStream = new FileInputStream(new File(templateFilePath));
    }

    String jtwigInline = IOUtils.toString(inputStream, encoding);
    JtwigTemplate template = JtwigTemplate.inlineTemplate(jtwigInline);

    JtwigModel model = JtwigModel.newModel(modelMap);

    return template.render(model);
  }

  /**
   * Render resource file template string.
   *
   * @param templateFilePath the template file path
   * @param modelMap         the model map
   * @param encoding         the encoding
   * @return the string
   * @throws Throwable the throwable
   */
  public String renderResourceFileTemplate(String templateFilePath, Map<String, Object> modelMap, String encoding)
      throws Throwable {
    return renderTemplate(templateFilePath, modelMap, encoding, true);
  }

  /**
   * Render custom file template string.
   *
   * @param templateFilePath the template file path
   * @param modelMap         the model map
   * @param encoding         the encoding
   * @return the string
   * @throws Throwable the throwable
   */
  public String renderCustomFileTemplate(String templateFilePath, Map<String, Object> modelMap, String encoding)
      throws Throwable {
    return renderTemplate(templateFilePath, modelMap, encoding, false);
  }
}

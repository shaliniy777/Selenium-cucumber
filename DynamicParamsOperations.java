/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers.powercurve;

import com.experian.automation.helpers.Config;
import com.experian.automation.helpers.powercurve.parameters.dynamic.Data;
import com.experian.automation.helpers.powercurve.parameters.dynamic.Description;
import com.experian.automation.helpers.powercurve.parameters.dynamic.ID;
import com.experian.automation.helpers.powercurve.parameters.dynamic.Parameter;
import com.experian.automation.helpers.powercurve.parameters.dynamic.Response;
import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.opencsv.CSVReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;

/**
 * https://confluenceglobal.experian.local/confluence/display/GPD/RESTful+Service+for+BPS+Dynamic+Parameter+Management
 */
public class DynamicParamsOperations {
  private static final String NO_NULL_ID_MESSAGE = "ID cannot be null!";

  private final String username;
  private final String password;

  /**
   * Instantiates a new Dynamic params operations.
   *
   * @param username the username
   * @param password the password
   */
  public DynamicParamsOperations(String username, String password) {
    this.username = username;
    this.password = password;
  }

  /**
   * List all parameter group map.
   *
   * @return the map
   * @throws Exception the exception
   */
  public Map<Description, ID> listAllParameterGroup() throws Exception {

    String res = get(EndPoints.LIST_ALL.toString());
    return new Gson().fromJson(res, Response.class).getData();
  }

  /**
   * List children map.
   *
   * @param id the id
   * @return the map
   * @throws Exception the exception
   */
  public Map<Description, ID> listChildren(ID id) throws Exception {
    Assert.assertNotNull(NO_NULL_ID_MESSAGE, id);

    String res = get(EndPoints.LIST_CHILDREN.withId(id));
    return new Gson().fromJson(res, Response.class).getData();
  }

  /**
   * Add child id.
   *
   * @param id        the id
   * @param parameter the parameter
   * @return the id
   * @throws Exception the exception
   */
  public ID addChild(ID id, Parameter parameter) throws Exception {

    Assert.assertNotNull(NO_NULL_ID_MESSAGE, id);

    Map<Description, ID> children = listChildren(id);

    if (!children.containsKey(parameter.getDescription())) {

      String res = post(EndPoints.ADD_CHILD.withId(id), parameter.toJson());
      return new Gson().fromJson(res, Data.class).getId();
    }

    return children.get(parameter.getDescription());
  }

  /**
   * Update description id.
   *
   * @param id        the id
   * @param parameter the parameter
   * @return the id
   * @throws Exception the exception
   */
  public ID updateDescription(ID id, Parameter parameter) throws Exception {
    Assert.assertNotNull(NO_NULL_ID_MESSAGE, id);

    String res = post(EndPoints.UPDATE_DESC.withId(id), parameter.toJson());
    return new Gson().fromJson(res, Data.class).getId();
  }

  /**
   * Import from csv.
   *
   * @param description the description
   * @param file        the file
   * @throws Exception the exception
   */
  public void importFromCSV(String description, String file) throws Exception {
    List<LinkedList<Parameter>> params = fromCSV(file);

    Map<Description, ID> groups = listAllParameterGroup();
    ID groupID = groups.get(new Description(description));

    Assert.assertNotNull("No dynamic group found!", groupID);
    for (LinkedList<Parameter> list : params) {

      ID previousID = null;
      for (Parameter p : list) {
        previousID = previousID == null ? addChild(groupID, p) : addChild(previousID, p);
      }
    }
  }

  private List<LinkedList<Parameter>> fromCSV(String file) throws IOException {
    List<LinkedList<Parameter>> list = new ArrayList<>();

    try (CSVReader csvReader = new CSVReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
      String[] values;
      while ((values = csvReader.readNext()) != null) {
        LinkedList<Parameter> params = new LinkedList<>();
        List<String> tokens = Arrays.asList(values);

        Assert.assertEquals("Expecting even size - Key value pair", 0, tokens.size() % 2);

        for (int i = 0; i < tokens.size(); i += 2) {
          String value = tokens.get(i);
          String description = tokens.get(i + 1);

          if (StringUtils.isEmpty(value) || StringUtils.isEmpty(description)) {
            continue;
          }

          Parameter p = new Parameter();
          p.setValue(value);
          p.setDescription(new Description(description));
          params.add(p);
        }

        list.add(params);
      }
    }

    return list;
  }

  private String get(String url) throws UnirestException {

    HttpResponse<String> response = Unirest.get(url)
        .header("content-type", "application/json")
        .basicAuth(username, password)
        .asString();

    Assert.assertEquals(response.getBody(), 200, response.getStatus());

    return response.getBody();
  }

  private String post(String url, String body) throws UnirestException {
    HttpResponse<String> response = Unirest.post(url)
        .header("content-type", "application/json")
        .basicAuth(username, password)
        .body(body)
        .asString();

    System.out.println(body);
    Assert.assertEquals(response.getBody(), 200, response.getStatus());

    return response.getBody();
  }

  private enum EndPoints {

    /**
     * List all end points.
     */
    LIST_ALL("services/v0/dynamic-parameters/groups"),
    /**
     * List children end points.
     */
    LIST_CHILDREN("services/v0/dynamic-parameters/%s/children"),
    /**
     * Add child end points.
     */
    ADD_CHILD("services/v0/dynamic-parameters/%s/children"),
    /**
     * Update desc end points.
     */
    UPDATE_DESC("services/v0/dynamic-parameters/%s");

    private final String url;

    EndPoints(String url) {
      this.url = url;
    }

    /**
     * With id string.
     *
     * @param id the id
     * @return the string
     */
    public String withId(ID id) {
      return String.format(this.toString(), id);
    }

    @Override
    public String toString() {
      return String.format("http://%s:%s/%s", Config.get("bps.host"), Config.get("bps.http.port"), url);
    }
  }
}


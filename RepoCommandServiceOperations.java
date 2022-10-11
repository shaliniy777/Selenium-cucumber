/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers.powercurve;

import com.experian.automation.helpers.Config;
import com.experian.automation.helpers.FSOperations;
import com.experian.automation.helpers.ServiceOperations;
import com.experian.automation.logger.Logger;
import com.experian.automation.helpers.TaskKill;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import java.io.File;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minidev.json.JSONArray;
import org.json.JSONObject;
import org.apache.http.HttpStatus;
import org.apache.http.HttpException;

/**
 * The type Repo command service operations.
 */
public class RepoCommandServiceOperations extends ServiceOperations {

  private static final String ACCEPT_HEADER_NAME = "accept";
  private static final String ACCEPT_HEADER_VALUE = "application/json";
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String SUCCESS = "SUCCESS";
  public static final String ERROR = "ERROR";
  public static final String DONE = "DONE";
  private static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
  private static final String RECORD_NOT_FOUND = "RECORD_NOT_FOUND";


  private static final String EXT_COMP_TYPES_LOC = "/conf/externalComponentTypes.xml";

  /**
   * The Service base url.
   */
  String serviceBaseURL = "http://localhost:" + Config.get("repocommandservice.port");

  /**
   * Instantiates a new Repo command service operations.
   *
   * @throws Exception the exception
   */
  public RepoCommandServiceOperations() throws Exception {

    startTimeout = 120 * 1000;
    startWorkDirPath = Config.getAsUnixPath("repocommandservice.jar.path");
    pidFilePath = Config.getAsUnixPath("temp.dir") + "/repo-command-service.pid";
    String repoCommandServiceLog = "RepoCommandService.log";

    startExecutable = "java -jar " + Config.get("repocommandservice.jar") + "> " + repoCommandServiceLog + " 2>&1";
  }

  @Override
  protected List<Integer> getServicePorts() throws Exception {

    List<Integer> servicePorts = new ArrayList<Integer>();
    servicePorts.add(Integer.parseInt(Config.get("repocommandservice.port")));

    return servicePorts;
  }

  @Override
  public void start() throws Exception {
    configFileCopy();
    super.start();
  }

  @Override
  public void stop() throws Exception {
    TaskKill taskKill = new TaskKill();
    for (Integer port : getServicePorts()) {
      taskKill.byPort(port, "Listen");
    }
  }

  /**
   * Deploy string.
   *
   * @param solutionPath the solution path
   * @param yamlPath     the yaml path
   * @return the string
   * @throws Exception the exception
   */
  public String deploy(String solutionPath, String yamlPath) throws Exception {
    // clear the Headers first
    Unirest.clearDefaultHeaders();
    // call the repo-command-service
    HttpResponse<String> response = Unirest.post(serviceBaseURL + "/upload")
        .header(ACCEPT_HEADER_NAME, ACCEPT_HEADER_VALUE)
        .field("solution", new File(solutionPath))
        .field("yaml", new File(yamlPath))
        .asString();
    // check if there is no error
    if (response.getStatus() != HttpStatus.SC_OK) {
      String msg = String.format(
          "After calling RepoCommand service, you got an error!\n http Return Code: %d\nHttp return message: %s\nReceived Body: %s",
          response.getStatus(), response.getStatusText(), response.getBody());
      throw new HttpException(msg);
    }
    // check for id in the JSON response
    return JsonPath.parse(response.getBody()).read("$.id");
  }

  /**
   * Wait for deploy string.
   *
   * @param jobID the job id
   * @return the string
   * @throws Exception the exception
   */
  public String waitForDeploy(String jobID) throws Exception {
    return waitForDeploy(jobID, 20);
  }

  /**
   * Wait for deploy string.
   *
   * @param jobID   the job id
   * @param timeout the timeout
   * @return the string
   * @throws Exception the exception
   */
  public String waitForDeploy(String jobID, int timeout) throws Exception {

    // Wait for deploy to finish
    String status;
    long timeoutTime = System.currentTimeMillis() + timeout * 60 * 1000;

    do {
      status = getStatus(jobID);
      Thread.sleep(10 * 1000);
    } while (status.equals("IN_PROGRESS") && System.currentTimeMillis() < timeoutTime);

    return status;
  }

  /**
   * Gets status.
   *
   * @param jobID the job id
   * @return the status
   * @throws Exception the exception
   */
  public String getStatus(String jobID) throws Exception {

    DocumentContext docContext = getStatusResponse(jobID);

    // in case of a solution import if response is an array (ex: [{"deployableName" ... ])
    // the import was done successfully
    if (docContext.read("$") instanceof JSONArray) {
      return DONE;
    }

    String status = docContext.read("$.status");
    // in case of a content or security import
    // if we have {"status":"SUCCESS"} then the import success
    if (status != null && status.equals(SUCCESS)) {
      return DONE;
    }
    // in the case getStatusResponse is retrieved as {"status":"INTERNAL_SERVER_ERROR"} or {"status":"RECORD_NOT_FOUND"}.
    // It means something went wrong in repo command service
    if (status == null || status.equals(INTERNAL_SERVER_ERROR) || status.equals(RECORD_NOT_FOUND)) {
      return ERROR;
    }

    // other statuses can only be {"status":"IN_PROGRESS"}.
    return docContext.read("$.status");


  }

  /**
   * Gets deployables.
   *
   * @param jobID the job id
   * @return the deployables
   * @throws Exception the exception
   */
  public DocumentContext getDeployables(String jobID) throws Exception {

    if (getStatus(jobID).equals("DONE")) {
      return getStatusResponse(jobID);
    }

    return null;
  }

  private DocumentContext getStatusResponse(String jobID) throws Exception {

    // clear the Headers first
    Unirest.clearDefaultHeaders();

    HttpResponse<String> response = Unirest.post(serviceBaseURL + "/json")
        .header(ACCEPT_HEADER_NAME, ACCEPT_HEADER_VALUE)
        .queryString("id", jobID)
        .asString();

    return JsonPath.parse(response.getBody());
  }

  /**
   * Gets health status.
   *
   * @return the health status
   * @throws Exception the exception
   */
  public boolean getHealthStatus() throws Exception {

    final Logger logger = Logger.getLogger(this.getClass());

    // clear the Headers first
    Unirest.clearDefaultHeaders();

    boolean edaServerState = false;
    JSONObject edaHealth;
    long timeoutTime = System.currentTimeMillis() + 180 * 1000;
    do {
      HttpResponse<JsonNode> response = Unirest.get(serviceBaseURL + "/health")
          .header(ACCEPT_HEADER_NAME, ACCEPT_HEADER_VALUE)
          .asJson();
      edaHealth = response.getBody().getObject();
      boolean repoServerState = (boolean) edaHealth.get("repoServerUp");
      boolean jobServerState = (boolean) edaHealth.get("jobServerUp");
      if ((repoServerState == true) && (jobServerState == true)) {
        edaServerState = true;
      }
      Thread.sleep(1000);
      logger.info(String.format("Checking Job and Repo server state: %s", response.getBody()));
    } while (!edaServerState && System.currentTimeMillis() < timeoutTime);
    return edaServerState;
  }

  /**
   * Config file copy.
   *
   * @throws Exception the exception
   */
  public void configFileCopy() throws Exception {

    FSOperations fsOps = new FSOperations();
    if (!fsOps.exists(startWorkDirPath + EXT_COMP_TYPES_LOC)) {
      fsOps.copyFile(Config.get("repository.path") + EXT_COMP_TYPES_LOC,
                     startWorkDirPath + EXT_COMP_TYPES_LOC);
    }
  }


  /**
   * Deploy repository. It can be either security or solution repository
   *
   * @param requestParameters Map with all parameters required to call the service
   * @throws Exception in the case there is an issue in the service call or configuration
   */
  public void deployRepository(Map<String, String> requestParameters) throws Exception {

    areMandatoryParametersPresent(requestParameters);

    // clear the Headers first
    Unirest.clearDefaultHeaders();
    // call the repo-command-service
    HttpResponse<JsonNode> response = Unirest.post(serviceBaseURL + "/deploy")
        .header(CONTENT_TYPE, ACCEPT_HEADER_VALUE)
        .body(new Gson().toJson(requestParameters, new TypeToken<HashMap>() {
        }.getType()))
        .asJson();

    // check if there is no error
    if (response.getStatus() != HttpStatus.SC_OK) {
      String msg = String.format(
          "After calling RepoCommand service, you got an error! %n http Return Code: %d %nHttp return message: %s %nReceived Body: %s",
          response.getStatus(), response.getStatusText(), response.getBody());
      throw new HttpException(msg);
    }

  }

  /**
   * Check if all the mandatory parameters for the import repository properly configured
   *
   * @param requestParameters map containing all the parameters needed to perform an import of repository
   * @return areKeysPresent : True if parameters are OK, False if parameters are missing
   */
  private boolean areMandatoryParametersPresent(Map<String, String> requestParameters) throws Exception {
    List<String> expectedKeys = Arrays.asList("solutionFilePath", "taskId", "repoServerHost", "repoServerPort",
                                              "jobServerHost",
                                              "jobServerPort", "sysAdminName", "sysAdminPwd", "serverId", "serverType","securitySysAdminPwd");

    if (!requestParameters.keySet().containsAll(expectedKeys)) {
      throw new Exception(
          "Mandatory parameters are not present. Got:" + requestParameters.keySet().toString() + " Expected:"
              + expectedKeys.toString());
    } else {
      return true;
    }
  }
}
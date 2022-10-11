/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers.powercurve;

import com.experian.automation.helpers.ArchiversOperations;
import com.experian.automation.helpers.CommandLineExecutor;
import com.experian.automation.helpers.Config;
import com.experian.automation.helpers.FSOperations;
import com.experian.automation.helpers.ServiceOperations;
import com.experian.automation.helpers.TextFileOperations;
import com.experian.automation.helpers.databases.MSSQLDBOperations;
import com.experian.automation.helpers.databases.OracleDBOperations;
import com.experian.automation.helpers.databases.PostgreDBOperations;
import com.experian.automation.logger.Logger;
import com.experian.automation.transformers.VariablesTransformer;
import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;

/**
 * The type Bps operations.
 */
public class BPSOperations extends ServiceOperations {

  private static final String BPE_PATH = "bpe.path";
  private static final String BPS_PATH = "bps.path";

  private static final Logger logger = Logger.getLogger(BPSOperations.class);
  private final String bpeLogFile = Config.get(BPE_PATH) + "/Log/BPE.log";

  /**
   * Instantiates a new Bps operations.
   */
  public BPSOperations() {
    if (SystemUtils.IS_OS_WINDOWS) {
      startExecutable = "run.bat";
      stopExecutable = "stop.bat";
    } else {
      startExecutable = "./run.sh";
      stopExecutable = "./stop.sh";
    }
    startWorkDirPath = Config.get(BPS_PATH);
    stopWorkDirPath = Config.get(BPS_PATH);
    startTimeout = 180 * 1000;
  }

  @Override
  public void start() throws Exception {
    logPortsState();
    new FSOperations().delete(bpeLogFile);
    super.start();
  }

  @Override
  public void waitToStart() throws Exception {
    super.waitToStart();
    final String regex = "Business Process Engine v[0-9.]{3}.*started successfully";
    int fileLookupTimeout = 1200000;

    boolean bpeLogExist = new FSOperations().waitForFile(bpeLogFile, fileLookupTimeout);

    if (bpeLogExist) {
      new TextFileOperations().filterByRegex(bpeLogFile, regex, 72, 2000L);
    } else {
      throw new Exception("BPE.log file not available");
    }
  }

  @Override
  protected List<Integer> getServicePorts() {
    List<Integer> servicePorts = new ArrayList<>();
    servicePorts.add(Integer.valueOf(Objects.requireNonNull(Config.get("bps.http.port"))));
    servicePorts.add(URI.create(Objects.requireNonNull(Config.get("bps.jms.server.url"))).getPort());
    servicePorts.add(Integer.valueOf(Objects.requireNonNull(Config.get("bps.osgi.shell.telnet.port"))));
    return servicePorts;
  }

  @Override
  public List<String> getServiceLogs() throws Exception {
    FSOperations fsOperations = new FSOperations();
    List<String> logs = new ArrayList<>();
    logs.add(Config.getAsUnixPath(BPS_PATH) + "/logs/consolidated_logs.log");
    logs.add(fsOperations.getHomePath() + "/PowerCurve/Business Process Engine/Log/BPE.log");
    return logs;
  }

  /**
   * Deploy file.
   *
   * @param artefactType the artefact type
   * @param filePath the file path
   * @throws Exception the exception
   */
  public void deployFile(String artefactType, String filePath) throws Exception {
    getArtifact(artefactType).deploy(filePath);
  }

  /**
   * Check deployed file for specific tenant.
   *
   * @param artefactType the artefact type
   * @param filePath the file path
   * @param tenantName the tenant name for deployment
   * @throws Exception the exception
   */
  public void deployFile(String artefactType, String filePath, String tenantName) throws Exception {
    getArtifact(artefactType).deployNow(filePath,
                                        getArtifact(artefactType).getDeployDir() + "/" + tenantName); // NOI18N
  }

  /**
   * Check deployed file.
   *
   * @param artefactType the artefact type
   * @param fileName the file name
   * @throws Exception the exception
   */
  public void checkDeployedFile(String artefactType, String fileName) throws Exception {
    HashMap<String, String> files = new HashMap<>();
    files.put(artefactType, fileName);
    checkDeployedFiles(files);
  }

  /**
   * Check deployed files.
   *
   * @param files the files
   * @throws Exception the exception
   */
  public void checkDeployedFiles(HashMap<String, String> files) throws Exception {
    FSOperations fso = new FSOperations();
    int waitTime = 0;
    List<Entry<String, String>> entries = new CopyOnWriteArrayList<>(files.entrySet());
    int BPS_DEPLOY_TIMEOUT = 640000;
    while (waitTime < BPS_DEPLOY_TIMEOUT) {
      for (Map.Entry<String, String> pair : entries) {
        Artefacts artefactType = getArtifact(pair.getKey());
        String fileName = pair.getValue();

        String deployFilePath = artefactType.getDeployFile(fileName);
        String deployedFilePath = artefactType.getDeployedFile(fileName);

        if (fso.exists(deployedFilePath)) {
          entries.remove(pair);
        } else if (fso.exists(deployFilePath + ".fail")) {
          throw new Exception("The following file failed to deploy: " + deployFilePath);
        }
      }

      if (entries.isEmpty()) {
        break;
      }
      waitTime += 1000;
      Thread.sleep(1000);
    }

    if (!entries.isEmpty()) {
      throw new Exception("The following files were not deployed within " + BPS_DEPLOY_TIMEOUT + "milliseconds - "
                              + files.keySet().toString());
    }
  }

  /**
   * Clean database.
   *
   * @throws Exception the exception
   */
  public void cleanDatabase() throws Exception {
    FSOperations fileOperations = new FSOperations();
    String bpsDBConfigPath = Config.get(BPS_PATH) + "/conf/db-config.properties";

    String bpsDatabaseVendor = fileOperations.getValueFromPropertiesFile(bpsDBConfigPath, "database.vendor");
    String bpsDatabaseName = fileOperations.getValueFromPropertiesFile(bpsDBConfigPath, "database.name");
    String bpsUsername = fileOperations.getValueFromPropertiesFile(bpsDBConfigPath, "database.username");
    String bpsPassword = fileOperations.getValueFromPropertiesFile(bpsDBConfigPath, "database.password");
    String bpsTenantSchema = fileOperations.getValueFromPropertiesFile(bpsDBConfigPath,
                                                                       "tenantschemaname.default");
    String bpsSystemTenantSchema = fileOperations.getValueFromPropertiesFile(bpsDBConfigPath,
                                                                             "tenantschemaname.system");

    switch (bpsDatabaseVendor.toUpperCase()) {
      case "MSSQL":
        MSSQLDBOperations mssqlDBOperations = new MSSQLDBOperations();
        mssqlDBOperations.dropDatabase(bpsDatabaseName);
        mssqlDBOperations.createDatabase(bpsDatabaseName);
        mssqlDBOperations.createLogin(bpsUsername, bpsPassword);
        mssqlDBOperations.createUserForLogin(bpsDatabaseName, bpsUsername, bpsUsername);
        break;
      case "POSTGRESQL":
        PostgreDBOperations postgreDBOperations = new PostgreDBOperations();
        postgreDBOperations.dropDatabase(bpsDatabaseName);
        postgreDBOperations.createDatabase(bpsDatabaseName);
        postgreDBOperations.createLogin(bpsUsername, bpsPassword);
        postgreDBOperations.grantDatabasePrivilegesToUser(bpsDatabaseName, bpsUsername);
        break;
      case "ORACLE":
        OracleDBOperations oracleDBOperations = new OracleDBOperations();
        oracleDBOperations.dropSchema(bpsUsername);
        oracleDBOperations.dropSchema(bpsTenantSchema);
        oracleDBOperations.dropSchema(bpsSystemTenantSchema);
        oracleDBOperations.createSchema(bpsUsername, bpsPassword);
        break;
      case "DERBY":
        String derbyFolderLocation = fileOperations.getValueFromPropertiesFile(bpsDBConfigPath,
                                                                               "database.host");
        fileOperations.delete(derbyFolderLocation);
        break;
      default:
        throw new IllegalArgumentException("Database vendor: " + Config.get("database.platform") + " is not supported");
    }

  }

  /**
   * Clean activemq.data directory.
   *
   * @throws Exception the exception
   */
  public void cleanActiveMqData() throws Exception {
    File mqDataDir = new File(Config.get(BPS_PATH) + "/activemq.data");
    if (mqDataDir.exists()) {
      FileUtils.cleanDirectory(mqDataDir);
    }
  }

  /**
   * Clean logs.
   *
   * @throws Exception the exception
   */
  public void cleanLogs() throws Exception {
    File logDir = new File(Config.get(BPE_PATH) + "/Log");
    if (logDir.exists()) {
      FileUtils.cleanDirectory(logDir);
    }
  }

  /**
   * Clean deployed files.
   *
   * @throws Exception the exception
   */
  public void cleanDeployedFiles() throws Exception {
    FSOperations fso = new FSOperations();

    for (Artefacts a : EnumSet.allOf(Artefacts.class)) {
      fso.delete(a.getDeployDir());
      fso.makeDirectory(a.getDeployDir());

      fso.delete(a.getDeployedDir());
      fso.makeDirectory(a.getDeployedDir());
    }
  }

  private Artefacts getArtifact(String artifactType) {
    return Artefacts.valueOf(artifactType.toUpperCase().replace(" ", "_"));
  }


  /**
   * The enum Artefacts.
   */
  public enum Artefacts {
    /**
     * Dbpdm artefacts.
     */
    DBPDM,
    /**
     * Bpc artefacts.
     */
    BPC,
    /**
     * Dmc artefacts.
     */
    DMC,
    /**
     * Strategy artefacts.
     */
    STRATEGY,
    /**
     * The Enrichment solution.
     */
    ENRICHMENT_SOLUTION("Enrichment Solutions", "Enrichment Solutions");

    /**
     * The Deploy dir.
     */
    String deployDir;

    /**
     * Deployed dir artefacts.
     */
    String deployedDir;

    Artefacts(String deployDir, String deployedDir) {
      this.deployDir = deployDir;
      this.deployedDir = deployedDir;
    }

    Artefacts() {
      this.deployDir = "Deploy";
      this.deployedDir = "Deployed";
    }

    /**
     * Deploy.
     *
     * @param filePath the file path
     * @throws Exception the exception
     */
    void deploy(String filePath) throws Exception {
      switch (this) {
        case ENRICHMENT_SOLUTION:
          String fileNameWithOutExt = FilenameUtils.removeExtension(Paths.get(filePath).getFileName().toString());
          String tempDir = Config.get("temp.dir");
          new ArchiversOperations().unzip(filePath, tempDir);
          filePath = tempDir + "/" + fileNameWithOutExt + ".ser";
          deployNow(filePath, getDeployDir());
          break;
        case DBPDM:
          if (Objects.equals(Config.get("bps.schemas.autocreate"), "false")) {
            logger.info("BPS schemas autocreate options are disabled. Executing produced DBPDM script ...");

            ArchiversOperations archiversOperations = new ArchiversOperations();
            FSOperations fsOperations = new FSOperations();

            File tempDBPDMdir = new File(Config.get("temp.dir") + "/DBPDMtemp");
            tempDBPDMdir.deleteOnExit();
            fsOperations.deleteDirectoryContents(tempDBPDMdir);

            archiversOperations.unzip(filePath, tempDBPDMdir.getAbsolutePath());

            String bpsDBConfigPath = Config.get(BPS_PATH) + "/conf/db-config.properties";

            String bpsUsername = fsOperations.getValueFromPropertiesFile(bpsDBConfigPath,
                                                                         "database.username");
            String bpsPassword = fsOperations.getValueFromPropertiesFile(bpsDBConfigPath,
                                                                         "database.password");

            File dbpdmScript = new File(tempDBPDMdir.getAbsolutePath() + "/Oracle Physical Data Model.sql");
            File tempdbTenantFile = File.createTempFile("tmp", ".sql");
            File tempdbEdasysFile = File.createTempFile("tmp", ".sql");
            String dbTenantScript = FileUtils.readFileToString(dbpdmScript, StandardCharsets.UTF_8);
            String dbEdasysScript = FileUtils.readFileToString(dbpdmScript, StandardCharsets.UTF_8);
            dbTenantScript = dbTenantScript.replace("EDA_TENANT1",
                                                    Objects.requireNonNull(Config.get("bps.tenantschemaname")));
            dbEdasysScript = dbEdasysScript.replace("EDA_TENANT1",
                                                    Objects.requireNonNull(Config.get("bps.systemschemaname")));
            FileUtils.writeStringToFile(tempdbTenantFile, dbTenantScript, Charset.defaultCharset());
            FileUtils.writeStringToFile(tempdbEdasysFile, dbEdasysScript, Charset.defaultCharset());

            String commandTemplate = VariablesTransformer.transformSingleValue(
                "exit | sqlplus " + bpsUsername + "/" + bpsPassword
                    + "@${database.host}:${database.port}/${database.sid} \"@%s\"");

            new CommandLineExecutor().execute(String.format(commandTemplate, tempdbTenantFile.getAbsolutePath()), "",
                                              new HashMap<>(), true, 60000);
            new CommandLineExecutor().execute(String.format(commandTemplate, tempdbEdasysFile.getAbsolutePath()), "",
                                              new HashMap<>(), true, 60000);
          }
          deployNow(filePath, getDeployDir());
          break;
        default:
          deployNow(filePath, getDeployDir());
      }
    }

    /**
     * Gets deploy file.
     *
     * @param file the file
     * @return the deploy file
     */
    String getDeployFile(String file) {
      return prepareDeploymentFilePath(file, getDeployDir());
    }

    /**
     * Gets deployed file.
     *
     * @param file the file
     * @return the deployed file
     */
    String getDeployedFile(String file) {
      return prepareDeploymentFilePath(file, getDeployedDir());
    }

    /**
     * Gets deploy dir.
     *
     * @return the deploy dir
     */
    String getDeployDir() {
      return Config.get(BPE_PATH) + "/" + deployDir;
    }

    /**
     * Gets deployed dir.
     *
     * @return the deployed dir
     */
    String getDeployedDir() {
      return Config.get(BPE_PATH) + "/" + deployedDir;
    }

    private String prepareDeploymentFilePath(String file, String deploymentDir) {
      if (this == Artefacts.ENRICHMENT_SOLUTION) {
        String fileNameWithOutExt = FilenameUtils.removeExtension(Paths.get(file).getFileName().toString());
        return deploymentDir + "/" + fileNameWithOutExt + ".ser";
      }
      return deploymentDir + "/" + file;
    }

    private void deployNow(String filePath, String deployDirectory) throws Exception {
      FSOperations fso = new FSOperations();
      fso.copyFileToDirectory(filePath, deployDirectory);
    }
  }
}
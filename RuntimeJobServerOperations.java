/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers.powercurve;

import com.experian.automation.helpers.Config;
import com.experian.automation.helpers.ServiceOperations;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.SystemUtils;

/**
 * The type Runtime Job Server operations.
 */

public class RuntimeJobServerOperations extends ServiceOperations {
    private static final String RUNTIMEJOBSERVER_PATH = "bps.runtimejobserver.path";

    /**
     * Instantiates a new Runtime Job Server operations.
     */
    public RuntimeJobServerOperations() throws Exception {
        startExecutable = (SystemUtils.IS_OS_WINDOWS) ? "run.bat" : "./run.sh";
        stopExecutable = (SystemUtils.IS_OS_WINDOWS) ? "stop.bat" : "./stop.sh";
        startWorkDirPath = Config.get(RUNTIMEJOBSERVER_PATH);
        stopWorkDirPath = Config.get(RUNTIMEJOBSERVER_PATH);
        startTimeout = 60 * 7 * 100;
        stopTimeout = 60 * 100;
    }

    @Override
    protected List<Integer> getServicePorts() throws Exception {
        List<Integer> servicePorts = new ArrayList<Integer>();
        String configFilePath = this.startWorkDirPath + "/conf/org.ops4j.pax.web.cfg";
        servicePorts.add(getPortFromConfigPropertiesFile(configFilePath, "org.osgi.service.http.port"));
        return servicePorts;
    }

    @Override
    protected List<String> getServiceLogs() throws Exception {
        List<String> logs = new ArrayList<>();
        logs.add(Config.getAsUnixPath(RUNTIMEJOBSERVER_PATH) + "/logs/consolidated_logs.log");
        logs.add(Config.getAsUnixPath(RUNTIMEJOBSERVER_PATH) + "/logs/derby.log");
        return logs;
    }
}
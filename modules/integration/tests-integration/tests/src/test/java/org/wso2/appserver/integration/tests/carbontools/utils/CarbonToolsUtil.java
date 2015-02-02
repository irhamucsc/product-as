/*
 *
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * /
 */

package org.wso2.appserver.integration.tests.carbontools.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.automation.engine.FrameworkConstants;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.integration.common.admin.client.ServerAdminClient;
import org.wso2.carbon.integration.common.extensions.carbonserver.CarbonServerManager;
import org.wso2.carbon.integration.common.extensions.utils.ExtensionCommonConstants;
import org.wso2.carbon.integration.common.extensions.utils.ServerLogReader;
import org.wso2.carbon.integration.common.utils.ClientConnectionUtil;

import java.io.IOException;

/**
 * This class has the utility methods to carbon tools test cases.
*/

public class CarbonToolsUtil {

    private static String CARBON_HOME = null;
    private static final String SERVER_SHUTDOWN_MESSAGE = "Halting JVM";
    private static final long DEFAULT_START_STOP_WAIT_MS = 1000 * 60 * 5;
    private static final Log log = LogFactory.getLog(CarbonToolsUtil.class);

    /**
     * This method is to get carbon home.
     *
     * @param context - AutomationContext
     * @return - carbon home
     * @throws java.io.IOException - Error while setup carbon home from carbon zip file
     */
    public static String getCarbonHome(AutomationContext context)
            throws IOException {
        if (CARBON_HOME != null) {
            return CARBON_HOME;
        }
        String carbonZip = System.getProperty(FrameworkConstants.SYSTEM_PROPERTY_CARBON_ZIP_LOCATION);
        CarbonServerManager carbonServerManager = new CarbonServerManager(context);
        CARBON_HOME = carbonServerManager.setUpCarbonHome(carbonZip);
        return CARBON_HOME;
    }

    public static void serverShutdown(Process process,int portOffset,AutomationContext automationContext) throws Exception {
        if (process != null) {
            ServerLogReader inputStreamHandler = new ServerLogReader("inputStream", process.getInputStream());
            // start the stream readers
            inputStreamHandler.start();
            log.info("Shutting down server..");
            if (ClientConnectionUtil.isPortOpen(Integer.parseInt(ExtensionCommonConstants.
                SERVER_DEFAULT_HTTPS_PORT) + portOffset, automationContext.getInstance().getHosts().get("default"))) {
                int httpsPort = Integer.parseInt(FrameworkConstants.SERVER_DEFAULT_HTTPS_PORT) + portOffset;
                String url = automationContext.getContextUrls().getBackEndUrl();
                String backendURL = url.replaceAll("(:\\d+)", ":" + httpsPort);

                ServerAdminClient serverAdminServiceClient = new ServerAdminClient(backendURL,
                                  automationContext.getContextTenant().getTenantAdmin().getUserName(),
                                  automationContext.getContextTenant().getTenantAdmin().getPassword());

                serverAdminServiceClient.shutdown();
                long time = System.currentTimeMillis() + DEFAULT_START_STOP_WAIT_MS;
                while (!inputStreamHandler.getOutput().contains(SERVER_SHUTDOWN_MESSAGE) &&
                       System.currentTimeMillis() < time) {
                    // wait until server shutdown is completed
                }
                log.info("Server stopped successfully...");
                inputStreamHandler.stop();
                process.destroy();
            }
        }
    }
}

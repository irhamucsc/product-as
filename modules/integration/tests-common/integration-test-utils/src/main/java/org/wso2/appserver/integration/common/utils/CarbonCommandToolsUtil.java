package org.wso2.appserver.integration.common.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.automation.engine.FrameworkConstants;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.integration.common.admin.client.ServerAdminClient;
import org.wso2.carbon.integration.common.utils.ClientConnectionUtil;
import org.wso2.carbon.automation.extensions.servers.utils.ServerLogReader;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.rmi.RemoteException;


public class CarbonCommandToolsUtil {

    /**
     * This class has the method which using by carbon tools test cases
     */

    private static final Log log = LogFactory.getLog(CarbonCommandToolsUtil.class);
    private static int TIMEOUT = 180 * 1000; // Max time to wait
    private static final long DEFAULT_START_STOP_WAIT_MS = 1000 * 60 * 2;
    private static final String SERVER_STARTUP_MESSAGE = "Mgt Console URL";

    /**
     * This method is for start a as server
     *
     * @param carbonHome        - carbon home
     * @param portOffset        - port offset
     * @param automationContext - AutomationContext
     * @param parameters        - server startup arguments as an string array
     * @return Process of the startup execution
     * @throws Exception
     */
    public static Process startServerUsingCarbonHome(String carbonHome, int portOffset,
                                                     AutomationContext automationContext,
                                                     String[] parameters)
            throws Exception {

        Process tempProcess;
        String scriptName = "wso2server";
        File commandDir = new File(carbonHome);
        String[] cmdArray;
        log.info("Starting server............. ");
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            commandDir = new File(carbonHome + File.separator + "bin");
            cmdArray = new String[]{"cmd.exe", "/c", scriptName + ".bat", "-DportOffset=" + portOffset};
            cmdArray = mergePropertiesToCommandArray(parameters, cmdArray);
            tempProcess = Runtime.getRuntime().exec(cmdArray, null, commandDir);
        } else {
            cmdArray = new String[]{"sh", "bin/" + scriptName + ".sh", "-DportOffset=" + portOffset};
            cmdArray = mergePropertiesToCommandArray(parameters, cmdArray);
            tempProcess = Runtime.getRuntime().exec(cmdArray, null, commandDir);
        }
        InputStreamHandler errorStreamHandler =
                new InputStreamHandler("errorStream", tempProcess.getErrorStream());
        inputStreamHandler = new ServerLogReader("inputStream", tempProcess.getInputStream());
        // start the stream readers
        inputStreamHandler.start();
        errorStreamHandler.start();
        ClientConnectionUtil.waitForPort(Integer.parseInt(FrameworkConstants.SERVER_DEFAULT_HTTPS_PORT) +
                                         portOffset, DEFAULT_START_STOP_WAIT_MS, false,
                                         automationContext.getInstance().getHosts().get("default"));
        //wait until Mgt console url printed.
        long time = System.currentTimeMillis() + 60 * 1000;
        while (!inputStreamHandler.getOutput().contains(SERVER_STARTUP_MESSAGE) &&
               System.currentTimeMillis() < time) {
            // wait until server startup is completed
        }
        ClientConnectionUtil.waitForLogin(automationContext);
        log.info("Server started successfully.");
        return tempProcess;
    }

    /**
     * This method is to shutdown a server
     *
     * @param portOffset        - port offset
     * @param automationContext - AutomationContext
     * @throws XPathExpressionException - Error when getting data from automation.xml
     * @throws RemoteException          - Error when shutdown the server
     */
    public static void serverShutdown(int portOffset,
                                      AutomationContext automationContext)
            throws XPathExpressionException, RemoteException {
        long time = System.currentTimeMillis() + DEFAULT_START_STOP_WAIT_MS;
        log.info("Shutting down server..");
        boolean logOutSuccess = false;
        if (ClientConnectionUtil.isPortOpen(Integer.parseInt(ExtensionConstants.
                                                                     SERVER_DEFAULT_HTTPS_PORT))) {

            int httpsPort = Integer.parseInt(FrameworkConstants.SERVER_DEFAULT_HTTPS_PORT) + portOffset;
            String url = automationContext.getContextUrls().getBackEndUrl();
            String backendURL = url.replaceAll("(:\\d+)", ":" + httpsPort);

            ServerAdminClient serverAdminServiceClient = new ServerAdminClient(backendURL,
                                                                               automationContext.getContextTenant().getTenantAdmin().getUserName(),
                                                                               automationContext.getContextTenant().getTenantAdmin().getPassword());

            serverAdminServiceClient.shutdown();

            while (System.currentTimeMillis() < time && !logOutSuccess) {
                logOutSuccess = isServerDown(automationContext, portOffset);
                // wait until server shutdown is completed
            }
            log.info("Server stopped successfully...");
        }

    }

    /**
     * This method is to merge two arrays together
     *
     * @param parameters - Server startup arguments
     * @param cmdArray   - Server startup command
     * @return - merged array
     */
    private static String[] mergePropertiesToCommandArray(String[] parameters, String[] cmdArray) {
        if (parameters != null) {
            cmdArray = ArrayUtils.addAll(cmdArray, parameters);
        }
        return cmdArray;
    }

    /**
     * This method is to check whether server is down or not
     *
     * @param automationContext - AutomationContext
     * @param portOffset        - port offset
     * @return boolean - if server is down true : else false
     * @throws XPathExpressionException
     */
    public static boolean isServerDown(AutomationContext automationContext,
                                       int portOffset)
            throws XPathExpressionException {
        boolean isServerShutDown = false;
        try {
            long startTime = System.currentTimeMillis();
            // Looping the waitForPort method for a time to check the server is down or not
            while ((System.currentTimeMillis() - startTime) < TIMEOUT) {
                ClientConnectionUtil.waitForPort(
                        Integer.parseInt(FrameworkConstants.SERVER_DEFAULT_HTTPS_PORT) + portOffset,
                        10, false, automationContext.getInstance().getHosts().get("default"));
            }
        } catch (RuntimeException ex) {
            log.info("Server has shutdown successfully");//Getting this when sever shutdown
            isServerShutDown = true;
        }
        return isServerShutDown;
    }
}

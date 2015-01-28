package org.wso2.appserver.integration.common.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.engine.FrameworkConstants;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.integration.common.admin.client.LogViewerClient;
import org.wso2.carbon.integration.common.utils.ClientConnectionUtil;
import org.wso2.carbon.logging.view.stub.LogViewerLogViewerException;
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;

import javax.xml.xpath.XPathExpressionException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;


public class CarbonCommandToolsUtil {

    /**
     * This class has the method which using by carbon tools test cases
     */

    private static final Log log = LogFactory.getLog(CarbonCommandToolsUtil.class);
    private static int TIMEOUT = 180 * 1000; // Max time to wait
    private static final long DEFAULT_START_STOP_WAIT_MS = 1000 * 60 * 2;
    private static String CARBON_HOME = null;

    /**
     * This method is to execute commands and reading the logs to find the expected string.
     *
     * @param directory      - Directory which has the file to be executed .
     * @param cmdArray       - Command array to be executed.
     * @param expectedString - Expected string in  the log.
     * @return boolean - true : Found the expected string , false : not found the expected string.
     * @throws java.io.IOException - Error while getting the command directory
     */
    public static boolean isScriptRunSuccessfully(String directory, String[] cmdArray,
                                                  String expectedString) throws IOException {
        boolean isFoundTheMessage = false;
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        Process process = null;
        try {
            File commandDir = new File(directory);
            process = Runtime.getRuntime().exec(cmdArray, null, commandDir);
            String line;
            long startTime = System.currentTimeMillis();
            while ((System.currentTimeMillis() - startTime) < TIMEOUT) {
                is = process.getInputStream();
                isr = new InputStreamReader(is);
                br = new BufferedReader(isr);
                if (br != null) {
                    line = br.readLine();
                    if (line.contains(expectedString)) {
                        log.info("found the string " + expectedString + " in line " + line);
                        isFoundTheMessage = true;
                        break;
                    }
                }
            }
            return isFoundTheMessage;
        } catch (IOException ex) {
            log.error("Error when reading the InputStream when running shell script  " +
                      ex.getMessage(), ex);
            throw new IOException("Error when reading the InputStream when running shell script "
                                  + ex.getMessage(), ex);
        } finally {
            if (is != null) {
                is.close();
            }
            if (isr != null) {
                isr.close();
            }
            if (br != null) {
                br.close();
            }
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * This method is to execute commands and return the Process
     *
     * @param directory - Directory which has the file to be executed .
     * @param cmdArray  - Command array to be executed
     * @return Process - executed process
     * @throws java.io.IOException - Error while getting the execution directory
     */
    public static Process runScript(String directory, String[] cmdArray)
            throws IOException {

        Process process;
        try {
            File commandDir = new File(directory);
            process = Runtime.getRuntime().exec(cmdArray, null, commandDir);
            return process;
        } catch (IOException ex) {
            log.error("Error when reading the InputStream when running shell script " +
                      ex.getMessage(), ex);
            throw new IOException("Error when reading the InputStream when running shell script "
                                  + ex.getMessage(), ex);
        }
    }


    /**
     * This method to find multiple strings in same line in log
     *
     * @param backEndUrl - server back end url
     * @param stringArrayToFind
     * @param cookie - cookie
     * @return -  if found all the  string in one line: true else false
     * @throws java.rmi.RemoteException
     * @throws LogViewerLogViewerException
     * @throws InterruptedException
     */
    public static boolean findMultipleStringsInLog(String backEndUrl, String[] stringArrayToFind,
                                                   String cookie)
            throws RemoteException, InterruptedException, LogViewerLogViewerException {
        boolean expectedStringFound = false;
        LogViewerClient logViewerClient = new LogViewerClient(backEndUrl, cookie);

        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) < TIMEOUT) {
            LogEvent[] logs = logViewerClient.getAllRemoteSystemLogs();
            for (LogEvent item : logs) {
                String message = (String) item.getMessage();
                for (String stringToFind : stringArrayToFind) {
                    if (message.contains(stringToFind)) {
                        expectedStringFound = true;
                    } else {
                        expectedStringFound = false;
                        break;
                    }
                }
                if (expectedStringFound) {
                    break;
                }
            }
            if (expectedStringFound) {
                break;
            }
            Thread.sleep(500); // wait for 0.5 second to check the log again.
        }
        return expectedStringFound;
    }


    /**
     * This method to check whether server is up or not
     * This method wait for some time to check login status by checking the port and login
     * This will throw an exception if port is not open or couldn't login
     *
     * @param automationContext - AutomationContext
     * @return true: If server is up else false
     * @throws java.io.IOException                           - Error while waiting for login
     * @throws org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException - Authentication error when try to login
     * @throws javax.xml.xpath.XPathExpressionException              - Error while getting data from automation.xml
     */
    public static boolean isServerStartedUp(AutomationContext automationContext, int portOffset)
            throws IOException, LoginAuthenticationExceptionException, XPathExpressionException {

        ClientConnectionUtil.waitForPort(
                Integer.parseInt(FrameworkConstants.SERVER_DEFAULT_HTTPS_PORT) + portOffset,
                DEFAULT_START_STOP_WAIT_MS, false, automationContext.getInstance().getHosts().get("default"));

        ClientConnectionUtil.waitForLogin(automationContext);//TODO comment
        log.info("Server started successfully.");
        return true;
    }

    /**
     * This method is to check whether server is down or not
     *
     * @param automationContext - AutomationContext
     * @return true: If server is down else false
     */
    public static synchronized boolean isServerDown(AutomationContext automationContext,
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
        }catch (RuntimeException ex){
            log.info("Server has shutdown successfully");//Getting this when sever shutdown
            isServerShutDown = true;
        }
        return isServerShutDown;
    }

    /**
     * This method to check file has created or not
     *
     * @param filePathString - file path
     * @return - if file created true else false
     */
    public static boolean waitForFileCreation(String filePathString) {
        boolean isFileCreated = false;
        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) < TIMEOUT) {
            File createdFile = new File(filePathString);
            if (createdFile.exists() && !createdFile.isDirectory()) {
                isFileCreated = true;
                break;
            }
        }
        return isFileCreated;
    }

    /**
     * This method is to check running os is windows or not
     *
     * @return if current os is windows return true : else false
     */
    public static boolean isCurrentOSWindows(){
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            return true;
        }else{
            return false;
        }
    }
}

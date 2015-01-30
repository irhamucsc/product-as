package org.wso2.appserver.integration.tests.carbontools;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.appserver.integration.common.clients.AARServiceUploaderClient;
import org.wso2.appserver.integration.common.utils.ASIntegrationTest;
import org.wso2.appserver.integration.common.utils.CarbonCommandToolsUtil;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;
import java.rmi.RemoteException;

/**
 * This class is to test wsdl2java command by deploying a aar file and using the wsdl url of that
 */
public class Wsdl2JavaCommandTestCase extends ASIntegrationTest {

    private static final Log log = LogFactory.getLog(Wsdl2JavaCommandTestCase.class);
    private String serviceUrl = "http://localhost:9763/services/HelloService?wsdl";

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
    @BeforeClass(alwaysRun = true)
    public void testDeployService()
            throws Exception {
        super.init();
        AARServiceUploaderClient aarServiceUploaderClient
                = new AARServiceUploaderClient(backendURL, sessionCookie);

        aarServiceUploaderClient.
                uploadAARFile("HelloWorld.aar", FrameworkPathUtil.getSystemResourceLocation() + "artifacts" +
                               File.separator + "AS" + File.separator + "aar" + File.separator +
                               "HelloWorld.aar", "");

        String axis2Service = "HelloService";
        isServiceDeployed(axis2Service);

        log.info("Axis2Service.aar service uploaded successfully");

    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
    @Test(groups = "wso2.as", description = "generate client code HelloWorld service")
    public void testGenerateClass() throws Exception {
        String wsdlURL;
        boolean fileCreated = false;
        Process process = null;
        String commandDirectory;
        try {
            wsdlURL = asServer.getContextUrls().getServiceUrl() + "/HelloService?wsdl";
            log.info("Service URL -" + wsdlURL);
            String[] cmdArrayToWsdl2Java;
            if (CarbonCommandToolsUtil.isCurrentOSWindows()) {
                cmdArrayToWsdl2Java =
                        new String[]{"cmd.exe", "/c", "wsdl2java.bat", "-uri", serviceUrl};
                commandDirectory = System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "bin";
            } else {
                cmdArrayToWsdl2Java =
                        new String[]{"sh", "wsdl2java.sh", "-uri", serviceUrl};
                commandDirectory = System.getProperty(ServerConstants.CARBON_HOME) + "/bin";
            }
            process = CarbonCommandToolsUtil.runScript(commandDirectory, cmdArrayToWsdl2Java);
            fileCreated = CarbonCommandToolsUtil.waitForFileCreation(
                    System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "bin" +
                    File.separator + "src/org/wso2/www/types/HelloServiceStub.java");

        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        Assert.assertTrue(fileCreated, "Java file not created successfully");
    }

    @AfterClass(alwaysRun = true)
    public void cleanResources() throws RemoteException {
        deleteService("HelloService");
        super.cleanup();
    }
}
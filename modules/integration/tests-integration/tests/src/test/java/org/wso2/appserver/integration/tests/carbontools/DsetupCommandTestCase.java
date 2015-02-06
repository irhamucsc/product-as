package org.wso2.appserver.integration.tests.carbontools;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.appserver.integration.common.utils.ASIntegrationTest;
import org.wso2.appserver.integration.common.utils.CarbonCommandToolsUtil;
import org.wso2.appserver.integration.tests.carbontools.utils.CarbonToolsUtil;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.ContextXpathConstants;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;

import static org.testng.Assert.assertTrue;


/**
 * This class is to check -Dsetup command by populating some users to DB and delete them using this command
 */
public class DsetupCommandTestCase extends ASIntegrationTest {

    private static final Log log = LogFactory.getLog(DsetupCommandTestCase.class);
    private AuthenticatorClient authenticatorClient;
    private ServerConfigurationManager serverManager;

    private String carbonHome = null;
    private AutomationContext context = null;
    private int portOffset = 1;
    private Process process = null;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

        context = new AutomationContext("AS", "appServerInstance0002",
                                        ContextXpathConstants.SUPER_TENANT,
                                        ContextXpathConstants.ADMIN);

        authenticatorClient = new AuthenticatorClient(context.getContextUrls().getBackEndUrl());
        carbonHome = CarbonToolsUtil.getCarbonHome(context);
        log.info("replacing the master-datasources.xml file");
        File sourceFile =
                new File(TestConfigurationProvider.getResourceLocation() + File.separator +
                         "artifacts" + File.separator + "AS" + File.separator + "carbontools" +
                         File.separator + "master-datasources.xml");

        File targetFile =
                new File(carbonHome + File.separator + "repository" +
                         File.separator + "conf" + File.separator + "datasources" + File.separator +
                         "master-datasources.xml");
        serverManager = new ServerConfigurationManager(context);
        serverManager.applyConfigurationWithoutRestart(sourceFile, targetFile, true);


    }

    @Test(groups = "wso2.greg", description = "Add resource")
    public void testCleanResource() throws Exception {

        String[] cmdArrayToRecreateDB;
        if (CarbonCommandToolsUtil.isCurrentOSWindows()) {
            cmdArrayToRecreateDB = new String[]
                    {"cmd.exe", "/c", "start", "wso2server.bat", "-Dsetup", "-DportOffset=" + portOffset};

            process = CarbonCommandToolsUtil.runScript(
                    carbonHome + File.separator + "bin", cmdArrayToRecreateDB);
        } else {
            cmdArrayToRecreateDB =
                    new String[]{"sh", "wso2server.sh", "-Dsetup", "-DportOffset=1"};
            process = CarbonCommandToolsUtil.runScript(carbonHome + "/bin", cmdArrayToRecreateDB);
        }
        boolean startupStatus = CarbonCommandToolsUtil.isServerStartedUp(context, portOffset);
        log.info("Server startup status : " + startupStatus);

        boolean fileCreated = CarbonCommandToolsUtil.waitForFileCreation(carbonHome + File.separator +
                              "repository" + File.separator + "database" + File.separator + "DsetupCommandTEST_DB.h2.db");
        Assert.assertTrue(fileCreated, "Java file not created successfully");
        String loginStatusString = authenticatorClient.login
                ("admin", "admin", context.getInstance().getHosts().get("default"));
        assertTrue(loginStatusString.contains("JSESSIONID"), "Unsuccessful login");


    }


    @AfterClass(alwaysRun = true)
    public void cleanResources() throws Exception {
                    CarbonToolsUtil.serverShutdown(process, 1, context);
    }
}

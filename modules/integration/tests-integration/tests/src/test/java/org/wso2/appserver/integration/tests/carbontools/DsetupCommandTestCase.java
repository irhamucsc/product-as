package org.wso2.appserver.integration.tests.carbontools;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.appserver.integration.common.utils.ASIntegrationTest;
import org.wso2.appserver.integration.common.utils.CarbonCommandToolsUtil;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.ContextXpathConstants;
import org.wso2.carbon.automation.extensions.servers.carbonserver.TestServerManager;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;
import org.wso2.carbon.integration.common.extensions.usermgt.UserPopulator;


/**
 * This class is to check -Dsetup command by populating some users to DB and delete them using this command
 */
public class DsetupCommandTestCase extends ASIntegrationTest {

    private static final Log log = LogFactory.getLog(ChangeUserPasswordH2DBTestCase.class);
    private TestServerManager testServerManager;
    private AutomationContext context;
    private AuthenticatorClient authenticatorClient;
    Process process;
    String loginStatusString;
    boolean loginStatus = false;
    private int portOffset = 1;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

        context = new AutomationContext("AS", "appServerInstance0002", ContextXpathConstants.SUPER_TENANT,
                                        ContextXpathConstants.ADMIN);

        authenticatorClient = new AuthenticatorClient(context.getContextUrls().getBackEndUrl());
        testServerManager = new TestServerManager(context, 1) {
            public void configureServer() throws Exception {
                testServerManager.startServer();
                UserPopulator userPopulator = new UserPopulator("AS", "appServerInstance0002");
                userPopulator.populateUsers();

                loginStatusString = authenticatorClient.login("testu1", "testu123", context.getInstance().
                        getHosts().get("default"));
                log.info("Login status : " + loginStatusString.contains("JSESSIONID"));
                testServerManager.stopServer();
            }
        };

    }

    @Test(groups = "wso2.as", description = "User login test")
    public void test() throws Exception {
        try {
            testServerManager.startServer();
            String[] cmdArrayToDsetup;
            if (CarbonCommandToolsUtil.isCurrentOSWindows()) {
                cmdArrayToDsetup =
                        new String[]{"cmd.exe", "/c", "wso2server.bat", "-DportOffset=1", "-Dsetup"};
            } else {
                cmdArrayToDsetup = new String[]{"sh", "wso2server.sh", "-DportOffset=1", "-Dsetup"};
            }
            process = CarbonCommandToolsUtil.runScript(testServerManager.getCarbonHome(), cmdArrayToDsetup);
            boolean startupStatus = CarbonCommandToolsUtil.isServerStartedUp(context, portOffset);
            log.info("Server startup status : " + startupStatus);

            loginStatusString = authenticatorClient.login("testu1", "testu123", context.getInstance().
                    getHosts().get("default"));
            if (loginStatusString.contains("JSESSIONID")) {
                loginStatus = true;
            }
        } catch (Exception e) {
            loginStatus = false;
        } finally {
            Process shutDownProcess = null;
            try {
                shutDownProcess = CarbonCommandToolsUtil.runScript(
                        testServerManager.getCarbonHome() + "/bin",
                        new String[]{"sh", "wso2server.sh", "-DportOffset=1", "--stop"});

                boolean shutDownStatus = CarbonCommandToolsUtil.isServerDown(context, portOffset);
                log.info("Server shutdown status : " + shutDownStatus);
            } finally {
                if (shutDownProcess != null) {
                    shutDownProcess.destroy();
                }
            }
        }
        Assert.assertFalse(loginStatus, "Unsuccessful login");
    }

    @AfterClass(alwaysRun = true)
    public void cleanResources() throws Exception {
        testServerManager.stopServer();
    }


}

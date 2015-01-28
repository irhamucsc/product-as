package org.wso2.appserver.integration.tests.carbontools;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.appserver.integration.common.utils.ASIntegrationConstants;
import org.wso2.appserver.integration.common.utils.ASIntegrationTest;
import org.wso2.appserver.integration.common.utils.CarbonCommandToolsUtil;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.ContextXpathConstants;
import org.wso2.carbon.automation.extensions.servers.carbonserver.TestServerManager;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;
import org.wso2.carbon.integration.common.extensions.usermgt.UserPopulator;

import java.io.File;

/**
 * This class is to test change H2DB user password with special characters using chpasswd.sh/chpasswd.bat
 */
public class ChangeUserPasswordWithSpecialCharacterH2DBTestCase extends ASIntegrationTest {

    private static final Log log = LogFactory.getLog(ChangeUserPasswordH2DBTestCase.class);
    private TestServerManager testServerManager;
    private AutomationContext context;
    private AuthenticatorClient authenticatorClient;
    private boolean scriptRunStatus;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

        context =
                new AutomationContext("AS", "appServerInstance0002",
                                      ContextXpathConstants.SUPER_TENANT,
                                      ContextXpathConstants.ADMIN);

        authenticatorClient = new AuthenticatorClient(context.getContextUrls().getBackEndUrl());
        testServerManager = new TestServerManager(context, 1) {
            public void configureServer() throws Exception {
                testServerManager.startServer();
                UserPopulator userPopulator = new UserPopulator("AS", "appServerInstance0002");
                userPopulator.populateUsers();
                testServerManager.stopServer();
                String[] cmdArray;
                String commandDirectory;
                if (CarbonCommandToolsUtil.isCurrentOSWindows()) {
                    cmdArray = new String[]
                            {"cmd.exe", "/c", "chpasswd.bat", "--db-url", "jdbc:h2:" +
                              testServerManager.getCarbonHome() + ASIntegrationConstants.H2DB_DB_URL,
                             "--db-driver", "org.h2.Driver", "--db-username", "wso2carbon", "--db-password",
                             "wso2carbon", "--username", "testu1", "--new-password", "testu123!*"};
                    commandDirectory = testServerManager.getCarbonHome() + File.separator + "bin";
                } else {
                    cmdArray = new String[]
                            {"sh", "chpasswd.sh", "--db-url", "jdbc:h2:" + testServerManager.getCarbonHome() +
                              ASIntegrationConstants.H2DB_DB_URL, "--db-driver", "org.h2.Driver", "--db-username",
                             "wso2carbon", "--db-password", "wso2carbon", "--username", "testu1", "--new-password",
                             "testu123!*"};
                    commandDirectory = testServerManager.getCarbonHome() + "/bin";
                }

                scriptRunStatus = CarbonCommandToolsUtil.isScriptRunSuccessfully(
                        commandDirectory, cmdArray, "Password updated successfully");
                log.info("Script running status : " + scriptRunStatus);
            }
        };

    }


    @Test(groups = "wso2.as", description = "Password changing script run test")
    public void testScriptRun() throws Exception {
        testServerManager.startServer();
        Assert.assertTrue(scriptRunStatus, "Unsuccessful login");

    }

    @Test(groups = "wso2.as", description = "User login test", dependsOnMethods = {"testScriptRun"})
    public void test() throws Exception {
        String loginStatusString = authenticatorClient.login(
                "testu1", "testu123!*", context.getInstance().getHosts().get("default"));
        Assert.assertTrue(loginStatusString.contains("JSESSIONID"), "Unsuccessful login");
    }


    @AfterClass(alwaysRun = true)
    public void cleanResources() throws Exception {
        testServerManager.stopServer();
    }


}
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
 * This class is to test change MySQL user password using chpasswd.sh/chpasswd.bat
 */
public class ChangeUserPasswordMySQLDBTestCase extends ASIntegrationTest {

    private static final Log log = LogFactory.getLog(ChangeUserPasswordH2DBTestCase.class);
    private TestServerManager testServerManager;
    private AutomationContext context;
    private AuthenticatorClient authenticatorClient;
    private boolean scriptRunStatus;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        context = new AutomationContext("AS", "appServerInstance0002", ContextXpathConstants.SUPER_TENANT,
                                        ContextXpathConstants.ADMIN);
        authenticatorClient = new AuthenticatorClient(context.getContextUrls().getBackEndUrl());
    }


    @Test(groups = "wso2.as", description = "MySQL Password changing script run test", enabled = false)
    public void testScriptRun() throws Exception {

        testServerManager = new TestServerManager(context, 1) {
            public void configureServer() throws Exception {
                testServerManager.startServer();
                UserPopulator userPopulator = new UserPopulator("AS", "appServerInstance0002");
                userPopulator.populateUsers();
                testServerManager.stopServer();
                String[] cmdArray;
                String commandDirectory;

                if (CarbonCommandToolsUtil.isCurrentOSWindows()) {
                    cmdArray = new String[]{
                            "cmd.exe", "/c", "chpasswd.sh", "--db-url", ASIntegrationConstants.MYSQL_DB_URL,
                            "--db-driver", "com.mysql.jdbc.Driver", "--db-username", "root", "--db-password",
                            "root123", "--username", "sameera", "--new-password", "sameera123"};
                    commandDirectory = testServerManager.getCarbonHome() + "/bin";
                } else {
                    cmdArray = new String[]{
                            "sh", "chpasswd.sh", "--db-url", ASIntegrationConstants.MYSQL_DB_URL,
                            "--db-driver", "com.mysql.jdbc.Driver", "--db-username", "root", "--db-password",
                            "root123", "--username", "sameera", "--new-password", "sameera123"};
                    commandDirectory = testServerManager.getCarbonHome() + File.separator + "bin";
                }
                scriptRunStatus = CarbonCommandToolsUtil.isScriptRunSuccessfully(
                        commandDirectory, cmdArray, "Password updated successfully");
                log.info("Script running status : " + scriptRunStatus);
            }
        };

        testServerManager.startServer();
    }

    @Test(groups = "wso2.as", description = "MySQL password change test", dependsOnMethods = {"testScriptRun"},
          enabled = false)
    public void test() throws Exception {
        String loginStatusString = authenticatorClient.login(
                "testu1", "testu123", context.getInstance().getHosts().get("default"));

        Assert.assertTrue(loginStatusString.contains("JSESSIONID"), "Unsuccessful login");
    }


    @AfterClass(alwaysRun = true)
    public void cleanResources() throws Exception {
        testServerManager.stopServer();
    }


}

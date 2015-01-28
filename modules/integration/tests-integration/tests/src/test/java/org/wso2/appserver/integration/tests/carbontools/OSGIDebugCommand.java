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
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.ContextXpathConstants;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;

/**
 * This class is to test -DosgiDebugOptions
 */

public class OSGIDebugCommand extends ASIntegrationTest {

    private static final Log log = LogFactory.getLog(OSGIDebugCommand.class);
    AutomationContext context;
    String commandDirectory;
    private int portOffset = 1;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        context = new AutomationContext("AS", "appServerInstance0002",
                                        ContextXpathConstants.SUPER_TENANT,
                                        ContextXpathConstants.ADMIN);
    }

    @Test(groups = {"wso2.as"}, description = "OSGI debug command test")
    public void testOSGIDebugCommand()
            throws IOException, InterruptedException, XPathExpressionException,
                   LoginAuthenticationExceptionException {
        String[] cmdArray;
        if (CarbonCommandToolsUtil.isCurrentOSWindows()) {
            cmdArray = new String[]
                    {"cmd.exe", "/c", "wso2server.bat", "-DosgiDebugOptions", "-DportOffset=1"};
            commandDirectory = CarbonToolsUtil.getCarbonHome(context) + File.separator + "bin";
        } else {
            cmdArray = new String[]
                    {"sh", "wso2server.sh", "-DosgiDebugOptions", "-DportOffset=1"};
            commandDirectory = CarbonToolsUtil.getCarbonHome(context) + "/bin";
        }

        boolean isDebug = CarbonCommandToolsUtil.
                isScriptRunSuccessfully(commandDirectory, cmdArray,
                                        "OSGi debugging has been enabled with options:");

        CarbonCommandToolsUtil.isServerStartedUp(context, portOffset);
        Assert.assertTrue(isDebug, "Java file not created successfully");
    }

    @AfterClass(alwaysRun = true)
    public void cleanResources() throws Exception {
        Process shutDownProcess = null;
        String[] cmdArrayToShutdown;

        try {
            if (CarbonCommandToolsUtil.isCurrentOSWindows()) {
                cmdArrayToShutdown = new String[]
                        {"cmd.exe", "/c", "wso2server.bat", "-DportOffset=1", "--stop"};
            } else {
                cmdArrayToShutdown = new String[]{"sh", "wso2server.sh", "-DportOffset=1", "--stop"};
            }
            shutDownProcess = CarbonCommandToolsUtil.runScript(commandDirectory, cmdArrayToShutdown);
            boolean shutDownStatus = CarbonCommandToolsUtil.isServerDown(context, portOffset);
            log.info("Server shutdown status : " + shutDownStatus);
        } finally {
            if (shutDownProcess != null) {
                shutDownProcess.destroy();
            }
        }
    }

}

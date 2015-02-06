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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This class is to test -DosgiDebugOptions
 */

public class OSGIDebugCommandTestCase extends ASIntegrationTest {

    private static final Log log = LogFactory.getLog(OSGIDebugCommandTestCase.class);
    AutomationContext context;
    String commandDirectory;
    private int portOffset = 1;
    Process process = null;

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
        String expectedString = "OSGi debugging has been enabled with options:";
        boolean isFoundTheMessage = false;
        InputStream is = null;
        InputStreamReader isr = null;
        int timeout = 30000;

        BufferedReader br = null;
        if (CarbonCommandToolsUtil.isCurrentOSWindows()) {
            cmdArray = new String[]
                    {"cmd.exe", "/c", "wso2server.bat", "-DosgiDebugOptions", "-DportOffset=1"};
            commandDirectory = CarbonToolsUtil.getCarbonHome(context) + File.separator + "bin";
        } else {
            cmdArray = new String[]
                    {"sh", "wso2server.sh", "-DosgiDebugOptions", "-DportOffset=1"};
            commandDirectory = CarbonToolsUtil.getCarbonHome(context) + "/bin";
        }
        try {
            process = CarbonCommandToolsUtil.runScript(commandDirectory, cmdArray);
            String line;
            long startTime = System.currentTimeMillis();
            while ((System.currentTimeMillis() - startTime) < timeout) {
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
        }
        CarbonCommandToolsUtil.isServerStartedUp(context, portOffset);
        Assert.assertTrue(isFoundTheMessage, "Java file not created successfully");
    }

    @AfterClass(alwaysRun = true)
    public void cleanResources() throws Exception {
        CarbonToolsUtil.serverShutdown(process, 1, context);
    }

}

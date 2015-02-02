package org.wso2.appserver.integration.tests.carbontools;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.appserver.integration.common.utils.ASIntegrationTest;
import org.wso2.appserver.integration.common.utils.CarbonCommandToolsUtil;
import org.wso2.appserver.integration.tests.carbontools.utils.CarbonToolsUtil;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.ContextXpathConstants;

import java.io.File;

import static org.testng.Assert.assertTrue;

// This class is to test build.xml by running ant check repository lib folder for jars and
// run ant localize and check repository/components/dropins for language bundle

public class RunBuildXMLTestCase extends ASIntegrationTest {

    private static final Log log = LogFactory.getLog(RunBuildXMLTestCase.class);
    AutomationContext context;
    String carbonHome;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        context = new AutomationContext("AS", "appServerInstance0002",
                                        ContextXpathConstants.SUPER_TENANT,
                                        ContextXpathConstants.ADMIN);
        carbonHome= CarbonToolsUtil.getCarbonHome(context);
    }

    @Test(groups = {"wso2.as"}, description = "Server restart test")
    public void testBuildXMLGenerateRemoteRegistryClients() throws Exception {
        boolean isJarCreated = false;
        Process process = null;
        try {

            File folder = new File(carbonHome + File.separator + "repository" + File.separator + "lib");
            File[] listOfFilesBeforeRunAntCommand = folder.listFiles();
            process = CarbonCommandToolsUtil.runScript(carbonHome + "/bin", new String[]{"ant"});
            long startTime = System.currentTimeMillis();
            long timeout = 2000;
            while ((System.currentTimeMillis() - startTime) < timeout) {
                File[] listOfFilesAfterRunAntCommand = folder.listFiles();
                if (listOfFilesAfterRunAntCommand.length > listOfFilesBeforeRunAntCommand.length) {
                    log.info("Jars created successfully");
                    isJarCreated = true;
                    break;
                }
            }
        }finally {
            if (process != null) {
                process.destroy();
            }
        }
        assertTrue(isJarCreated, "Jar not created successfully");
    }

    @Test(groups = {"wso2.as"}, description = "Server restart test",
          dependsOnMethods = {"testBuildXMLGenerateRemoteRegistryClients"})
    public void testBuildXMLGenerateLanguageBundle() throws Exception {
        boolean isJarCreated = false;
        Process process = null;
        try {
            process = CarbonCommandToolsUtil.runScript(carbonHome + "bin",
                                                       new String[]{"ant", "localize"});
            File folder = new File(carbonHome + File.separator + "repository" + File.separator +
                                   "components" + File.separator + "dropins");
            long startTime = System.currentTimeMillis();
            long timeout = 2000;
            while ((System.currentTimeMillis() - startTime) < timeout) {
                if (folder.exists() && folder.isDirectory()) {
                    File[] listOfFiles = folder.listFiles();
                    for (File file : listOfFiles) {//Check rep lib as well
                        if (file.getName().contains("languageBundle") && file.getName().contains("jar")) {
                            log.info("LanguageBundle jar created successfully");
                            isJarCreated = true;
                            break;
                        }
                    }
                    if (isJarCreated) {
                        break;
                    }
                }
            }
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        assertTrue(isJarCreated, "Jar not created successfully");
    }
}

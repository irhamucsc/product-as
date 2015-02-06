package org.wso2.appserver.integration.tests.carbontools;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.appserver.integration.common.utils.ASIntegrationTest;
import org.wso2.appserver.integration.common.utils.CarbonCommandToolsUtil;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;

/**
 * This class is to test java2wsdl command by providing a java class and get wsdl file
 */
public class Java2WsdlCommandTestCase extends ASIntegrationTest {

    private static final Log log = LogFactory.getLog(CarbonServerBasicOperationTestCase.class);
    Process process;


    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        File sourceFile =
                new File(TestConfigurationProvider.getResourceLocation() + File.separator +
                         "artifacts" + File.separator + "AS" + File.separator + "carbontools" +
                         File.separator + "testjava2wsdl");
        File targetFile = new File(
                System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "bin/testjava2wsdl");
        copyFolder(sourceFile, targetFile);
    }

    @AfterClass(alwaysRun = true)
    public void cleanResources() throws RemoteException {
        process.destroy();
    }

    public void copyFolder(File src, File dest)
            throws IOException, IOException {
        if (src.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdir();
                log.info("Directory copied from " + src + "  to " + dest);
            }
            String files[] = src.list();
            for (String file : files) {
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                copyFolder(srcFile, destFile);
            }

        } else {
            //if file, then copy it
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            in.close();
            out.close();
            log.info("File copied from " + src + " to " + dest);
        }
    }

    @Test(groups = "wso2.as", description = "Java to wsdl test")
    public void testJava2Wsdl() throws Exception {
        String[] cmdArrayToWsdl2Java;
        String commandDirectory;
        if (CarbonCommandToolsUtil.isCurrentOSWindows()) {
            throw new SkipException("Issue with wsdl2java.bat");
//            https://wso2.org/jira/browse/CARBON-15150
//            cmdArrayToWsdl2Java =
//                    new String[]{"cmd.exe", "/c", "start", "java2wsdl.bat", "-cn", "testjava2wsdl.Java2Wsdl"};
//            commandDirectory = System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "bin";
        } else {
            cmdArrayToWsdl2Java = new String[]{"sh", "java2wsdl.sh", "-cn", "testjava2wsdl.Java2Wsdl"};
            commandDirectory = System.getProperty(ServerConstants.CARBON_HOME) + "/bin";
        }
        process = CarbonCommandToolsUtil.runScript(commandDirectory, cmdArrayToWsdl2Java);
        boolean fileCreated = CarbonCommandToolsUtil.waitForFileCreation(
                System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "bin" +
                File.separator + "Java2Wsdl.wsdl");
        Assert.assertTrue(fileCreated, "Java file not created successfully");

    }

}

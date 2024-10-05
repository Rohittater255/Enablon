package setup;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import org.apache.log4j.Logger;
import org.testng.ITestContext;
import org.testng.annotations.BeforeSuite;

import java.util.Iterator;
import java.util.Map;

public class AutomationSetup {

    private static final Logger logger = Logger.getLogger(AutomationSetup.class);

    @BeforeSuite(description = "This method will load parameter/configurations from testng.xml file")
    public void loadConfigurations(ITestContext context) {
        Iterator iterator = context.getSuite().getXmlSuite().getAllParameters().entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, String> map = (Map.Entry) iterator.next();
            System.setProperty(map.getKey(), map.getValue());
            logger.info("Setting Parameter " + map.getKey() + "=" + map.getValue());
        }
    }

}

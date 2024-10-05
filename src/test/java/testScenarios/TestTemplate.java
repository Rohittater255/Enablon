package testScenarios;

import assertions.ToDoPageAssertions;
import org.apache.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import pages.ToDoPage;
import utils.ScreenshotUtils;

import static reporting.ExtentReporter.test;

public class TestTemplate {

    private static final Logger logger = Logger.getLogger(TestTemplate.class);
    protected ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    public static ThreadLocal<String> screenshotPath = new ThreadLocal<>();
    protected ToDoPage toDoPage;
    protected ToDoPageAssertions toDoPageAssertions;


    @BeforeMethod(description = "This Test case will create")
    @Parameters("browserType")
    public void initBrowser(String browserType) {
        switch (browserType.toLowerCase()) {
            case "chrome":
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--start-maximized");
                driver.set(new ChromeDriver(chromeOptions));
                break;
            case "firefox":
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                firefoxOptions.addArguments("--start-maximized");
                driver.set(new FirefoxDriver(firefoxOptions));
                break;
            default:
                throw new IllegalArgumentException("Unsupported browser type: " + browserType);
        }
        String url = System.getProperty("application.Url");
        driver.get().get(url);
    }

    @BeforeMethod(dependsOnMethods = "initBrowser", description = "This Test case will create")
    public void init() {
        toDoPage = new ToDoPage(driver.get());
        toDoPageAssertions = new ToDoPageAssertions(driver.get());
    }


    @AfterMethod(description = "This Test case will create")
    public void closeBrowser(ITestResult result) {
        try {
            ScreenshotUtils screenshotUtils = new ScreenshotUtils();
            screenshotUtils.takeScreenshot(result, driver.get());
        } catch (Exception e) {
            System.out.println("Unable To capture screenshot");
        }


        if (driver.get() != null)
            driver.get().quit();//Quits the entire browser session
    }

}

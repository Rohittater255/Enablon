package utils;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;

import java.io.File;
import java.io.IOException;

public class ScreenshotUtils {


    public void takeScreenshot(ITestResult result, WebDriver driver) {
        File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        System.out.println("Screenshot created at local folder path: " + scrFile);

        String screenshotDirectory = new File(System.getProperty("user.dir")).getAbsolutePath();
        File destFile = new File(screenshotDirectory + "/test-output/" + result.getMethod().getMethodName() + ".png");

        try {
            FileUtils.copyFile(scrFile, destFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

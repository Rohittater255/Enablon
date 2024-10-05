package utils;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;

import java.io.File;
import java.io.IOException;

public class ScreenshotUtils {


    public String takeScreenshot(ITestResult result, WebDriver driver) {
        File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        System.out.println("Screenshot created at local folder path: " + scrFile);


        // Generate a unique filename based on the test method name and current timestamp

        String methodName = result.getMethod().getMethodName();
//        File destFile = new File("test_output/" + methodName + "_" + System.currentTimeMillis() + ".png");
        String screenshotDirectory = new File(System.getProperty("user.dir")).getAbsolutePath();
        File destFile = new File(screenshotDirectory + "/test-output/" + methodName + ".png");

        try {
//            // Move the screenshot file to the destination


            FileUtils.copyFile(scrFile, destFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return destFile.getPath();

    }
}

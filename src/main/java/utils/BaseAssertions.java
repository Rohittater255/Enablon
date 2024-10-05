package utils;


import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.asserts.SoftAssert;

import java.time.Duration;

public class BaseAssertions extends SoftAssert {
    private static final Logger logger = Logger.getLogger(BaseAssertions.class);
    protected WebDriver driver;

    protected boolean assertElementNotPresent(String name) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[text()='" + name + "']"))).isDisplayed();
            logger.info("Element is present");
            return false;
        } catch (NoSuchElementException | TimeoutException e) {
            logger.info("Element is not present");
            return true;
        }
    }
}

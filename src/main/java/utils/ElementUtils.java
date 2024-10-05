package utils;

import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;

import static reporting.ExtentReporter.test;

public class ElementUtils {

    private static final Logger logger = Logger.getLogger(ElementUtils.class);

    protected WebDriver driver;
    protected Actions action;

    protected void log(String message) {
        test.get().info(message);
    }



    public void clickOnElement(String locator) {
        driver.findElement(By.xpath(locator));
        log("Clicked on element:- "+locator);
    }

    public void clickOnElementWithMouse(String locator) {
        driver.findElement(By.xpath(locator));
        log("Clicked on element:- "+locator);
    }
    public void enterText(String locator, String text) {
        driver.findElement(By.xpath(locator)).clear();
        log("Cleared Text on element:- "+locator);
        driver.findElement(By.xpath(locator)).sendKeys(text);
        log("Entered text= "+text);
    }

    public void performEnter() {
        log("Performing Keyboard Enter");
        action.keyDown(Keys.ENTER).keyUp(Keys.ENTER).build().perform();
    }

    public String xPathParser(String xpath, String replaceTo) {
      return xpath.replace("$ToBeReplaced",replaceTo);
    }

}

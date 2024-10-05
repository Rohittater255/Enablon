package utils;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;

import static reporting.ExtentReporter.test;

public class ElementUtils {

    protected WebDriver driver;
    protected Actions action;

    protected void log(String message) {
        test.get().info(message);
    }

    public void clickOnElement(String locator) {
        driver.findElement(By.xpath(locator)).click();
        log("Clicked on element:- " + locator);
    }

    public void mouseDoubleClick(String locator) {
        action.moveToElement(driver.findElement(By.xpath(locator))).doubleClick().build().perform();
        log("Double Clicked on element:- " + locator);
    }

    public void mouseHover(String locator) {
        action.moveToElement(driver.findElement(By.xpath(locator))).build().perform();
        log("Mouse Hover on element:- " + locator);
    }

    public void enterText(String locator, String text) {
        driver.findElement(By.xpath(locator)).clear();
        log("Cleared Text on element:- " + locator);
        driver.findElement(By.xpath(locator)).sendKeys(text);
        log("Entered text= " + text);
    }

    public void enterTextByScript(String script, String text) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(script + ".value='" + text + "';");
        log("Entered text= " + text);
    }


    public void performEnter() {
        log("Performing Keyboard Enter");
        action.keyDown(Keys.ENTER).keyUp(Keys.ENTER).build().perform();
    }

    public String replaceXPathPlaceholder(String xpath, String replaceTo) {
        return xpath.replace("$ToBeReplaced", replaceTo);
    }

}

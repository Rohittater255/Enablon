package assertions;

import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import utils.BaseAssertions;

public class ToDoPageAssertions extends BaseAssertions {



    public ToDoPageAssertions(WebDriver driver) {
        super.driver = driver;
    }


    public void assertToDoExists(String name) {
        Assert.assertTrue(driver.findElement(By.xpath("//*[@class='todo-list']/li/div/label[text()='" + name + "']")).isDisplayed());
    }

    public void assertToDoNotExist(String name) {
        Assert.assertTrue(assertElementNotPresent(name));
    }

}

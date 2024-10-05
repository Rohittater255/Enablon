package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import utils.ElementUtils;

import java.time.Duration;
import java.util.NoSuchElementException;

public class ToDoPage extends ElementUtils {

    public ToDoPage(WebDriver driver) {
        super.driver = driver;
        super.action = new Actions(driver);
    }

    private String tab_all = "//a[text()='All']";
    private String tab_active = "//a[text()='Active']";
    private String tab_complete = "//a[text()='Completed']";
    private String tab_clearCompleted = "//a[text()='Clear completed']";
    private String txt_todo = "//input[@id='todo-input']";
    private String hover_todo = "//input[@id='todo-input']";
    private String button_completeTodo = "//label[text()='$ToBeReplaced']/preceding-sibling::input";
    private String button_deleteTodo = "//label[text()='$ToBeReplaced']/following-sibling::button";


    public void clickOnAllTab() {
        clickOnElement(tab_all);
    }

    public void clickOnActiveTab() {
        clickOnElement(tab_active);
    }

    public void clickOnCompletedTab() {
        clickOnElement(tab_complete);
    }

    public void clickOnClearCompletedTab() {
        clickOnElement(tab_clearCompleted);
    }


    public void createToDo(String name) {
        enterText(txt_todo, name);
        performEnter();
    }


    public void editToDo(String originalName, String updatedName) {
        String hover_todo_parsed = xPathParser(hover_todo, originalName);
//        clickOnElementWithMouse(hover_todo_parsed);
        action.moveToElement(driver.findElement(By.xpath("//label[text()='" + originalName + "']"))).click().build().perform();
        action.moveToElement(driver.findElement(By.xpath("//label[text()='" + originalName + "']"))).click().build().perform();
        driver.findElement(By.xpath("//*[@class='todo-list']/li/div/label[text()='" + originalName + "']")).clear();
        driver.findElement(By.xpath("//*[@class='todo-list']/li/div/label[text()='" + originalName + "']")).sendKeys(updatedName);
        performEnter();
    }


    public void deleteToDo(String name) {
        action.moveToElement(driver.findElement(By.xpath("//label[text()='" + name + "']"))).build().perform();
        FluentWait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(100))
                .pollingEvery(Duration.ofSeconds(10))
                .ignoring(NoSuchElementException.class);

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//label[text()='" + name + "']/following-sibling::button")));

//        String button_de = xPathParser(button_de, name);
//        clickOnElement(button_completedTodo_parsed);
        driver.findElement(By.xpath("//label[text()='" + name + "']/following-sibling::button")).click();
    }

    public void markAsCompleted(String name) {
        String button_completedTodo_parsed = xPathParser(button_completeTodo, name);
        clickOnElement(button_completedTodo_parsed);
    }

}

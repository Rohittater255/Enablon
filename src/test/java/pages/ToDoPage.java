package pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import utils.ElementUtils;

public class ToDoPage extends ElementUtils {

    public ToDoPage(WebDriver driver) {
        super.driver = driver;
        super.action = new Actions(driver);
    }


    private String tab_complete = "//a[text()='Completed']";
    private String txt_todo = "//input[@id='todo-input']";
    private String label_todo = "//label[text()='$ToBeReplaced']";
    private String button_completeTodo = "//label[text()='$ToBeReplaced']/preceding-sibling::input";
    private String button_deleteTodo = "//label[text()='$ToBeReplaced']/following-sibling::button";


    public void clickOnCompletedTab() {
        clickOnElement(tab_complete);
    }


    public void createToDo(String name) {
        enterText(txt_todo, name);
        performEnter();
    }


    public void editToDo(String originalName, String updatedName) {
        String label_todo_parsed = replaceXPathPlaceholder(label_todo, originalName);
        mouseDoubleClick(label_todo_parsed);
        String script = "document.querySelector('#root > main > ul > li > div > div > input')";
        enterTextByScript(script, updatedName);
        performEnter();
    }


    public void deleteToDo(String name) {
        String label_todo_parsed = replaceXPathPlaceholder(label_todo, name);
        mouseHover(label_todo_parsed);
        String button_deleteTodo_parsed = replaceXPathPlaceholder(button_deleteTodo, name);
        clickOnElement(button_deleteTodo_parsed);
    }

    public void markAsCompleted(String name) {
        String button_completedTodo_parsed = replaceXPathPlaceholder(button_completeTodo, name);
        clickOnElement(button_completedTodo_parsed);
    }

}

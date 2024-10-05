package testScenarios;

import org.testng.annotations.Test;

public class NegativeScenarios extends TestTemplate {


    @Test(description = "This test case will attempt to create a ToDo with an empty string", groups = "negative")
    public void createToDoWithEmptyString() {
        String name = "";
        toDoPage.createToDo(name);
        toDoPageAssertions.assertToDoNotExist(name);
    }


    @Test(description = "This test case will attempt to create a new ToDo with a single character", groups = "negative")
    public void createToDoWithSingleCharacter() {
        String name = "A";
        toDoPage.createToDo(name);
        toDoPageAssertions.assertToDoNotExist(name);
    }

    @Test(description = "This test case will attempt to create a new ToDo with special characters", groups = "negative")
    public void createToDoWithSpecialCharacters() {
        String name = ";";
        toDoPage.createToDo(name);
        toDoPageAssertions.assertToDoNotExist(name);
    }


    @Test(description = "This test case will attempt to create a ToDo with leading and trailing spaces", groups = "negative")
    public void createToDoWithLeadingAndTrailingSpaces() {
        String name = "   Test   ";
        toDoPage.createToDo(name);
        toDoPageAssertions.assertToDoNotExist(name);

    }

}

package testScenarios;

import org.apache.log4j.Logger;
import org.testng.annotations.Test;

public class PositiveScenarios extends TestTemplate {

    private static final Logger logger = Logger.getLogger(PositiveScenarios.class);

    @Test(description = "This test case will create a new ToDo", groups = "positive")
    public void createToDo() {
        String name = "Rohit_Create";
        logger.info("Creating ToDo: " + name);
        toDoPage.createToDo(name);
        toDoPageAssertions.assertToDoExists(name);
        logger.info("ToDo created and verified: " + name);
    }

    @Test(description = "This test case will edit a newly created ToDo", groups = "positive")
    public void editToDo() {
        String name = "Rohit_Original";
        String updatedName = "Rohit_Updated";

        logger.info("Creating ToDo for edit: " + name);
        toDoPage.createToDo(name);
        toDoPageAssertions.assertToDoExists(name);

        logger.info("Editing ToDo: " + name + " to: " + updatedName);
        toDoPage.editToDo(name, updatedName);
        toDoPageAssertions.assertToDoExists(updatedName);
        logger.info("ToDo edited and verified: " + updatedName);
    }

    @Test(description = "This test case will delete a newly created ToDo", groups = "positive")
    public void deleteToDo() {
        String toDoName = "Rohit_Delete";

        logger.info("Creating ToDo for deletion: " + toDoName);
        toDoPage.createToDo(toDoName);
        toDoPageAssertions.assertToDoExists(toDoName);

        logger.info("Deleting ToDo: " + toDoName);
        toDoPage.deleteToDo(toDoName);
        toDoPageAssertions.assertToDoNotExist(toDoName);
        logger.info("ToDo deleted and verified: " + toDoName);
    }

    @Test(description = "This test case will mark a newly created ToDo as completed", groups = "positive")
    public void markToDoAsCompleted() {
        String toDoName = "Rohit_MarkCompleted";

        logger.info("Creating ToDo to mark as completed: " + toDoName);
        toDoPage.createToDo(toDoName);
        toDoPageAssertions.assertToDoExists(toDoName);

        logger.info("Marking ToDo as completed: " + toDoName);
        toDoPage.markAsCompleted(toDoName);
        toDoPage.clickOnCompletedTab();
        toDoPageAssertions.assertToDoExists(toDoName);
        logger.info("ToDo marked as completed and verified: " + toDoName);
    }
}

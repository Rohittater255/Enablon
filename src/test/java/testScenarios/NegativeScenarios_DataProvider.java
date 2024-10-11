package testScenarios;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class NegativeScenarios_DataProvider extends TestTemplate {

    @DataProvider(name = "testData")
    public Object[][] createData() {
        return new Object[][] {
                {";"},
                {" "},
                {"  "},
                {"#"},
                {"%"}
        };
    }

    @Test(description = "This test case will attempt to create a ToDo with an empty string", groups = "negative", dataProvider = "testData")
    public void createToDoWithEmptyString(String data) {
        toDoPage.createToDo(data);
        toDoPageAssertions.assertToDoNotExist(data);
    }





}

################# ToDo Application Test Suite #################
#################  Introduction  #################
This Test Suite is designed to ensure the functionality and reliability of the ToDo application. The suite includes both
positive and negative test cases that validate different aspects of the application.


################# Project Structure #################
src/
├── main/
│   └── java/
│       └── pages/
│           └── ToDoPage.java
│       └── utils/
│           ├── ElementUtils.java
│       └── reporting/
│           └── CustomReport.java
│   └── resources/
│       └── log4j.properties
├── test/
│   └── java/
│       └── assertions/
│           └── ToDoPageAssertion.java
│       └── pages/
│           └── ToDoPage.java
│       └── testScenarios/
│           ├── PositiveScenarios.java
│           ├── NegativeScenarios.java
pom.xml



################# Packages #################

1) pages: Contains Page Object Model (POM) classes representing different pages of the application.

2) testScenarios: Contains the test classes.

3) utils: Contains utility classes for common functions.

4) reporting: Contains custom reporting logic.

################# Patterns Used #################

Page Object Model (POM)
Why: It promotes code reusability and separation of concerns by encapsulating the page structure and behaviors.

How: Each page class (e.g., ToDoPage) contains methods to interact with that specific page, making the tests easier to
write and maintain.



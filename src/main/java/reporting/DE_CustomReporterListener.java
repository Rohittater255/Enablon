package reporting;

import io.opentelemetry.api.internal.StringUtils;
import org.apache.log4j.Logger;
import org.testng.*;
import org.testng.collections.Lists;
import org.testng.internal.ResultMap;
import org.testng.xml.XmlSuite;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * final report for release
 * same format as old report
 */
@Deprecated
public class DE_CustomReporterListener implements IReporter {

    private final static Logger LOGGER = Logger.getLogger(DE_CustomReporterListener.class);

    /**
     * A constant that defines a comparator for TestNG TestResults. This comparator returns the TestNG TestResults ordered by execution date. For example, if the test A ran at 08:00 and the test B ran at 07:00, it will say that the correct
     * order for these elements is [B, A].
     */
    public final Comparator<ITestResult> EXECUTION_DATE_COMPARATOR = new ExecutionDateCompator_new();

    private PrintWriter writer;
    private String reportTitle = "Test Execution Report";
    private String reportFileName = "custom-report.html";
    private String testFailuresCollection = "";
    private String suiteFailuresCollection = "";
    private final String AUTOMATION_SETUP = "Automation Setup";
    public int suite_counter, test_counter, test_step_counter, stack_trace_counter;

    public HashMap<String, String> epicsInSuite;
    //public JSONArray testJsonArray;
    public HashMap<String, String> suiteNameStatusMap;
    // HashMap<String, HashMap<String, String>> testwiseData = new HashMap<>();
// HashMap<String, HashMap<String, Object>> classwiseData = new HashMap<>();
    private int testCounter = 0;
    private int classCounter = 0;
    private int methodCounter = 0;
    private int logCounter = 0;

    /**
     * Generate Report - main method
     */
    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {

        // Initialization
        epicsInSuite = new HashMap<>();
        suiteNameStatusMap = new HashMap<>();

        getData(suites);

        // Setting file Name and creating File
        setReportTitleAndReportFileNameFromSuiteOfSuiteXMLSuiteTagName(suites);
        try {
            writer = createWriter(outputDirectory);
        } catch (IOException e) {
            LOGGER.error("Unable to create output file");
            e.printStackTrace();
            return;
        }

        // initializing counters to 0
        suite_counter = 0;
        test_counter = 0;
        test_step_counter = 0;
        stack_trace_counter = 0;

        // Start writing HTML Contents
        startHtml(writer);
        defineHiddenDiv();
        writeFixedNavbar();
        writeReportTitle(reportTitle);
        writeFeaturesBox();

        //writeAPICoverageSummary();

        generateSuiteSummaryReport1();

        endHtml(writer);
        writer.flush();
        writer.close();
        //CNFTOutputWritter.createReportFile(outputDirectory, reportFileName);
    }


    /*
     * For Printing SuiteOfSuite xml name at the top of the table in report
     */
    public void setReportTitleAndReportFileNameFromSuiteOfSuiteXMLSuiteTagName(List<ISuite> suites) {
        // For multithreaded parallel execution the thread name will contain testNGThread to identify reports to merge
        if (!Thread.currentThread().getName().contains("testNGThread")) {
            for (ISuite suite : suites) {
                if (suites.size() >= 1) {
                    // Suite-of-Suite xmls are not having any methods, it only
                    // contains suites
                    if (suite.getAllMethods().size() == 0) {
                        this.reportTitle = suite.getName().toUpperCase();
                        // Below code will set the report file name : this will
                        // split the suite-of-suite name as per space and pick
                        // the
                        // last word in array after space :
                        // Final report name will be:
                        // lastWordAfterSpaceInSuiteName-report.html in lower
                        // case
                        String[] reportFileNameArray = suite.getName().split("\\s+");
                        this.reportFileName = reportFileNameArray[reportFileNameArray.length - 1].toLowerCase()
                                + "-report.html";
                    }
                }
            }
        } else {
            this.reportFileName = Thread.currentThread().getName() + this.reportFileName;
            //this.reportTitle=TestNGParallelExecutionConfiguration.getSuiteOfSuiteName().toUpperCase();
        }
    }

    int total_passed = 0, overall_passed = 0;
    int total_failed = 0, overall_skipped = 0;
    int total_skipped = 0, overall_failed = 0;
    long total_execution_time = 0, overall_execution_time = 0;


    @Deprecated
    public void generateSuiteSummaryReport(List<ISuite> suites) {

        // int passed, overall_passed;
        // int failed, overall_skipped;
        // int skipped, overall_failed;

        //String testNameClass = "", testNameStatus = "";

        // overall_passed = 0;
        // overall_failed = 0;
        // overall_skipped = 0;
        // overall_execution_time = 0;

        // Suite Block SUITEBLOCK
        writer.println("<div class=\"card m-2 p-2 border-secondary\" id=\"SuitesList\">");
        writer.println("<div class=\"h2 text-center font-weight-light pt-2 text-secondary\" style=\"text-shadow: 1px 1px 3px #5C5858;\">Suites</div>");
        writer.println("<hr/>");
        writer.println("<div class=\"container-fluid p-0\">"); // main container-fluid start containing all suites and tests along with automation setup

        writeAutomationSetup(suites);

        // Get all "groupBy"
        //List<String> _groupBy = new ArrayList<>();
        HashMap<String, List<ISuite>> groupedBy = new HashMap<>();

        List<ISuite> suitesToRemove = new ArrayList<>();
        for (ISuite suite : suites) {
            if (suite.getParameter("groupBy") != null && suite.getParameter("groupBy") != "") {
                if (groupedBy.keySet().contains(suite.getParameter("groupBy"))) {
                    groupedBy.get(suite.getParameter("groupBy")).add(suite);
                    suitesToRemove.add(suite);
                } else {
                    List<ISuite> suitesList = new ArrayList<ISuite>();
                    suitesList.add(suite);
                    groupedBy.put(suite.getParameter("groupBy"), suitesList);
                    suitesToRemove.add(suite);
                }
            }
        }
        for (ISuite suite : suitesToRemove) {
            suites.remove(suite);
        }

        int passedInGroupBy = 0, skippedInGroupBy = 0, failedInGroupBy = 0;
        long total_execution_time_groupBy = 0;
        for (String groupBy : groupedBy.keySet()) {

            // Add "GroupBy" expandable header

            //int total_passed_groupBy, total_failed_groupBy, total_skipped_groupBy;
            passedInGroupBy = 0;
            skippedInGroupBy = 0;
            failedInGroupBy = 0;
            total_execution_time_groupBy = 0;

            for (ISuite suite : groupedBy.get(groupBy)) {

                Map<String, ISuiteResult> res = suite.getResults();
                //passedInSuite = skippedInSuite = failedInSuite = 0;
                for (ISuiteResult suiteRes : res.values()) {

                    ITestContext overview = suiteRes.getTestContext();

                    passedInGroupBy += getMethodSet(overview.getPassedTests(), suite).size();
                    skippedInGroupBy += getMethodSet(overview.getSkippedTests(), suite).size();
                    failedInGroupBy += getMethodSet(overview.getFailedTests(), suite).size();

                    total_execution_time_groupBy += (overview.getEndDate().getTime() - overview.getStartDate().getTime());
                }

            }

            //Execution details - pass, fail, skip, start end time

            for (ISuite suite : groupedBy.get(groupBy)) {
                //reportSuite(suite);
                //Map<String, ISuiteResult> res = suite.getResults();

            }

        }

        for (ISuite suite : suites) {
            total_passed = 0;
            total_failed = 0;
            total_skipped = 0;
            total_execution_time = 0;

            if (suites.size() >= 1 && !(suite.getAllMethods().size() == 0)) {
                //reportSuite(suite);
            }

        }

        // putting time elapsed as a hidden element for reporting
        writer.println("<span id=\"totalruntime\" hidden>" + convertMiliseconds(overall_execution_time) + "</span>");

        // putting passrate as a hidden for reporting and passrate checking sscript
        int overall_executed = overall_passed + overall_failed + overall_skipped;
        String overall_passrate = String.format("%.2f", ((double) overall_passed * 100) / (double) overall_executed);
        writer.println("<span id=\"passrateforeresult\" hidden>" + overall_passrate + "</span>");
        writer.println("<span id=\"passedtcforeresult\" hidden>" + overall_passed + "</span>");
        writer.println("<span id=\"failedtcforeresult\" hidden>" + overall_failed + "</span>");
        writer.println("<span id=\"skippedtcforeresult\" hidden>" + overall_skipped + "</span>");
        writer.println("<span id=\"totaltcforeresult\" hidden>" + overall_executed + "</span>");

        writer.println("</div>"); // main container-fluid end
        writer.println("</div>"); // SuitesList end

    }


    @Deprecated
    private void writeAutomationSetup(List<ISuite> suites) {

        int _failedTests = 0;
        int _skippedTests = 0;
        int _passedTests = 0;


        for (ISuite suite : suites) {

            for (ISuiteResult suiteResult : suite.getResults().values()) {
                ITestContext tContext = suiteResult.getTestContext();
                if (tContext.getName().equalsIgnoreCase("Automation Setup") && (tContext.getEndDate().getTime() - tContext.getStartDate().getTime()) > 100) {

                    Map<String, ISuiteResult> suiteResults = suite.getResults();
                    //List<ITestNGMethod> suiteResults1 = suite.getAllMethods();
                    //for (ISuiteResult asResult : suiteResults.values()) {
                    _failedTests = tContext.getFailedTests().size();
                    _skippedTests = tContext.getSkippedTests().size();
                    _passedTests = tContext.getPassedTests().size();

                    //}
                }
            }
        }
        String testStatusStyle = "";
        String testStatusName = "";

        if (_failedTests > 0) {
            testStatusStyle = "danger";
            testStatusName = "Failure";
        } else if (_skippedTests > 0 && _passedTests > 0) {
            testStatusStyle = "warning";
            testStatusName = "Warning";
        } else if (_skippedTests > 0 && _passedTests == 0) {
            testStatusStyle = "secondary";
            testStatusName = "Skipped";
        } else {
            testStatusStyle = "success";
            testStatusName = "Success";
        }


        writer.println("<div class=\"card text-secondary border-primary p-0 mx-0 my-2\">");
        writer.println("<div style=\"cursor: pointer;\" id=\"AutomationSetup_1_Header\" data-toggle=\"collapse\" data-target=\"#AutomationSetup_1_Body\" class=\"card-header text-secondary bg-light mx-1 mt-1 font-weight-bold text-center\">&nbsp " +
                "<span style=\"cursor: pointer;\" class=\"btn btn-" + testStatusStyle + " btn-sm\" role=\"button\" data-toggle=\"collapse\" data-target=\"#AutomationSetup_1_Body\">" + testStatusName + "</span>&nbsp&nbsp" +
                "<span >Automation Setup</span> &nbsp</div>");
        writer.println("<div class=\"card-body text-dark mb-1 p-0 collapse\" id=\"AutomationSetup_1_Body\" aria-labelledby=\"AutomationSetup_1_Header\">");
        writer.println("<div class=\"container-fluid m-0 p-1\">");

        writer.println("<table class=\"table table-secondary table-hover table-sm\">");
        writer.println("<thead class=\"bg-secondary text-white\">");
        writer.println("<tr class=\"\">");
        writer.println("</tr>");
        writer.println("</thead>");
        writer.print("<tbody>");

        //writer.println(RestAssuredLogger.getHTML_Modal("properties.json", new JSONHelper(AutomationSetup.propertiesJSON).getJSON(), "JSON"));

        for (ISuite suite : suites) {
            Map<String, ISuiteResult> suiteResults = suite.getResults();
            //List<ITestNGMethod> suiteResults1 = suite.getAllMethods();
            for (ISuiteResult suiteResult : suiteResults.values()) {
                ITestContext tContext = suiteResult.getTestContext();
                if (tContext.getName().equalsIgnoreCase("Automation Setup") && (tContext.getEndDate().getTime() - tContext.getStartDate().getTime()) > 100) {

                    List<ITestResult> listResults = getTestNGResultsOrderedByExecutionDate(tContext);

                    writer.println(getExpandableTestCaseDetails(listResults.get(0)));
                    generateTestSummaryRerport(suiteResult);

                }
                suiteResults.remove(suiteResult);

            }
        }

        writer.print("</tbody>");
        writer.println("</table>");
        writer.println("</div>"); // container-fluid
        writer.println("</div>"); // card-body
        writer.println("</div>"); // card text-primary

    }

    private String getExpandableTestCaseDetails(ITestResult testResult) {
        String _output = "";

        // Creating parameter table
        Map<String, String> params = testResult.getMethod().getXmlTest().getAllParameters();
        String xmlFileName = testResult.getMethod().getXmlTest().getSuite().getFileName();
        boolean hasParameters = !params.isEmpty();
        Object[] parameters = testResult.getParameters();
        // Object[] parameters = testResult.getParameters();
        // boolean hasParameters = parameters != null && parameters.length > 0;

        if (hasParameters) {

            //_output += ("<div class=\"table-responsive\"><b>XML file name: </b>" + xmlFileName + "</div>");
            _output += ("<div class=\"table-responsive\">");
            _output += ("<table class=\"table able-secondary table-hover table-sm\">");

            _output += ("<thead class=\"bg-secondary text-white\">");
            _output += ("<tr class=\"\">");
            _output += getHeaderColumn("Parameter Name");
            _output += getHeaderColumn("Parameter Value");
            _output += ("</tr>");
            _output += ("</thead>");

            _output += ("<tbody>");
            _output += ("<tr class=\"table-light\"><td colspan=\"2\"><b>XML file name: </b>" + xmlFileName + "</td></tr>");

            for (String param : params.keySet()) {
                try {
                    if (param != null) {
                        _output += ("<tr>");
                        _output += ("<td><b>" + param + "</b></td>"); // Parameter name
                        if (params.get(param).length() > 100)
                            _output += ("<td>" + get_ModalWindow(param, params.get(param)) + "</td>"); // Parameter value in modal window
                        else
                            _output += ("<td>" + params.get(param) + "</td>"); // Parameter value
                        _output += ("</tr>");
                    }

                } catch (NullPointerException e) {
                    //writer.println("<td></td><td> null </td>");
                }

            }

            _output += ("</tbody>");

            _output += ("</table>");
            _output += ("</div>");
        }

        // Creating method log ie. table for REST API details

// _output += ("<div class=\"table-responsive\">");
// _output += getMethodLog(testResult);
// _output += ("</div>");
        return _output;
    }

    private void writeAutomationSetup1() {

        int _failedTests = 0;
        int _skippedTests = 0;
        int _passedTests = 0;

        int _totalTests = 0;
        List<ClassData> cRs = getClassesByXMLTestName(AUTOMATION_SETUP);

        for (ClassData cr : cRs) {
            _failedTests += cr.getFailedTestsCount();
            _skippedTests += cr.getSkippedTestsCount();
            _passedTests += cr.getPassedTestsCount();
        }


        String testStatusStyle = "";
        String testStatusName = "";

        if (_failedTests > 0) {
            testStatusStyle = "danger";
            testStatusName = "Failure";
        } else if (_skippedTests > 0 && _passedTests > 0) {
            testStatusStyle = "warning";
            testStatusName = "Warning";
        } else if (_skippedTests > 0 && _passedTests == 0) {
            testStatusStyle = "secondary";
            testStatusName = "Skipped";
        } else {
            testStatusStyle = "success";
            testStatusName = "Success";
        }


        writer.println("<div class=\"card text-secondary border-primary p-0 mx-0 my-2\">");
        writer.println("<div style=\"cursor: pointer;\" id=\"AutomationSetup_1_Header\" data-toggle=\"collapse\" data-target=\"#AutomationSetup_1_Body\" class=\"card-header text-secondary bg-light mx-1 mt-1 font-weight-bold text-center\">&nbsp " +
                "<span style=\"cursor: pointer;\" class=\"btn btn-" + testStatusStyle + " btn-sm\" role=\"button\" data-toggle=\"collapse\" data-target=\"#AutomationSetup_1_Body\">" + testStatusName + "</span>&nbsp&nbsp" +
                "<span >Automation Setup</span> &nbsp</div>");
        writer.println("<div class=\"card-body text-dark mb-1 p-0 collapse\" id=\"AutomationSetup_1_Body\" aria-labelledby=\"AutomationSetup_1_Header\">");
        writer.println("<div class=\"container-fluid m-0 p-1\">");

        writer.println("<table class=\"table table-secondary table-hover table-sm\">");
        writer.println("<thead class=\"bg-secondary text-white\">");
        writer.println("<tr class=\"\">");
        writer.println("</tr>");
        writer.println("</thead>");
        writer.print("<tbody>");

        //writer.println(RestAssuredLogger.getHTML_Modal("properties.json", new JSONHelper(AutomationSetup.propertiesJSON).getJSON(), "JSON"));

// for (ISuite suite : suites) {
// Map<String, ISuiteResult> suiteResults = suite.getResults();
// //List<ITestNGMethod> suiteResults1 = suite.getAllMethods();
// for (ISuiteResult suiteResult : suiteResults.values()) {
// ITestContext tContext = suiteResult.getTestContext();
// if (tContext.getName().equalsIgnoreCase("Automation Setup") && (tContext.getEndDate().getTime() - tContext.getStartDate().getTime()) > 100) {
//
// List<ITestResult> listResults = getTestNGResultsOrderedByExecutionDate(tContext);
//
        writer.println(getExpandableTestCaseDetails(cRs.get(0).getClassMethodsResults().get(0)));
// generateTestSummaryRerport(suiteResult);
//
// }
// //suiteResults.remove(suiteResult);
//
// }
// }


        for (ClassData cr : cRs) {
            generateClassResults(cr);
        }

        xmlTests.remove(AUTOMATION_SETUP);
        writer.print("</tbody>");
        writer.println("</table>");
        writer.println("</div>"); // container-fluid
        writer.println("</div>"); // card-body
        writer.println("</div>"); // card text-primary

    }

    public void generateSuiteSummaryReport1() {


        // Suite Block SUITEBLOCK
        writer.println("<div class=\"card m-2 p-2 border-secondary\" id=\"SuitesList\">");
        //writer.println("<div class=\"h2 text-center font-weight-light pt-2 text-secondary\" style=\"text-shadow: 1px 1px 3px #5C5858;\">Suites</div>");
        writer.println("<hr/>");
        writer.println("<div class=\"container-fluid p-0\">"); // main container-fluid start containing all suites and tests along with automation setup




        for (String xmlTest : xmlTests) {
            total_passed = 0;
            total_failed = 0;
            total_skipped = 0;
            total_execution_time = 0;

            reportSuite1(xmlTest);

        }

        // putting time elapsed as a hidden element for reporting
        writer.println("<span id=\"totalruntime\" hidden>" + convertMiliseconds(overall_execution_time) + "</span>");

        // putting passrate as a hidden for reporting and passrate checking sscript
        int overall_executed = overall_passed + overall_failed + overall_skipped;
        String overall_passrate = String.format("%.2f", ((double) overall_passed * 100) / (double) overall_executed);
        writer.println("<span id=\"passrateforeresult\" hidden>" + overall_passrate + "</span>");
        writer.println("<span id=\"passedtcforeresult\" hidden>" + overall_passed + "</span>");
        writer.println("<span id=\"failedtcforeresult\" hidden>" + overall_failed + "</span>");
        writer.println("<span id=\"skippedtcforeresult\" hidden>" + overall_skipped + "</span>");
        writer.println("<span id=\"totaltcforeresult\" hidden>" + overall_executed + "</span>");

        writer.println("</div>"); // main container-fluid end
        writer.println("</div>"); // SuitesList end

    }

    // LinkedHashMap<String, LinkedHashMap<String, ClassData>> suiteResults = new LinkedHashMap<String, LinkedHashMap<String, ClassData>>();
    LinkedHashMap<String, ClassData> classResults = new LinkedHashMap<String, ClassData>();
    List<String> xmlTests = new ArrayList<>();

    private List<ClassData> getClassesByXMLTestName(String xmlTestName) {
        List<ClassData> result = new ArrayList<>();
        for (ClassData cR : classResults.values()) {
            if (cR.getXmlTestName().equalsIgnoreCase(xmlTestName))
                result.add(cR);
        }
        return result;
    }

    private void getData(List<ISuite> suites) {


        // get all classes from each suite
        for (ISuite suite : suites) {
            Map<String, ISuiteResult> classes = suite.getResults();

            for (ISuiteResult suiteResult : classes.values()) {

                String flowName = suiteResult.getTestContext().getName();
                ITestNGMethod[] allTestMethods = suite.getResults().get(suiteResult.getTestContext().getName()).getTestContext().getAllTestMethods();

                for (ITestNGMethod method : allTestMethods) {

                    if (classResults.get(method.getTestClass().getName()) == null) {
                        // new class, put all methods in it
                        ClassData cRR = new ClassData();
                        cRR.setName(method.getTestClass().getName());
                        cRR.setXmlTestName(flowName);

                        cRR.setIsDeprecated(method.getTestClass().getRealClass().isAnnotationPresent(Deprecated.class));

                        for (ITestNGMethod methodInClass : allTestMethods) {
                            if (methodInClass.getTestClass().getName().equalsIgnoreCase(method.getTestClass().getName())) {
                                MethodData methodData = new MethodData();
                                methodData.set_name(methodInClass.getMethodName());
                                methodData.set_iTestNGMethod(methodInClass);

                                if (suiteResult.getTestContext().getPassedTests().getResults(methodInClass).size() > 0) {
                                    methodData.set_status("PASS");
                                    methodData.set_iTestResult(suiteResult.getTestContext().getPassedTests().getResults(methodInClass).iterator().next());
                                }
                                if (suiteResult.getTestContext().getSkippedTests().getResults(methodInClass).size() > 0) {
                                    methodData.set_status("SKIPPED");
                                    methodData.set_iTestResult(suiteResult.getTestContext().getSkippedTests().getResults(methodInClass).iterator().next());
                                }
                                if (suiteResult.getTestContext().getFailedTests().getResults(methodInClass).size() > 0) {
                                    methodData.set_status("FAILED");
                                    methodData.set_iTestResult(suiteResult.getTestContext().getFailedTests().getResults(methodInClass).iterator().next());
                                }
                                cRR.addMethod(methodData);
                            }
                        }
                        classResults.put(cRR.getName(), cRR);
                    }
                }

            }
            for (ClassData classData : classResults.values()) {
                if (!xmlTests.contains(classData.getXmlTestName()))
                    xmlTests.add(classData.getXmlTestName());
            }
        }

    }

    private void reportSuite1(String xmlTestName) {

        //getClassesForSuite(suite);
        String suiteName = xmlTestName;
        List<ClassData> listClassData = getClassesByXMLTestName(xmlTestName);


        int passed = 0;
        int failed = 0;
        int skipped = 0;
        //long overall_execution_time = 0;

// String groupBy = (suite.getParameter("groupBy") != null) ? suite.getParameter("groupBy") : "Ungrouped";
// String mappedEpic = (suite.getParameter("epicsList") != null) ? suite.getParameter("epicsList").replaceAll("_", "-") : "Unmapped";
// String suiteName = suite.getName();
        String testNameClass = "", testNameStatus = "";

        suiteFailuresCollection = "";

        suite_counter++;


        writer.println("<div class=\"SuiteBlock card text-white border-secondary mb-1\" id=\"SuiteBlock_" + suite_counter + "\">"); // Suite-Card starts here for each suite it will create new card

        int passedInSuite = 0, skippedInSuite = 0, failedInSuite = 0;
        long suiteExecutionTime = 0;
        Boolean isDepricated = false;
        // get overall status for xml test

        for (ClassData _eachClass : listClassData) {
            passedInSuite += _eachClass.getPassedTestsCount();
            skippedInSuite += _eachClass.getSkippedTestsCount();
            failedInSuite += _eachClass.getFailedTestsCount();
            if (_eachClass.getIsDeprecated())
                isDepricated = true;
        }


        String suiteNameClass, suiteNameStatus;
        if (failedInSuite > 0) {
            suiteNameClass = "danger";
            suiteNameStatus = "Failure";
        } else if (skippedInSuite > 0 && passedInSuite > 0) {
            suiteNameClass = "warning";
            suiteNameStatus = "Warning";
        } else if (skippedInSuite > 0 && passedInSuite == 0) {
            suiteNameClass = "secondary";
            suiteNameStatus = "Skipped";
        } else {
            suiteNameClass = "success";
            suiteNameStatus = "Success";
        }

        // This is header of the suite-card
        writer.println("<div class=\"SuiteBlockHeader card-header text-light bg-info mb-1 font-weight-bold\">");
        writer.println("<span style=\"cursor: pointer;\" class=\"btn btn-" + suiteNameClass + " btn-sm\" role=\"button\" data-toggle=\"collapse\" data-target=\"#" + suiteName + "_" + suite_counter + "_Body\">" + suiteNameStatus + "</span>&nbsp&nbsp");
        if (isDepricated)
            writer.println("<span style=\"cursor: pointer;\" class=\"btn btn-warning btn-sm\" role=\"button\" data-toggle=\"collapse\" data-target=\"#" + suiteName + "_" + suite_counter + "_Body\"><b>DEPRICATED</b></span>&nbsp&nbsp");
        //writer.println("<span class=\"epicList\" hidden>" + mappedEpic + "</span>");
        writer.println("<span style=\"cursor: pointer;\" id=\"" + suiteName + "_" + suite_counter + "_Header\" data-toggle=\"collapse\" data-target=\"#" + suiteName + "_" + suite_counter + "_Body\">" + suiteName + "</span>&nbsp&nbsp");
        writer.println("<div class=\"float-right\" id=\"suite_summary_" + suite_counter + "\"> Passrate Summary.</div>");
        writer.println("</div>");

        //Suite name in Error List -- header in failure list
        suiteFailuresCollection += "<div class=\"text-danger h6\" id=\"test_in_suite_fail_head_" + suite_counter + "\" data-toggle=\"collapse\" data-target=\"#test_in_suite_fail_body_" + suite_counter + "\" >" + suiteName + "</div>";

        // Here body of the suite-card i.e. execution details of the suite starts
        writer.println("<div class=\"SuiteBlockBody card-body text-dark mb-1 p-0 collapse\" id=\"" + suiteName + "_" + suite_counter + "_Body\" aria-labelledby=\"" + suiteName + "_" + suite_counter + "_Header\">");

        //writer.println("<div class=\"TestBlock container-fluid m-0 p-1\"><b>Full class name:</b> " + classD + "</div>"); // container-fluid for each test starts here

        //Map<String, ISuiteResult> tests = suite.getResults();

        for (ClassData classData : listClassData) {
            testFailuresCollection = "";
            test_counter++;

            writer.println("<div id=\"TestBlock_" + test_counter + "\" class=\"TestBlock container-fluid m-0 p-1\">"); // container-fluid for each test starts here


            passed = classData.getPassedTestsCount();
            skipped = classData.getSkippedTestsCount();
            failed = classData.getFailedTestsCount();
            total_passed += passed;
            total_skipped += skipped;
            total_failed += failed;

            //Status class string to disable button
            String is_passed_disabled = (passed > 0) ? "" : "disabled";
            String is_skipped_disabled = (skipped > 0) ? "" : "disabled";
            String is_failed_disabled = (failed > 0) ? "" : "disabled";

            if (failed > 0) {
                testNameClass = "danger";
                testNameStatus = "Failure";
            } else if (skipped > 0 && passed > 0) {
                testNameClass = "warning";
                testNameStatus = "Warning";
            } else if (skipped > 0 && passed == 0) {
                testNameClass = "secondary";
                testNameStatus = "Skipped";
            } else {
                testNameClass = "success";
                testNameStatus = "Success";
            }

            //suiteExecutionTime += (overview.getEndDate().getTime() - overview.getStartDate().getTime());
            suiteExecutionTime += classData.get_duration();

            // Test name header start here
            writer.println("<div class=\"\" id=\"" + classData.getName() + "_" + test_counter + "_Header\" >");

            // Status indicator button ie. PASS or FAIL
            writer.println(
                    "<span style=\"cursor: pointer;\" class=\"btn btn-" + testNameClass + " btn-sm\" role=\"button\" data-toggle=\"collapse\" data-target=\"#" + classData.getName() + "_" + test_counter + "_Body\">" + testNameStatus + "</span>&nbsp&nbsp");
            if (classData.getIsDeprecated())
                writer.println("<span style=\"cursor: pointer;\" class=\"btn btn-warning btn-sm\" role=\"button\" data-toggle=\"collapse\" data-target=\"#" + classData.getName() + "_" + test_counter + "_Body\">DEPRICATED</span>&nbsp&nbsp");

            //Test name
            writer.println("<span style=\"cursor: pointer;\" class=\"card-title h6 text-truncate\" style=\"max-width: 350px;\" data-toggle=\"collapse\" data-target=\"#" + classData.getName() + "_" + test_counter + "_Body\" >" + classData.getName() + "</span>");

            //Execution details - pass, fail, skip, start end time
            writer.println("<div class=\"float-right\">");
            writer.println("<button class=\"btn btn-light btn-sm\" " + is_passed_disabled + ">Pass <span class=\"badge badge-success mx-3\">" + passed + "</span></button>");
            writer.println("<button class=\"btn btn-light btn-sm\" " + is_failed_disabled + " data-toggle=\"modal\" data-target=\"#failure_modal_value_" + test_counter + "\">Fail <span class=\"badge badge-danger mx-3\">" + failed + "</span></button>");

            //Data Modal for Failure List
            writer.println("<div class=\"modal fade\" id=\"failure_modal_value_" + test_counter + "\" tabindex=\"-1\" role=\"dialog\">");
            writer.println("<div class=\"modal-dialog modal-lg\" role=\"document\">");
            writer.println("<div class=\"modal-content\">");
            writer.println("<div class=\"modal-header text-white bg-info\">");
            writer.println("<h6 class=\"modal-title\" id=\"title1\"> " + classData.getName() + " Failures list </h6>");
            writer.println("<button type=\"button\" class=\"close\" data-dismiss=\"modal\" aria-label=\"Close\">");
            writer.println("<span aria-hidden=\"true\">&times;</span>");
            writer.println("</button>");
            writer.println("</div>");
            writer.println("<div class=\"modal-body\" id=\"failure_modal_body_" + test_counter + "\">");
            writer.println(testFailuresCollection);
            writer.println("</div>");
            writer.println("</div>");
            writer.println("</div>");
            writer.println("</div>");

            writer.println("<button class=\"btn btn-light btn-sm\" " + is_skipped_disabled + ">Skip <span class=\"badge badge-secondary mx-3\">" + skipped + "</span></button>");
            writer.println("<span class=\"btn btn-dark btn-sm\">" + convertMiliseconds(classData.get_duration()) + " <span class=\"badge badge-light ml-2\"> " + convertMiliseconds(classData.get_startTime())
                    + " - " + convertMiliseconds(classData.get_endTime()) + " </span></span>");
            writer.println("</div>");

            writer.println("</div>"); // Test name header ends here

            // Test name body starts here
            writer.println("<div class=\"p-1 collapse\" id=\"" + classData.getName() + "_" + test_counter + "_Body\">");

            generateClassResults(classData);

            writer.println("</div>"); // Test name body ends here

            writer.println("</div>"); // container-fluid for each test ends here

            if (!testFailuresCollection.isEmpty()) {
                suiteFailuresCollection += "<div class=\"text-dark h6\" id=\"test_in_suite_fail_body_" + suite_counter + "\">" + testFailuresCollection.replaceAll("\"", "\\\"") + "</div>";
            }

            //Javascript to update failure List

            writer.println("<script type=\"text/javascript\">");
            writer.println("var destination = document.getElementById('failure_modal_body_" + test_counter + "');");
            writer.println("destination.innerHTML = \"" + testFailuresCollection.replaceAll("\"", "\\\"") + "\";");
            writer.println("</script>");

        }

        // Total Summary
        int executedInSuite = passedInSuite + failedInSuite + skippedInSuite;
        String passrate = String.format("%.2f", ((double) passedInSuite * 100) / (double) executedInSuite);

        //Status class string to disable button
        String is_total_passed_disabled = (total_passed > 0) ? "" : "disabled";
        String is_total_skipped_disabled = (total_skipped > 0) ? "" : "disabled";
        String is_total_failed_disabled = (total_failed > 0) ? "" : "disabled";

        //Execution details - pass, fail, skip, start end time
        writer.println("<div class=\"float-right\" invisible id=\"suite_summary_detailed_" + suite_counter + "\">");
        writer.println("<button class=\"btn btn-info btn-sm\" " + is_total_passed_disabled + ">Pass <span class=\"badge badge-success mx-3\">" + passedInSuite + "</span></button>");
        writer.println(
                "<button class=\"btn btn-info btn-sm\" " + is_total_failed_disabled + " data-toggle=\"modal\" data-target=\"#total_failure_modal_value_" + suite_counter + "\">Fail <span class=\"badge badge-danger mx-3\">" + failedInSuite + "</span></button>");

        //Modal
        //Data Modal for Failure List
        writer.println("<div class=\"modal fade\" id=\"total_failure_modal_value_" + suite_counter + "\" tabindex=\"-1\" role=\"dialog\">");
        writer.println("<div class=\"modal-dialog modal-lg\" role=\"document\">");
        writer.println("<div class=\"modal-content\">");
        writer.println("<div class=\"modal-header text-white bg-info\">");
        writer.println("<h6 class=\"modal-title\" id=\"title1\"> " + suiteName + " Failures list </h6>");
        writer.println("<button type=\"button\" class=\"close\" data-dismiss=\"modal\" aria-label=\"Close\">");
        writer.println("<span aria-hidden=\"true\">&times;</span>");
        writer.println("</button>");
        writer.println("</div>");
        writer.println("<div class=\"modal-body\" id=\"total_failure_modal_body_" + suite_counter + "\">");
        writer.println(suiteFailuresCollection);
        writer.println("</div>");
        writer.println("</div>");
        writer.println("</div>");
        writer.println("</div>");

        writer.println("<button class=\"btn btn-info btn-sm\" " + is_total_skipped_disabled + ">Skip <span class=\"badge badge-secondary mx-3\">" + skippedInSuite + "</span></button>");
        writer.println("<span class=\"btn btn-dark btn-sm\">" + convertMiliseconds(suiteExecutionTime) + "</span>");
        writer.println("<span class=\"btn btn-light btn-sm\">" + passrate + "% </span>");
        writer.println("</div>");

        //Javascript to replace the summary in the header from bottom
        writer.println("<script type=\"text/javascript\">");
        writer.println("var destination = document.getElementById('suite_summary_" + suite_counter + "');");
        writer.println("var source = document.getElementById('suite_summary_detailed_" + suite_counter + "');");
        writer.println("destination.innerHTML = source.innerHTML;");
        writer.println("source.innerHTML = '';");
        writer.println("</script>");

        overall_passed += passedInSuite;
        overall_failed += failedInSuite;
        overall_skipped += skippedInSuite;
        overall_execution_time += suiteExecutionTime;

        writer.println("</div>"); // Body of Suite-Card starts here

        writer.println("</div>"); // Suite-Card starts here

        //Javascript to update failure List

        writer.println("<script type=\"text/javascript\">");
        writer.println("var destination = document.getElementById('total_failure_modal_body_" + suite_counter + "');");
        writer.println("destination.innerHTML = '" + suiteFailuresCollection + "';");
        writer.println("</script>");
    }

    private void generateClassResults(ClassData classData) {

        String testStatus = "", statusClass = "";
        String className = "", methodName = "";
        boolean is_failed_test = false;
        writer.println("<div class=\"TestBlock container-fluid m-0 p-1\"><b>Full class name:</b> " + classData.getFullName() + "</div>");
        //ITestContext testContext = suiteResult.getTestContext();

// List<ITestResult> sortedList = getTestNGResultsOrderedByExecutionDate(testContext);

        List<ITestResult> sortedList = classData.getClassMethodsResults();

        for (int counter = 0; counter < sortedList.size(); counter++) {

            is_failed_test = false;
            test_step_counter++;

            switch (sortedList.get(counter).getStatus()) {
                case 1:
                    statusClass = "success";
                    testStatus = "PASS";
                    break;
                case 2:
                    statusClass = "danger";
                    testStatus = "FAIL";
                    is_failed_test = true;
                    break;
                case 3:
                    statusClass = "secondary";
                    testStatus = "SKIP";
                    break;
            }

            className = sortedList.get(counter).getInstanceName();
            methodName = sortedList.get(counter).getName();

            // For display in Reports
            // Header of the test set name and its status
            writer.println("<div class=\"my-1 card-text\" data-toggle=\"collapse\" data-target=\"#" + className + "_" + methodName + "_" + test_step_counter + "_body\" id=\"" + className + "_" + methodName + "_" + test_step_counter + "_header\">");
            writer.println(
                    "<span style=\"cursor: pointer;\" class=\"MethodBlockStatus badge badge-" + statusClass + " btn-sm\" data-toggle=\"collapse\" data-target=\"#" + className + "_" + methodName + "_" + test_step_counter + "_body\">" + testStatus + "</span>");
            writer.println("<span style=\"cursor: pointer;\" class=\"MethodBlockName text-primary text-truncate\" style=\"max-width: 550px;\"> " + methodName + "</span>");
            writer.println("</div>");// test step header ends here

            // For display in Reports
            // here body of test step starts
            writer.println("<div id=\"" + className + "_" + methodName + "_" + test_step_counter + "_body\" class=\"collapse\">");

            if (is_failed_test) {

                //For Display in Failures List
                // Header of the test set name and its status
                testFailuresCollection += "<div class=\"my-1 card-text\" data-toggle=\"collapse\" data-target=\"#error_modal_" + className + "_" + methodName + "_" + test_step_counter + "_body\" id=\"error_modal_" + className + "_" + methodName + "_"
                        + test_step_counter + "_header\">" + "<span class=\"badge h6 badge-" + statusClass + " btn-sm\" data-toggle=\"collapse\" data-target=\"#error_modal_" + className + "_" + methodName + "_" + test_step_counter + "_body\">" + testStatus
                        + "</span>" + "<span class=\"text-primary h6 ml-1 text-truncate\" style=\"max-width: 250px;\">" + methodName + "</span>" + "</div>";// test step header ends here

                // For display in Failures List
                // here body of test step starts
                testFailuresCollection += "<div id=\"error_modal_" + className + "_" + methodName + "_" + test_step_counter + "_body\" class=\"collapse\">";

            }

            writeExpandableTestCaseDetails(sortedList.get(counter));

            if (is_failed_test)
                writeExpandableTestCaseDetails_InFailureList(sortedList.get(counter));

            writer.println("</div>"); // here body of test step ends
            if (is_failed_test)
                testFailuresCollection += "</div>"; // here body of test step ends

        }

    }

    class ClassData {
        private int passedTests = 0;
        private int failedTests = 0;
        private int skippedTests = 0;

        private long _startTime = Long.MAX_VALUE;
        private long _endTime;

        private String name;
        private String xmlTestName;
        LinkedHashMap<String, ITestNGMethod> testResults = new LinkedHashMap<>();
        LinkedHashMap<String, MethodData> methodsData = new LinkedHashMap<>();
        private Boolean isDeprecated = false;

        public String getName() {
            return name.substring(name.lastIndexOf(".") + 1);
        }

        public String getFullName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void addMethod(MethodData methodData) {
            methodsData.put(methodData.get_name(), methodData);
            switch (methodData.get_status()) {
                case "PASS":
                    passedTests++;
                    break;
                case "FAILED":
                    failedTests++;
                    break;
                case "SKIPPED":
                    skippedTests++;
                    break;
            }
            if (methodData.get_startTime() < this._startTime)
                this._startTime = methodData.get_startTime();
            if (methodData.get_endTime() > this._endTime)
                this._endTime = methodData.get_endTime();

        }

        public String getXmlTestName() {
            return xmlTestName;
        }

        public void setXmlTestName(String xmlTestName) {
            this.xmlTestName = xmlTestName;
        }

        public int getPassedTestsCount() {
            return passedTests;
        }

        public int getFailedTestsCount() {
            return failedTests;
        }

        public int getSkippedTestsCount() {
            return skippedTests;
        }

        public int getTotal() {
            return passedTests + failedTests + skippedTests;
        }

        public Boolean getIsDeprecated() {
            return isDeprecated;
        }

        public void setIsDeprecated(Boolean deprecated) {
            isDeprecated = deprecated;
        }

        public List<ITestResult> getClassMethodsResults() {
            List<ITestResult> results = new ArrayList<>();
            for (MethodData methodInClass : methodsData.values()) {
                results.add(methodInClass.get_iTestResult());
            }
            return results;
        }

        public long get_duration() {
            return _endTime - _startTime;
        }

        public long get_endTime() {
            return _endTime;
        }

        public long get_startTime() {
            return _startTime;
        }
    }

    class MethodData {
        private String _name;
        private String _status;
        private ITestNGMethod _iTestNGMethod;
        private ITestContext _context;
        private ITestResult _iTestResult;
        private long _startTime;
        private long _endTime;
        private long _duration;


        public ITestNGMethod get_iTestNGMethod() {
            return _iTestNGMethod;
        }

        public void set_iTestNGMethod(ITestNGMethod iTestNGMethod) {
            this._iTestNGMethod = iTestNGMethod;
        }

        public String get_name() {
            return _name;
        }

        public void set_name(String _name) {
            this._name = _name;
        }

        public String get_status() {
            return _status;
        }

        public void set_status(String _status) {
            this._status = _status;
        }

        public ITestContext get_context() {
            return _context;
        }

        public void set_context(ITestContext _context) {
            this._context = _context;
        }

        public ITestResult get_iTestResult() {
            return _iTestResult;
        }

        public void set_iTestResult(ITestResult iTestResult) {
            this._iTestResult = iTestResult;
            this._startTime = this._iTestResult.getStartMillis();
            this._endTime = this._iTestResult.getEndMillis();
            this._duration = this._endTime - this._startTime;
        }

        public long get_startTime() {
            return this._startTime;
        }

        public long get_endTime() {
            return this._endTime;
        }

        public long get_duration() {
            return this._duration;
        }
    }
// class testInXMLResult
// {
// private String name;
// //List<methodResult> methodResults;
//
// public String getName() {
// return name;
// }
//
// public void setName(String name) {
// this.name = name;
// }
// }


    private String getMethodLog(ITestResult testResult) {
        String _output = "";
        //ENV_HOST = testResult.getHost();

        List<String> msgs = Reporter.getOutput(testResult);

        _output += ("<table class=\"table table-hover table-sm\">");

        _output += ("<thead class=\"bg-secondary text-white\">");
        _output += ("<tr class=\"\">");
        _output += getHeaderColumn("Key");
        _output += getHeaderColumn("Value");
        _output += ("</tr>");
        _output += ("</thead>");

        _output += ("<tbody>");
        for (String line : msgs) {
            _output += (line);
        }
        _output += getTestNGStackTrace(testResult);

        _output += ("</tbody>");

        _output += ("</table>");
        return _output;
    }

    private String get_ModalWindow(String title, String content) {
        String _output = "";
        stack_trace_counter++;

        _output += ("<tr class=\"table-light\">");
        _output += ("<td class=\"text-dark font-weight-bold\" >" + title + "</td>");
        _output += ("<td class=\"\" >");
        _output += ("<a href=\"\" class=\"alert-link\" data-toggle=\"modal\" data-target=\"#modal_" + stack_trace_counter + "\">Show</a>");
        _output += ("<div class=\"modal fade\" id=\"modal_" + stack_trace_counter + "\" tabindex=\"-1\" role=\"dialog\">");
        _output += ("<div class=\"modal-dialog\" role=\"document\">");
        _output += ("<div class=\"modal-content\">");
        _output += ("<div class=\"modal-header text-white bg-info\">");
        _output += ("<h5 class=\"modal-title\" id=\"title1\">" + title + "</h5>");
        _output += ("<span class=\"h6 text-light ml-2 mt-2\">(click on the Text or select part of it to copy)</span>");
        _output += ("<button type=\"button\" class=\"close\" data-dismiss=\"modal\" aria-label=\"Close\">");
        _output += ("<span aria-hidden=\"true\">&times;</span>");
        _output += ("</button>");
        _output += ("</div>");
        _output += ("<div onClick=\"copyMePlease(this)\" class=\"modal-body\">");
        _output += content;
        _output += ("</div>");
        _output += ("</div>");
        _output += ("</div>");
        _output += ("</div>");
        _output += ("</td>");
        _output += ("</tr>");
        return _output;
    }

    private String getStackTrace_Modal(Throwable exception) {
        String _output = "";
        stack_trace_counter++;

        _output += ("<tr>");
        _output += ("<td class=\"text-dark font-weight-bold\" >StackTrace :</td>");
        _output += ("<td class=\"\" >");
        _output += ("<a href=\"\" class=\"alert-link\" data-toggle=\"modal\" data-target=\"#modal_" + stack_trace_counter + "\">Show</a>");
        _output += ("<div class=\"modal fade\" id=\"modal_" + stack_trace_counter + "\" tabindex=\"-1\" role=\"dialog\">");
        _output += ("<div class=\"modal-dialog\" role=\"document\">");
        _output += ("<div class=\"modal-content\">");
        _output += ("<div class=\"modal-header text-white bg-info\">");
        _output += ("<h5 class=\"modal-title\" id=\"title1\">StackTrace</h5>");
        _output += ("<span class=\"h6 text-light ml-2 mt-2\">(click on the Text or select part of it to copy)</span>");
        _output += ("<button type=\"button\" class=\"close\" data-dismiss=\"modal\" aria-label=\"Close\">");
        _output += ("<span aria-hidden=\"true\">&times;</span>");
        _output += ("</button>");
        _output += ("</div>");
        _output += ("<div onClick=\"copyMePlease(this)\" class=\"modal-body\">");
        _output += (convertExceptionToString(exception));
        _output += ("</div>");
        _output += ("</div>");
        _output += ("</div>");
        _output += ("</div>");
        _output += ("</td>");
        _output += ("</tr>");
        return _output;
    }

    private String getTestNGStackTrace(ITestResult testResult) {
        String _output = "";
        Throwable exception = testResult.getThrowable();
        boolean hasThrowable = exception != null;
        if (hasThrowable) {
            _output += getStackTrace_Modal(exception);
        }
        return _output;
    }

    private String getHeaderColumn(String label) {
        return ("<th>" + label + "</th>");
    }

    private void getClassesForSuite(ISuite suite) {

// Map<String, ISuiteResult> suiteResults = suite.getResults();
// for (String ddd :suiteResults.keySet()) {
// ISuiteResult sR = suiteResults.get(ddd);
// sR.getTestContext().
// }


    }


    /**
     * Return an ordered list of TestNG TestResult from a given TestNG Test Context.
     *
     * @param testContext TestNG Test Context
     * @return Ordered list of TestNG TestResults
     */
    public List<ITestResult> getTestNGResultsOrderedByExecutionDate(ITestContext testContext) {
        Map<String, IResultMap> results = new LinkedHashMap<String, IResultMap>();

        results.put("passed", testContext.getPassedTests());
        results.put("failed", testContext.getFailedTests());
        results.put("failedBut", testContext.getFailedButWithinSuccessPercentageTests());
        results.put("skipped", testContext.getSkippedTests());

        ResultMap total = new ResultMap();

        addAll(total, results.get("passed"));
        addAll(total, results.get("failed"));
        addAll(total, results.get("failedBut"));
        addAll(total, results.get("skipped"));

        ITestNGMethod[] allMethodsInCtx = testContext.getAllTestMethods();
        for (int i = 0; i < allMethodsInCtx.length; i++) {
            ITestNGMethod methodInCtx = allMethodsInCtx[i];

            Collection<ITestNGMethod> allMethodsFound = total.getAllMethods();
            boolean exists = false;
            for (ITestNGMethod methodFound : allMethodsFound) {
                if (methodInCtx.getTestClass().getName().equals(methodFound.getTestClass().getName()) && methodInCtx.getConstructorOrMethod().getName().equals(methodFound.getConstructorOrMethod().getName())) {
                    exists = true;
                }
            }
            if (!exists) {
                //ITestResult skippedTestResult = new org.testng.internal.TestResult(methodInCtx.getTestClass(), methodInCtx.getInstance(), methodInCtx, null, testContext.getStartDate().getTime(), testContext.getEndDate().getTime(), testContext);
                //skippedTestResult.setStatus(ITestResult.SKIP);
                //total.addResult(skippedTestResult, methodInCtx);
            }
        }

        List<ITestResult> testNGTestResults = new ArrayList<ITestResult>(total.getAllResults());
        Collections.sort(testNGTestResults, EXECUTION_DATE_COMPARATOR);

        return testNGTestResults;
    }

    public void addAll(ResultMap total, IResultMap map) {
        for (ITestResult testResult : map.getAllResults()) {
            //total.addResult(testResult, testResult.getMethod());
        }
    }

    private void generateTestSummaryRerport(ISuiteResult suiteResult) {

        // Ordering the TCs and displaying
        sortAndDisplaySuiteResults(suiteResult);

    }

    private void sortAndDisplaySuiteResults(ISuiteResult suiteResult) {

        String testStatus = "", statusClass = "";
        String className = "", methodName = "";
        boolean is_failed_test = false;

        ITestContext testContext = suiteResult.getTestContext();

        List<ITestResult> sortedList = getTestNGResultsOrderedByExecutionDate(testContext);

        for (int counter = 0; counter < sortedList.size(); counter++) {

            is_failed_test = false;
            test_step_counter++;

            switch (sortedList.get(counter).getStatus()) {
                case 1:
                    statusClass = "success";
                    testStatus = "PASS";
                    break;
                case 2:
                    statusClass = "danger";
                    testStatus = "FAIL";
                    is_failed_test = true;
                    break;
                case 3:
                    statusClass = "secondary";
                    testStatus = "SKIP";
                    break;
            }

            className = sortedList.get(counter).getInstanceName();
            methodName = sortedList.get(counter).getName();

            // For display in Reports
            // Header of the test set name and its status
            writer.println("<div class=\"my-1 card-text\" data-toggle=\"collapse\" data-target=\"#" + className + "_" + methodName + "_" + test_step_counter + "_body\" id=\"" + className + "_" + methodName + "_" + test_step_counter + "_header\">");
            writer.println(
                    "<span style=\"cursor: pointer;\" class=\"MethodBlockStatus badge badge-" + statusClass + " btn-sm\" data-toggle=\"collapse\" data-target=\"#" + className + "_" + methodName + "_" + test_step_counter + "_body\">" + testStatus + "</span>");
            writer.println("<span style=\"cursor: pointer;\" class=\"MethodBlockName text-primary text-truncate\" style=\"max-width: 550px;\"> Class : " + className + " | Method : " + methodName + "</span>");
            writer.println("</div>");// test step header ends here

            // For display in Reports
            // here body of test step starts
            writer.println("<div id=\"" + className + "_" + methodName + "_" + test_step_counter + "_body\" class=\"collapse\">");

            if (is_failed_test) {

                //For Display in Failures List
                // Header of the test set name and its status
                testFailuresCollection += "<div class=\"my-1 card-text\" data-toggle=\"collapse\" data-target=\"#error_modal_" + className + "_" + methodName + "_" + test_step_counter + "_body\" id=\"error_modal_" + className + "_" + methodName + "_"
                        + test_step_counter + "_header\">" + "<span class=\"badge h6 badge-" + statusClass + " btn-sm\" data-toggle=\"collapse\" data-target=\"#error_modal_" + className + "_" + methodName + "_" + test_step_counter + "_body\">" + testStatus
                        + "</span>" + "<span class=\"text-primary h6 ml-1 text-truncate\" style=\"max-width: 250px;\">" + methodName + "</span>" + "</div>";// test step header ends here

                // For display in Failures List
                // here body of test step starts
                testFailuresCollection += "<div id=\"error_modal_" + className + "_" + methodName + "_" + test_step_counter + "_body\" class=\"collapse\">";

            }

            writeExpandableTestCaseDetails(sortedList.get(counter));

            if (is_failed_test)
                writeExpandableTestCaseDetails_InFailureList(sortedList.get(counter));

            writer.println("</div>"); // here body of test step ends
            if (is_failed_test)
                testFailuresCollection += "</div>"; // here body of test step ends

        }

    }

    private void writeExpandableTestCaseDetails(ITestResult testResult) {


        // Creating method log ie. table for REST API details

        writer.println("<div class=\"table-responsive\">");

        writeMethodLog(testResult);
        writer.println("</div>");

    }

    private void writeExpandableTestCaseDetails_InFailureList(ITestResult testResult) {

        // Creating method log ie. table for REST API details

        testFailuresCollection += "<div class=\"table-responsive\">";
        writeMethodLog_inFailureList(testResult);
        testFailuresCollection += "</div>";

    }

    private String printRequestToLog(String request) {
        String forPostman = "";
        writer.println("<table id=\"request_id\" class=\"table table-hover table-sm\">");

        writer.println("<thead class=\"bg-secondary text-white\">");
        writer.println("<tr class=\"\">");
        headerColumn("Key");
        headerColumn("Value");
        writer.println("</tr>");
        writer.println("</thead>");

        Pattern patternRequest = Pattern.compile("^(\\bINFO|\\bERROR|\\bDEBUG)");
        Matcher matcherRequest = patternRequest.matcher(request);
        request = matcherRequest.replaceFirst("");
        String[] lines = request.trim().split(System.lineSeparator());
        writer.print("<tbody>");

        for (int i = 0; i < lines.length; i++) {
            String _line = lines[i];
            Pattern patternURL = Pattern.compile("^(\\bGET|\\bPOST|\\bDELETE)");
            Matcher matcherURL = patternURL.matcher(_line);
            if (matcherURL.lookingAt()) {
                // print URL
                String _reqType = _line.substring(0, _line.indexOf("http")).trim();
                forPostman = "curl -X " + _reqType + " \\" + System.lineSeparator();
                writer.println("<tr class=\"table-info\"><td><b>" + _reqType + "</b></td><td>" + _line.replace(_reqType, "").trim() + "</td></tr>");
                forPostman += "'" + _line.replace(_reqType, "").trim() + "' \\" + "<br>";
            }
            if (_line.startsWith("Headers:")) {
                // print headers
// String _headersAsString = _line.replace("Headers:", "").trim();
// if (_headersAsString.isEmpty())
// continue;
// String[] _headers = (_line.replace("Headers:", "").trim()).split("\n");
                writer.println("<tr class=\"table-light\"><td colspan=\"2\"><b>Headers:<b></td></tr>");
                String[] _headers = request.substring(request.indexOf("Headers:") + 9, request.indexOf("Form:")).split("[\\r\\n]");
                for (String _header : _headers) {
                    String[] _headerDetails = _header.split("=");
                    if (_headerDetails[0].equalsIgnoreCase("Authorization"))
                        writer.println(get_ModalWindow("Authorization", _headerDetails[1]));
                    else
                        writer.println("<tr class=\"table-light\"><td><b>" + _headerDetails[0] + "</b></td><td>" + _headerDetails[1] + "</td></tr>");

                    if (_headerDetails[0].equalsIgnoreCase("Content-Type") && _headerDetails[0].equalsIgnoreCase("URLENC"))
                        forPostman += " -H 'Content-Type: application/x-www-form-urlencoded'" + "<br>";
                    else
                        forPostman += " -H '" + _headerDetails[0] + ":" + _headerDetails[1] + "' \\" + "<br>";
                }

            }
            if (_line.startsWith("Payload:")) {
                // print payload
                String _payload = _line.replace("Payload:", "");
                if (StringUtils.isNullOrEmpty(_payload)) {
                    writer.println(get_ModalWindow("Payload", _payload));
                    forPostman += "-d '" + _payload + "' \\";
                }
            }
            if (_line.startsWith("Form:")) {
                // print form params
                String _formParam = (_line.replace("Form:", "").replace("{", "").replace("}", "")).trim();
                if (StringUtils.isNullOrEmpty(_formParam))
                    continue;

                String formParamsAsString = null;
                Iterator<String> listFormParams = Arrays.asList(_formParam.split(",")).iterator();

                formParamsAsString = "";

                while (listFormParams.hasNext()) {
                    String entry = listFormParams.next();
                    if (listFormParams.hasNext()) {
                        formParamsAsString += entry.trim() + "&";
                    } else {
                        formParamsAsString += entry.trim();
                    }
                }

                writer.println("<tr class=\"table-light\"><td><b>Form:</b></td><td >" + formParamsAsString + "</td></tr>");


                forPostman += "-d '" + formParamsAsString + "' \\";
            }
        }

        writer.println(get_ModalWindow("For Postman(Import -> Paste Raw Text)", forPostman));
// writer.println("</tbody>");
// writer.println("</table>");

        return forPostman;
    }

    private void printResponseToLog(String response) {
// writer.println("<table id=\"response_id\" class=\"table table-hover table-sm\">");

        Pattern patternRequest = Pattern.compile("^(\\bINFO|\\bERROR|\\bDEBUG)");
        Matcher matcherRequest = patternRequest.matcher(response);
        response = matcherRequest.replaceFirst("");
        String[] lines = response.trim().split(System.lineSeparator());

        for (int i = 0; i < lines.length; i++) {
            String _line = lines[i];
            Pattern patternResponseCode = Pattern.compile("(\\d{3})");
            Matcher matcherResponseCode = patternResponseCode.matcher(_line);
            if (matcherResponseCode.matches()) // line contains response
            {
                // print response code
            }
            if (_line.contains("Headers:")) {
                // print headers
                writer.println(get_ModalWindow("Response Headers", _line.replace("Response Headers:", "").replace("Headers:", "").replace("{", "").replace("}", "")));
            }
            if (_line.contains("Body:")) {
                // print body
                writer.println(get_ModalWindow("Response Body", _line.replaceFirst("Response Body:", "").replaceFirst("Body:", "")));
            }
// writer.println("</table>");

        }
    }

    private void printAssertToLog(String response) {
        writer.println("<table id=\"request_id\" class=\"table table-hover table-sm\">");

        Pattern patternAssert = Pattern.compile("^(\\bINFO|\\bERROR|\\bDEBUG).(\\bAssert:)");
        Matcher matcherAssert = patternAssert.matcher(response);
        response = matcherAssert.replaceFirst("");
        String[] lines = response.trim().split("\n");

        if (lines[0].equalsIgnoreCase("PASS"))
            writer.println("<tr class=\"table-success\"><td colspan=\"2\"><span class=\"text-md-left badge badge-pill badge-success\"> TRUE </span> <span class=\"text-md-left text-info font-weight-bold\">" + lines[1] + "</span></td></tr>");
// writer.println("<tr class=\"table-success\"><td colspan=\"2\"><span class=\"text-md-left badge badge-pill badge-success\"> PASS </span><span class=\"text-md-left text-dark font-weight-bold\">"+lines[2]+"</span><span class=\"text-md-left text-dark font-weight-bold\">"+lines[3]+"</span><span class=\"text-md-left text-info font-weight-bold\">"+lines[1]+"</span></td></tr>");
        else
            writer.println("<tr class=\"table-danger\"><td colspan=\"2\"><span class=\"text-md-left badge badge-pill badge-danger\"> FALSE </span> <span class=\"text-md-left text-info font-weight-bold\">" + lines[1] + "</span></td></tr>");


        writer.println("</table>");


// for (int i = 0; i < lines.length; i++) {
// System.out.println(lines[i]);
// }
    }


    //Actual reporting
    private void writeMethodLog(ITestResult testResult) {
        writer.println("<tr class=\"table-light\"><td> <b>Test description: </b></td><td>" + testResult.getMethod().getDescription() + "</td></tr><br>");
        //ENV_HOST = testResult.getHost();

        List<String> msgs = Reporter.getOutput(testResult);
        for (int i = 0; i < msgs.size(); i++) {
            String message = msgs.get(i);
            //String[] lines = message.split(System.lineSeparator());

            // Request RegEx
            Pattern patternRequest = Pattern.compile("^(\\bINFO|\\bERROR|\\bDEBUG).(\\bPOST|\\bGET|\\bDELETE|\\bUPDATE).*");
            Matcher matcherRequest = patternRequest.matcher(message);

            // Response RegEx
            Pattern patternResponse = Pattern.compile("(\\bINFO|\\bERROR|\\bDEBUG).(\\d{3})");
            Matcher matcherResponse = patternResponse.matcher(message);

            // Assert RegEx
            Pattern patternAssert = Pattern.compile("^(\\bINFO|\\bERROR|\\bDEBUG).(\\bAssert).*");
            Matcher matcherAssert = patternAssert.matcher(message);

            //writer.println("<table id=\"request_id\" class=\"table table-hover table-sm\">");


            if (matcherRequest.lookingAt()) // line contains request
            {
                printRequestToLog(message);
            } else if (matcherResponse.lookingAt()) // line contains response
            {
                printResponseToLog(message);
                writer.println("</tbody>");
                writer.println("</table>");
            } else if (matcherAssert.lookingAt()) // line contains request
            {
                printAssertToLog(message);
            } else // simple message
            {
                writer.println("<div class=\"table-light\">" + message + "</div>");
            }
        }

        printTestNGStackTrace(testResult);

        //writer.print("</tbody>");

        //writer.println("</table>");
    }

    private void writeMethodLog_inFailureList(ITestResult testResult) {

        //ENV_HOST = testResult.getHost();

        List<String> msgs = Reporter.getOutput(testResult);
        String responseString = "";
        testFailuresCollection += "<table class=\"table table-hover table-sm\">";
        //testFailuresCollection += "<thead class=\"bg-secondary text-white\"><tr><th>Key</th><th>Value</th></tr></thead>";
        testFailuresCollection += "<tbody>";
        for (String line : msgs) {
            if (line.contains("http://ilde")) {
                //To Print URL
                //testFailuresCollection += line;
            } else if (line.contains("errorType")) {
                responseString = line;
                //testFailuresCollection += msgs
                // .get(msgs.indexOf("<td class=\"text-dark font-weight-bold\" >Response :</td>") + 13);
            }
        }

        if (!responseString.isEmpty()) {
            String row = "<tr><td class=\"font-weight-bold\">Response</td><td class=\"text-danger\"> " + responseString + "</td></tr>";
            testFailuresCollection += row;
        } else {
            String row = "<tr><td class=\"font-weight-bold\">Response</td><td class=\"text-danger\"> NULL </td></tr>";
            testFailuresCollection += row;
        }

        printTestNGStackTrace_InFailureList(testResult);

        testFailuresCollection += "</tbody>";

        testFailuresCollection += "</table>";
    }

    private void printTestNGStackTrace(ITestResult testResult) {
        Throwable exception = testResult.getThrowable();
        boolean hasThrowable = exception != null;
        if (hasThrowable) {
// writer.println("<br>");
            writeStackTrace_ModalInDiv(exception);
        }

    }

    private void printTestNGStackTrace_InFailureList(ITestResult testResult) {
        Throwable exception = testResult.getThrowable();
        boolean hasThrowable = exception != null;
        if (hasThrowable) {
            testFailuresCollection += "<tr><td class=\"font-weight-bold\">Error : </td><td class=\"text-danger\"> " + exception.getMessage() + "</td></tr>";

        }

    }

    private void writeStackTrace_Modal(Throwable exception) {
        stack_trace_counter++;

        writer.println("<tr>");
        writer.println("<td class=\"text-dark font-weight-bold\" >StackTrace :</td>");
        writer.println("<td class=\"\" >");
        writer.println("<a href=\"\" class=\"alert-link\" data-toggle=\"modal\" data-target=\"#modal_" + stack_trace_counter + "\">Show</a>");
        writer.println("<div class=\"modal fade\" id=\"modal_" + stack_trace_counter + "\" tabindex=\"-1\" role=\"dialog\">");
        writer.println("<div class=\"modal-dialog\" role=\"document\">");
        writer.println("<div class=\"modal-content\">");
        writer.println("<div class=\"modal-header text-white bg-info\">");
        writer.println("<h5 class=\"modal-title\" id=\"title1\">StackTrace</h5>");
        writer.println("<span class=\"h6 text-light ml-2 mt-2\">(click on the Text or select part of it to copy)</span>");
        writer.println("<button type=\"button\" class=\"close\" data-dismiss=\"modal\" aria-label=\"Close\">");
        writer.println("<span aria-hidden=\"true\">&times;</span>");
        writer.println("</button>");
        writer.println("</div>");
        writer.println("<div onClick=\"copyMePlease(this)\" class=\"modal-body\">");
        writer.println(convertExceptionToString(exception));
        writer.println("</div>");
        writer.println("</div>");
        writer.println("</div>");
        writer.println("</div>");
        writer.println("</td>");
        writer.println("</tr>");
    }

    private void writeStackTrace_ModalInDiv(Throwable exception) {
        stack_trace_counter++;

// writer.println("<tr>");
        writer.println("<div class=\"text-dark\" style=\"text-align:center;\">StackTrace :</td>");
// writer.println("<td class=\"\" >");
        writer.println("<a href=\"\" class=\"alert-link\" data-toggle=\"modal\" data-target=\"#modal_" + stack_trace_counter + "\">Show</a>");
        writer.println("<div class=\"modal fade\" id=\"modal_" + stack_trace_counter + "\" tabindex=\"-1\" role=\"dialog\">");
        writer.println("<div class=\"modal-dialog\" role=\"document\">");
        writer.println("<div class=\"modal-content\">");
        writer.println("<div class=\"modal-header text-white bg-info\">");
        writer.println("<h5 class=\"modal-title\" id=\"title1\">StackTrace</h5>");
        writer.println("<span class=\"h6 text-light ml-2 mt-2\">(click on the Text or select part of it to copy)</span>");
        writer.println("<button type=\"button\" class=\"close\" data-dismiss=\"modal\" aria-label=\"Close\">");
        writer.println("<span aria-hidden=\"true\">&times;</span>");
        writer.println("</button>");
        writer.println("</div>");
        writer.println("<div style=\"text-align:left;\" onClick=\"copyMePlease(this)\" class=\"modal-body\">");
        writer.println(convertExceptionToString(exception));
        writer.println("</div>");
        writer.println("</div>");
        writer.println("</div>");
        writer.println("</div>");
        writer.println("</div>");
// writer.println("</td>");
// writer.println("</tr>");
    }


    private void writeFixedNavbar() {
        writer.println("<div class=\"fixed-top\">");
        writer.println("<nav class=\"navbar navbar-expand-lg navbar-dark bg-dark\">");
        writer.println("<a class=\"navbar-brand ml-3\" href=\"#Home\">Test Execution report</a>");
        writer.println("<button class=\"navbar-toggler\" type=\"button\" data-toggle=\"collapse\" data-target=\"#navbarSupportedContent\" aria-controls=\"navbarSupportedContent\" aria-expanded=\"false\" aria-label=\"Toggle navigation\">");
        writer.println("<span class=\"navbar-toggler-icon\"></span>");
        writer.println("</button>");
        writer.println("");
        writer.println("<div class=\"collapse navbar-collapse\" id=\"navbarSupportedContent\">");
        writer.println("<ul class=\"navbar-nav mr-auto\">");
        writer.println("<li class=\"nav-item active\">");
        writer.println("<a class=\"nav-link\" href=\"#\" onclick=\"loadFeatures();\">Home <span class=\"sr-only\">(current)</span></a>");
        writer.println("</li>");
        writer.println("");
        writer.println("<li class=\"nav-item\">");
        writer.println("<a class=\"nav-link\" href=\"#\" onclick=\"showFeatures();\">Features</a>");
        writer.println("</li>");
        writer.println("");
        writer.println("<li class=\"nav-item dropdown\">");
        writer.println("<a class=\"nav-link dropdown-toggle\" href=\"#\" id=\"statusDropdown\" role=\"button\" data-toggle=\"dropdown\" aria-haspopup=\"true\" aria-expanded=\"false\"> Status </a>");
        writer.println("<div class=\"dropdown-menu\" aria-labelledby=\"statusDropdown\" id=\"status_dropdown_menu\">");
        writer.println("<a class=\"dropdown-item text-success\" href=\"#\" onClick=\"showPassedFlows()\">SUCCESS</a>");
        writer.println("<a class=\"dropdown-item text-danger\" href=\"#\" onClick=\"showFailedFlows()\">FAILURE</a>");
        writer.println("<a class=\"dropdown-item text-warning\" href=\"#\" onClick=\"showSkippedFlows()\">SKIPPED</a>");
        writer.println("<a class=\"dropdown-item text-dark\" href=\"#\" onClick=\"showAllFlowsWithFilter()\">ALL</a>");
        writer.println("</div>");
        writer.println("</li>");
        writer.println("");
/*
 writer.println("<li class=\"nav-item\">");
 writer.println("<a class=\"nav-link\" href=\"#\" onclick=\"showEpics();\">Epic Mapping</a>");
 writer.println("</li>");
 writer.println("");
*/
/*
 writer.println("<li class=\"nav-item\">");
 writer.println("<a class=\"nav-link\" href=\"#\" onclick=\"showSuites();\">Suites</a>");
 writer.println("</li>");
 writer.println("");
*/
/*
 writer.println("<li class=\"nav-item\">");
 writer.println("<a class=\"nav-link text-danger\" href=\"#\" onclick=\"showFailures();\">Failures</a>");
 writer.println("</li>");
 writer.println("");
*/

/*
 writer.println("<li class=\"nav-item\">");
 writer.println("<a class=\"nav-link\" href=\"#\" onclick=\"showAPICoverage();\">API Coverage</a>");
 writer.println("</li>");
 writer.println("");

 writer.println("<li class=\"nav-item\">");
 writer.println("<a class=\"nav-link\" href=\"#\" onclick=\"showPodRestarts();\">POD Restarts</a>");
 writer.println("</li>");
 writer.println("");
*/
        writer.println("</ul>");
        writer.println("<span id=\"searchnote\" class=\"text-white mr-4\"> 0 Flows. </span>");
        writer.println("<form class=\"form-inline my-2 my-lg-0\">");
        writer.println("<input type=\"text\" class=\"form-control mr-sm-2\" placeholder=\"Search Flows\" aria-label=\"Search Flows\" onKeyUp=\"searchByFlowName(this)\">");
        writer.println("</form>");
        writer.println("</div>");
        writer.println("");
        writer.println("</nav>");
        writer.println("</div>");
    }

    private void writeReportTitle(String title) {
        writer.println("<div class=\"jumbotron bg-dark pb-1 m-0\" id=\"Home\">");

        writer.println("<div class =\"lead text-warning\" id=\"suite_summary_replace\">");
        writer.println("<div class=\"container-fluid\">");
        writer.println("<div class=\"row\">");
        writer.println("<div class=\"col\">");
        writer.println("<p class=\"h1 text-white font-weight-light\" style=\"font-size: 50px;\">" + title + " <sub class=\"lead ml-2 h6\">" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")) + "</sub></p>");
        //writer.println("<p class=\"h1 text-white font-weight-light\" style=\"font-size: 50px;\">" + title + " <sub class=\"lead ml-2 h6\">2019/06/18 18:25:54</sub></p>");
        writer.println("</div>");
        writer.println("</div>");
        writer.println("<div class=\"row text-center\">");
        writer.println("<div class=\"col\">");
        writer.println("<div class=\"row pt-4\">");
        writer.println("<div class=\"col pt-4\" style=\"text-shadow: 1px 1px 1px #000000;\">");
        writer.println("<span class=\"font-weight-bolder h1 text-success\" id=\"passrate\">00.00%</span><br>");
        writer.println("<span class=\"text-light font-weight-normal h5\">Pass rate</span>");
        writer.println("</div>");
        writer.println("<div class=\"col pt-4\" style=\"text-shadow: 1px 1px 1px #000000;\">");
        writer.println("<span class=\"font-weight-bolder h1 text-info\" id=\"totalflows\">0</span><br>");
        writer.println("<span class=\"text-light font-weight-normal h5\">Flows</span>");
        writer.println("</div>");
        writer.println("<div class=\"col pt-4\" style=\"text-shadow: 1px 1px 1px #000000;\">");
        writer.println("<span class=\"font-weight-bolder h1 text-primary\" id=\"totaltc\">42</span><br>");
        writer.println("<span class=\"text-light font-weight-normal h5\">Tests</span>");
        writer.println("</div>");
        writer.println("<div class=\"col pt-4\" style=\"text-shadow: 1px 1px 1px #000000;\">");
        writer.println("<span class=\"font-weight-bolder h1 text-secondary\" id=\"totaltime\">00:01:09.848</span><br>");
        writer.println("<span class=\"text-light font-weight-normal h5\">Time</span>");
        writer.println("</div>");
        writer.println("<div id=\"passedtc\" hidden></div>");
        writer.println("<div id=\"failedtc\" hidden></div>");
        writer.println("<div id=\"skippedtc\" hidden></div>");
        writer.println("</div>");
        writer.println("</div>");
        writer.println("<div class=\"col\">");
        writer.println("<div class=\"container-fluid\" id=\"piechart1\"></div>");
        writer.println("</div>");
        writer.println("</div>");
        writer.println("</div>");
        writer.println("</div>");
        writer.println("</div>");

    }

    private void writeFeaturesBox() {
/*
 writer.println("<div id=\"FeatureView\">");
 writer.println("<div class=\"card text-secondary border-secondary mx-2 my-2 pt-1 pb-2\">");
 writer.println("<div class=\"container-fluid\"> ");
 writer.println("<div class=\"h2 text-center font-weight-light pt-2\" style=\"text-shadow: 1px 1px 3px #5C5858;\">Features Overview</div>");
 writer.println("<hr/>");
 writer.println("<div class=\"row\" id=\"FeatureRow\"> ");
 writer.println("</div>");
 writer.println("</div>");
 writer.println("</div>");
 writer.println("</div>");
 */
    }


    private void writeAPICoverageSummary() {

 /*writer.println("<div id=\"APICoverage\">");
 writer.println("<div class=\"card text-secondary border-secondary mx-2 my-2 pt-1 pb-2\">");
 writer.println("<div class=\"container-fluid\">");
 writer.println("<div class=\"h2 text-center font-weight-light pt-2 text-secondary\" style=\"text-shadow: 1px 1px 3px #5C5858;\">API Coverage</div>");
 writer.println("<hr/>");
 writer.println("");
 writer.println("<div id=\"FlowWiseCoverage\">");
 writer.println("<div class=\"h5 text-left font-weight-light pt-2 text-dark\" >Flow-wise Coverage</div>");
 writer.println("<hr>");
 writer.println("");
 */
        writer.println("<div class=\"dropdown\">");
        writer.println("<button class=\"btn btn-sm btn-secondary dropdown-toggle\" type=\"button\" id=\"dropdownMenuSuites\" data-toggle=\"dropdown\" aria-haspopup=\"true\" aria-expanded=\"false\">");
        writer.println("Select Suite");
        writer.println("</button>");
        writer.println("<div id=\"SuiteDropDownList\" class=\"dropdown-menu\" aria-labelledby=\"dropdownMenuSuites\">");
        writer.println("<a class=\"dropdown-item\" href=\"#\" onclick=\"showSuiteWiseCoverage(this)\">Action</a>");
        writer.println("</div>");
        writer.println("</div>");
        writer.println("<div class=\"my-2 mr-2 p-1\">");
        writer.println("<span class=\"h6 font-weight-bold\">Selected Suite : </span><span class=\"h6 font-weight-light\" id=\"SelectedSuiteName\">No Suite</span>");
        writer.println("</div>");
        writer.println("</div>");
        writer.println("");
        writer.println("<hr/>");
        writer.println("<div class=\"\">");
        writer.println("<table class=\"table\">");
        writer.println("<thead>");
        writer.println("<tr class=\"text-dark\">");
        writer.println("<th class=\"w-5\" scope=\"col\">No.</th>");
        writer.println("<th class=\"w-20 text-wrap\" scope=\"col\">Class</th>");
        writer.println("<th class=\"w-15 text-wrap\" scope=\"col\">Method</th>");
        writer.println("<th class=\"w-10\" scope=\"col\">occurrence</th>");
        writer.println("<th class=\"w-50 text-wrap\" scope=\"col\">API & Suites</th>");
        writer.println("</tr>");
        writer.println("</thead>");
        writer.println("<tbody id =\"APICoverageTableBody\" class=\"\">");
        writer.println("</tbody>");
        writer.println("</table>");
        writer.println("</div>");
        writer.println("</div>");
        writer.println("</div>");
        writer.println("</div>");

    }

    private void headerColumn(String label) {
        writer.print("<th scope=\"col\">" + label + "</th>");
    }

    /**
     * Starts HTML stream
     */
    private void startHtml(PrintWriter out) {
        out.println("<!DOCTYPE html>");
        out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
        out.println("<head>");
        out.println("<title>TestNG Report</title>");
        out.println("<meta charset=\"utf-8\">");
        out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        out.println("<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css\">");
        out.println("<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js\"></script>");
        out.println("<script src=\"https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.12.9/umd/popper.min.js\"></script>");
        out.println("<script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js\"></script>");
        out.println("<style type=\"text/css\"> ");
        out.println("::selection { font-weight: bold; background: rgba(114,110,109,0.4); color:#5C5858; text-shadow: 1px 1px 1px #FFFFFF; }");
        out.println("table { font-size: 14px; }");
        out.println(".modal { font-size: 12px; }");
        out.println("</style>");
        writePieChartJavaScript();
        writeSearchBarJavaScript();
        out.println("</head>");
        out.println("<body class=\"p-0 m-0\" onLoad=\"loadFeatures()\">");

    }

    /**
     * Used to define hidden elements in HTML to use as variable
     */
    private void defineHiddenDiv() {

        writer.println("<span id=\"feature\" hidden></span>");

    }

    private void writeCopyToClipboardJavaScript() {

        writer.println("<script type=\"text/javascript\">");
        writer.println("function getSelectedText(element)");
        writer.println("{");
        writer.println("var txt = '';");
        writer.println("if (window.getSelection)");
        writer.println("{");
        writer.println("txt = window.getSelection();");
        writer.println("}");
        writer.println("else if (document.getSelection)");
        writer.println("{");
        writer.println("txt = document.getSelection();");
        writer.println("}");
        writer.println("else if (document.selection)");
        writer.println("{");
        writer.println("txt = document.selection.createRange().text;");
        writer.println("}");
        writer.println("return txt;");
        writer.println("}");

        writer.println("function copyMePlease(element) {");
        writer.println("element.focus();");
        writer.println("var text = getSelectedText(element);");
        writer.println("if(text == '')");
        writer.println("{");
        writer.println("selectElementText(element);");
        writer.println("}");
        writer.println("document.execCommand('copy');");
        writer.println("deselect();");
        writer.println("} ");

        writer.println("function selectElementText(el){");
        writer.println("var range = document.createRange() // create new range object");
        writer.println("range.selectNodeContents(el) // set range to encompass desired element text");
        writer.println("var selection = window.getSelection() // get Selection object from currently user selected text");
        writer.println("selection.removeAllRanges() // unselect any user selected text (if any)");
        writer.println("selection.addRange(range) // add range to Selection object to select it");
        writer.println("} ");

        writer.println("function deselect(){");
        writer.println("var selection = window.getSelection() // get Selection object from currently user selected text");
        writer.println("selection.removeAllRanges() // unselect any user selected text (if any)");
        writer.println("}");
        writer.println("</script>");

    }

    private void writePieChartJavaScript() {

        writer.println("<script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>");
        writer.println("<script type=\"text/javascript\">");

        writer.println("$(document).ready(function(){");
        writer.println("$(\"body\").tooltip({ selector: '[data-toggle=tooltip]' });");
        writer.println("});");
        writer.println("");

        writer.println("function createCustomPassFail(title, count, percent) {");
        writer.println("return '<div class=\"p-2 bg-secondary text-dark\" style=\"text-shadow: 1px 1px 1px #5C5858;\">'+");
        writer.println("'<span class=\"font-weight-bolder h1\">'+count+' </span><br>'+");
        writer.println("'<span class=\"text-light font-weight-normal\">'+percent+'% </span><br>'+");
        writer.println("'<span class=\"text-light font-weight-normal h6\">'+title+'</span>'+");
        writer.println("'</div>';");
        writer.println("}");
        writer.println("");

        writer.println("google.charts.load('current', {'packages':['corechart']});");
        writer.println("google.charts.setOnLoadCallback(drawChart);");
        writer.println("function drawChart() {");

        writer.println("var passed = parseInt(document.getElementById(\"passedtc\").innerHTML);");
        writer.println("var failed = parseInt(document.getElementById(\"failedtc\").innerHTML);");
        writer.println("var skipped = parseInt(document.getElementById(\"skippedtc\").innerHTML);");
        writer.println("var total = parseInt(document.getElementById(\"totaltc\").innerHTML);");

        writer.println("var dataTable = new google.visualization.DataTable();");
        writer.println("dataTable.addColumn('string', 'Task');");
        writer.println("dataTable.addColumn('number', 'Total Number Of TestCases');");
        writer.println("// A column for custom tooltip content");
        writer.println("dataTable.addColumn({'type': 'string', 'role': 'tooltip', 'p': { 'html': true}});");
        writer.println("dataTable.addRows([");
        writer.println("['Failed',failed,createCustomPassFail('Failed',failed,parseFloat(eval(\"failed*100/total\")).toFixed(2))],");
        writer.println("['Skipped',skipped,createCustomPassFail('Skipped',skipped,parseFloat(eval(\"skipped*100/total\")).toFixed(2))],");
        writer.println("['Passed',passed,createCustomPassFail('Passed',passed,parseFloat(eval(\"passed*100/total\")).toFixed(2))]");
        writer.println("]);");

        writer.println("var options = { ");
        writer.println("height: 200,");
        writer.println("width: 300,");
        writer.println("is3D : true,");
        writer.println("colors: ['#dc3545','#ffc107','#28a745'],");
        writer.println("focusTarget: 'category',");
        writer.println("legend: 'none',");
        writer.println("backgroundColor: 'transparent', ");
        writer.println("tooltip: { isHtml: true }");
        writer.println("}; ");
        writer.println("var chart = new google.visualization.PieChart(document.getElementById('piechart1')); ");
        writer.println("chart.draw(dataTable, options);");
        writer.println("}");
        writer.println("</script>");

    }

    private void writeSearchBarJavaScript() {

        // Function to search all flows by Text
        writer.println("<script type=\"text/javascript\">");
        writer.println("function searchByFlowName(element) {");
        writer.println("var counter=0;");
        writer.println("searchByFeaturesFromHiddenElement();");
        writer.println("const searchtext = element.value.toLowerCase();");
        writer.println("const flows = document.getElementsByClassName(\"card-header text-light bg-info mb-1 font-weight-bold\")");
        writer.println("Array.from(flows).forEach(function(flow){");
        writer.println("const title = flow.getElementsByTagName(\"span\")[2].innerHTML.toLowerCase();");
        writer.println("if(flow.parentElement.style.display != 'none') {");
        writer.println("if(title.indexOf(searchtext)!= -1) {");
        writer.println("counter++;");
        writer.println("flow.parentElement.style.display = 'block';");
        writer.println("} else {");
        writer.println("flow.parentElement.style.display = 'none';");
        writer.println("}");
        writer.println("}");
        writer.println("});");
        writer.println("document.getElementById(\"searchnote\").innerHTML = counter + \" Flows loaded.\";");
        writer.println("updateSummary();");
        writer.println("}");

        // Function to search Flows by Feature
        writer.println("function searchByFeatures(element) {");
        writer.println("var counter = 0;");
        writer.println("const searchtext = element.innerHTML.toLowerCase();");
        writer.println("const flows = document.getElementsByClassName(\"card-header text-light bg-info mb-1 font-weight-bold\")");
        writer.println("Array.from(flows).forEach(function(flow){");
        writer.println("const title = flow.getElementsByTagName(\"span\")[1].innerHTML.toLowerCase();");
        writer.println("if(title.indexOf(searchtext)!= -1) {");
        writer.println("counter++;");
        writer.println("flow.parentElement.style.display = 'block';");
        writer.println("} else {");
        writer.println("flow.parentElement.style.display = 'none';");
        writer.println("}");
        writer.println("});");
        writer.println("document.getElementById(\"searchnote\").innerHTML = counter + \" Flows loaded.\";");
        writer.println("document.getElementById(\"feature\").innerHTML = searchtext;");
        writer.println("updateSummary();");
        writer.println("}");

        // Function to search features by already selected feature
        writer.println("function searchByFeaturesFromHiddenElement(searchtext) {");
        writer.println("var counter = 0;");
        writer.println("const all = \"all\";");
        writer.println("const searchhiddentext = document.getElementById(\"feature\").innerHTML.toLowerCase();");
        writer.println("const flows = document.getElementsByClassName(\"card-header text-light bg-info mb-1 font-weight-bold\")");
        writer.println("Array.from(flows).forEach(function(flow){");
        writer.println("const title = flow.getElementsByTagName(\"span\")[1].innerHTML.toLowerCase();");

        writer.println("if(searchhiddentext === all) {");
        writer.println("counter++;");
        writer.println("flow.parentElement.style.display = 'block';");
        writer.println("} else if(title === searchhiddentext) {");
        writer.println("counter++;");
        writer.println("flow.parentElement.style.display = 'block';");
        writer.println("} else {");
        writer.println("flow.parentElement.style.display = 'none';");
        writer.println("}");
        writer.println("});");
        writer.println("document.getElementById(\"searchnote\").innerHTML = counter + \" Flows loaded.\";");
        writer.println("updateSummary();");
        writer.println("}");

        // Function to show flows by epic ID
        writer.println("");
        writer.println("function searchByEpicId(element) {");
        writer.println("var counter = 0;");
        writer.println("const searchtext = element.id.toLowerCase();");
        writer.println("const flows = document.getElementsByClassName(\"SuiteBlockHeader\")");
        writer.println("Array.from(flows).forEach(function(flow){");
        writer.println("const epic = flow.getElementsByTagName(\"span\")[2].innerHTML.toLowerCase();");
        writer.println("console.log(epic+':'+searchtext);");
        writer.println("console.log(epic.includes(searchtext));");
        writer.println("if(epic.includes(searchtext)) {");
        writer.println("counter++;");
        writer.println("flow.parentElement.style.display = 'block';");
        writer.println("} else {");
        writer.println("flow.parentElement.style.display = 'none';");
        writer.println("}");
        writer.println("});");
        writer.println("document.getElementById(\"searchnote\").innerHTML = counter + \" Flows loaded.\";");
        writer.println("updateSummary();");
        writer.println("}");

        // Function to show passed flows
        writer.println("function showPassedFlows() {");
        writer.println("var counter =0;");
        writer.println("document.getElementById(\"FeatureView\").style.display = 'none';");
        writer.println("document.getElementById(\"EpicMapping\").style.display = 'none';");
        writer.println("document.getElementById(\"PodsRestarts\").style.display = 'none';");
        writer.println("document.getElementById(\"SuitesList\").style.display = 'block';");
        writer.println("const searchtext = \"success\";");
        writer.println("searchByFeaturesFromHiddenElement();");
        writer.println("const flows = document.getElementsByClassName(\"card-header text-light bg-info mb-1 font-weight-bold\")");
        writer.println("Array.from(flows).forEach(function(flow){");
        writer.println("const title = flow.getElementsByTagName(\"span\")[0].innerHTML.toLowerCase();");
        writer.println("if(flow.parentElement.style.display != 'none') {");
        writer.println("if(title === searchtext) {");
        writer.println("counter++;");
        writer.println("flow.parentElement.style.display = 'block';");
        writer.println("} else {");
        writer.println("flow.parentElement.style.display = 'none';");
        writer.println("}");
        writer.println("}");
        writer.println("});");
        writer.println("document.getElementById(\"searchnote\").innerHTML = counter + \" Flows loaded.\";");
        writer.println("updateSummary();");
        writer.println("}");

        writer.println("function showFailedFlows() {");
        writer.println("var counter =0;");
        writer.println("document.getElementById(\"FeatureView\").style.display = 'none';");
        writer.println("document.getElementById(\"EpicMapping\").style.display = 'none';");
        writer.println("document.getElementById(\"PodsRestarts\").style.display = 'none';");
        writer.println("document.getElementById(\"SuitesList\").style.display = 'block';");
        writer.println("const searchtext = \"failure\";");
        writer.println("searchByFeaturesFromHiddenElement();");
        writer.println("const flows = document.getElementsByClassName(\"card-header text-light bg-info mb-1 font-weight-bold\")");
        writer.println("Array.from(flows).forEach(function(flow){");
        writer.println("const title = flow.getElementsByTagName(\"span\")[0].innerHTML.toLowerCase();");
        writer.println("if(flow.parentElement.style.display != 'none') {");
        writer.println("if(title === searchtext) {");
        writer.println("counter++;");
        writer.println("flow.parentElement.style.display = 'block';");
        writer.println("} else {");
        writer.println("flow.parentElement.style.display = 'none';");
        writer.println("}");
        writer.println("}");
        writer.println("});");
        writer.println("document.getElementById(\"searchnote\").innerHTML = counter + \" Flows loaded.\";");
        writer.println("updateSummary();");
        writer.println("}");

        writer.println("function showSkippedFlows() {");
        writer.println("var counter = 0;");
        writer.println("document.getElementById(\"FeatureView\").style.display = 'none';");
        writer.println("document.getElementById(\"EpicMapping\").style.display = 'none';");
        writer.println("document.getElementById(\"PodsRestarts\").style.display = 'none';");
        writer.println("document.getElementById(\"SuitesList\").style.display = 'block';");
        writer.println("const searchtext = \"warning\";");
        writer.println("searchByFeaturesFromHiddenElement();");
        writer.println("const flows = document.getElementsByClassName(\"card-header text-light bg-info mb-1 font-weight-bold\")");
        writer.println("Array.from(flows).forEach(function(flow){");
        writer.println("const title = flow.getElementsByTagName(\"span\")[0].innerHTML.toLowerCase();");
        writer.println("if(flow.parentElement.style.display != 'none') {");
        writer.println("if(title === searchtext) {");
        writer.println("counter++;");
        writer.println("flow.parentElement.style.display = 'block';");
        writer.println("} else {");
        writer.println("flow.parentElement.style.display = 'none';");
        writer.println("}");
        writer.println("}");
        writer.println("});");
        writer.println("document.getElementById(\"searchnote\").innerHTML = counter + \" Flows loaded.\";");
        writer.println("updateSummary();");
        writer.println("}");

        // Function to display all Flows
        writer.println("function showAllFlows() {");
        writer.println("var counter = 0 ;");
        writer.println("document.getElementById(\"FeatureView\").style.display = 'none';");
        writer.println("document.getElementById(\"EpicMapping\").style.display = 'none';");
        writer.println("document.getElementById(\"PodsRestarts\").style.display = 'none';");
        writer.println("document.getElementById(\"SuitesList\").style.display = 'block';");

        writer.println("const flows = document.getElementsByClassName(\"card-header text-light bg-info mb-1 font-weight-bold\")");
        writer.println("Array.from(flows).forEach(function(flow){");
        writer.println("const title = flow.getElementsByTagName(\"span\")[0].innerHTML.toLowerCase();");
        writer.println("counter++;");
        writer.println("flow.parentElement.style.display = 'block';");
        writer.println("});");
        writer.println("document.getElementById(\"searchnote\").innerHTML = counter + \" Flows loaded.\";");
        writer.println("}");

        // Function to display all methods as per filter selected
        writer.println("function showAllFlowsWithFilter() {");
        writer.println("var counter = 0 ;");
        writer.println("document.getElementById(\"FeatureView\").style.display = 'none';");
        writer.println("document.getElementById(\"EpicMapping\").style.display = 'none';");
        writer.println("document.getElementById(\"PodsRestarts\").style.display = 'none';");
        writer.println("document.getElementById(\"SuitesList\").style.display = 'block';");
        writer.println("searchByFeaturesFromHiddenElement();");
        writer.println("const flows = document.getElementsByClassName(\"card-header text-light bg-info mb-1 font-weight-bold\")");
        writer.println("Array.from(flows).forEach(function(flow){");
        writer.println("const title = flow.getElementsByTagName(\"span\")[0].innerHTML.toLowerCase();");
        writer.println("if(flow.parentElement.style.display != 'none') {");
        writer.println("counter++;");
        writer.println("flow.parentElement.style.display = 'block';");
        writer.println("}");
        writer.println("});");
        writer.println("document.getElementById(\"searchnote\").innerHTML = counter + \" Flows loaded.\";");
        writer.println("}");

        // Function to load all features at runtime (called onLoad() method on body)
        writer.println("function loadFeatures() {");
        writer.println("displayFlowCount();");
        writer.println("updateSummary();");
        writer.println("summarizeFeatures();");
        writer.println("showEverything();");
        writer.println("}");

        // Function to display count of loaded flows
        writer.println("function displayFlowCount() {");
        writer.println("// counting flows");
        writer.println("var counter =0;");
        writer.println("const flows = document.getElementsByClassName(\"card-header text-light bg-info mb-1 font-weight-bold\")");
        writer.println("Array.from(flows).forEach(function(flow){");
        writer.println("flow.parentElement.style.display = 'block';");
        writer.println("counter++;");
        writer.println("});");
        writer.println("document.getElementById(\"searchnote\").innerHTML = counter + \" Flows loaded.\";");
        writer.println("}");

        // Function to update summary of passrate in Jumbotron
        writer.println("function updateSummary() {");
        writer.println("var counter = 0;");
        writer.println("var visibleFlows = 0;");
        writer.println("var hiddenFlows = 0;");
        writer.println("var passed = 0;");
        writer.println("var failed = 0;");
        writer.println("var skipped = 0;");
        writer.println("const flows = document.getElementsByClassName(\"card-header text-light bg-info mb-1 font-weight-bold\")");
        writer.println("Array.from(flows).forEach(function(flow){");
        writer.println("if(flow.parentElement.style.display == 'block') {");
        writer.println("var thisPassed = parseInt(flow.getElementsByClassName(\"badge badge-success mx-3\")[0].innerHTML);");
        writer.println("passed = eval(\"passed + thisPassed\");");
        writer.println("var thisFailed = parseInt(flow.getElementsByClassName(\"badge badge-danger mx-3\")[0].innerHTML);");
        writer.println("failed = eval(\"failed + thisFailed\");");
        writer.println("var thisSkipped = parseInt(flow.getElementsByClassName(\"badge badge-secondary mx-3\")[0].innerHTML);");
        writer.println("skipped = eval(\"skipped + thisSkipped\");");
        writer.println("visibleFlows++");
        writer.println("counter++;");
        writer.println("} else {");
        writer.println("hiddenFlows++;");
        writer.println("}");
        writer.println("});");
        writer.println("total = eval(\"passed+failed+skipped\");");
        writer.println("passrate = parseFloat(eval(\"passed*100/total\")).toFixed(2);");
        writer.println("// updating data in summary");
        writer.println("document.getElementById(\"passrate\").innerHTML = passrate+\"% \";");
        writer.println("document.getElementById(\"totaltc\").innerHTML = total;");
        writer.println("document.getElementById(\"passedtc\").innerHTML = passed;");
        writer.println("document.getElementById(\"failedtc\").innerHTML = failed;");
        writer.println("document.getElementById(\"skippedtc\").innerHTML = skipped;");
        writer.println("document.getElementById(\"totalflows\").innerHTML = counter;");
        // TODO for time calculation - currently fixed as total time
        writer.println("var totalruntime = document.getElementById(\"totalruntime\").innerHTML;");
        writer.println("document.getElementById(\"totaltime\").innerHTML = totalruntime;");

        writer.println("// Redraw Pie Chart");// Redraw Pie Chart
        writer.println("google.charts.setOnLoadCallback(drawChart);");

        writer.println("");
        writer.println("}");

        // Summarize method analyises and writes features in form of blocks
        writer.println("function summarizeFeatures() {");
        writer.println("//Traversing features in each suite and storing unique names in array ");
        writer.println("var featuresArray = [];");
        writer.println("const features = document.getElementsByClassName(\"btn btn-light text-primary border-primary btn-sm\")");
        writer.println("Array.from(features).forEach(function(feature){");
        writer.println("const featureName = feature.innerHTML;");
        writer.println("if(featuresArray.indexOf(featureName) == -1) {");
        writer.println("featuresArray.push(featureName);");
        writer.println("}");
        writer.println("});");
        writer.println("");
        writer.println("// Emptying feature box");
        writer.println("var featureRow = document.getElementById(\"FeatureRow\");");
        writer.println("featureRow.innerHTML = \"\";");
        writer.println("// Traversing Each Feature");
        writer.println("featuresArray.forEach(function(data) {");
        writer.println("// fetching data and calculating passrate");
        writer.println("var passed = 0;");
        writer.println("var failed = 0;");
        writer.println("var skipped = 0;");
        writer.println("");
        writer.println("const flows = document.getElementsByClassName(\"card-header text-light bg-info mb-1 font-weight-bold\")");
        writer.println("Array.from(flows).forEach(function(flow){");
        writer.println("const featureName = flow.getElementsByTagName(\"span\")[1].innerHTML.toLowerCase();");
        writer.println("if(featureName === data.toLowerCase()) {");
        writer.println("var thisPassed = parseInt(flow.getElementsByClassName(\"badge badge-success mx-3\")[0].innerHTML);");
        writer.println("passed = eval(\"passed + thisPassed\");");
        writer.println("var thisFailed = parseInt(flow.getElementsByClassName(\"badge badge-danger mx-3\")[0].innerHTML);");
        writer.println("failed = eval(\"failed + thisFailed\");");
        writer.println("var thisSkipped = parseInt(flow.getElementsByClassName(\"badge badge-secondary mx-3\")[0].innerHTML);");
        writer.println("skipped = eval(\"skipped + thisSkipped\");");
        writer.println("}");
        writer.println("});");
        writer.println("");
        writer.println("");
        writer.println("var total = eval(\"passed+failed+skipped\");");
        writer.println("var passrateintop = parseFloat(eval(\"passed*100/total\")).toFixed(2);");
        writer.println("");
        writer.println("var passrate = Math.round(eval(\"passed*100/total\"));");
        writer.println("var failrate = Math.round(eval(\"failed*100/total\"));");
        writer.println("var skiprate = Math.round(eval(\"skipped*100/total\"));");
        writer.println("");
        writer.println("// Calculating skipped = 100 - (passed+failed) to have passrate bar aligned in one row - UI fix");
        writer.println("failrate = eval(\"100-(passrate+skiprate)\")");
        writer.println("");
        writer.println("");
        writer.println("// Putting Data in Row");
        writer.println("var featureRow = document.getElementById(\"FeatureRow\");");
        writer.println("");
        writer.println("var cardPlaceHolder = '<div class=\"col m-2 text-center\">'+");
        writer.println("'<div class=\"card p-2 border-secondary\" style=\"width:22rem;height: 12rem\">'+");
        writer.println("'<div class=\"text-success font-weight-light\" style=\"font-size: 36px;text-shadow: 1px 1px 3px #5C5858;\" >'+passrate+'%</div>'+");
        writer.println("'<div class=\"card-body\"> '+");
        writer.println("'<p class=\"card-text text-secondary font-weight-normal\" style=\"font-size: 15px;text-shadow: 1px 1px 2px #B6B6B4;\">'+total+' tests</p>'+");
        writer.println("'<div class=\"btn text-dark font-weight-normal flex-wrap\" style=\"font-size: 22px;text-shadow: 1px 1px 3px #B6B6B4;\" onClick=\"displayFeatureSuites(this)\">' + data + '</div>'+");
        writer.println("'<div class=\"row border-dark\" style=\"box-shadow: 2px 2px 2px #5C5858;\">';");
        writer.println("");
        writer.println("if(passrate != 0) {");
        writer.println("cardPlaceHolder += '<span class=\"pt-2 pb-1 bg-success\" style=\"width:'+passrate+'%;\" data-toggle=\"tooltip\" data-placement=\"top\" title=\"'+passrate+'%\"></span>';");
        writer.println("}");
        writer.println("if(skiprate != 0) {");
        writer.println("cardPlaceHolder += '<span class=\"pt-2 pb-1 bg-warning\" style=\"width:'+skiprate+'%;\" data-toggle=\"tooltip\" data-placement=\"top\" title=\"'+skiprate+'%\"></span>';");
        writer.println("}");
        writer.println("");
        writer.println("if(failrate != 0) {");
        writer.println("cardPlaceHolder += '<span class=\"pt-2 pb-1 bg-danger\" style=\"width:'+failrate+'%;\" data-toggle=\"tooltip\" data-placement=\"top\" title=\"'+failrate+'%\"></span>';");
        writer.println("}");
        writer.println("");
        writer.println("cardPlaceHolder += '</div>'+");
        writer.println("'</div>'+");
        writer.println("'</div>'+");
        writer.println("'</div>';");
        writer.println("");
        writer.println("featureRow.innerHTML = featureRow.innerHTML + cardPlaceHolder;");
        writer.println("");
        writer.println("});");
        writer.println("}");

        // This method summarizes all failures and displays it
        writer.println("function analyizeFailures() {");
        writer.println("var failureMap = {};");
        writer.println("var routingMap = {}; // suite");
        writer.println("");
        writer.println("var suiteCounter = 0;");
        writer.println("var suites = document.getElementsByClassName(\"SuiteBlock\");");
        writer.println("Array.from(suites).forEach(function(suite){");
        writer.println("var suiteName = suite.getElementsByClassName(\"SuiteBlockHeader\")[0].getElementsByTagName(\"span\")[3].innerHTML;");
        writer.println("suiteCounter++;");
        writer.println("var testCounter = 0;");
        writer.println("var tests = suite.getElementsByClassName(\"TestBlock\");");
        writer.println("Array.from(tests).forEach(function(test){");
        writer.println("var testName = test.getElementsByTagName(\"div\")[0].getElementsByTagName(\"span\")[1].innerHTML;");
        writer.println("testCounter++;");
        writer.println("var methods = test.getElementsByClassName(\"MethodBlockName\");");
        writer.println("Array.from(methods).forEach(function(method){");
        writer.println("var name = method.innerHTML;");
        writer.println("var status = method.parentElement.getElementsByClassName(\"MethodBlockStatus\")[0].innerHTML;");
        writer.println("if(status === \"FAIL\") {");
        writer.println("// failureMap");
        writer.println("if(name in failureMap) {");
        writer.println("var count = parseInt(failureMap[name]);");
        writer.println("count++;");
        writer.println("failureMap[name] = count;");
        writer.println("} else {");
        writer.println("failureMap[name] = 1;");
        writer.println("}");
        writer.println("// Routing Map");
        writer.println("if(name in routingMap) {");
        writer.println("routingMap[name] = routingMap[name] + \"|\" + suiteName+\"#\"+testName;");
        writer.println("} else {");
        writer.println("routingMap[name] = suiteName+\"#\"+testName;");
        writer.println("} ");
        writer.println("}");
        writer.println("});");
        writer.println("});");
        writer.println("});");
        writer.println("");
        writer.println("// Putting data in tables");
        writer.println("var tablebody = document.getElementById(\"FailureTableBody\");");
        writer.println("tablebody.innerHTML = \"\";");
        writer.println("for (var i = 0, keys = Object.keys(failureMap), ii = keys.length; i < ii; i++) {");
        writer.println("var data = \"<tr>\";");
        writer.println("var key = keys[i];");
        writer.println("var value = failureMap[key];");
        writer.println("");
        writer.println("data += \"<td class=\\\"text-wrap\\\">\"+i+\"</td>\";");
        writer.println("data += \"<td class=\\\"text-wrap\\\">\"+key.split(\" \")[3]+\"</td>\";");
        writer.println("data += \"<td class=\\\"text-wrap\\\">\"+key.split(\" \")[7]+\"</td>\";");
        writer.println("data += \"<td class=\\\"text-wrap\\\">\"+value+\"</td>\";");
        writer.println("");
        writer.println("// fetching suite and tests");
        writer.println("var suiteTest = \"\";");
        writer.println("");
        writer.println("suiteTest += \"<div id=\\\"linkheader_\"+i+\"\\\" class=\\\"m-0 btn btn-sm btn-outline-secondary\\\" data-toggle=\\\"collapse\\\" data-target=\\\"#linkbody_\"+i+\"\\\">Expand/Collapse</div>\";");
        writer.println("suiteTest += \"<div id=\\\"linkbody_\"+i+\"\\\" class=\\\"collapse pt-1\\\">\";");
        writer.println("var rowlinks = routingMap[key].split(\"|\");");
        writer.println("Array.from(rowlinks).forEach(function(link) {");
        writer.println("suiteTest += \"<div class=\\\"alert alert-secondary border-dark p-1 text-wrap\\\">\";");
        writer.println("suiteTest += \"<b>\"+link.split(\"#\")[0]+\"</b><br>\"");
        writer.println("suiteTest += \"\"+link.split(\"#\")[1]+\"<br>\"");
        writer.println("suiteTest += \"</div>\";");
        writer.println("});");
        writer.println("suiteTest += \"</div>\";");
        writer.println("");
        writer.println("data += \"<td class=\\\"text-wrap\\\"> \"+suiteTest+\" </td>\";");
        writer.println("");
        writer.println("");
        writer.println("data += \"</tr>\";");
        writer.println("tablebody.innerHTML = tablebody.innerHTML + data;");
        writer.println("}");
        writer.println("}");

        // API Coverage display
        writer.println("function analyizeAPICoverage() {");
        writer.println("var coverageMap = {};");
        writer.println("var routingMap = {}; // suite");
        writer.println("var apiMap = {};");
        writer.println("");
        writer.println("var suiteCounter = 0;");
        writer.println("var suites = document.getElementsByClassName(\"SuiteBlock\");");
        writer.println("Array.from(suites).forEach(function(suite){");
        writer.println("var suiteName = suite.getElementsByClassName(\"SuiteBlockHeader\")[0].getElementsByTagName(\"span\")[3].innerHTML;");
        writer.println("suiteCounter++;");
        writer.println("var testCounter = 0;");
        writer.println("var tests = suite.getElementsByClassName(\"TestBlock\");");
        writer.println("Array.from(tests).forEach(function(test){");
        writer.println("var testName = test.getElementsByTagName(\"div\")[0].getElementsByTagName(\"span\")[1].innerHTML;");
        writer.println("testCounter++;");
        writer.println("var methods = test.getElementsByClassName(\"MethodBlockName\");");
        writer.println("Array.from(methods).forEach(function(method){");
        writer.println("var name = method.innerHTML;");
        writer.println("var status = method.parentElement.getElementsByClassName(\"MethodBlockStatus\")[0].innerHTML;");
        writer.println("");
        writer.println("// API map");
        writer.println("var restTypeList = method.parentElement.nextElementSibling.getElementsByClassName(\"RequestType\"); // Fetching rest type");
        writer.println("if(restTypeList.length != 0 ) {");
        writer.println("");
        writer.println("if(!(name in apiMap)) {");
        writer.println("");
        writer.println("if(['POST','PATCH','DEL','GET'].indexOf(restTypeList[restTypeList.length-1].innerHTML) >= 0) {");
        writer.println("var restType = restTypeList[restTypeList.length-1].innerHTML;");
        writer.println("var apiName = restTypeList[restTypeList.length-1].nextElementSibling.innerHTML;");
        writer.println("apiMap[name] = \"\"+restType+\"|\"+apiName.split('?')[0]+\"\";");
        writer.println("}");
        writer.println("");
        writer.println("}");
        writer.println("");
        writer.println("// coverageMap");
        writer.println("if(name in coverageMap) {");
        writer.println("var count = parseInt(coverageMap[name]);");
        writer.println("count++;");
        writer.println("coverageMap[name] = count;");
        writer.println("} else {");
        writer.println("coverageMap[name] = 1;");
        writer.println("}");
        writer.println("");
        writer.println("// status check");
        writer.println("var color = \"\";");
        writer.println("switch(status) {");
        writer.println("case \"FAIL\":");
        writer.println("color = \"danger\";");
        writer.println("break;");
        writer.println("case \"PASS\":");
        writer.println("color = \"success\";");
        writer.println("break;");
        writer.println("case \"SKIP\": ");
        writer.println("color = \"warning\";");
        writer.println("break;");
        writer.println("default:");
        writer.println("color = \"secondary\";");
        writer.println("}");
        writer.println("");
        writer.println("");
        writer.println("// Routing Map");
        writer.println("if(name in routingMap) {");
        writer.println("routingMap[name] = routingMap[name] + \"|\" + suiteName+\"#\"+testName;");
        writer.println("} else {");
        writer.println("routingMap[name] = suiteName+\"#<span class=\\\"text-\"+color+\"\\\">\"+testName+\"</span>\";");
        writer.println("} ");
        writer.println("");
        writer.println("}");
        writer.println("");
        writer.println("});");
        writer.println("});");
        writer.println("});");
        writer.println("");
        writer.println("// Putting data in tables");
        writer.println("var tablebody = document.getElementById(\"APICoverageTableBody\");");
        writer.println("tablebody.innerHTML = \"\"; // emptying beofre loading");
        writer.println("for (var i = 0, keys = Object.keys(coverageMap), ii = keys.length; i < ii; i++) {");
        writer.println("var data = \"<tr>\";");
        writer.println("var key = keys[i];");
        writer.println("var value = coverageMap[key];");
        writer.println("");
        writer.println("data += \"<td class=\\\"text-wrap\\\">\"+i+\"</td>\";");
        writer.println("data += \"<td class=\\\"text-wrap\\\">\"+key.split(\" \")[3]+\"</td>\";");
        writer.println("data += \"<td class=\\\"text-wrap\\\">\"+key.split(\" \")[7]+\"</td>\";");
        writer.println("data += \"<td class=\\\"text-wrap\\\">\"+value+\"</td>\";");
        writer.println("");
        writer.println("// fetching suite and tests");
        writer.println("var suiteTest = \"\";");
        writer.println(
                "suiteTest += \"<div id=\\\"linkheader_\"+i+\"\\\" class=\\\"mt-2 btn btn-sm btn-outline-secondary\\\" data-toggle=\\\"collapse\\\" data-target=\\\"#linkbody_\"+i+\"\\\">\"+apiMap[key].split(\"|\")[0]+\"</div><span class=\\\"m-1 p-1 text-info\\\">\"+apiMap[key].split(\"|\")[1]+\"<span>\";");
        writer.println("suiteTest += \"<div id=\\\"linkbody_\"+i+\"\\\" class=\\\"collapse pt-1\\\">\";");
        writer.println("var rowlinks = routingMap[key].split(\"|\");");
        writer.println("Array.from(rowlinks).forEach(function(link) {");
        writer.println("suiteTest += \"<div class=\\\"alert alert-secondary border-dark p-1 text-wrap\\\">\";");
        writer.println("suiteTest += \"<b>\"+link.split(\"#\")[0]+\"</b><br>\"");
        writer.println("suiteTest += \"\"+link.split(\"#\")[1]+\"<br>\"");
        writer.println("suiteTest += \"</div>\";");
        writer.println("});");
        writer.println("suiteTest += \"</div>\";");
        writer.println("");
        writer.println("data += \"<td class=\\\"text-wrap\\\"> \"+suiteTest+\" </td>\";");
        writer.println("");
        writer.println("");
        writer.println("data += \"</tr>\";");
        writer.println("tablebody.innerHTML = tablebody.innerHTML + data;");
        writer.println("}");
        writer.println("}");
        writer.println("");

        // Loads all suites into drop down
        writer.println("function loadSuitesDropDownList() {");
        writer.println("");
        writer.println("var element = document.getElementById(\"SuiteDropDownList\");");
        writer.println("element.innerHTML = \"<a class=\\\"dropdown-item\\\" href=\\\"#\\\" onclick=\\\"showSuiteWiseCoverage(this)\\\">Overall</a>\";");
        writer.println("");
        writer.println("var suites = document.getElementsByClassName(\"SuiteBlock\");");
        writer.println("Array.from(suites).forEach(function(suite){");
        writer.println("var suiteName = suite.getElementsByClassName(\"SuiteBlockHeader\")[0].getElementsByTagName(\"span\")[3].innerHTML;");
        writer.println("element.innerHTML = element.innerHTML + \"<a class=\\\"dropdown-item\\\" href=\\\"#\\\" onclick=\\\"showSuiteWiseCoverage(this)\\\">\"+suiteName+\"</a>\";");
        writer.println("});");
        writer.println("");
        writer.println("}");
        writer.println("");

        // Display suite wise coverage
        writer.println("function showSuiteWiseCoverage(element) {");
        writer.println("");
        writer.println("var selectedSuiteName = element.innerHTML;");
        writer.println("document.getElementById(\"SelectedSuiteName\").innerHTML = selectedSuiteName;");
        writer.println("");
        writer.println("if(selectedSuiteName === 'Overall') { ");
        writer.println("analyizeAPICoverage();");
        writer.println("} else {");
        writer.println("loadCoverageBySuiteName(selectedSuiteName);");
        writer.println("}");
        writer.println("}");
        writer.println("");

        // load data suite wise
        writer.println("function loadCoverageBySuiteName(selectedSuiteName) {");
        writer.println("");
        writer.println("var coverageMap = {};");
        writer.println("var routingMap = {}; // suite");
        writer.println("var apiMap = {};");
        writer.println("");
        writer.println("var suiteCounter = 0;");
        writer.println("var suites = document.getElementsByClassName(\"SuiteBlock\");");
        writer.println("Array.from(suites).forEach(function(suite){");
        writer.println("var suiteName = suite.getElementsByClassName(\"SuiteBlockHeader\")[0].getElementsByTagName(\"span\")[3].innerHTML;");
        writer.println("if(suiteName === selectedSuiteName) {");
        writer.println("suiteCounter++;");
        writer.println("var testCounter = 0;");
        writer.println("var tests = suite.getElementsByClassName(\"TestBlock\");");
        writer.println("Array.from(tests).forEach(function(test){");
        writer.println("var testName = test.getElementsByTagName(\"div\")[0].getElementsByTagName(\"span\")[1].innerHTML;");
        writer.println("testCounter++;");
        writer.println("var methods = test.getElementsByClassName(\"MethodBlockName\");");
        writer.println("Array.from(methods).forEach(function(method){");
        writer.println("var name = method.innerHTML;");
        writer.println("var status = method.parentElement.getElementsByClassName(\"MethodBlockStatus\")[0].innerHTML;");
        writer.println("");
        writer.println("// API map");
        writer.println("var restTypeList = method.parentElement.nextElementSibling.getElementsByClassName(\"RequestType\"); // Fetching rest type");
        writer.println("if(restTypeList.length != 0 ) {");
        writer.println("");
        writer.println("if(!(name in apiMap)) {");
        writer.println("");
        writer.println("if(['POST','PATCH','DEL','GET'].indexOf(restTypeList[restTypeList.length-1].innerHTML) >= 0) {");
        writer.println("var restType = restTypeList[restTypeList.length-1].innerHTML;");
        writer.println("var apiName = restTypeList[restTypeList.length-1].nextElementSibling.innerHTML;");
        writer.println("apiMap[name] = \"\"+restType+\"|\"+apiName.split('?')[0]+\"\";");
        writer.println("}");
        writer.println("");
        writer.println("}");
        writer.println("");
        writer.println("// coverageMap");
        writer.println("if(name in coverageMap) {");
        writer.println("var count = parseInt(coverageMap[name]);");
        writer.println("count++;");
        writer.println("coverageMap[name] = count;");
        writer.println("} else {");
        writer.println("coverageMap[name] = 1;");
        writer.println("}");
        writer.println("");
        writer.println("// status check");
        writer.println("var color = \"\";");
        writer.println("switch(status) {");
        writer.println("case \"FAIL\":");
        writer.println("color = \"danger\";");
        writer.println("break;");
        writer.println("case \"PASS\":");
        writer.println("color = \"success\";");
        writer.println("break;");
        writer.println("case \"SKIP\": ");
        writer.println("color = \"warning\";");
        writer.println("break;");
        writer.println("default:");
        writer.println("color = \"secondary\";");
        writer.println("}");
        writer.println("");
        writer.println("");
        writer.println("// Routing Map");
        writer.println("if(name in routingMap) {");
        writer.println("routingMap[name] = routingMap[name] + \"|\" + suiteName+\"#\"+testName;");
        writer.println("} else {");
        writer.println("routingMap[name] = suiteName+\"#<span class=\\\"text-\"+color+\"\\\">\"+testName+\"</span>\";");
        writer.println("} ");
        writer.println("");
        writer.println("}");
        writer.println("");
        writer.println("});");
        writer.println("});");
        writer.println("}");
        writer.println("});");
        writer.println("");
        writer.println("// Putting data in tables");
        writer.println("var tablebody = document.getElementById(\"APICoverageTableBody\");");
        writer.println("tablebody.innerHTML = \"\"; // emptying beofre loading");
        writer.println("for (var i = 0, keys = Object.keys(coverageMap), ii = keys.length; i < ii; i++) {");
        writer.println("var data = \"<tr>\";");
        writer.println("var key = keys[i];");
        writer.println("var value = coverageMap[key];");
        writer.println("");
        writer.println("data += \"<td class=\\\"text-wrap\\\">\"+i+\"</td>\";");
        writer.println("data += \"<td class=\\\"text-wrap\\\">\"+key.split(\" \")[3]+\"</td>\";");
        writer.println("data += \"<td class=\\\"text-wrap\\\">\"+key.split(\" \")[7]+\"</td>\";");
        writer.println("data += \"<td class=\\\"text-wrap\\\">\"+value+\"</td>\";");
        writer.println("");
        writer.println("// fetching suite and tests");
        writer.println("var suiteTest = \"\";");
        writer.println(
                "suiteTest += \"<div id=\\\"linkheader_\"+i+\"\\\" class=\\\"mt-2 btn btn-sm btn-outline-secondary\\\" data-toggle=\\\"collapse\\\" data-target=\\\"#linkbody_\"+i+\"\\\">\"+apiMap[key].split(\"|\")[0]+\"</div><span class=\\\"m-1 p-1 text-info\\\">\"+apiMap[key].split(\"|\")[1]+\"<span>\";");
        writer.println("suiteTest += \"<div id=\\\"linkbody_\"+i+\"\\\" class=\\\"collapse pt-1\\\">\";");
        writer.println("var rowlinks = routingMap[key].split(\"|\");");
        writer.println("Array.from(rowlinks).forEach(function(link) {");
        writer.println("suiteTest += \"<div class=\\\"alert alert-secondary border-dark p-1 text-wrap\\\">\";");
        writer.println("suiteTest += \"<b>\"+link.split(\"#\")[0]+\"</b><br>\"");
        writer.println("suiteTest += \"\"+link.split(\"#\")[1]+\"<br>\"");
        writer.println("suiteTest += \"</div>\";");
        writer.println("});");
        writer.println("suiteTest += \"</div>\";");
        writer.println("");
        writer.println("data += \"<td class=\\\"text-wrap\\\"> \"+suiteTest+\" </td>\";");
        writer.println("");
        writer.println("");
        writer.println("data += \"</tr>\";");
        writer.println("tablebody.innerHTML = tablebody.innerHTML + data;");
        writer.println("}");
        writer.println("");
        writer.println("}");

        // Displaying epic wise Suites
        writer.println("function displayEpicWiseSuite(element) {");
        writer.println("showSuites();");
        writer.println("searchByEpicId(element);");
        writer.println("}");

        // Displaying feature wise suites
        writer.println("function displayFeatureSuites(element) {");
        writer.println("showSuites();");
        writer.println("document.getElementById(\"feature\").innerHTML = element.innerHTML;");
        writer.println("searchByFeaturesFromHiddenElement(element);");
        writer.println("}");

        // Shows every block on the pages
        writer.println("function showEverything() {");
        writer.println("document.getElementById(\"FeatureView\").style.display = 'block';");
        writer.println("document.getElementById(\"EpicMapping\").style.display = 'none';");
        writer.println("document.getElementById(\"PodsRestarts\").style.display = 'none';");
        writer.println("document.getElementById(\"SuitesList\").style.display = 'block';");
        writer.println("document.getElementById(\"FailureList\").style.display = 'none';");
        writer.println("document.getElementById(\"APICoverage\").style.display = 'none';");
        writer.println("document.getElementById(\"feature\").innerHTML = \"ALL\";");
        writer.println("searchByFeaturesFromHiddenElement(document.getElementById(\"feature\"));");
        writer.println("updateSummary();");
        writer.println("}");

        // Hides all but Features block
        writer.println("function showFeatures() {");
        writer.println("document.getElementById(\"FeatureView\").style.display = 'block';");
        writer.println("document.getElementById(\"EpicMapping\").style.display = 'none';");
        writer.println("document.getElementById(\"PodsRestarts\").style.display = 'none';");
        writer.println("document.getElementById(\"SuitesList\").style.display = 'none';");
        writer.println("document.getElementById(\"FailureList\").style.display = 'none';");
        writer.println("document.getElementById(\"APICoverage\").style.display = 'none';");
        writer.println("document.getElementById(\"feature\").innerHTML = \"ALL\";");
        writer.println("searchByFeaturesFromHiddenElement(document.getElementById(\"feature\"));");
        writer.println("updateSummary();");
        writer.println("}");

        // Hides all but Epic Mapping block
        writer.println("function showEpics() {");
        writer.println("document.getElementById(\"FeatureView\").style.display = 'none';");
        writer.println("document.getElementById(\"EpicMapping\").style.display = 'block';");
        writer.println("document.getElementById(\"PodsRestarts\").style.display = 'none';");
        writer.println("document.getElementById(\"SuitesList\").style.display = 'none';");
        writer.println("document.getElementById(\"FailureList\").style.display = 'none';");
        writer.println("document.getElementById(\"APICoverage\").style.display = 'none';");
        writer.println("document.getElementById(\"feature\").innerHTML = \"ALL\";");
        writer.println("searchByFeaturesFromHiddenElement(document.getElementById(\"feature\"));");
        writer.println("updateSummary();");
        writer.println("document.getElementById(\"Epic_Mapping_Summary_1_Body\").classList.remove(\"collapse\");");
        writer.println("document.getElementById(\"Epic_Mapping_Summary_1_Body\").classList.add(\"show\");");
        writer.println("}");

        // Hides all but Pod Restart block
        writer.println("function showPodRestarts() {");
        writer.println("document.getElementById(\"FeatureView\").style.display = 'none';");
        writer.println("document.getElementById(\"EpicMapping\").style.display = 'none';");
        writer.println("document.getElementById(\"PodsRestarts\").style.display = 'block';");
        writer.println("document.getElementById(\"SuitesList\").style.display = 'none';");
        writer.println("document.getElementById(\"FailureList\").style.display = 'none';");
        writer.println("document.getElementById(\"APICoverage\").style.display = 'none';");
        writer.println("document.getElementById(\"feature\").innerHTML = \"ALL\";");
        writer.println("searchByFeaturesFromHiddenElement(document.getElementById(\"feature\"));");
        writer.println("updateSummary();");
        writer.println("}");

        // Hides all but Suites block
        writer.println("function showSuites() {");
        writer.println("document.getElementById(\"FeatureView\").style.display = 'none';");
        writer.println("document.getElementById(\"EpicMapping\").style.display = 'none';");
        writer.println("document.getElementById(\"PodsRestarts\").style.display = 'none';");
        writer.println("document.getElementById(\"SuitesList\").style.display = 'block';");
        writer.println("document.getElementById(\"FailureList\").style.display = 'none';");
        writer.println("document.getElementById(\"APICoverage\").style.display = 'none';");
        writer.println("document.getElementById(\"feature\").innerHTML = \"ALL\";");
        writer.println("searchByFeaturesFromHiddenElement(document.getElementById(\"feature\"));");
        writer.println("updateSummary();");
        writer.println("}");

        writer.println("function showFailures() {\r\n");
        writer.println("document.getElementById(\"FeatureView\").style.display = 'none';");
        writer.println("document.getElementById(\"EpicMapping\").style.display = 'none';");
        writer.println("document.getElementById(\"PodsRestarts\").style.display = 'none';");
        writer.println("document.getElementById(\"SuitesList\").style.display = 'none';");
        writer.println("document.getElementById(\"FailureList\").style.display = 'block';");
        writer.println("document.getElementById(\"APICoverage\").style.display = 'none';");
        writer.println("analyizeFailures();");
        writer.println("document.getElementById(\"feature\").innerHTML = \"ALL\";");
        writer.println("searchByFeaturesFromHiddenElement(document.getElementById(\"feature\"));");
        writer.println("updateSummary();\r\n");
        writer.println("}");

        writer.println("function showAPICoverage() {");
        writer.println("document.getElementById(\"FeatureView\").style.display = 'none';");
        writer.println("document.getElementById(\"EpicMapping\").style.display = 'none';");
        writer.println("document.getElementById(\"PodsRestarts\").style.display = 'none';");
        writer.println("document.getElementById(\"SuitesList\").style.display = 'none';");
        writer.println("document.getElementById(\"FailureList\").style.display = 'none';");
        writer.println("document.getElementById(\"APICoverage\").style.display = 'block';");
        writer.println("loadSuitesDropDownList();");
        writer.println("analyizeAPICoverage();");
        writer.println("document.getElementById(\"feature\").innerHTML = \"ALL\";");
        writer.println("searchByFeaturesFromHiddenElement(document.getElementById(\"feature\"));");
        writer.println("updateSummary();");
        writer.println("}");
        writer.println("");

        writer.println("</script>");

    }

    /**
     * Finishes HTML stream
     */
    private void endHtml(PrintWriter out) {

        writeCopyToClipboardJavaScript();
        out.println("<br><br><center><span class=\"lead\"> TestNG Report </span></center>");
        out.println("</body></html>");
    }

    /**
     * Since the methods will be sorted chronologically, we want to return the ITestNGMethod from the invoked methods.
     */
    private Collection<ITestNGMethod> getMethodSet(IResultMap tests, ISuite suite) {

        List<IInvokedMethod> r = Lists.newArrayList();
        List<IInvokedMethod> invokedMethods = suite.getAllInvokedMethods();
        for (IInvokedMethod im : invokedMethods) {
            if (tests.getAllMethods().contains(im.getTestMethod())) {
                r.add(im);
            }
        }

        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        System.setProperty("java.util.Collections.useLegacyMergeSort", "true");
        Collections.sort(r, new TestSorter());
        List<ITestNGMethod> result = Lists.newArrayList();

        // Add all the invoked methods
        for (IInvokedMethod m : r) {
            for (ITestNGMethod temp : result) {
                if (!temp.equals(m.getTestMethod()))
                    result.add(m.getTestMethod());
            }
        }

        // Add all the methods that weren't invoked (e.g. skipped) that we
        // haven't added yet
        Collection<ITestNGMethod> allMethodsCollection = tests.getAllMethods();
        List<ITestNGMethod> allMethods = new ArrayList<ITestNGMethod>(allMethodsCollection);
        Collections.sort(allMethods, new TestMethodSorter());

        for (ITestNGMethod m : allMethods) {
            if (!result.contains(m)) {
                result.add(m);
            }
        }
        return result;
    }

    public static String convertExceptionToString(Throwable throwable) {

        StringWriter exceptionString = new StringWriter();
        PrintWriter printWriter = new PrintWriter(exceptionString);
        throwable.printStackTrace(printWriter);
        //String exceptionMessage = throwable.getMessage() + "\n" + exceptionString.toString();
        return exceptionString.toString();
    }

    /**
     * @param outdir
     * @return
     * @throws IOException
     */
    protected PrintWriter createWriter(String outdir) throws IOException {
        new File(outdir).mkdirs();
        return new PrintWriter(new BufferedWriter(new FileWriter(new File(outdir, reportFileName))));
    }

    // ~ Inner Classes --------------------------------------------------------

    /**
     * Arranges methods by classname and method name
     */
    private class TestSorter implements Comparator<IInvokedMethod> {
        // ~ Methods
        // -------------------------------------------------------------

        /**
         * Arranges methods by classname and method name
         */
        @Override
        public int compare(IInvokedMethod obj1, IInvokedMethod obj2) {
            int r = obj1.getTestMethod().getTestClass().getName().compareTo(obj2.getTestMethod().getTestClass().getName());
            return r;
        }
    }

    private class TestMethodSorter implements Comparator<ITestNGMethod> {
        @Override
        public int compare(ITestNGMethod obj1, ITestNGMethod obj2) {
            int r = obj1.getTestClass().getName().compareTo(obj2.getTestClass().getName());
            if (r == 0) {
                r = obj1.getMethodName().compareTo(obj2.getMethodName());
            }
            return r;
        }
    }

    /**
     * Converts time in milliseconds to hh:mm:ss:SSS format
     *
     * @param timeInMillis
     * @return
     * @author pranavd
     */
    private String convertMiliseconds(long timeInMillis) {

        String sign = "";
        if (timeInMillis < 0) {
            sign = "-";
            timeInMillis = Math.abs(timeInMillis);
        }

        long hours = (timeInMillis / (1000 * 60 * 60)) % 24;
        long minutes = (timeInMillis / (1000 * 60)) % 60;
        long seconds = (timeInMillis / 1000) % 60;
        long millis = timeInMillis % 1000;

        final StringBuilder formatted = new StringBuilder(20);
        formatted.append(sign);
        formatted.append(String.format("%02d", hours));
        formatted.append(String.format(":%02d", minutes));
        formatted.append(String.format(":%02d", seconds));
        formatted.append(String.format(".%03d", millis));

        return formatted.toString();
    }


// public void prepareData(List<ISuite> suites) {
//
// List<classDetails> classList = new ArrayList<>();
// int testOverallIndex = 100000;
// // Suites
// for (ISuite suite : suites) {
//
// // Tests
// for (ISuiteResult suiteResult : suite.getResults().values()) {
//
// classDetails cDetails = new classDetails();
//
// testCounter++;
// String testName = suiteResult.getTestContext().getName();
//
// HashMap<String, String> testInformation = new HashMap<>();
// testInformation.put("key", testName);
// testInformation.put("counter", String.valueOf(testCounter));
// testInformation.put("classes", "");
//
// // Class and Methods
// for (ITestNGMethod method : suiteResult.getTestContext().getAllTestMethods()) {
//
// String className = method.getTestClass().getName();
//
// if (!testInformation.get("classes").contains(className)) {
//
// System.out.println("Writing class [" + className + "] for first time");
//
// classCounter++;
// testInformation.put("classes", testInformation.get("classes") + "," + className);
//
// HashMap<String, Object> classInformation = new HashMap<String, Object>();
// classInformation.put("name", className);
// classInformation.put("counter", String.valueOf(classCounter));
// classInformation.put("passed", "0");
// classInformation.put("failed", "0");
// classInformation.put("skipped", "0");
//
// ArrayList<ITestNGMethod> methods = new ArrayList<>();
// methods.add(method);
// classInformation.put("ITestNGMethod", methods);
//
// classwiseData.put(testName + "_" + className, classInformation);
//
// } else {
//
// System.out.println("Writing data to exiting class [" + className + "] ");
//
// HashMap<String, Object> classInformationTemp = classwiseData.get(testName + "_" + className);
//
// if (suiteResult.getTestContext().getPassedTests().getAllMethods().contains(method)) {
// // Passed
// classInformationTemp.put("passed", String.valueOf(Integer.parseInt(classInformationTemp.get("passed").toString()) + 1));
// } else if (suiteResult.getTestContext().getFailedTests().getAllMethods().contains(method)) {
// // Failed
// classInformationTemp.put("failed", String.valueOf(Integer.parseInt(classInformationTemp.get("failed").toString()) + 1));
// } else {
// // Skipped
// classInformationTemp.put("skipped", String.valueOf(Integer.parseInt(classInformationTemp.get("skipped").toString()) + 1));
// }
//
// ArrayList<ITestNGMethod> tempmethods = (ArrayList) classInformationTemp.get("ITestNGMethod");
// tempmethods.add(method);
// classInformationTemp.put("ITestNGMethod", tempmethods);
//
// classwiseData.put(testName + "_" + className, classInformationTemp);
// }
//
// }
//
// testwiseData.put(testName, testInformation);
// }
// }
//
//
// }

}

//class testDetails
//{
// String testName;
// String className;
// String suiteName;
// String xmlSuiteName;
//
// int passedTests = 0;
// int skippedTests = 0;
// int failedTests = 0;
// int testIndex;
// List<ITestNGMethod> classMethods = new ArrayList<>();
//
//
//}
//
//class classDetails
//{
// String name;
// List<testDetails> tests = new ArrayList<>();
//
//
//
//}

class ExecutionDateCompator_new implements Comparator<ITestResult>, Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * (non-Javadoc)
     *
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(ITestResult o1, ITestResult o2) {
        if (o1.getStartMillis() > o2.getStartMillis()) {
            return 1;
        }
        return -1;
    }
}
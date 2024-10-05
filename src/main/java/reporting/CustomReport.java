package reporting;

import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.xml.XmlSuite;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CustomReport implements IReporter {

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        StringBuilder reportContent = new StringBuilder();
        reportContent.append("<html><head><title>Test Report</title></head><body>");
        reportContent.append("<h1>Test Execution Report</h1>");

        for (ISuite suite : suites) {
            reportContent.append("<h2>Suite: ").append(suite.getName()).append("</h2>");
            Map<String, ISuiteResult> suiteResults = suite.getResults();

            for (ISuiteResult sr : suiteResults.values()) {
                ITestContext tc = sr.getTestContext();
                reportContent.append("<h3>Test: ").append(tc.getName()).append("</h3>");
                reportContent.append("<p>Passed tests: ").append(tc.getPassedTests().getAllResults().size()).append("</p>");
                reportContent.append("<p>Failed tests: ").append(tc.getFailedTests().getAllResults().size()).append("</p>");
                reportContent.append("<p>Skipped tests: ").append(tc.getSkippedTests().getAllResults().size()).append("</p>");
                reportContent.append("<p>Start time: ").append(tc.getStartDate()).append("</p>");
                reportContent.append("<p>End time: ").append(tc.getEndDate()).append("</p>");
            }
        }

        reportContent.append("</body></html>");

        try (FileWriter writer = new FileWriter(outputDirectory + "/custom-report.html")) {
            writer.write(reportContent.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

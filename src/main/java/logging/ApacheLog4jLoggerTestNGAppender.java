package logging;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.testng.Reporter;


public class ApacheLog4jLoggerTestNGAppender extends AppenderSkeleton {
    protected void append(LoggingEvent loggingEvent) {
        Reporter.log(loggingEvent.getLevel().toString() + " " + loggingEvent.getMessage().toString());
    }

    public void close() {

    }

    public boolean requiresLayout() {
        return false;
    }
}

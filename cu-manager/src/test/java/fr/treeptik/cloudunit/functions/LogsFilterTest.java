package fr.treeptik.cloudunit.functions;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.treeptik.cloudunit.dto.LogResource;

/**
 * Created by nicolas on 04/01/2016.
 */
public class LogsFilterTest {
    private final List<LogResource> logs = Arrays.asList(
            new LogResource("catalina.out", "Hello, I am a log stackstrace!"),
            new LogResource("catalina.out", "Goodbye, I am an another message!"),
            new LogResource("localhost.txt", "Goodbye, I am an wrong message!"),
            new LogResource("localhost.txt", "Goodbye, I am an empty message!"));

    @Test
    public void testFilterSource() {
        List<LogResource> logsFiltered = LogsFilter.bySource.apply("catalina.out", logs);
        Assert.assertEquals(2, logsFiltered.size());

        logsFiltered = LogsFilter.bySource.apply("NO FILE", logs);
        Assert.assertEquals(0, logsFiltered.size());
    }

    @Test
    public void testFilterMessage() {
        List<LogResource> logsFiltered = LogsFilter.byMessage.apply("stackstrace", logs);
        Assert.assertEquals(1, logsFiltered.size());

        logsFiltered = LogsFilter.byMessage.apply("GOODB", logs);
        Assert.assertEquals(3, logsFiltered.size());

        logsFiltered = LogsFilter.bySource.apply("NOTHING HERE", logs);
        Assert.assertEquals(0, logsFiltered.size());
    }
}
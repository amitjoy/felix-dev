package org.apache.felix.hc.core.impl.executor.async.cron;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class CronParserTest extends Assert {

    @Test
    public void testAt50Seconds() {
        assertEquals(new Date(2012, 6, 2, 1, 0).getTime(),
                new CronParser("*/15 * 1-4 * * *").next(new Date(2012, 6, 1, 9, 53, 50).getTime()));
    }

    @Test
    public void testAt0Seconds() {
        assertEquals(new Date(2012, 6, 2, 1, 0).getTime(),
                new CronParser("*/15 * 1-4 * * *").next(new Date(2012, 6, 1, 9, 53).getTime()));
    }

    @Test
    public void testAt0Minutes() {
        assertEquals(new Date(2012, 6, 2, 1, 0).getTime(),
                new CronParser("0 */2 1-4 * * *").next(new Date(2012, 6, 1, 9, 0).getTime()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWith0Increment() {
        new CronParser("*/0 * * * * *").next(new Date(2012, 6, 1, 9, 0).getTime());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithNegativeIncrement() {
        new CronParser("*/-1 * * * * *").next(new Date(2012, 6, 1, 9, 0).getTime());
    }

}
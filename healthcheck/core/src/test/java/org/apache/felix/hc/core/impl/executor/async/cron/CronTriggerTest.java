package org.apache.felix.hc.core.impl.executor.async.cron;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CronTriggerTest extends Assert {

    private final Calendar calendar = new GregorianCalendar();

    private final Date date;

    private final TimeZone timeZone;

    public CronTriggerTest(final Date date, final TimeZone timeZone) {
        this.date = date;
        this.timeZone = timeZone;
    }

    @Parameters(name = "date [{0}], time zone [{1}]")
    public static List<Object[]> getParameters() {
        final List<Object[]> list = new ArrayList<>();
        list.add(new Object[] { new Date(), TimeZone.getTimeZone("PST") });
        list.add(new Object[] { new Date(), TimeZone.getTimeZone("CET") });
        return list;
    }

    private static void roundup(final Calendar calendar) {
        calendar.add(Calendar.SECOND, 1);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    @Before
    public void setUp() {
        calendar.setTimeZone(timeZone);
        calendar.setTime(date);
        roundup(calendar);
    }

    @Test
    public void testMatchAll() {
        final CronParser trigger = new CronParser("* * * * * *", timeZone);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testMatchLastSecond() {
        final CronParser trigger = new CronParser("* * * * * *", timeZone);
        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.set(Calendar.SECOND, 58);
        assertMatchesNextSecond(trigger, calendar);
    }

    @Test
    public void testMatchSpecificSecond() {
        final CronParser trigger = new CronParser("10 * * * * *", timeZone);
        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.set(Calendar.SECOND, 9);
        assertMatchesNextSecond(trigger, calendar);
    }

    @Test
    public void testIncrementSecondByOne() {
        final CronParser trigger = new CronParser("11 * * * * *", timeZone);
        calendar.set(Calendar.SECOND, 10);
        final Date date = calendar.getTime();
        calendar.add(Calendar.SECOND, 1);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testIncrementSecondWithPreviousExecutionTooEarly() {
        final CronParser trigger = new CronParser("11 * * * * *", timeZone);
        calendar.set(Calendar.SECOND, 11);
        final Date date = calendar.getTime();

        calendar.add(Calendar.MINUTE, 1);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testIncrementSecondAndRollover() {
        final CronParser trigger = new CronParser("10 * * * * *", timeZone);
        calendar.set(Calendar.SECOND, 11);
        final Date date = calendar.getTime();
        calendar.add(Calendar.SECOND, 59);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testSecondRange() {
        final CronParser trigger = new CronParser("10-15 * * * * *", timeZone);
        calendar.set(Calendar.SECOND, 9);
        assertMatchesNextSecond(trigger, calendar);
        calendar.set(Calendar.SECOND, 14);
        assertMatchesNextSecond(trigger, calendar);
    }

    @Test
    public void testIncrementMinute() {
        final CronParser trigger = new CronParser("0 * * * * *", timeZone);
        calendar.set(Calendar.MINUTE, 10);
        Date date = calendar.getTime();
        calendar.add(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
        calendar.add(Calendar.MINUTE, 1);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
    }

    @Test
    public void testIncrementMinuteByOne() {
        final CronParser trigger = new CronParser("0 11 * * * *", timeZone);
        calendar.set(Calendar.MINUTE, 10);
        final Date date = calendar.getTime();

        calendar.add(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testIncrementMinuteAndRollover() {
        final CronParser trigger = new CronParser("0 10 * * * *", timeZone);
        calendar.set(Calendar.MINUTE, 11);
        calendar.set(Calendar.SECOND, 0);
        final Date date = calendar.getTime();
        calendar.add(Calendar.MINUTE, 59);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testIncrementHour() {
        final CronParser trigger = new CronParser("0 0 * * * *", timeZone);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        calendar.set(Calendar.HOUR_OF_DAY, 11);
        calendar.set(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        Date date = calendar.getTime();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
        calendar.set(Calendar.HOUR_OF_DAY, 13);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testIncrementHourAndRollover() {
        final CronParser trigger = new CronParser("0 0 * * * *", timeZone);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DAY_OF_MONTH, 10);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        Date date = calendar.getTime();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 11);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
        calendar.set(Calendar.HOUR_OF_DAY, 1);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testIncrementDayOfMonth() {
        final CronParser trigger = new CronParser("0 0 0 * * *", timeZone);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date date = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
        assertEquals(2, calendar.get(Calendar.DAY_OF_MONTH));
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
        assertEquals(3, calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testIncrementDayOfMonthByOne() {
        final CronParser trigger = new CronParser("* * * 10 * *", timeZone);
        calendar.set(Calendar.DAY_OF_MONTH, 9);
        final Date date = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testIncrementDayOfMonthAndRollover() {
        final CronParser trigger = new CronParser("* * * 10 * *", timeZone);
        calendar.set(Calendar.DAY_OF_MONTH, 11);
        final Date date = calendar.getTime();
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 10);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testDailyTriggerInShortMonth() {
        final CronParser trigger = new CronParser("0 0 0 * * *", timeZone);
        calendar.set(Calendar.MONTH, 8); // September: 30 days
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        Date date = calendar.getTime();
        calendar.set(Calendar.MONTH, 9); // October
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());

        calendar.set(Calendar.DAY_OF_MONTH, 2);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testDailyTriggerInLongMonth() {
        final CronParser trigger = new CronParser("0 0 0 * * *", timeZone);
        calendar.set(Calendar.MONTH, 7); // August: 31 days and not a daylight saving boundary
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        Date date = calendar.getTime();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 31);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());

        calendar.set(Calendar.MONTH, 8); // September
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testDailyTriggerOnDaylightSavingBoundary() {
        final CronParser trigger = new CronParser("0 0 0 * * *", timeZone);
        calendar.set(Calendar.MONTH, 9); // October: 31 days and a daylight saving boundary in CET
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        Date date = calendar.getTime();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 31);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());

        calendar.set(Calendar.MONTH, 10); // November
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testIncrementMonth() {
        final CronParser trigger = new CronParser("0 0 0 1 * *", timeZone);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        Date date = calendar.getTime();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MONTH, 10);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());

        calendar.set(Calendar.MONTH, 11);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testIncrementMonthAndRollover() {
        final CronParser trigger = new CronParser("0 0 0 1 * *", timeZone);
        calendar.set(Calendar.MONTH, 11);
        calendar.set(Calendar.DAY_OF_MONTH, 31);
        calendar.set(Calendar.YEAR, 2010);
        Date date = calendar.getTime();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MONTH, 0);
        calendar.set(Calendar.YEAR, 2011);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());

        calendar.set(Calendar.MONTH, 1);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testMonthlyTriggerInLongMonth() {
        final CronParser trigger = new CronParser("0 0 0 31 * *", timeZone);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        final Date date = calendar.getTime();
        calendar.set(Calendar.DAY_OF_MONTH, 31);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testMonthlyTriggerInShortMonth() {
        final CronParser trigger = new CronParser("0 0 0 1 * *", timeZone);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        final Date date = calendar.getTime();
        calendar.set(Calendar.MONTH, 10);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testIncrementDayOfWeekByOne() {
        final CronParser trigger = new CronParser("* * * * * 2", timeZone);
        calendar.set(Calendar.DAY_OF_WEEK, 2);
        final Date date = calendar.getTime();
        calendar.add(Calendar.DAY_OF_WEEK, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
        assertEquals(Calendar.TUESDAY, calendar.get(Calendar.DAY_OF_WEEK));
    }

    @Test
    public void testIncrementDayOfWeekAndRollover() {
        final CronParser trigger = new CronParser("* * * * * 2", timeZone);
        calendar.set(Calendar.DAY_OF_WEEK, 4);
        final Date date = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, 6);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
        assertEquals(Calendar.TUESDAY, calendar.get(Calendar.DAY_OF_WEEK));
    }

    @Test
    public void testSpecificMinuteSecond() {
        final CronParser trigger = new CronParser("55 5 * * * *", timeZone);
        calendar.set(Calendar.MINUTE, 4);
        calendar.set(Calendar.SECOND, 54);
        Date date = calendar.getTime();

        calendar.add(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 55);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());

        calendar.add(Calendar.HOUR, 1);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testSpecificHourSecond() {
        final CronParser trigger = new CronParser("55 * 10 * * *", timeZone);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.SECOND, 54);
        Date date = calendar.getTime();

        calendar.add(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 55);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());

        calendar.add(Calendar.MINUTE, 1);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testSpecificMinuteHour() {
        final CronParser trigger = new CronParser("* 5 10 * * *", timeZone);
        calendar.set(Calendar.MINUTE, 4);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        Date date = calendar.getTime();
        calendar.add(Calendar.MINUTE, 1);
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.SECOND, 0);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());

        // next trigger is in one second because second is wildcard
        calendar.add(Calendar.SECOND, 1);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testSpecificDayOfMonthSecond() {
        final CronParser trigger = new CronParser("55 * * 3 * *", timeZone);
        calendar.set(Calendar.DAY_OF_MONTH, 2);
        calendar.set(Calendar.SECOND, 54);
        Date date = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 55);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());

        calendar.add(Calendar.MINUTE, 1);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testSpecificDate() {
        final CronParser trigger = new CronParser("* * * 3 11 *", timeZone);
        calendar.set(Calendar.DAY_OF_MONTH, 2);
        calendar.set(Calendar.MONTH, 9);
        Date date = calendar.getTime();

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MONTH, 10); // 10=November
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());

        calendar.add(Calendar.SECOND, 1);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonExistentSpecificDate() {
        final CronParser trigger = new CronParser("0 0 0 31 6 *", timeZone);
        calendar.set(Calendar.DAY_OF_MONTH, 10);
        calendar.set(Calendar.MONTH, 2);
        final Date date = calendar.getTime();
        trigger.next(date.getTime());
    }

    @Test
    public void testLeapYearSpecificDate() {
        final CronParser trigger = new CronParser("0 0 0 29 2 *", timeZone);
        calendar.set(Calendar.YEAR, 2007);
        calendar.set(Calendar.DAY_OF_MONTH, 10);
        calendar.set(Calendar.MONTH, 1); // 2=February
        Date date = calendar.getTime();

        calendar.set(Calendar.YEAR, 2008);
        calendar.set(Calendar.DAY_OF_MONTH, 29);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
        calendar.add(Calendar.YEAR, 4);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
    }

    @Test
    public void testWeekDaySequence() {
        final CronParser trigger = new CronParser("0 0 7 ? * MON-FRI", timeZone);
        // This is a Saturday
        calendar.set(2009, Calendar.SEPTEMBER, 26);
        Date date = calendar.getTime();
        // 7 am is the trigger time
        calendar.set(Calendar.HOUR_OF_DAY, 7);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        // Add two days because we start on Saturday
        calendar.add(Calendar.DAY_OF_MONTH, 2);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
        // Next day is a week day so add one
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
    }

    @Test
    public void testDayOfWeekIndifferent() {
        final CronParser trigger1 = new CronParser("* * * 2 * *", timeZone);
        final CronParser trigger2 = new CronParser("* * * 2 * ?", timeZone);
        assertEquals(trigger1, trigger2);
    }

    @Test
    public void testSecondIncrementer() {
        final CronParser trigger1 = new CronParser("57,59 * * * * *", timeZone);
        final CronParser trigger2 = new CronParser("57/2 * * * * *", timeZone);
        assertEquals(trigger1, trigger2);
    }

    @Test
    public void testSecondIncrementerWithRange() {
        final CronParser trigger1 = new CronParser("1,3,5 * * * * *", timeZone);
        final CronParser trigger2 = new CronParser("1-6/2 * * * * *", timeZone);
        assertEquals(trigger1, trigger2);
    }

    @Test
    public void testHourIncrementer() {
        final CronParser trigger1 = new CronParser("* * 4,8,12,16,20 * * *", timeZone);
        final CronParser trigger2 = new CronParser("* * 4/4 * * *", timeZone);
        assertEquals(trigger1, trigger2);
    }

    @Test
    public void testDayNames() {
        final CronParser trigger1 = new CronParser("* * * * * 0-6", timeZone);
        final CronParser trigger2 = new CronParser("* * * * * TUE,WED,THU,FRI,SAT,SUN,MON",
                timeZone);
        assertEquals(trigger1, trigger2);
    }

    @Test
    public void testSundayIsZero() {
        final CronParser trigger1 = new CronParser("* * * * * 0", timeZone);
        final CronParser trigger2 = new CronParser("* * * * * SUN", timeZone);
        assertEquals(trigger1, trigger2);
    }

    @Test
    public void testSundaySynonym() {
        final CronParser trigger1 = new CronParser("* * * * * 0", timeZone);
        final CronParser trigger2 = new CronParser("* * * * * 7", timeZone);
        assertEquals(trigger1, trigger2);
    }

    @Test
    public void testMonthNames() {
        final CronParser trigger1 = new CronParser("* * * * 1-12 *", timeZone);
        final CronParser trigger2 = new CronParser(
                "* * * * FEB,JAN,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC *", timeZone);
        assertEquals(trigger1, trigger2);
    }

    @Test
    public void testMonthNamesMixedCase() {
        final CronParser trigger1 = new CronParser("* * * * 2 *", timeZone);
        final CronParser trigger2 = new CronParser("* * * * Feb *", timeZone);
        assertEquals(trigger1, trigger2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSecondInvalid() {
        new CronParser("77 * * * * *", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSecondRangeInvalid() {
        new CronParser("44-77 * * * * *", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinuteInvalid() {
        new CronParser("* 77 * * * *", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinuteRangeInvalid() {
        new CronParser("* 44-77 * * * *", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHourInvalid() {
        new CronParser("* * 27 * * *", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHourRangeInvalid() {
        new CronParser("* * 23-28 * * *", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDayInvalid() {
        new CronParser("* * * 45 * *", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDayRangeInvalid() {
        new CronParser("* * * 28-45 * *", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMonthInvalid() {
        new CronParser("0 0 0 25 13 ?", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMonthInvalidTooSmall() {
        new CronParser("0 0 0 25 0 ?", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDayOfMonthInvalid() {
        new CronParser("0 0 0 32 12 ?", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMonthRangeInvalid() {
        new CronParser("* * * * 11-13 *", timeZone);
    }

    @Test
    public void testWhitespace() {
        final CronParser trigger1 = new CronParser("*  *  * *  1 *", timeZone);
        final CronParser trigger2 = new CronParser("* * * * 1 *", timeZone);
        assertEquals(trigger1, trigger2);
    }

    @Test
    public void testMonthSequence() {
        final CronParser trigger = new CronParser("0 30 23 30 1/3 ?", timeZone);
        calendar.set(2010, Calendar.DECEMBER, 30);
        Date date = calendar.getTime();
        // set expected next trigger time
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 30);
        calendar.set(Calendar.SECOND, 0);
        calendar.add(Calendar.MONTH, 1);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTime(), date);

        // Next trigger is 3 months latter
        calendar.add(Calendar.MONTH, 3);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTime(), date);

        // Next trigger is 3 months latter
        calendar.add(Calendar.MONTH, 3);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTime(), date);
    }

    @Test
    public void testDaylightSavingMissingHour() {
        // This trigger has to be somewhere in between 2am and 3am
        final CronParser trigger = new CronParser("0 10 2 * * *", timeZone);
        calendar.set(Calendar.DAY_OF_MONTH, 31);
        calendar.set(Calendar.MONTH, Calendar.MARCH);
        calendar.set(Calendar.YEAR, 2013);
        calendar.set(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.SECOND, 54);
        final Date date = calendar.getTime();
        if (timeZone.equals(TimeZone.getTimeZone("CET"))) {
            // Clocks go forward an hour so 2am doesn't exist in CET for this date
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.MINUTE, 10);
        calendar.set(Calendar.SECOND, 0);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    private void assertMatchesNextSecond(final CronParser trigger, final Calendar calendar) {
        final Date date = calendar.getTime();
        roundup(calendar);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

}
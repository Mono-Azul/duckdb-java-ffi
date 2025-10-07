package test.java;

import mau.duckdbffi.driver.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestLocalDateTimeColumn
{
    static Database DbForTestRun;

    @BeforeAll
    static void setupDB() throws DuckDbException
    {
        DbForTestRun = new Database(":memory:");
        Connection con = DbForTestRun.getConnection();

        con.query("CREATE TABLE LocalDateTimeTest (ID INT4, NullValue TIMESTAMP);");
        con.query("INSERT INTO LocalDateTimeTest (SELECT rng, make_timestamp_ms(rng * 1000000000) FROM (SELECT * AS rng FROM RANGE(10000)));");
        con.query("INSERT INTO LocalDateTimeTest (ID) VALUES (-1);");
    }

    @AfterAll
    static void closeDB() throws DuckDbException
    {
        DbForTestRun.close();
    }

    @Test
    void testLocalDateTimeArray()
    {
        LocalDateTime[] compArray = new LocalDateTime[5];
        compArray[0] = LocalDateTime.parse("2286-11-09T04:00:00");
        compArray[1] = LocalDateTime.parse("2286-10-28T14:13:20");
        compArray[2] = LocalDateTime.parse("2286-10-17T00:26:40");
        compArray[3] = LocalDateTime.parse("2286-10-05T10:40:00");
        compArray[4] = LocalDateTime.parse("2286-09-23T20:53:20");

        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM LocalDateTimeTest ORDER BY id DESC LIMIT 5;");

            LocalDateTimeColumn Col = (LocalDateTimeColumn)res.Columns.getFirst();

            assertEquals(0, Arrays.compare(compArray, Col.VectorArray));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testLocalDateTimeRows()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM LocalDateTimeTest WHERE ID >= 0 ORDER BY id LIMIT 3000;");

            for (int row = 0; row < 3000; row++)
            {
                assertEquals((LocalDateTime)res.getRow(row).getFirst(), LocalDateTime.ofEpochSecond(row * 1000000L
                        , 0, ZoneOffset.UTC));
            }
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testLocalDateTimeNull()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT ID, NullValue FROM LocalDateTimeTest WHERE ID = -1;");

            assertNull(res.getRow(0).get(1));
            assertEquals(-1, res.getRow(0).get(0));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }
}

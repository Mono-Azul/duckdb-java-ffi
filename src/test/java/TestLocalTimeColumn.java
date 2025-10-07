package test.java;

import mau.duckdbffi.driver.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestLocalTimeColumn
{
    static Database DbForTestRun;

    @BeforeAll
    static void setupDB() throws DuckDbException
    {
        DbForTestRun = new Database(":memory:");
        Connection con = DbForTestRun.getConnection();

        con.query("CREATE TABLE LocalTimeTest (ID INT4, NullValue TIME);");
        con.query("INSERT INTO LocalTimeTest (SELECT rng, make_timestamp_ms(rng * 1000000000) FROM (SELECT * AS rng FROM RANGE(10000)));");
        con.query("INSERT INTO LocalTimeTest (ID) VALUES (-1);");
    }

    @AfterAll
    static void closeDB() throws DuckDbException
    {
        DbForTestRun.close();
    }

    @Test
    void testLocalTimeArray()
    {
        LocalTime[] compArray = new LocalTime[5];
        compArray[0] = LocalTime.parse("04:00:00");
        compArray[1] = LocalTime.parse("14:13:20");
        compArray[2] = LocalTime.parse("00:26:40");
        compArray[3] = LocalTime.parse("10:40:00");
        compArray[4] = LocalTime.parse("20:53:20");

        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM LocalTimeTest ORDER BY id DESC LIMIT 5;");

            LocalTimeColumn Col = (LocalTimeColumn)res.Columns.getFirst();

            assertEquals(0, Arrays.compare(compArray, Col.VectorArray));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testLocalTimeRows()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM LocalTimeTest WHERE ID >= 0 ORDER BY id LIMIT 3000;");

            for (int row = 0; row < 3000; row++)
            {
                assertEquals((LocalTime)res.getRow(row).getFirst(), LocalTime.ofSecondOfDay((row * 49600) % 86400));
            }
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testLocalTimeNull()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT ID, NullValue FROM LocalTimeTest WHERE ID = -1;");

            assertNull(res.getRow(0).get(1));
            assertEquals(-1, res.getRow(0).get(0));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }
}

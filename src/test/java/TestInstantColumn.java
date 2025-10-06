package test.java;

import mau.duckdbffi.driver.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestInstantColumn
{
    static Database DbForTestRun;

    @BeforeAll
    static void setupDB() throws DuckDbException
    {
        DbForTestRun = new Database(":memory:");
        Connection con = DbForTestRun.getConnection();

        con.query("CREATE TABLE InstantTest (ID INT4, NullValue TIMESTAMPTZ);");
        con.query("SET TimeZone = 'America/Lima';");
        con.query("INSERT INTO InstantTest (SELECT rng, to_timestamp(rng * 1000000) FROM (SELECT * AS rng FROM RANGE(10000)));");
        con.query("INSERT INTO InstantTest (ID) VALUES (-1);");
    }

    @AfterAll
    static void closeDB() throws DuckDbException
    {
        DbForTestRun.close();
    }

    @Test
    void testLongArray()
    {
        Instant[] compArray = new Instant[5];
        compArray[0] = Instant.parse("2286-11-09T04:00:00Z");
        compArray[1] = Instant.parse("2286-10-28T14:13:20Z");
        compArray[2] = Instant.parse("2286-10-17T00:26:40Z");
        compArray[3] = Instant.parse("2286-10-05T10:40:00Z");
        compArray[4] = Instant.parse("2286-09-23T20:53:20Z");

        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM InstantTest ORDER BY id DESC LIMIT 5;");

            InstantColumn Col = (InstantColumn)res.Columns.getFirst();

            assertEquals(0, Arrays.compare(compArray, Col.VectorArray));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testLongRows()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM InstantTest WHERE ID >= 0 ORDER BY id LIMIT 3000;");

            for (int row = 0; row < 3000; row++)
            {
                assertEquals((Instant)res.getRow(row).getFirst(), Instant.EPOCH.plusSeconds(row * 1000000L));
            }
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testLongNull()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT ID, NullValue FROM InstantTest WHERE ID = -1;");

            assertNull(res.getRow(0).get(1));
            assertEquals(-1, res.getRow(0).get(0));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }
}

package test.java;

import mau.duckdbffi.driver.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestDoubleObjectColumn
{
    static Database DbForTestRun;

    @BeforeAll
    static void setupDB() throws DuckDbException
    {
        DbForTestRun = new Database(":memory:");
        Connection con = DbForTestRun.getConnection();

        con.query("CREATE TABLE DoubleTest (ID INT4, NullValue Double);");
        con.query("INSERT INTO DoubleTest (SELECT rng, rng::Double * 1.123  FROM (SELECT * AS rng FROM RANGE(10000)));");
        con.query("INSERT INTO DoubleTest (ID) VALUES (-1);");
    }

    @AfterAll
    static void closeDB() throws DuckDbException
    {
        DbForTestRun.close();
    }

    @Test
    void testDoubleArray()
    {
        Double[] compArray = new Double[] {11228.877, 11227.754, 11226.631, 11225.508, 11224.385};

        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM DoubleTest ORDER BY id DESC LIMIT 5;", true);

            DoubleObjectColumn Col = (DoubleObjectColumn)res.Columns.getFirst();

            assertEquals(0, Arrays.compare(compArray, Col.VectorArray));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testDoubleRows()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM DoubleTest WHERE ID >= 0 ORDER BY id LIMIT 3000;", true);

            for (int row = 0; row < 3000; row++)
            {
                assertEquals((Double)res.getRow(row).getFirst(), (double)row * 1.123);
            }
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testDoubleNull()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT ID, NullValue FROM DoubleTest WHERE ID = -1;", true);

            assertNull(res.getRow(0).get(1));
            assertEquals(-1, res.getRow(0).get(0));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }
}

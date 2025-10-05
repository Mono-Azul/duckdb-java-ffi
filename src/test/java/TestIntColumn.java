package test.java;

import mau.duckdbffi.driver.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class TestIntColumn
{
    static Database DbForTestRun;

    @BeforeAll
    static void setupDB() throws DuckDbException
    {
        DbForTestRun = new Database(":memory:");
        Connection con = DbForTestRun.getConnection();

        con.query("CREATE TABLE IntTest (ID INT4, NullValue INT4);");
        con.query("INSERT INTO IntTest (SELECT *, 1 FROM RANGE(10000));");
        con.query("INSERT INTO IntTest (ID) VALUES (-1);");
    }

    @AfterAll
    static void closeDB() throws DuckDbException
    {
        DbForTestRun.close();
    }

    @Test
    void testIntArray()
    {
        int[] compArray = new int[] {9999, 9998, 9997, 9996, 9995};

        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT ID, NullValue FROM IntTest ORDER BY id DESC LIMIT 5;");

            IntColumn Col = (IntColumn)res.Columns.getFirst();

            assertEquals(0, Arrays.compare(compArray, Col.VectorArray));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testIntRows()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT ID, NullValue FROM IntTest WHERE ID >= 0 ORDER BY id LIMIT 3000;");

            for (int row = 0; row < 3000; row++)
            {
                assertEquals(row, (int)res.getRow(row).getFirst());
            }
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testIntNull()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("select ID, NullValue from IntTest WHERE ID = -1;");

            assertNull(res.getRow(0).get(1));
            assertEquals(-1, res.getRow(0).get(0));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }
}

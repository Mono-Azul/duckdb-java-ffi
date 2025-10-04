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
        con.query("INSERT INTO IntTest (SELECT *, 1 FROM RANGE(100));");
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
        int[] compArray = new int[] {99, 98, 97, 96, 95};

        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("select ID, NullValue from IntTest order by id desc limit 5;");

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
        int[] compArray = new int[] {99, 98, 97, 96, 95};

        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("select ID, NullValue from IntTest order by id desc limit 5;");

            for (int row = 0; row < 5; row++)
            {
                assertEquals(compArray[row], (int)res.getRow(row).getFirst());
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
            Result res = con.query("select ID, NullValue from IntTest order by id asc limit 5;");

            assertNull(res.getRow(0).get(1));
            assertEquals(1, res.getRow(1).get(1));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }
}

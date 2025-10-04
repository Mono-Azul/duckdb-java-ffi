package test.java;

import mau.duckdbffi.driver.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestPrimitiveValidityMask
{
    static Database DbForTestRun;

    @BeforeAll
    static void setupDB() throws DuckDbException
    {
        DbForTestRun = new Database(":memory:");
        Connection con = DbForTestRun.getConnection();

        con.query("CREATE TABLE TestValidityMask (ID INT4, NullValue INT4);");
        con.query("INSERT INTO TestValidityMask (SELECT *, * FROM RANGE(10000));");

        // Add some NULL values
        con.query("UPDATE TestValidityMask SET NullValue = null WHERE ID = 99;");
        con.query("UPDATE TestValidityMask SET NullValue = null WHERE ID = 2857;");
        con.query("UPDATE TestValidityMask SET NullValue = null WHERE ID = 5007;");
        con.query("UPDATE TestValidityMask SET NullValue = null WHERE ID = 9558;");
        con.query("UPDATE TestValidityMask SET NullValue = null WHERE ID = 9559;");

        // Add some uneven rows
        con.query("INSERT INTO TestValidityMask (SELECT id + 10000, id + 10000 FROM (SELECT * AS id FROM RANGE(777)));");
        con.query("INSERT INTO TestValidityMask (SELECT id + 10777, id + 10777 FROM (SELECT * AS id FROM RANGE(1598)));");
        
        // One more NULL
        con.query("UPDATE TestValidityMask SET NullValue = null WHERE ID = 12001;");
    }

    @AfterAll
    static void closeDB() throws DuckDbException
    {
        DbForTestRun.close();
    }

//    @Test
//    void testNoMask()
//    {
//        try (Connection con = DbForTestRun.getConnection())
//        { // We need a better query
//            Result res = con.query("SELECT ID, NullValue FROM TestValidityMask WHERE ID = 99999;");
//
//            IntColumn Col = (IntColumn)res.Columns.getFirst();
//
//            assertNull(Col.ValidityMask);
//        } catch (DuckDbException e)
//        {
//            throw new RuntimeException(e);
//        }
//    }

    @Test
    void testFullMask()           
    {
        var compArray = new Integer[] {99, 2857, 5007, 9558, 9559, -1};
        var NullListIter = new ArrayList<>(Arrays.asList(compArray)).listIterator();

        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM TestValidityMask ORDER BY ID LIMIT 10000;");

            IntColumn Col = (IntColumn)res.Columns.getFirst();

            Integer ActualNullValue = NullListIter.next();

            for (int row = 0; row < 10000; row++)
            {
                var ActualValue = (Integer)(res.getRow(row)).getFirst();

                if (row == ActualNullValue)
                {
                    assertNull(ActualValue);
                    ActualNullValue = NullListIter.next();
                }
                else
                {
                    assertEquals(row, ActualValue);
                }
            }
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testMaskParts()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM TestValidityMask ORDER BY ID;");

            IntColumn Col = (IntColumn)res.Columns.getFirst();

            for (int row = 10000; row < 12375; row++)
            {
                var ActualValue = (Integer)(res.getRow(row)).getFirst();

                if (row == 12001)
                {
                    assertNull(ActualValue);
                }
                else
                {
                    assertEquals(row, ActualValue);
                }
            }
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }
}

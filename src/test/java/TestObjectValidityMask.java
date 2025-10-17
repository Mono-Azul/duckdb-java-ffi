/*
 * duckdb-java-ffi - A DuckDB (non JDBC) Java client using FFI
 *
 * Copyright (C) 2025  Jens Hofer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package test.java;

import mau.duckdbffi.driver.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestObjectValidityMask
{
    static Database DbForTestRun;

    @BeforeAll
    static void setupDB() throws DuckDbException
    {
        DbForTestRun = new Database(":memory:");
        Connection con = DbForTestRun.getConnection();

        con.query("CREATE TABLE TestValidityMask (ID INT8, NullValue INT2);");
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

    @Test
    void testFullMask()           
    {
        var compArray = new Short[] {99, 2857, 5007, 9558, 9559, -1};
        var NullListIter = new ArrayList<>(Arrays.asList(compArray)).listIterator();

        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM TestValidityMask ORDER BY ID LIMIT 10000;", true);

            ShortObjectColumn Col = (ShortObjectColumn)res.Columns.getFirst();

            Short ActualNullValue = NullListIter.next();

            for (short row = 0; row < 10000; row++)
            {
                var ActualValue = (Short)(res.getRow(row)).getFirst();

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
            Result res = con.query("SELECT NullValue, ID FROM TestValidityMask ORDER BY ID;", true);

            ShortObjectColumn Col = (ShortObjectColumn)res.Columns.getFirst();

            for (short row = 10000; row < 12375; row++)
            {
                var ActualValue = (Short)(res.getRow(row)).getFirst();

                if (row == 12001)
                {
                    assertNull(ActualValue);
                }
                else
                {
                    assertEquals(Short.valueOf(row), ActualValue);
                }
            }
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }
}

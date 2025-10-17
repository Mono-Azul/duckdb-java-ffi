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

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class TestBooleanObjectColumn
{
    static Database DbForTestRun;

    @BeforeAll
    static void setupDB() throws DuckDbException
    {
        DbForTestRun = new Database(":memory:");
        Connection con = DbForTestRun.getConnection();

        con.query("CREATE TABLE BooleanTest (ID INT4, NullValue BOOLEAN);");
        con.query("INSERT INTO BooleanTest (SELECT rng, rng > 1000 FROM (SELECT * AS rng FROM RANGE(10000)));");
        con.query("INSERT INTO BooleanTest (ID) VALUES (-1);");
    }

    @AfterAll
    static void closeDB() throws DuckDbException
    {
        DbForTestRun.close();
    }

    @Test
    void testBoolArray()
    {
        Boolean[] compArray = new Boolean[] {true, true, true, true, true};

        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM BooleanTest ORDER BY id DESC LIMIT 5;", true);

            BooleanObjectColumn Col = (BooleanObjectColumn)res.Columns.getFirst();

            assertEquals(0, Arrays.compare(compArray, Col.VectorArray));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testBoolRows()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM BooleanTest WHERE ID >= 0 ORDER BY id LIMIT 3000;", true);

            for (int row = 0; row < 3000; row++)
            {
                if (row > 1000)
                {
                    assertTrue((Boolean)res.getRow(row).getFirst());
                }
                else
                {
                    assertFalse((Boolean)res.getRow(row).getFirst());
                }
            }
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testBoolNull()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT ID, NullValue FROM BooleanTest WHERE ID = -1;", true);

            assertNull(res.getRow(0).get(1));
            assertEquals(-1, res.getRow(0).get(0));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }
}

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestFloatColumn
{
    static Database DbForTestRun;

    @BeforeAll
    static void setupDB() throws DuckDbException
    {
        DbForTestRun = new Database(":memory:");
        Connection con = DbForTestRun.getConnection();

        con.query("CREATE TABLE FloatTest (ID INT4, NullValue FLOAT);");
        con.query("INSERT INTO FloatTest (SELECT rng, rng::float * 1.123  FROM (SELECT * AS rng FROM RANGE(10000)));");
        con.query("INSERT INTO FloatTest (ID) VALUES (-1);");
    }

    @AfterAll
    static void closeDB() throws DuckDbException
    {
        DbForTestRun.close();
    }

    @Test
    void testFloatArray()
    {
        float[] compArray = new float[] {11228.877F, 11227.754F, 11226.631F, 11225.508F, 11224.386F};

        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM FloatTest ORDER BY id DESC LIMIT 5;");

            FloatColumn Col = (FloatColumn)res.Columns.getFirst();

            assertEquals(0, Arrays.compare(compArray, Col.VectorArray));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testFloatRows()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM FloatTest WHERE ID >= 0 ORDER BY id LIMIT 3000;");

            for (int row = 0; row < 3000; row++)
            {
                assertEquals((Float)res.getRow(row).getFirst(), (float)row * 1.123F);
            }
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testFloatNull()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT ID, NullValue FROM FloatTest WHERE ID = -1;");

            assertNull(res.getRow(0).get(1));
            assertEquals(-1, res.getRow(0).get(0));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }
}

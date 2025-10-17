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

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.chrono.ChronoPeriod;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestIntervalColumn
{
    static Database DbForTestRun;

    @BeforeAll
    static void setupDB() throws DuckDbException
    {
        DbForTestRun = new Database(":memory:");
        Connection con = DbForTestRun.getConnection();

        con.query("CREATE TABLE IntervalTest (ID INT4, NullValue INTERVAL);");
        con.query("INSERT INTO IntervalTest (SELECT rng, INTERVAL (rng) DAY + INTERVAL (rng) SECOND FROM "
            + "(SELECT * AS rng FROM RANGE(10000)));");
        con.query("INSERT INTO IntervalTest (ID) VALUES (-1);");
    }

    @AfterAll
    static void closeDB() throws DuckDbException
    {
        DbForTestRun.close();
    }

    @Test
    void testIntervalArray()
    {
        Interval[] compArray = new Interval[5];
        compArray[0] = new Interval(Period.of(0, 0, 9999), Duration.ofSeconds(9999));
        compArray[1] = new Interval(Period.of(0, 0, 9998), Duration.ofSeconds(9998));
        compArray[2] = new Interval(Period.of(0, 0, 9997), Duration.ofSeconds(9997));
        compArray[3] = new Interval(Period.of(0, 0, 9996), Duration.ofSeconds(9996));
        compArray[4] = new Interval(Period.of(0, 0, 9995), Duration.ofSeconds(9995));

        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM IntervalTest ORDER BY id DESC LIMIT 5;");

            IntervalColumn Col = (IntervalColumn)res.Columns.getFirst();

            assertEquals(0, Arrays.compare(compArray, Col.VectorArray, Interval.COMPARATOR));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testIntervalRows()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM IntervalTest WHERE ID >= 0 ORDER BY id LIMIT 3000;");

            for (int row = 0; row < 3000; row++)
            {
                assertEquals((Interval)res.getRow(row).getFirst(),
                        new Interval(Period.of(0, 0, row), Duration.ofSeconds(row)));
                //System.out.println(res.getRow(row).get(1));
            }
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testIntervalNull()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT ID, NullValue FROM IntervalTest WHERE ID = -1;");

            assertNull(res.getRow(0).get(1));
            assertEquals(-1, res.getRow(0).get(0));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }
}

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

import java.math.BigInteger;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestBigIntegerColumn
{
    static Database DbForTestRun;

    @BeforeAll
    static void setupDB() throws DuckDbException
    {
        DbForTestRun = new Database(":memory:");
        Connection con = DbForTestRun.getConnection();

        con.query("CREATE TABLE BigIntTest (ID INT4, NullValue INT128);");
        con.query("INSERT INTO BigIntTest (SELECT rng, rng::hugeint + 10000000000 FROM (SELECT * AS rng FROM RANGE(10000)));");
        con.query("INSERT INTO BigIntTest (ID) VALUES (-1);");
    }

    @AfterAll
    static void closeDB() throws DuckDbException
    {
        DbForTestRun.close();
    }

    @Test
    void testBigIntegerArray()
    {
        BigInteger[] compArray = new BigInteger[5];
        compArray[0] = new BigInteger("10000009999");
        compArray[1] = new BigInteger("10000009998");
        compArray[2] = new BigInteger("10000009997");
        compArray[3] = new BigInteger("10000009996");
        compArray[4] = new BigInteger("10000009995");

        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM BigIntTest ORDER BY id DESC LIMIT 5;");

            BigIntegerColumn Col = (BigIntegerColumn)res.Columns.getFirst();

            assertEquals(0, Arrays.compare(compArray, Col.VectorArray));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testBigIntegerRows()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM BigIntTest WHERE ID >= 0 ORDER BY id LIMIT 3000;");

            for (BigInteger row = BigInteger.ZERO; row.compareTo(BigInteger.ZERO) > 0; row = row.subtract(BigInteger.ONE))
            {
                assertEquals((BigInteger)res.getRow(row.intValue()).getFirst(), row);
            }
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testBigIntegerNull()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT ID, NullValue FROM BigIntTest WHERE ID = -1;");

            assertNull(res.getRow(0).get(1));
            assertEquals(-1, res.getRow(0).get(0));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }
}

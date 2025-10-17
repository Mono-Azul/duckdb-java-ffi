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

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestDecimalColumn
{
    static Database DbForTestRun;

    @BeforeAll
    static void setupDB() throws DuckDbException
    {
        DbForTestRun = new Database(":memory:");
        Connection con = DbForTestRun.getConnection();

        con.query("CREATE TABLE DecimalTestSmall (ID INT4, NullValue DECIMAL(4,1));");
        con.query("INSERT INTO DecimalTestSmall (select rng, rng::decimal(4,1) * 0.1 from (select * as rng from range(100)));");

        con.query("CREATE TABLE DecimalTest (ID INT4, NullValue4 DECIMAL(9,3), NullValue8 DECIMAL(15,4), NullValue16 DECIMAL(20,8));");
        con.query("INSERT INTO DecimalTest (select rng, rng::decimal(9,3) * 0.001, rng::decimal(15,4) * 0.001, rng::decimal(20,8) * 0.001 "
                + "from (select * as rng from range(10000)));");

        con.query("INSERT INTO DecimalTest (ID) VALUES (-1);");
    }

    @AfterAll
    static void closeDB() throws DuckDbException
    {
        DbForTestRun.close();
    }

    @Test
    void testDecimal2Array()
    {
        BigDecimal[] compArray = new BigDecimal[5];
        compArray[0] = new BigDecimal("9.9");
        compArray[1] = new BigDecimal("9.8");
        compArray[2] = new BigDecimal("9.7");
        compArray[3] = new BigDecimal("9.6");
        compArray[4] = new BigDecimal("9.5");

        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("select ID, NullValue from DecimalTestSmall order by id desc limit 5;");

            DecimalColumn Col = (DecimalColumn)res.Columns.get(1);

            assertEquals(0, Arrays.compare(compArray, Col.VectorArray));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testDecimal4Array()
    {
        BigDecimal[] compArray = new BigDecimal[5];
        compArray[0] = new BigDecimal("9.999");
        compArray[1] = new BigDecimal("9.998");
        compArray[2] = new BigDecimal("9.997");
        compArray[3] = new BigDecimal("9.996");
        compArray[4] = new BigDecimal("9.995");

        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("select ID, NullValue4, NullValue8, NullValue16 from DecimalTest order by id desc limit 5;");

            DecimalColumn Col = (DecimalColumn)res.Columns.get(1);

            assertEquals(0, Arrays.compare(compArray, Col.VectorArray));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testDecimal8Array()
    {
        BigDecimal[] compArray = new BigDecimal[5];
        compArray[0] = new BigDecimal("9.999");
        compArray[1] = new BigDecimal("9.998");
        compArray[2] = new BigDecimal("9.997");
        compArray[3] = new BigDecimal("9.996");
        compArray[4] = new BigDecimal("9.995");

        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("select ID, NullValue4, NullValue8, NullValue16 from DecimalTest order by id desc limit 5;");

            DecimalColumn Col = (DecimalColumn)res.Columns.get(2);

            assertEquals(0, Arrays.compare(compArray, Col.VectorArray));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testDecimal16Array()
    {
        BigDecimal[] compArray = new BigDecimal[5];
        compArray[0] = new BigDecimal("9.999");
        compArray[1] = new BigDecimal("9.998");
        compArray[2] = new BigDecimal("9.997");
        compArray[3] = new BigDecimal("9.996");
        compArray[4] = new BigDecimal("9.995");

        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("select ID, NullValue4, NullValue8, NullValue16 from DecimalTest order by id desc limit 5;");

            DecimalColumn Col = (DecimalColumn)res.Columns.get(3);

            assertEquals(0, Arrays.compare(compArray, Col.VectorArray));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testDecimalNull()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("select ID, NullValue4, NullValue8, NullValue16 from DecimalTest WHERE ID = -1;");

            assertNull(res.getRow(0).get(1));
            assertNull(res.getRow(0).get(2));
            assertNull(res.getRow(0).get(3));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }
}

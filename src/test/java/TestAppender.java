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
import java.math.BigInteger;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TestAppender
{
    static Database DbForTestRun;

    @BeforeAll
    static void setupDB() throws DuckDbException
    {
        DbForTestRun = new Database(":memory:");
        Connection con = DbForTestRun.getConnection();

        // Table with relevant data type columns
        con.query("""
            CREATE TABLE AppenderTest (ID INT8, Tiny INT1, Small INT2, Medium INT4, Large INT8, Huge INT128,
                BitBool BOOL, TinyDec DECIMAL(4,2), SmallDec DECIMAL(7,3), MediumDec DECIMAL(15,4),
                BigDec DECIMAL(30,10), FloatPt FLOAT, DoublePt DOUBLE, LocTime TIME, LocDate DATE, LocTs TIMESTAMP,
                Instant TIMESTAMPTZ, Intv INTERVAL, Str STRING, UniqueId UUID, NullValue INT8 NULL,
                DefaultValue STRING DEFAULT 'default');
            """);
    }

    @AfterAll
    static void closeDB() throws DuckDbException
    {
        DbForTestRun.close();
    }

    @Test
    void testAppendObjects()
    {
        try (Connection con = DbForTestRun.getConnection();
             Appender appender = con.createAppender("AppenderTest"))
        {
            appender.beginRow();
            appender.appendValue((long)-10);
            appender.appendValue((byte)0);
            appender.appendValue((short)2);
            appender.appendValue(3);
            appender.appendValue(5564888779L);
            appender.appendValue(BigInteger.valueOf(16548887789L));
            appender.appendValue(true);
            appender.appendValue(BigDecimal.valueOf(3214, 2));
            appender.appendValue(BigDecimal.valueOf(321456, 3));
            appender.appendValue(BigDecimal.valueOf(99999999, 4));
            appender.appendValue(BigDecimal.valueOf(123456789012345L, 10));
            appender.appendValue((float)1234.5678);
            appender.appendValue((double)234567.89101);
            appender.appendValue(LocalTime.now());
            appender.appendValue(LocalDate.now());
            appender.appendValue(LocalDateTime.now());
            appender.appendValue(Instant.now());
            appender.appendValue(new Interval(Period.of(2, 12, 4), Duration.ofMillis(60000*18123)));
            appender.appendValue("a text value xväö;!:{");
            appender.appendValue(UUID.randomUUID());
            appender.appendNull();
            appender.appendDefault();
            appender.endRow();

            appender.beginRow();
            appender.appendValue((long)-10);
            appender.appendValue((byte)0);
            appender.appendValue((short)2);
            appender.appendValue(3);
            appender.appendValue(Long.valueOf(5564888779L));
            appender.appendValue(BigInteger.valueOf(16548887789L));
            appender.appendValue(Boolean.valueOf(true));
            appender.appendValue(BigDecimal.valueOf(3214, 2));
            appender.appendValue(BigDecimal.valueOf(321456, 3));
            appender.appendValue(BigDecimal.valueOf(99999999, 4));
            appender.appendValue(BigDecimal.valueOf(123456789012345L, 10));
            appender.appendValue(Float.valueOf((float)1234.5678));
            appender.appendValue(Double.valueOf((double)234567.89101));
            appender.appendValue(LocalTime.now());
            appender.appendValue(LocalDate.now());
            appender.appendValue(LocalDateTime.now());
            appender.appendValue(Instant.now());
            appender.appendValue(new Interval(Period.of(2, 12, 4), Duration.ofMillis(60000*18123)));
            appender.appendValue("a text value xväö;!:{");
            appender.appendValue(UUID.randomUUID());
            appender.appendNull();
            appender.appendDefault();
            appender.endRow();
            appender.flush();
            appender.beginRow();
            appender.appendValue((byte)-10);
            appender.appendValue((byte)0);
            appender.appendValue((byte)2);
            appender.appendValue((byte)3);
            appender.appendValue(5564888779L);
            appender.appendValue(BigInteger.valueOf(16548887789L));
            appender.appendValue(true);
            appender.appendValue(BigDecimal.valueOf(3214, 2));
            appender.appendValue(BigDecimal.valueOf(321456, 3));
            appender.appendValue(BigDecimal.valueOf(99999999, 4));
            appender.appendValue(BigDecimal.valueOf(123456789012345L, 10));
            appender.appendValue((float)1234.5678);
            appender.appendValue((double)234567.89101);
            appender.appendValue(LocalTime.now());
            appender.appendValue(LocalDate.now());
            appender.appendValue(LocalDateTime.now());
            appender.appendValue(Instant.now());
            appender.appendValue(new Interval(Period.of(2, 12, 4), Duration.ofMillis(60000*18123)));
            appender.appendValue("a text value xväö;!:{");
            appender.appendValue(UUID.randomUUID());
            appender.appendNull();
            appender.appendDefault();
            appender.endRow();
        }
        catch (DuckDbException e)
        {
            fail("DuckDbException thrown:" + e.toString());
        }

        try (Connection con = DbForTestRun.getConnection())
        {
            var Res = con.query("SELECT COUNT(*) FROM AppenderTest WHERE ID = -10;");

            assertEquals(1, Res.getRowCount());
            assertEquals(3L, Res.getRow(0).getFirst());
        } catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testAppendPrimitives()
    {
        var LocTime = LocalTime.now().truncatedTo(ChronoUnit.MICROS);
        var LocDate = LocalDate.now();
        var LocDt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        var Inst = Instant.now().truncatedTo(ChronoUnit.MICROS);
        var uuid = UUID.randomUUID();

        try (Connection con = DbForTestRun.getConnection();
             Appender appender = con.createAppender("AppenderTest"))
        {
            appender.beginRow();
            appender.appendLong(99);
            appender.appendByte((byte)0);
            appender.appendShort((short)2);
            appender.appendInt(3);
            appender.appendLong(5564888779L);
            appender.appendValue(BigInteger.valueOf(16548887789L));
            appender.appendBool(true);
            appender.appendValue(BigDecimal.valueOf(3214, 2));
            appender.appendValue(BigDecimal.valueOf(321456, 3));
            appender.appendValue(BigDecimal.valueOf(99999999, 4));
            appender.appendValue(BigDecimal.valueOf(123456789012345L, 10));
            appender.appendFloat((float)1234.5678);
            appender.appendDouble((double)234567.89101);
            appender.appendValue(LocTime);
            appender.appendValue(LocDate);
            appender.appendValue(LocDt);
            appender.appendValue(Inst);
            appender.appendValue(new Interval(Period.of(2, 12, 4), Duration.ofMillis(60000*18123)));
            appender.appendValue("a text value xväö;!:{");
            appender.appendValue(uuid);
            appender.appendNull();
            appender.appendDefault();
            appender.endRow();
        }
        catch (DuckDbException e)
        {
            fail("DuckDbException thrown:" + e.toString());
        }

        try (Connection con = DbForTestRun.getConnection())
        {
            var Res = con.query("SELECT COUNT(*) FROM AppenderTest WHERE ID = 99;");

            assertEquals(1, Res.getRowCount());
            assertEquals(1L, Res.getRow(0).getFirst());

            var Res2 = con.query("""
                    SELECT ID, Tiny, Small, Medium, Large, Huge, BitBool, TinyDec, SmallDec, MediumDec, BigDec, FloatPt
                            , DoublePt, LocTime, LocDate, LocTs, Instant, Intv, Str, UniqueId, NullValue, DefaultValue
                        FROM AppenderTest WHERE ID = 99;
                    """);

            List ResultRow = Res2.getRow(0);

            assertEquals(99L, (Long)ResultRow.get(0));
            assertEquals((byte)0, (Byte)ResultRow.get(1));
            assertEquals((short)2, (Short)ResultRow.get(2));
            assertEquals(3, (Integer)ResultRow.get(3));
            assertEquals(5564888779L, (Long)ResultRow.get(4));
            assertEquals(BigInteger.valueOf(16548887789L), (BigInteger)ResultRow.get(5));
            assertEquals(true, (Boolean)ResultRow.get(6));
            assertEquals(BigDecimal.valueOf(3214, 2), (BigDecimal)ResultRow.get(7));
            assertEquals(BigDecimal.valueOf(321456, 3), (BigDecimal)ResultRow.get(8));
            assertEquals(BigDecimal.valueOf(99999999, 4), (BigDecimal)ResultRow.get(9));
            assertEquals(BigDecimal.valueOf(123456789012345L, 10), (BigDecimal)ResultRow.get(10));
            assertEquals((float)1234.5678, (Float)ResultRow.get(11));
            assertEquals((double)234567.89101, (Double)ResultRow.get(12));
            assertEquals(LocTime, (LocalTime)ResultRow.get(13));
            assertEquals(LocDate, (LocalDate)ResultRow.get(14));
            assertEquals(LocDt, (LocalDateTime)ResultRow.get(15));
            assertEquals(Inst, (Instant)ResultRow.get(16));
            assertEquals(0, Interval.COMPARATOR.compare(((Interval)ResultRow.get(17)).normalized(),
                    (new Interval(Period.of(2, 12, 4), Duration.ofMillis(60000*18123))).normalized()));
            assertEquals("a text value xväö;!:{", (String)ResultRow.get(18));
            assertEquals(uuid, (UUID)ResultRow.get(19));
            assertNull(ResultRow.get(20));
            assertEquals("default", (String)ResultRow.get(21));
        } catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }
}

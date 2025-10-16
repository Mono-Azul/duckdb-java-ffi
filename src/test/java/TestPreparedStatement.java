package test.java;

import mau.duckdbffi.driver.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.Period;

import static org.junit.jupiter.api.Assertions.*;

public class TestPreparedStatement
{
    static Database DbForTestRun;

    @BeforeAll
    static void setupDB() throws DuckDbException
    {
        DbForTestRun = new Database(":memory:");
        Connection con = DbForTestRun.getConnection();

        // Table with relevant data type columns
        con.query("""
            CREATE TABLE PrepStmtTest (ID INT8, Tiny INT1, Small INT2, Medium INT4, Large INT8, Huge INT128,
                BitBool BOOL, TinyDec DECIMAL(4,2), SmallDec DECIMAL(7,3), MediumDec DECIMAL(15,4),
                BigDec DECIMAL(30,10), FloatPt FLOAT, DoublePt DOUBLE, LocTime TIME, LocDate DATE, LocTs TIMESTAMP,
                Instant TIMESTAMPTZ, Intv INTERVAL, Str STRING, UniqueId UUID);
            """);
    }

    @AfterAll
    static void closeDB() throws DuckDbException
    {
        DbForTestRun.close();
    }

    @Test
    void testBoolean()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            List Paras = new ArrayList<>();
            Paras.add(Boolean.TRUE);

            Result res = con.queryWithParameters("INSERT INTO PrepStmtTest (BitBool) VALUES (?);", Paras);

            assertFalse(res.hasError());

            res = con.queryWithParameters("SELECT BitBool FROM PrepStmtTest WHERE BitBool = ?;", Paras);

            assertFalse(res.hasError());
            assertEquals(res.getRow(0).getFirst(), Paras.getFirst());
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testInt1()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            List Paras = new ArrayList<>();
            Paras.add((byte)-158);

            Result res = con.queryWithParameters("INSERT INTO PrepStmtTest (Tiny) VALUES (?);", Paras);

            assertFalse(res.hasError());

            res = con.queryWithParameters("SELECT Tiny FROM PrepStmtTest WHERE Tiny = ?;", Paras);

            assertFalse(res.hasError());
            assertEquals(res.getRow(0).getFirst(), Paras.getFirst());
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testInt2()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            List Paras = new ArrayList<>();
            Paras.add((short)9999);

            Result res = con.queryWithParameters("INSERT INTO PrepStmtTest (Small) VALUES (?);", Paras);

            assertFalse(res.hasError());

            res = con.queryWithParameters("SELECT Small FROM PrepStmtTest WHERE Small = ?;", Paras);

            assertFalse(res.hasError());
            assertEquals(res.getRow(0).getFirst(), Paras.getFirst());
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testInt4()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            List Paras = new ArrayList<>();
            Paras.add(-222229999);

            Result res = con.queryWithParameters("INSERT INTO PrepStmtTest (Medium) VALUES (?);", Paras);

            assertFalse(res.hasError());

            res = con.queryWithParameters("SELECT Medium FROM PrepStmtTest WHERE Medium = ?;", Paras);

            assertFalse(res.hasError());
            assertEquals(res.getRow(0).getFirst(), Paras.getFirst());
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testInt8()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            List Paras = new ArrayList<>();
            Paras.add(9999888877776666L);

            Result res = con.queryWithParameters("INSERT INTO PrepStmtTest (Large) VALUES (?);", Paras);

            assertFalse(res.hasError());

            res = con.queryWithParameters("SELECT Large FROM PrepStmtTest WHERE Large = ?;", Paras);

            assertFalse(res.hasError());
            assertEquals(res.getRow(0).getFirst(), Paras.getFirst());
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testBigInt()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            List Paras = new ArrayList<>();
            var BigInt = BigInteger.valueOf(-12345678910L).multiply(BigInteger.valueOf(999999999L));
            Paras.add(BigInt);

            Result res = con.queryWithParameters("INSERT INTO PrepStmtTest (Huge) VALUES (?);", Paras);

            assertFalse(res.hasError());

            res = con.queryWithParameters("SELECT Huge FROM PrepStmtTest WHERE Huge = ?;", Paras);

            assertFalse(res.hasError());
            assertEquals(res.getRow(0).getFirst(), Paras.getFirst());
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testFloat()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            List Paras = new ArrayList<>();
            Paras.add((float)1234.5678);

            Result res = con.queryWithParameters("INSERT INTO PrepStmtTest (FloatPt) VALUES (?);", Paras);

            assertFalse(res.hasError());

            res = con.queryWithParameters("SELECT FloatPt FROM PrepStmtTest WHERE FloatPt = ?;", Paras);

            assertFalse(res.hasError());
            assertEquals(res.getRow(0).getFirst(), Paras.getFirst());
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testDouble()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            List Paras = new ArrayList<>();
            Paras.add((double)112999988887777.6666);

            Result res = con.queryWithParameters("INSERT INTO PrepStmtTest (DoublePt) VALUES (?);", Paras);

            assertFalse(res.hasError());

            res = con.queryWithParameters("SELECT DoublePt FROM PrepStmtTest WHERE DoublePt = ?;", Paras);

            assertFalse(res.hasError());
            assertEquals(res.getRow(0).getFirst(), Paras.getFirst());
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testBigDecimal()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            List Paras = new ArrayList<>();
            var TinyBigDec = new BigDecimal("-12.5");
            var SmallBigDec = new BigDecimal("-8877.65");
            var MediumBigDec = new BigDecimal("156548779.456");
            var LargeBigDec = new BigDecimal("-3216549874489.55648879");
            Paras.add(TinyBigDec);
            Paras.add(SmallBigDec);
            Paras.add(MediumBigDec);
            Paras.add(LargeBigDec);

            Result res = con.queryWithParameters("""
                INSERT INTO PrepStmtTest (TinyDec, SmallDec, MediumDec, BigDec) VALUES (?, ?, ?, ?);
                """, Paras);

            assertFalse(res.hasError());

            res = con.queryWithParameters("""
            SELECT TinyDec, SmallDec, MediumDec, BigDec FROM PrepStmtTest WHERE TinyDec = ? AND SmallDec = ? 
            AND MediumDec = ? AND BigDec = ?;
            """, Paras);

            assertFalse(res.hasError());
            assertEquals(0, ((BigDecimal)res.getRow(0).getFirst()).compareTo((BigDecimal)Paras.getFirst()));
            assertEquals(0, ((BigDecimal)res.getRow(0).get(1)).compareTo((BigDecimal)Paras.get(1)));
            assertEquals(0, ((BigDecimal)res.getRow(0).get(2)).compareTo((BigDecimal)Paras.get(2)));
            assertEquals(0, ((BigDecimal)res.getRow(0).get(3)).compareTo((BigDecimal)Paras.get(3)));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testLocalTime()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            List Paras = new ArrayList<>();
            var LocTime = LocalTime.of(1, 55, 59, 555666000);
            Paras.add(LocTime);

            Result res = con.queryWithParameters("INSERT INTO PrepStmtTest (LocTime) VALUES (?);", Paras);

            assertFalse(res.hasError());

            res = con.queryWithParameters("SELECT LocTime FROM PrepStmtTest WHERE LocTime = ?;", Paras);

            assertFalse(res.hasError());
            assertEquals(res.getRow(0).getFirst(), Paras.getFirst());
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testLocalDate()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            List Paras = new ArrayList<>();
            var LocDate = LocalDate.of(2025, 12, 23);
            Paras.add(LocDate);

            Result res = con.queryWithParameters("INSERT INTO PrepStmtTest (LocDate) VALUES (?);", Paras);

            assertFalse(res.hasError());

            res = con.queryWithParameters("SELECT LocDate FROM PrepStmtTest WHERE LocDate = ?;", Paras);

            assertFalse(res.hasError());
            assertEquals(res.getRow(0).getFirst(), Paras.getFirst());
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testInstant()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            List Paras = new ArrayList<>();
            var Inst = Instant.now().truncatedTo(ChronoUnit.MICROS);
            Paras.add(Inst);

            Result res = con.queryWithParameters("INSERT INTO PrepStmtTest (Instant) VALUES (?);", Paras);

            assertFalse(res.hasError());

            res = con.queryWithParameters("SELECT Instant FROM PrepStmtTest WHERE Instant = ?;", Paras);

            assertFalse(res.hasError());
            assertEquals(res.getRow(0).getFirst(), Paras.getFirst());
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testInterval()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            List Paras = new ArrayList<>();
            var Inv = new Interval(Period.of(2, 12, 4),
                    Duration.ofMillis(60000*18123));
            Paras.add(Inv);

            Result res = con.queryWithParameters("INSERT INTO PrepStmtTest (Intv) VALUES (?);", Paras);

            assertFalse(res.hasError());

            res = con.queryWithParameters("SELECT Intv FROM PrepStmtTest WHERE Intv = ?;", Paras);

            assertFalse(res.hasError());
            assertEquals(0, Interval.COMPARATOR.compare(((Interval)res.getRow(0).getFirst()).normalized(),
                    ((Interval)Paras.getFirst()).normalized()));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testString()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            List Paras = new ArrayList<>();
            var Str = "";
            Paras.add(Str);

            Result res = con.queryWithParameters("INSERT INTO PrepStmtTest (Str) VALUES (?);", Paras);

            assertFalse(res.hasError());

            res = con.queryWithParameters("SELECT Str FROM PrepStmtTest WHERE Str = ?;", Paras);

            assertFalse(res.hasError());
            assertEquals(res.getRow(0).getFirst(), Paras.getFirst());
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testUUID()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            List Paras = new ArrayList<>();
            var Uuid = UUID.randomUUID();
            Paras.add(Uuid);

            Result res = con.queryWithParameters("INSERT INTO PrepStmtTest (UniqueId) VALUES (?);", Paras);

            assertFalse(res.hasError());

            res = con.queryWithParameters("SELECT UniqueId FROM PrepStmtTest WHERE UniqueId = ?;", Paras);

            assertFalse(res.hasError());
            assertEquals(res.getRow(0).getFirst(), Paras.getFirst());
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }
}

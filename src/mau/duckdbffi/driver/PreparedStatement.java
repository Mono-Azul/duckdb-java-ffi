package mau.duckdbffi.driver;

import mau.duckdbffi.jextractffi.duckdb_decimal;
import mau.duckdbffi.jextractffi.duckdb_h;
import mau.duckdbffi.jextractffi.duckdb_interval;
import mau.duckdbffi.jextractffi.duckdb_result;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

import static mau.duckdbffi.jextractffi.duckdb_h.*;

public class PreparedStatement implements AutoCloseable
{
    private final Arena PreparedStatementArena;
    private final MemorySegment PreparedStmtSegment;
    private final String ErrorMessage;

    public PreparedStatement(MemorySegment DuckDbConnection, String sql)
    {
        PreparedStatementArena = Arena.ofConfined();

        MemorySegment PreparedStmtSegmentPtr = PreparedStatementArena.allocate(8)
                .reinterpret(PreparedStatementArena, duckdb_h::duckdb_destroy_prepare);
        int duckDbState = duckdb_prepare(DuckDbConnection, PreparedStatementArena.allocateFrom(sql)
                , PreparedStmtSegmentPtr);

        this.PreparedStmtSegment = PreparedStmtSegmentPtr.get(C_POINTER, 0);
        String ErrorMessage = null;

        if (duckDbState == DuckDBError())
        {
            System.out.println("Error preparing query: " + sql);
            ErrorMessage = duckdb_prepare_error(PreparedStmtSegment).getString(0);
        }
        this.ErrorMessage = ErrorMessage;
    }

    public int bindObject(Object BindValue, int pos)
    {
        int duckDbState = 0;

        // Special case for NULL
        if (BindValue == null)
        {
             return duckdb_bind_null(PreparedStmtSegment, pos);
        }

        Arena BindArena = Arena.ofConfined();

        switch(BindValue)
        {
            case Boolean bo -> duckDbState = duckdb_bind_boolean(PreparedStmtSegment, pos, bo);
            case Byte b -> duckDbState = duckdb_bind_int8(PreparedStmtSegment, pos, b);
            case Short s -> duckDbState = duckdb_bind_int16(PreparedStmtSegment, pos, s);
            case Integer i -> duckDbState = duckdb_bind_int32(PreparedStmtSegment, pos, i);
            case Long l -> duckDbState = duckdb_bind_int64(PreparedStmtSegment, pos, l);
            case BigInteger bi -> duckDbState = duckdb_bind_hugeint(PreparedStmtSegment, pos, bigInteger2Hugeint(bi));
            case BigDecimal bd -> duckDbState = duckdb_bind_decimal(PreparedStmtSegment, pos,
                    bigDecimal2Decimal(bd, BindArena));
            case Float f -> duckDbState = duckdb_bind_float(PreparedStmtSegment, pos, f);
            case Double d -> duckDbState = duckdb_bind_double(PreparedStmtSegment, pos, d);
            case LocalTime lt -> duckDbState = duckdb_bind_time(PreparedStmtSegment, pos, localTime2Long(lt));
            case LocalDate ld -> duckDbState = duckdb_bind_date(PreparedStmtSegment, pos, localDate2Int(ld));
            case LocalDateTime ldt -> duckDbState = duckdb_bind_timestamp(PreparedStmtSegment, pos,
                    localDateTime2Long(ldt));
            case Instant inst -> duckDbState = duckdb_bind_timestamp_tz(PreparedStmtSegment, pos, instant2Long(inst));
            case Interval intv -> duckDbState = duckdb_bind_interval(PreparedStmtSegment, pos,
                    interval2Interval(intv, BindArena));
            case String str -> duckDbState = duckdb_bind_varchar(PreparedStmtSegment, pos, BindArena.allocateFrom(str));
            //case UUID uuid -> duckDbState = duckdb_bind_(PreparedStmtSegment, pos, s);
            default -> duckDbState = 1;
        }

        BindArena.close();

        return duckDbState;
    }

    public Result executeStatement(boolean primitivesAsObjects)
    {
        try (Arena ResultArena = Arena.ofConfined())
        {
            // Create duckdb_result struct
            MemorySegment DuckDbResult = ResultArena.allocate(duckdb_result.sizeof())
                    .reinterpret(ResultArena, duckdb_h::duckdb_destroy_result);
            MemorySegment DuckDbResultPtr = MemorySegment.ofAddress(DuckDbResult.address());

            int duckDbState = duckdb_execute_prepared(PreparedStmtSegment, DuckDbResultPtr);

            if (duckDbState == DuckDBError())
            {
                String ErrorMessage = duckdb_result_error(DuckDbResultPtr).getString(0);
                Integer ErrorNumber = duckdb_result_error_type(DuckDbResultPtr);
                return new Result(ErrorMessage, ErrorNumber);
            }
            return new Result(DuckDbResultPtr, DuckDbResult, primitivesAsObjects);
        }
    }

    public boolean hasError()
    {
        return ErrorMessage != null;
    }

    public String getErrorMessage()
    {
        if (!hasError())
        {
            return "No errors!";
        }
        return ErrorMessage;
    }

    @Override
    public void close() throws DuckDbException
    {
        try
        {
            PreparedStatementArena.close();
        } catch (Exception e)
        {
            throw new DuckDbException(e);
        }
    }

    private MemorySegment bigInteger2Hugeint(BigInteger BigInt)
    {
        var byteArray = BigInt.toByteArray();

        byte[] swappedArray = new byte[16];

        for (int pos = 0; pos < byteArray.length; pos++)
        {
            // Swap pos 0 => array length and then going backwards to last pos in source = target 0
            swappedArray[byteArray.length - 1 - pos] = byteArray[pos];
        }

        return MemorySegment.ofArray(swappedArray);
    }

    private MemorySegment localTime2Long(LocalTime LocTime)
    {
        long[] larray = new long[] {LocTime.toNanoOfDay() / 1000};

        return MemorySegment.ofArray(larray);
    }

    private MemorySegment localDate2Int(LocalDate LocDate)
    {
        int[] larray = new int[] {(int)LocDate.getLong(ChronoField.EPOCH_DAY)};

        return MemorySegment.ofArray(larray);
    }

    private MemorySegment localDateTime2Long(LocalDateTime LocDateTime)
    {

        long[] larray = new long[] {LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC)
                .until(LocDateTime, ChronoUnit.MICROS)};

        return MemorySegment.ofArray(larray);
    }

    private MemorySegment instant2Long(Instant Inst)
    {

        long[] larray = new long[] {Instant.EPOCH.until(Inst, ChronoUnit.MICROS)};

        return MemorySegment.ofArray(larray);
    }

    private MemorySegment interval2Interval(Interval Intv, Arena BindArena)
    {
        MemorySegment IntervallSegment = BindArena.allocate(duckdb_interval.sizeof());

        IntervallSegment.set(ValueLayout.JAVA_INT, 0, (int)Intv.Period().toTotalMonths());
        IntervallSegment.set(ValueLayout.JAVA_INT, 4, Intv.Period().normalized().getDays());

        long durationMicros = Intv.Duration().getSeconds() * 1000000;
        durationMicros += Intv.Duration().getNano() / 1000;

        IntervallSegment.set(ValueLayout.JAVA_LONG, 8, durationMicros);

        return IntervallSegment;
    }

    private MemorySegment bigDecimal2Decimal(BigDecimal BigDec, Arena BindArena)
    {
        MemorySegment IntervallSegment = BindArena.allocate(duckdb_decimal.sizeof());

        IntervallSegment.set(ValueLayout.JAVA_BYTE, 0, (byte)BigDec.precision());
        IntervallSegment.set(ValueLayout.JAVA_BYTE, 1, (byte)BigDec.scale());

        var byteArray = BigDec.unscaledValue().toByteArray();

        byte[] bigDecimalAsArray = BigDec.unscaledValue().toByteArray();

        for (int pos = 0; pos < bigDecimalAsArray.length; pos++)
        {
            // Swap pos 0 => array length and then going backwards to last pos in source = target 0 (only little endian)
            // Memory layout due to alignment means the hugeint should start at byte 8
            IntervallSegment.set(ValueLayout.JAVA_BYTE, pos + 8, bigDecimalAsArray[bigDecimalAsArray.length - 1 - pos]);
        }

        return IntervallSegment;
    }
}

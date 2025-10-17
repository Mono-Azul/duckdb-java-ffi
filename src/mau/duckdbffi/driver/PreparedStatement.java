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

package mau.duckdbffi.driver;

import mau.duckdbffi.jextractffi.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;

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
        int duckDbState = 1;

        // Special case for NULL
        if (BindValue == null)
        {
             return duckdb_bind_null(PreparedStmtSegment, pos);
        }

        Arena BindArena = Arena.ofConfined();
        MemorySegment BindSegment = null;

        switch(BindValue)
        {
            case Boolean bo -> BindSegment = duckdb_create_bool(bo); //duckDbState = duckdb_bind_boolean(PreparedStmtSegment, pos, bo);
            case Byte b -> BindSegment = duckdb_create_int8(b); // duckDbState = duckdb_bind_int8(PreparedStmtSegment, pos, b);
            case Short s -> BindSegment = duckdb_create_int16(s); //duckDbState = duckdb_bind_int16(PreparedStmtSegment, pos, s);
            case Integer i -> BindSegment = duckdb_create_int32(i); //duckDbState = duckdb_bind_int32(PreparedStmtSegment, pos, i);
            case Long l -> BindSegment = duckdb_create_int64(l);//duckDbState = duckdb_bind_int64(PreparedStmtSegment, pos, l);
            case BigInteger bi -> BindSegment = duckdb_create_hugeint(bigInteger2Hugeint(bi));//duckDbState = duckdb_bind_hugeint(PreparedStmtSegment, pos, bigInteger2Hugeint(bi));
            case BigDecimal bd -> BindSegment = duckdb_create_decimal(bigDecimal2Decimal(bd, BindArena));//duckDbState = duckdb_bind_decimal(PreparedStmtSegment, pos, bigDecimal2Decimal(bd, BindArena));
            case Float f -> BindSegment = duckdb_create_float(f); //duckDbState = duckdb_bind_float(PreparedStmtSegment, pos, f);
            case Double d -> BindSegment = duckdb_create_double(d); //duckDbState = duckdb_bind_double(PreparedStmtSegment, pos, d);
            case LocalTime lt -> BindSegment = duckdb_create_time(localTime2Long(lt)); //duckDbState = duckdb_bind_time(PreparedStmtSegment, pos, localTime2Long(lt));
            case LocalDate ld -> BindSegment = duckdb_create_date(localDate2Int(ld)); //duckDbState = duckdb_bind_date(PreparedStmtSegment, pos, localDate2Int(ld));
            case LocalDateTime ldt -> BindSegment = duckdb_create_timestamp(localDateTime2Long(ldt)); //duckDbState = duckdb_bind_timestamp(PreparedStmtSegment, pos,localDateTime2Long(ldt));
            case Instant inst -> BindSegment = duckdb_create_timestamp_tz(instant2Long(inst)); //duckDbState = duckdb_bind_timestamp_tz(PreparedStmtSegment, pos, instant2Long(inst));
            case Interval intv -> BindSegment = duckdb_create_interval(interval2Interval(intv, BindArena)); //duckDbState = duckdb_bind_interval(PreparedStmtSegment, pos,interval2Interval(intv, BindArena));
            case String str -> BindSegment = duckdb_create_varchar(BindArena.allocateFrom(str)); //duckDbState = duckdb_bind_varchar(PreparedStmtSegment, pos, BindArena.allocateFrom(str));
            case UUID uuid -> BindSegment = duckdb_create_uuid(uuid2hugeint(uuid, BindArena)); //duckdb_bind_varchar(PreparedStmtSegment, pos, BindArena.allocateFrom(uuid.toString()));
            default -> duckDbState = 1;
        }

        if (BindSegment != null)
        {
            MemorySegment BindSegmentPtr = BindArena.allocate(8);
            BindSegmentPtr.set(ValueLayout.JAVA_LONG, 0, BindSegment.address());
            duckDbState = duckdb_bind_value(PreparedStmtSegment, pos, BindSegment);
            duckdb_destroy_value(BindSegmentPtr);
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

        // Negative values need an "all 1" array as basis
        if (BigInt.compareTo(BigInteger.ZERO) < 0)
        {
            Arrays.fill(swappedArray, (byte)-1); // -1 => all bits = 1
        }

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
        MemorySegment DecimaSegment = BindArena.allocate(duckdb_decimal.sizeof());

        DecimaSegment.set(ValueLayout.JAVA_BYTE, 0, (byte)BigDec.precision());
        DecimaSegment.set(ValueLayout.JAVA_BYTE, 1, (byte)BigDec.scale());

        byte[] bigDecimalAsArray = BigDec.unscaledValue().toByteArray();

        for (int pos = 0; pos < bigDecimalAsArray.length; pos++)
        {
            // Swap pos 0 => array length and then going backwards to last pos in source = target 0 (only little endian)
            // Memory layout due to alignment means the hugeint should start at byte 8
            DecimaSegment.set(ValueLayout.JAVA_BYTE, pos + 8, bigDecimalAsArray[bigDecimalAsArray.length - 1 - pos]);
        }

        // Negative values need "all one" array as basis
        if (BigDec.compareTo(BigDecimal.ZERO) < 0)
        {
            for (int pos = bigDecimalAsArray.length; pos < 16; pos++)
            {
                // Set remaining bytes to -1 = "all one"
                // Memory layout due to alignment means the hugeint should start at byte 8
                DecimaSegment.set(ValueLayout.JAVA_BYTE, pos + 8, (byte)-1);
            }
        }

        return DecimaSegment;
    }

    private MemorySegment uuid2hugeint(UUID Uuid, Arena BindArena)
    {
        MemorySegment UuidSegment = BindArena.allocate(duckdb_hugeint.sizeof());

        UuidSegment.set(ValueLayout.JAVA_LONG, 0, Uuid.getLeastSignificantBits());
        UuidSegment.set(ValueLayout.JAVA_LONG, 8, Uuid.getMostSignificantBits());

        return UuidSegment;
    }

}

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
    private final MemorySegment PreparedStmtSegmentPtr;
    private final String ErrorMessage;

    public PreparedStatement(MemorySegment DuckDbConnection, String sql)
    {
        PreparedStatementArena = Arena.ofConfined();

        PreparedStmtSegmentPtr = PreparedStatementArena.allocate(C_POINTER);

        int duckDbState = duckdb_prepare(DuckDbConnection, PreparedStatementArena.allocateFrom(sql)
                , PreparedStmtSegmentPtr);

        this.PreparedStmtSegment = PreparedStmtSegmentPtr.get(duckdb_prepared_statement, 0);
        String ErrorMessage = null;

        if (duckDbState == DuckDBError())
        {
            System.out.println("Error preparing query: " + sql);
            ErrorMessage = duckdb_prepare_error(PreparedStmtSegmentPtr).getString(0);
        }
        this.ErrorMessage = ErrorMessage;
    }

    public int bindObject(Object BindValue, int pos) throws DuckDbException
    {
        int duckDbState = 1;

        // Special case for NULL
        if (BindValue == null)
        {
             return duckdb_bind_null(PreparedStmtSegment, pos);
        }

        Arena BindArena = Arena.ofConfined();
        MemorySegment BindSegment = null;

        BindSegment = DuckDbValue.createDuckDbValueFromObject(BindValue, BindArena);

        MemorySegment BindSegmentPtr = BindArena.allocate(8);
        BindSegmentPtr.set(ValueLayout.JAVA_LONG, 0, BindSegment.address());
        duckDbState = duckdb_bind_value(PreparedStmtSegment, pos, BindSegment);
        duckdb_destroy_value(BindSegmentPtr);

        BindArena.close();

        return duckDbState;
    }

    public Result executeStatement(boolean primitivesAsObjects)
    {
        try (Arena ResultArena = Arena.ofConfined())
        {
            // Create duckdb_result struct
            MemorySegment DuckDbResult = duckdb_result.allocate(ResultArena);

            int duckDbState = duckdb_execute_prepared(PreparedStmtSegment, DuckDbResult);

            if (duckDbState == DuckDBError())
            {
                String ErrorMessage = duckdb_result_error(DuckDbResult).getString(0);
                Integer ErrorNumber = duckdb_result_error_type(DuckDbResult);

                // Destroy result before closing the Arena
                duckdb_destroy_result(DuckDbResult);

                return new Result(ErrorMessage, ErrorNumber);
            }

            var tmpResult = new Result(DuckDbResult, primitivesAsObjects);

            // Destroy result before closing the Arena
            duckdb_destroy_result(DuckDbResult);

            return tmpResult;
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
            duckdb_destroy_prepare(PreparedStmtSegmentPtr);
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

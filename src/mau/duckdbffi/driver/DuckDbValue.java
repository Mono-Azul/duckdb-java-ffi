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

import mau.duckdbffi.jextractffi.duckdb_decimal;
import mau.duckdbffi.jextractffi.duckdb_hugeint;
import mau.duckdbffi.jextractffi.duckdb_interval;

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

// A lot helper functions to make a duckdb_value from an Object
class DuckDbValue
{
    protected static MemorySegment createDuckDbValueFromObject(Object Value, Arena ValueArene) throws DuckDbException
    {
        if (Value == null)
        {
            throw new DuckDbException("DuckDbValue creation from null!");
        }

        MemorySegment DuckDbValueSegment = null;

        switch(Value)
        {
            case Boolean bo -> DuckDbValueSegment = duckdb_create_bool(bo);
            case Byte b -> DuckDbValueSegment = duckdb_create_int8(b);
            case Short s -> DuckDbValueSegment = duckdb_create_int16(s);
            case Integer i -> DuckDbValueSegment = duckdb_create_int32(i);
            case Long l -> DuckDbValueSegment = duckdb_create_int64(l);
            case BigInteger bi -> DuckDbValueSegment = duckdb_create_hugeint(bigInteger2Hugeint(bi));
            case BigDecimal bd -> DuckDbValueSegment = duckdb_create_decimal(bigDecimal2Decimal(bd, ValueArene));
            case Float f -> DuckDbValueSegment = duckdb_create_float(f);
            case Double d -> DuckDbValueSegment = duckdb_create_double(d);
            case LocalTime lt -> DuckDbValueSegment = duckdb_create_time(localTime2Long(lt));
            case LocalDate ld -> DuckDbValueSegment = duckdb_create_date(localDate2Int(ld));
            case LocalDateTime ldt -> DuckDbValueSegment = duckdb_create_timestamp(localDateTime2Long(ldt));
            case Instant inst -> DuckDbValueSegment = duckdb_create_timestamp_tz(instant2Long(inst));
            case Interval intv -> DuckDbValueSegment = duckdb_create_interval(interval2Interval(intv, ValueArene));
            case String str -> DuckDbValueSegment = duckdb_create_varchar(ValueArene.allocateFrom(str));
            case UUID uuid -> DuckDbValueSegment = duckdb_create_uuid(uuid2hugeint(uuid, ValueArene));
            default -> throw new DuckDbException("DuckDbValue creation from unknown type!");
        }

        return DuckDbValueSegment;
    }

    protected static MemorySegment bigInteger2Hugeint(BigInteger BigInt)
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

    protected static MemorySegment localTime2Long(LocalTime LocTime)
    {
        long[] larray = new long[] {LocTime.toNanoOfDay() / 1000};

        return MemorySegment.ofArray(larray);
    }

    protected static MemorySegment localDate2Int(LocalDate LocDate)
    {
        int[] larray = new int[] {(int)LocDate.getLong(ChronoField.EPOCH_DAY)};

        return MemorySegment.ofArray(larray);
    }

    protected static MemorySegment localDateTime2Long(LocalDateTime LocDateTime)
    {

        long[] larray = new long[] {LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC)
                .until(LocDateTime, ChronoUnit.MICROS)};

        return MemorySegment.ofArray(larray);
    }

    protected static MemorySegment instant2Long(Instant Inst)
    {

        long[] larray = new long[] {Instant.EPOCH.until(Inst, ChronoUnit.MICROS)};

        return MemorySegment.ofArray(larray);
    }

    protected static MemorySegment interval2Interval(Interval Intv, Arena BindArena)
    {
        MemorySegment IntervallSegment = BindArena.allocate(duckdb_interval.sizeof());

        IntervallSegment.set(ValueLayout.JAVA_INT, 0, (int)Intv.Period().toTotalMonths());
        IntervallSegment.set(ValueLayout.JAVA_INT, 4, Intv.Period().normalized().getDays());

        long durationMicros = Intv.Duration().getSeconds() * 1000000;
        durationMicros += Intv.Duration().getNano() / 1000;

        IntervallSegment.set(ValueLayout.JAVA_LONG, 8, durationMicros);

        return IntervallSegment;
    }

    protected static MemorySegment bigDecimal2Decimal(BigDecimal BigDec, Arena BindArena)
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

    protected static MemorySegment uuid2hugeint(UUID Uuid, Arena BindArena)
    {
        MemorySegment UuidSegment = BindArena.allocate(duckdb_hugeint.sizeof());

        UuidSegment.set(ValueLayout.JAVA_LONG, 0, Uuid.getLeastSignificantBits());
        UuidSegment.set(ValueLayout.JAVA_LONG, 8, Uuid.getMostSignificantBits());

        return UuidSegment;
    }
}

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

import mau.duckdbffi.jextractffi.duckdb_timestamp;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.BitSet;

import static mau.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class LocalDateTimeColumn extends ObjectColumn<LocalDateTime>
{
    public LocalDateTimeColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = LocalDateTime.class;
    }

    private static int nanosPartMicros(long micros)
    {
        int microsMod = (int)(micros % 1000_000);
        if (microsMod >= 0)
        {
            return microsMod * 1000;
        }
        else
        {
            return (1000_000 + microsMod) * 1000;
        }
    }

    private static int nanosPartNanos(long nanos)
    {
        long nanosMod = nanos % 1_000_000_000L;
        if (nanosMod >= 0)
        {
            return (int)nanosMod;
        }
        else
        {
            return (int)((1_000_000_000L + nanosMod));
        }
    }

    private static long micros2seconds(long micros)
    {
        if ((micros % 1000_000L) >= 0)
        {
            return micros / 1000_000L;
        }
        else
        {
            return (micros / 1000_000L) - 1;
        }
    }

    private static long nanos2seconds(long nanos)
    {
        if ((nanos % 1_000_000_000L) >= 0)
        {
            return nanos / 1_000_000_000L;
        }
        else
        {
            return (nanos / 1_000_000_000L) - 1;
        }
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        LocalDateTime[] ResultArray = new LocalDateTime[dbChunkSize];

        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);

        try (Arena ColumnArena = Arena.ofConfined())
        {
            MemorySegment Timestamps = duckdb_timestamp.reinterpret(ResultVectorData, dbChunkSize, ColumnArena, null);

            BitSet ValidityMask = getValiditySetForChunk(ResultVector, dbChunkSize);

            // There is no validity mask => there are no null values
            boolean noNulls = ValidityMask.isEmpty();

            // Duplicating the hot loop to avoid ifs inside
            switch (ColumnDuckDbDataype.type)
            {
                case DuckDbDatatype.DUCKDB_TYPE_TIMESTAMP ->
                {
                    for (int pos = 0; pos < dbChunkSize; pos++)
                    {
                        // Immediately check if value is null and skip rest
                        if (!noNulls && !ValidityMask.get(pos))
                        {
                            ResultArray[pos] = null;
                            continue;
                        }

                        long micros = duckdb_timestamp.micros(duckdb_timestamp.asSlice(Timestamps, pos));
                        LocalDateTime TimestampDt = LocalDateTime.ofEpochSecond(micros2seconds(micros), nanosPartMicros(micros), ZoneOffset.UTC);
                        ResultArray[pos] = TimestampDt;
                    }
                }
                case DuckDbDatatype.DUCKDB_TYPE_TIMESTAMP_S ->
                {
                    for (int pos = 0; pos < dbChunkSize; pos++)
                    {
                        // Immediately check if value is null and skip rest
                        if (!noNulls && !ValidityMask.get(pos))
                        {
                            ResultArray[pos] = null;
                            continue;
                        }

                        long micros = duckdb_timestamp.micros(duckdb_timestamp.asSlice(Timestamps, pos));
                        ResultArray[pos] = LocalDateTime.ofEpochSecond(micros, 0, ZoneOffset.UTC);
                    }
                }
                case DuckDbDatatype.DUCKDB_TYPE_TIMESTAMP_MS ->
                {
                    for (int pos = 0; pos < dbChunkSize; pos++)
                    {
                        // Immediately check if value is null and skip rest
                        if (!noNulls && !ValidityMask.get(pos))
                        {
                            ResultArray[pos] = null;
                            continue;
                        }

                        long micros = duckdb_timestamp.micros(duckdb_timestamp.asSlice(Timestamps, pos));
                        ResultArray[pos] = LocalDateTime.ofEpochSecond(micros2seconds(micros * 1000), nanosPartMicros(micros * 1000), ZoneOffset.UTC);
                    }
                }
                case DuckDbDatatype.DUCKDB_TYPE_TIMESTAMP_NS ->
                {
                    for (int pos = 0; pos < dbChunkSize; pos++)
                    {
                        // Immediately check if value is null and skip rest
                        if (!noNulls && !ValidityMask.get(pos))
                        {
                            ResultArray[pos] = null;
                            continue;
                        }

                        long micros = duckdb_timestamp.micros(duckdb_timestamp.asSlice(Timestamps, pos));
                        ResultArray[pos] = LocalDateTime.ofEpochSecond(nanos2seconds(micros), nanosPartNanos(micros), ZoneOffset.UTC);
                    }
                }
            }
        }
        this.ChunkArrays.add(ResultArray);
    }
}

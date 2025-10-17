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
import java.time.Instant;

import static mau.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

// The timestamp is internally stored as micros since epoch in UTC. This means it is an instant.
// Changing to a timezone is easy in Java and up to the user.
public class InstantColumn extends ObjectColumn<Instant>
{
    public InstantColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = Instant.class;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        Instant[] ResultArray = new Instant[dbChunkSize];

        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);

        try (Arena ColumnArena = Arena.ofConfined())
        {
            MemorySegment Timestamps = duckdb_timestamp.reinterpret(ResultVectorData, dbChunkSize, ColumnArena, null);

            for (int col = 0; col < dbChunkSize; col++)
            {
                long micros = duckdb_timestamp.micros(duckdb_timestamp.asSlice(Timestamps, col));
                Instant TimestampDt = Instant.ofEpochSecond(micros / 1_000_000L, (micros % 1_000_000L) * 1_000L);
                ResultArray[col] = TimestampDt;
            }
            setValidityForChunk(ResultVector, dbChunkSize, ResultArray);
        }
        this.ChunkArrays.add(ResultArray);
    }
}

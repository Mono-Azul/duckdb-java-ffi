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

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.UUID;

import static mau.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class UuidColumn extends ObjectColumn<UUID>
{
    public UuidColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = UUID.class;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert Vector into long[] array first and then convert 2 longs to UUID[]
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        long[] PrimitiveResultArray = new long[dbChunkSize * 2];
        ResultVectorData.reinterpret((long)dbChunkSize * 16);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_LONG, 0, PrimitiveResultArray, 0, dbChunkSize * 2);

        UUID[] ResultArray = new UUID[dbChunkSize];
        long mask = (1L << 63);

        for (int pos = 0; pos < dbChunkSize; pos++)
        {
            // We have to flip the msb because of some ordering rules in DuckDB => XOR with mask
            ResultArray[pos] = new UUID(PrimitiveResultArray[pos * 2 + 1] ^ mask, PrimitiveResultArray[pos * 2]);
        }

        setValidityForChunk(ResultVector, dbChunkSize, ResultArray);
        this.ChunkArrays.add(ResultArray);
    }
}

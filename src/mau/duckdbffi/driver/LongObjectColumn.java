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
import java.util.Arrays;

import static mau.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class LongObjectColumn extends ObjectColumn<Long>
{
    public LongObjectColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = Long.class;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        // Switch to unsigned int path
        if (ColumnDuckDbDataype.type == DuckDbDatatype.DUCKDB_TYPE_UINTEGER)
        {
            addVectorChunkUnsignedInt(ResultVector, dbChunkSize);
            return;
        }

        // Convert Vector into long[] array first and then convert to Long[]
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        long[] PrimitiveResultArray = new long[dbChunkSize];
        ResultVectorData.reinterpret((long)dbChunkSize * 8);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_LONG, 0, PrimitiveResultArray, 0, dbChunkSize);

        Long[] ResultArray = Arrays.stream(PrimitiveResultArray).boxed().toArray(Long[]::new);
        setValidityForChunk(ResultVector, dbChunkSize, ResultArray);

        this.ChunkArrays.add(ResultArray);
    }

    protected void addVectorChunkUnsignedInt(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert Vector into int[] array first and then convert to Long[]
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        int[] PrimitiveResultArray = new int[dbChunkSize];
        ResultVectorData.reinterpret((long)dbChunkSize * 4);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_INT, 0, PrimitiveResultArray, 0, dbChunkSize);

        Long[] ResultArray = new Long[dbChunkSize];

        for (int pos = 0; pos < dbChunkSize; pos++)
        {
            ResultArray[pos] = Integer.toUnsignedLong(PrimitiveResultArray[pos]);
        }

        setValidityForChunk(ResultVector, dbChunkSize, ResultArray);
        this.ChunkArrays.add(ResultArray);
    }
}

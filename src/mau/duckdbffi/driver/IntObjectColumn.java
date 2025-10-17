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

// This class has Integer objects instead of primitive ints. Also, it is used as a carrier for unsigned short.
public class IntObjectColumn extends ObjectColumn<Integer>
{
    public IntObjectColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = Integer.class;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        // Switch to unsigned short path
        if (ColumnDuckDbDataype.type == DuckDbDatatype.DUCKDB_TYPE_USMALLINT)
        {
            addVectorChunkUnsignedShort(ResultVector, dbChunkSize);
            return;
        }

        // Convert Vector into int[] array first and then convert to Integer[]
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        int[] PrimitiveResultArray = new int[dbChunkSize];
        ResultVectorData.reinterpret((long)dbChunkSize * 4);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_INT, 0, PrimitiveResultArray, 0, dbChunkSize);

        Integer[] ResultArray = Arrays.stream(PrimitiveResultArray).boxed().toArray(Integer[]::new);
        setValidityForChunk(ResultVector, dbChunkSize, ResultArray);
        this.ChunkArrays.add(ResultArray);
    }

    protected void addVectorChunkUnsignedShort(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert Vector into short[] array first and then convert to Integer[]
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        short[] PrimitiveResultArray = new short[dbChunkSize];
        ResultVectorData.reinterpret((long)dbChunkSize * 2);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_SHORT, 0, PrimitiveResultArray, 0, dbChunkSize);

        Integer[] ResultArray = new Integer[dbChunkSize];

        for (int pos = 0; pos < dbChunkSize; pos++)
        {
            ResultArray[pos] = Short.toUnsignedInt(PrimitiveResultArray[pos]);
        }

        setValidityForChunk(ResultVector, dbChunkSize, ResultArray);
        this.ChunkArrays.add(ResultArray);
    }
}

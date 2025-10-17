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
import java.util.ArrayList;
import java.util.List;

import static mau.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class ShortColumn extends PrimitiveColumn<Short>
{
    private final List<short[]> ChunkArrays;
    public short[] VectorArray;

    public ShortColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);

        ChunkArrays = new ArrayList<>();
    }

    @Override
    protected void compactChunks()
    {
        VectorArray = new short[ResMetaData.rowCount()];
        int startPos = 0;

        // Concat all Arrays
        for (short[] arr : ChunkArrays)
        {
            System.arraycopy(arr, 0, VectorArray, startPos, arr.length);
            startPos += arr.length;
        }
        // Empty ChunkArrays
        ChunkArrays.clear();
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert Vector into short[] array
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        short[] ResultArray = new short[dbChunkSize];
        ResultVectorData.reinterpret((long)dbChunkSize * 2);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_SHORT, 0, ResultArray, 0, dbChunkSize);
        this.ChunkArrays.add(ResultArray);
        buildValidityMask(ResultVector, dbChunkSize);
    }

    @Override
    public Short getValue(int pos)
    {
        if (getValidity(pos))
        {
            return VectorArray[pos];
        }
        // Null value
        return null;
    }

    // Don't forget to check Validity before using the value as it could be null
    public short getPrimitiveValue(int pos)
    {
        return VectorArray[pos];
    }
}

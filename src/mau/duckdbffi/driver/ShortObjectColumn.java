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

import static mau.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class ShortObjectColumn extends ObjectColumn<Short>
{
    public ShortObjectColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = Short.class;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        // Switch to unsigned short path
        if (ColumnDuckDbDataype.type == DuckDbDatatype.DUCKDB_TYPE_UTINYINT)
        {
            addVectorChunkUnsignedByte(ResultVector, dbChunkSize);
            return;
        }

        // Convert Vector into short[] array first and then convert to Short[]
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        short[] PrimitiveResultArray = new short[dbChunkSize];
        ResultVectorData.reinterpret((long)dbChunkSize * 2);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_SHORT, 0, PrimitiveResultArray, 0, dbChunkSize);

        Short[] ResultArray = new Short[dbChunkSize];
        for (int pos = 0; pos < dbChunkSize; pos++)
        {
            ResultArray[pos] = PrimitiveResultArray[pos];
        }

        setValidityForChunk(ResultVector, dbChunkSize, ResultArray);

        this.ChunkArrays.add(ResultArray);
    }

    protected void addVectorChunkUnsignedByte(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert Vector into byte[] array first and then convert to Short[]
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        byte[] PrimitiveResultArray = new byte[dbChunkSize];
        ResultVectorData.reinterpret(dbChunkSize);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_BYTE, 0, PrimitiveResultArray, 0, dbChunkSize);

        Short[] ResultArray = new Short[dbChunkSize];

        for (int pos = 0; pos < dbChunkSize; pos++)
        {
            ResultArray[pos] = (short)Byte.toUnsignedInt(PrimitiveResultArray[pos]);
        }

        setValidityForChunk(ResultVector, dbChunkSize, ResultArray);
        this.ChunkArrays.add(ResultArray);
    }
}

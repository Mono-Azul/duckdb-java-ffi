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

import mau.duckdbffi.jextractffi.duckdb_string_t;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

import static mau.duckdbffi.jextractffi.duckdb_h.duckdb_string_is_inlined;
import static mau.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class StringColumn extends ObjectColumn<String>
{
    public StringColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = String.class;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        String[] ResultArray = new String[dbChunkSize];

        try (Arena ColumnArena = Arena.ofConfined())
        {
            MemorySegment String_t = duckdb_string_t.reinterpret(ResultVectorData, dbChunkSize, ColumnArena, null);

            BitSet ValidityMask = getValiditySetForChunk(ResultVector, dbChunkSize);

            // There is no validity mask => there are no null values
            boolean noNulls = ValidityMask.isEmpty();

            for (int pos = 0; pos < dbChunkSize; pos++)
            {
                // Immediately check if value is null and skip rest
                if (!noNulls && !ValidityMask.get(pos))
                {
                    ResultArray[pos] = null;
                    continue;
                }

                MemorySegment String_struct = duckdb_string_t.asSlice(String_t, pos);

                // Short strings can be inlined, longer ones have a pointer => this is a union struct!
                if (duckdb_string_is_inlined(String_struct))
                {
                    //var StrValue = duckdb_string_t.value(String_struct);
                    var StrInlined = duckdb_string_t.value.inlined(String_struct);
                    var InlinedStrArray = duckdb_string_t.value.inlined.inlined(StrInlined);
                    int length = duckdb_string_t.value.inlined.length(StrInlined);
                    byte[] InlineStrByteArray = InlinedStrArray.reinterpret(length).toArray(ValueLayout.JAVA_BYTE);
                    ResultArray[pos] = new String(InlineStrByteArray, StandardCharsets.UTF_8);
                }
                else
                {
                    //var StrValue = duckdb_string_t.value(String_struct);
                    var StrPtr = duckdb_string_t.value.pointer(String_struct);
                    var StrArrayPtr = duckdb_string_t.value.pointer.ptr(StrPtr);
                    int stringLength = duckdb_string_t.value.pointer.length(StrPtr);
                    if (stringLength > Integer.MAX_VALUE)
                    {
                        // For now we can _only_ handle 2 GB strings in Java
                        stringLength = Integer.MAX_VALUE;
                    }

                    byte[] byteString = StrArrayPtr.reinterpret(stringLength).toArray(ValueLayout.JAVA_BYTE);
                    ResultArray[pos] = new String(byteString, StandardCharsets.UTF_8);
                }
            }
        }
        this.ChunkArrays.add(ResultArray);
    }
}

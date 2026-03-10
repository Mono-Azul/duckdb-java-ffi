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
import java.time.LocalDate;
import java.util.BitSet;

import static mau.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class LocalDateColumn extends ObjectColumn<LocalDate>
{
    public LocalDateColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = LocalDate.class;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert Vector into int[] array
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        LocalDate[] ResultArray = new LocalDate[dbChunkSize];
        int[] TmpResultArray = new int[dbChunkSize];
        ResultVectorData.reinterpret(dbChunkSize);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_INT, 0, TmpResultArray, 0, dbChunkSize);

        BitSet ValidityMask = getValiditySetForChunk(ResultVector, dbChunkSize);

        // There is no validity mask => there are no null values
        boolean noNulls = ValidityMask.isEmpty();

        // Create LocalDate from int
        for (int pos = 0; pos < dbChunkSize; pos++)
        {
            // Immediately check if value is null and skip rest
            if (!noNulls && !ValidityMask.get(pos))
            {
                ResultArray[pos] = null;
                continue;
            }

            ResultArray[pos] = LocalDate.ofEpochDay(TmpResultArray[pos]);
        }

        this.ChunkArrays.add(ResultArray);
    }
}

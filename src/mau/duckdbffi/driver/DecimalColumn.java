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

import mau.duckdbffi.jextractffi.duckdb_hugeint;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.math.BigDecimal;

import static mau.duckdbffi.jextractffi.duckdb_h.*;

public class DecimalColumn extends ObjectColumn<BigDecimal>
{
    public DecimalColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = BigDecimal.class;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        MemorySegment ResultVectorType = duckdb_vector_get_column_type(ResultVector);
        byte DecimalWidth = duckdb_decimal_width(ResultVectorType);
        byte DecimalSize = duckdb_decimal_scale(ResultVectorType);

        BigDecimal[] ResultArray = new BigDecimal[dbChunkSize];

        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        try (Arena ColumnArena = Arena.ofConfined())
        {
            if (DecimalWidth < 5)
            {
                short[] Decimal4Vector = new short[dbChunkSize];
                MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_SHORT, 0, Decimal4Vector, 0, dbChunkSize);

                for (int i = 0; i < dbChunkSize; i++)
                {
                    ResultArray[i] = BigDecimal.valueOf(Decimal4Vector[i], DecimalSize);
                }
            }
            else if (DecimalWidth < 10)
            {
                int[] Decimal4Vector = new int[dbChunkSize];
                MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_INT, 0, Decimal4Vector, 0, dbChunkSize);

                for (int i = 0; i < dbChunkSize; i++)
                {
                    ResultArray[i] = BigDecimal.valueOf(Decimal4Vector[i], DecimalSize);
                }
            }
            else if (DecimalWidth < 19)
            {
                long[] Decimal4Vector = new long[dbChunkSize];
                MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_LONG, 0, Decimal4Vector, 0, dbChunkSize);

                for (int i = 0; i < dbChunkSize; i++)
                {
                    ResultArray[i] = BigDecimal.valueOf(Decimal4Vector[i], DecimalSize);
                }
            }
            else
            {
                MemorySegment HugeintVector = duckdb_hugeint.reinterpret(ResultVectorData, dbChunkSize, ColumnArena, null);

                for (int i = 0; i < dbChunkSize; i++)
                {
                    MemorySegment OneHugeint = duckdb_hugeint.asSlice(HugeintVector, i);
                    MemorySegment HIntString = duckdb_value_to_string(duckdb_create_hugeint(OneHugeint));
                    ResultArray[i] = new BigDecimal(HIntString.getString(0)).movePointLeft(DecimalSize);
                }
            }
            setValidityForChunk(ResultVector, dbChunkSize, ResultArray);
        }
        this.ChunkArrays.add(ResultArray);
    }
}

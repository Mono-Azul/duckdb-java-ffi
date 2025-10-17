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
import java.util.BitSet;
import java.util.List;

import static mau.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_validity;

abstract public class PrimitiveColumn<T> extends Column<T>
{
    public BitSet ValidityMask = new BitSet();
    // Unfortunately .length() does not count false bits at the end of the set => we have to keep the score
    public int validityMaskLength = 0;

    public PrimitiveColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
    }

    //abstract public List<?> getChunkArrays();
    abstract public T getValue(int pos);

    protected void buildValidityMask(MemorySegment ResultVector, int dbChunkSize)
    {
        // Create Validity Mask if necessary
        // Size is ChunkSize / 8 (8 results per byte) and rounded up
        long validityMaskSize = Math.ceilDiv(dbChunkSize, 8);
        MemorySegment ValidityPtr = duckdb_vector_get_validity(ResultVector);

        int startPos = validityMaskLength;
        // Null pointer indicates no need for mask => no nulls
        // We still add a Validity Mask with all true
        if (ValidityPtr.address() != 0)
        {
            BitSet tmpValidityMask = BitSet.valueOf(ValidityPtr.reinterpret(validityMaskSize).toArray(ValueLayout.JAVA_BYTE));

            for (int pos = 0; pos < dbChunkSize; pos++)
            {
                ValidityMask.set(startPos + pos, tmpValidityMask.get(pos));
            }
        }
        else
        {
            for (int pos = 0; pos < dbChunkSize; pos++)
            {
                ValidityMask.set(startPos + pos, true);
            }
        }
        validityMaskLength += dbChunkSize;
    }

    void compactValidityBitSet()
    {
        // All rows have a value => avoid checks completely
        if (validityMaskLength == ValidityMask.cardinality())
        {
            ValidityMask = null;
        }
    }

    public boolean getValidity(int pos)
    {
        // No Mask => no NULLs
        if (ValidityMask == null)
        {
            return true;
        }
        return ValidityMask.get(pos);
    }
}

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

abstract public class Column<T>
{
    public final DuckDbDatatype ColumnDuckDbDataype;
    public final String ColumnName;
    public ResultMetaData ResMetaData;

    public Column(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        this.ColumnName = ColumnName;
        this.ColumnDuckDbDataype = ColumnDatatype;
    }

    protected abstract void compactChunks();

    protected void addResultMetaData(ResultMetaData ResMetaData)
    {
        this.ResMetaData = ResMetaData;
    }

    protected abstract void addVectorChunk(MemorySegment ResultVector, int dbChunkSize);

    abstract public T getValue(int pos);
}

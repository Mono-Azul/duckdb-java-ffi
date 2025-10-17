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

public class ColumnFactory
{
    static Column createBooleanColumn(String ColumnName, DuckDbDatatype ColumnDatatype, boolean primitivesAsObject)
    {
        if (primitivesAsObject)
        {
            return new BooleanObjectColumn(ColumnName, ColumnDatatype);
        }

        return new BooleanColumn(ColumnName, ColumnDatatype);
    }

    static Column createByteColumn(String ColumnName, DuckDbDatatype ColumnDatatype, boolean primitivesAsObject)
    {
        if (primitivesAsObject)
        {
            return new ByteObjectColumn(ColumnName, ColumnDatatype);
        }

        return new ByteColumn(ColumnName, ColumnDatatype);
    }

    static Column createShortColumn(String ColumnName, DuckDbDatatype ColumnDatatype, boolean primitivesAsObject)
    {
        if (primitivesAsObject)
        {
            return new ShortObjectColumn(ColumnName, ColumnDatatype);
        }

        return new ShortColumn(ColumnName, ColumnDatatype);
    }

    static Column createIntColumn(String ColumnName, DuckDbDatatype ColumnDatatype, boolean primitivesAsObject)
    {
        if (primitivesAsObject)
        {
            return new IntObjectColumn(ColumnName, ColumnDatatype);
        }

        return new IntColumn(ColumnName, ColumnDatatype);
    }

    static Column createLongColumn(String ColumnName, DuckDbDatatype ColumnDatatype, boolean primitivesAsObject)
    {
        if (primitivesAsObject)
        {
            return new LongObjectColumn(ColumnName, ColumnDatatype);
        }

        return new LongColumn(ColumnName, ColumnDatatype);
    }

    static Column createFloatColumn(String ColumnName, DuckDbDatatype ColumnDatatype, boolean primitivesAsObject)
    {
        if (primitivesAsObject)
        {
            return new FloatObjectColumn(ColumnName, ColumnDatatype);
        }

        return new FloatColumn(ColumnName, ColumnDatatype);
    }

    static Column createDoubleColumn(String ColumnName, DuckDbDatatype ColumnDatatype, boolean primitivesAsObject)
    {
        if (primitivesAsObject)
        {
            return new DoubleObjectColumn(ColumnName, ColumnDatatype);
        }

        return new DoubleColumn(ColumnName, ColumnDatatype);
    }
}

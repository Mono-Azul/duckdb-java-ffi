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

import mau.duckdbffi.jextractffi.duckdb_h;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static mau.duckdbffi.jextractffi.duckdb_h.*;

public class Appender implements AutoCloseable
{
    private final MemorySegment Appender;
    private final MemorySegment AppenderPtr;
    private final Arena AppenderArena;


    protected Appender (MemorySegment DuckDbConnection, String schema, String table)
            throws DuckDbException
    {
        // Should be used by only one thread
        AppenderArena = Arena.ofConfined();
        AppenderPtr = AppenderArena.allocate(8)
                .reinterpret(AppenderArena, duckdb_h::duckdb_appender_destroy);
        int res = duckdb_appender_create(DuckDbConnection, AppenderArena.allocateFrom(schema),
                AppenderArena.allocateFrom(table),AppenderPtr);
        Appender = AppenderPtr.get(C_POINTER, 0);
        if (res == DuckDBError())
        {
            throw new DuckDbException("Error Appender creation: " + getErrorMessage(Appender));
        }
    }

    public void beginRow() throws DuckDbException
    {
        if (duckdb_appender_begin_row(Appender) == DuckDBError())
        {
            throw new DuckDbException("Error Appender - Begin Row: " + getErrorMessage(Appender));
        }
    }

    public void endRow() throws DuckDbException
    {
        if (duckdb_appender_end_row(Appender) == DuckDBError())
        {
            throw new DuckDbException("Error Appender - End Row: " + getErrorMessage(Appender));
        }
    }

    public void flush() throws DuckDbException
    {
        if (duckdb_appender_flush(Appender) == DuckDBError())
        {
            throw new DuckDbException("Error Appender - Flush: " + getErrorMessage(Appender));
        }
    }

    public void close() throws DuckDbException
    {
        int res = duckdb_appender_close(Appender);
        String ErrorMes = null;

        if (res == DuckDBError())
        {
            ErrorMes = getErrorMessage(Appender);
        }

        // Cleanup Arena before possibly throwing an error
        AppenderArena.close();

        if (res == DuckDBError())
        {
            throw new DuckDbException("Error Appender - Close: " + ErrorMes);
        }
    }

    public void appendNull() throws DuckDbException
    {
        if (duckdb_append_null(Appender) == DuckDBError())
        {
            throw new DuckDbException("Error Appender - Append null: " + getErrorMessage(Appender));
        }
    }

    public void appendDefault() throws DuckDbException
    {
        if (duckdb_append_default(Appender) == DuckDBError())
        {
            throw new DuckDbException("Error Appender - Append default: " + getErrorMessage(Appender));
        }
    }

    public void appendValue(Object AppendObject) throws DuckDbException
    {
        // Special case for NULL
        if (AppendObject == null)
        {
            appendNull();
        }

        MemorySegment DuckDbValueSegment = DuckDbValue.createDuckDbValueFromObject(AppendObject, AppenderArena);

        if (duckdb_append_value(Appender, DuckDbValueSegment) == DuckDBSuccess())
        {
            MemorySegment DuckDbValuePtr = AppenderArena.allocate(8);
            DuckDbValuePtr.set(ValueLayout.JAVA_LONG, 0, DuckDbValueSegment.address());
            duckdb_destroy_value(DuckDbValuePtr);
            return;
        }

        throw new DuckDbException("Error Appender - Append value: " + getErrorMessage(Appender));
    }

    // Primitive types to avoid boxing
    public void appendBool(boolean val) throws DuckDbException
    {
        if (duckdb_append_bool(Appender, val) == DuckDBError())
        {
            throw new DuckDbException("Error Appender - Append bool: " + getErrorMessage(Appender));
        }
    }

    public void appendByte(byte val) throws DuckDbException
    {
        if (duckdb_append_int8(Appender, val) == DuckDBError())
        {
            throw new DuckDbException("Error Appender - Append byte: " + getErrorMessage(Appender));
        }
    }

    public void appendShort(short val) throws DuckDbException
    {
        if (duckdb_append_int16(Appender, val) == DuckDBError())
        {
            throw new DuckDbException("Error Appender - Append short: " + getErrorMessage(Appender));
        }
    }

    public void appendInt(int val) throws DuckDbException
    {
        if (duckdb_append_int32(Appender, val) == DuckDBError())
        {
            throw new DuckDbException("Error Appender - Append int: " + getErrorMessage(Appender));
        }
    }
    public void appendLong(long val) throws DuckDbException
    {
        if (duckdb_append_int64(Appender, val) == DuckDBError())
        {
            throw new DuckDbException("Error Appender - Append long: " + getErrorMessage(Appender));
        }
    }

    public void appendFloat(float val) throws DuckDbException
    {
        if (duckdb_append_float(Appender, val) == DuckDBError())
        {
            throw new DuckDbException("Error Appender - Append float: " + getErrorMessage(Appender));
        }
    }

    public void appendDouble(double val) throws DuckDbException
    {
        if (duckdb_append_double(Appender, val) == DuckDBError())
        {
            throw new DuckDbException("Error Appender - Append double: " + getErrorMessage(Appender));
        }
    }

    private String getErrorMessage(MemorySegment Appender)
    {
        MemorySegment ErrorData = duckdb_appender_error_data(Appender);
        String ErrorMsg = duckdb_error_data_message(ErrorData).getString(0);
        destroyDuckDbErrorData(ErrorData);
        return ErrorMsg;
    }

    // Separate Consumer method to destroy the DuckDbErrorData, because a pointer is needed
    private static void destroyDuckDbErrorData(MemorySegment DuckDbErrorData)
    {
        try (Arena ClosingArena = Arena.ofConfined())
        {
            MemorySegment DuckDbErrorDataPtr = ClosingArena.allocate(8);
            DuckDbErrorDataPtr.set(ValueLayout.JAVA_LONG, 0, DuckDbErrorData.address());
            duckdb_destroy_error_data(DuckDbErrorDataPtr);
        } catch (Throwable e)
        {
            throw new RuntimeException(e);
        }
    }
}

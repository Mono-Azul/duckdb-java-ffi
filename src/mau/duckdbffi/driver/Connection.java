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

import mau.duckdbffi.jextractffi.duckdb_result;

import java.lang.foreign.*;
import java.util.List;

import static mau.duckdbffi.jextractffi.duckdb_h.*;

// Each thread must use its own Connection => ConnectionArea is confined
public class Connection implements AutoCloseable
{
    private final Arena ConnectionArena;
    private final MemorySegment DuckDbConnection; // _duckdb_connection
    private final MemorySegment DuckDbConnectionPtr;

    public Connection(MemorySegment DuckDbDatabase) throws DuckDbException
    {
        ConnectionArena = Arena.ofConfined();

        DuckDbConnectionPtr = ConnectionArena.allocate(C_POINTER);

        int duckDbState = duckdb_connect(DuckDbDatabase, DuckDbConnectionPtr);

        if (duckDbState == DuckDBError())
        {
            throw new DuckDbException("Error creating connection!");
        }

        // Get Connection from the pointer
        DuckDbConnection = DuckDbConnectionPtr.get(duckdb_connection, 0);
    }

    public Result query(String sql)
    {
        return query(sql, false);
    }

    public Result query(String sql, boolean primitivesAsObjects)
    {
        try (Arena ResultArena = Arena.ofConfined())
        {
            MemorySegment DuckDbResult = duckdb_result.allocate(ResultArena);

            // Run query
            int duckDbState = duckdb_query(DuckDbConnection, ResultArena.allocateFrom(sql), DuckDbResult);

            if (duckDbState == DuckDBError())
            {
                String ErrorMessage = duckdb_result_error(DuckDbResult).getString(0);
                Integer ErrorNumber = duckdb_result_error_type(DuckDbResult);

                // Destroy result before closing the Arena
                duckdb_destroy_result(DuckDbResult);

                return new Result(ErrorMessage, ErrorNumber);
            }

            var tmpResult = new Result(DuckDbResult, primitivesAsObjects);

            // Destroy result before closing the Arena
            duckdb_destroy_result(DuckDbResult);

            return tmpResult;
        } catch (Throwable e)
        {
            throw new RuntimeException(e);
        }
    }

    public PreparedStatement createPreparedStatement(String sql)
    {
        return new PreparedStatement(DuckDbConnection, sql);
    }

    public Result queryWithParameters(String sql, List<Object> Parameters) throws DuckDbException
    {
        return queryWithParameters(sql, Parameters, false);
    }

    public Result queryWithParameters(String sql, List<Object> Parameters, boolean primitivesAsObjects) throws DuckDbException
    {
        // Create PreparedStmt + Bind + Run in one step
        try (PreparedStatement PrepStmt = createPreparedStatement(sql))
        {
            if (PrepStmt.hasError())
            {
                return new Result(PrepStmt.getErrorMessage(), 0);
            }

            if (Parameters != null && !Parameters.isEmpty())
            {
                for (int pos = 0; pos < Parameters.size(); pos++)
                {
                    // Binding starts with 1 and not 0!
                    PrepStmt.bindObject(Parameters.get(pos), pos + 1);
                }
            }

            return PrepStmt.executeStatement(primitivesAsObjects);
        }
    }

    public Appender createAppender(String table) throws DuckDbException
    {
        var Res = query("SELECT value FROM duckdb_settings() WHERE name = 'schema';");

        if (Res.getRowCount() == 1)
        {
            var row = Res.getRow(0);
            if (row == null || row.isEmpty())
            {
                throw new DuckDbException("No default schema found (empty row)!");
            }

            Object first = row.getFirst();
            if (first == null)
            {
                throw new DuckDbException("No default schema found (null value)!");
            }

            if (!(first instanceof String))
            {
                throw new DuckDbException("Schema value is not a string: " + first.getClass());
            }

            return createAppender((String) first, table);
        }

        throw new DuckDbException("No default schema found!");
    }

    public Appender createAppender(String schema, String table) throws DuckDbException
    {
        return new Appender(DuckDbConnection, schema, table);
    }

    @Override
    public void close() throws DuckDbException
    {
        try
        {
            duckdb_disconnect(DuckDbConnectionPtr);
            ConnectionArena.close();
        } catch (Exception e)
        {
            throw new DuckDbException(e);
        }
    }
}

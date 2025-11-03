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
import mau.duckdbffi.jextractffi.duckdb_result;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
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

        // There could be problems with double freeing otherwise
        DuckDbConnectionPtr = ConnectionArena.allocate(8)
                .reinterpret(ConnectionArena, duckdb_h::duckdb_disconnect);

        int duckDbState = duckdb_connect(DuckDbDatabase, DuckDbConnectionPtr);

        if (duckDbState == DuckDBError())
        {
            throw new DuckDbException("Error creating connection!");
        }

        DuckDbConnection = DuckDbConnectionPtr.get(C_POINTER, 0);
    }

    public Result query(String sql)
    {
        return query(sql, false);
    }

    public Result query(String sql, boolean primitivesAsObjects)
    {
        try (Arena ResultArena = Arena.ofConfined())
        {
            // Create duckdb_result struct
            MemorySegment DuckDbResult = ResultArena.allocate(duckdb_result.sizeof())
                    .reinterpret(ResultArena, duckdb_h::duckdb_destroy_result);
            MemorySegment DuckDbResultPtr = MemorySegment.ofAddress(DuckDbResult.address());

            // Run query
            int duckDbState = duckdb_query(DuckDbConnection, ResultArena.allocateFrom(sql), DuckDbResultPtr);

            if (duckDbState == DuckDBError())
            {
                System.out.println("Error running query: " + sql);

                String ErrorMessage = duckdb_result_error(DuckDbResultPtr).getString(0);
                Integer ErrorNumber = duckdb_result_error_type(DuckDbResultPtr);

                return new Result(ErrorMessage, ErrorNumber);
            }

            return new Result(DuckDbResultPtr, DuckDbResult, primitivesAsObjects);
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
            return createAppender((String)(Res.getRow(0).getFirst()), table);
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
            ConnectionArena.close();
        } catch (Exception e)
        {
            throw new DuckDbException(e);
        }
    }
}

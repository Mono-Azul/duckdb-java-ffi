package monoazul.duckdbffi.driver;

import monoazul.duckdbffi.jextractffi.duckdb_h;
import monoazul.duckdbffi.jextractffi.duckdb_result;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static monoazul.duckdbffi.jextractffi.duckdb_h.*;

// Each thread must use its own Connection => ConnectionArea is confined
public class Connection implements AutoCloseable
{
    private final Arena ConnectionArena;
    private final MemorySegment DuckDbConnection; // _duckdb_connection
    private final MemorySegment DuckDbConnectionPtr;

    public Connection(MemorySegment DuckDbDatabase) throws Exception
    {
        ConnectionArena = Arena.ofConfined();

        // There could be problems with double freeing otherwise
        DuckDbConnectionPtr = ConnectionArena.allocate(8)
                .reinterpret(ConnectionArena, duckdb_h::duckdb_disconnect);

        int duckDbState = duckdb_connect(DuckDbDatabase, DuckDbConnectionPtr);

        if (duckDbState == DuckDBError())
        {
            System.out.println("Error creating connection!");
            throw new Exception();
        }

        DuckDbConnection = DuckDbConnectionPtr.get(C_POINTER, 0);
    }

    public Result query(String sql) throws Throwable
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

            return new Result(DuckDbResultPtr, DuckDbResult, false);
        }
    }

    @Override
    public void close() throws Exception
    {
        ConnectionArena.close();
    }
}

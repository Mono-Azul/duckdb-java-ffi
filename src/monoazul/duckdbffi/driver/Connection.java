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
    private MemorySegment DuckDbConnectionPtr;

    public Connection(MemorySegment DuckDbDatabase) throws Exception {
        ConnectionArena = Arena.ofConfined();

        // There could be problems with double freeing otherwise
        DuckDbConnectionPtr = ConnectionArena.allocate(8);
        DuckDbConnectionPtr = DuckDbConnectionPtr.reinterpret(ConnectionArena, duckdb_h::duckdb_disconnect);

        int duckDbState = duckdb_connect(DuckDbDatabase, DuckDbConnectionPtr);

        if (duckDbState == DuckDBError())
        {
            System.out.println("Error creating connection!");
            throw new Exception();
        }

        DuckDbConnection = DuckDbConnectionPtr.get(C_POINTER, 0);
    }

    public Result query(String sql) throws Throwable {
        // Create duckdb_result struct
        MemorySegment DuckDbResult = duckdb_result.allocate(ConnectionArena);
        MemorySegment DuckDbResultPtr = ConnectionArena.allocate(C_POINTER);
        DuckDbResultPtr.set(ValueLayout.JAVA_LONG, 0, DuckDbResult.address());

        // Run query
        int duckDbState = duckdb_query(DuckDbConnection, ConnectionArena.allocateFrom(sql), DuckDbResultPtr);

        if (duckDbState == DuckDBError())
        {
            System.out.println("Error running query: " + sql);
            throw new Exception();
        }

        // Create Result
        return new Result(DuckDbResultPtr, true);
    }

    @Override
    public void close() throws Exception
    {
        ConnectionArena.close();
    }
}

package monoazul.duckdbffi.driver;

import monoazul.duckdbffi.jextractffi.duckdb_timestamp;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.time.Instant;

import static monoazul.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

// The timestamp is internally stored as micros since epoch in UTC. This means it is an instant.
// Changing to a timezone is easy in Java and up to the user.
public class InstantColumn extends ObjectColumn<Instant>
{
    public InstantColumn (String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = Instant.class;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        Instant[] ResultArray = new Instant[dbChunkSize];

        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);

        try (Arena ColumnArena = Arena.ofConfined())
        {
            MemorySegment Timestamps = duckdb_timestamp.reinterpret(ResultVectorData, dbChunkSize, ColumnArena, null);

            for (int col = 0; col < dbChunkSize; col++)
            {
                long micros = duckdb_timestamp.micros(duckdb_timestamp.asSlice(Timestamps, col));
                Instant TimestampDt = Instant.ofEpochSecond(micros / 1_000_000L, (micros % 1_000_000L) * 1_000L);
                ResultArray[col] = TimestampDt;
            }
            setValidityForChunk(ResultVector, dbChunkSize, ResultArray);
        }
        this.VectorArrays.add(ResultArray);
    }
}

package monoazul.duckdbffi.driver;

import monoazul.duckdbffi.jextractffi.duckdb_timestamp;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static monoazul.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class LocalDateTimeColumn extends ObjectColumn<LocalDateTime>
{
    public LocalDateTimeColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = LocalDateTime.class;
    }

    private static int nanosPartMicros(long micros)
    {
        int microsMod = (int)(micros % 1000_000);
        if (microsMod >= 0)
        {
            return microsMod * 1000;
        }
        else
        {
            return (1000_000 + microsMod) * 1000;
        }
    }

    private static int nanosPartNanos(long nanos)
    {
        long nanosMod = nanos % 1_000_000_000L;
        if (nanosMod >= 0)
        {
            return (int)nanosMod;
        }
        else
        {
            return (int)((1_000_000_000L + nanosMod));
        }
    }

    private static long micros2seconds(long micros)
    {
        if ((micros % 1000_000L) >= 0)
        {
            return micros / 1000_000L;
        }
        else
        {
            return (micros / 1000_000L) - 1;
        }
    }

    private static long nanos2seconds(long nanos)
    {
        if ((nanos % 1_000_000_000L) >= 0)
        {
            return nanos / 1_000_000_000L;
        }
        else
        {
            return (nanos / 1_000_000_000L) - 1;
        }
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        LocalDateTime[] ResultArray = new LocalDateTime[dbChunkSize];

        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);

        try (Arena ColumnArena = Arena.ofConfined())
        {
            MemorySegment Timestamps = duckdb_timestamp.reinterpret(ResultVectorData, dbChunkSize, ColumnArena, null);

            // Duplicating the hot loop to avoid ifs inside
            switch (ColumnDuckDbDataype.type)
            {
                case DuckDbDatatype.DUCKDB_TYPE_TIMESTAMP ->
                {
                    for (int col = 0; col < dbChunkSize; col++)
                    {
                        long micros = duckdb_timestamp.micros(duckdb_timestamp.asSlice(Timestamps, col));
                        LocalDateTime TimestampDt = LocalDateTime.ofEpochSecond(micros2seconds(micros), nanosPartMicros(micros), ZoneOffset.UTC);
                        ResultArray[col] = TimestampDt;
                    }
                }
                case DuckDbDatatype.DUCKDB_TYPE_TIMESTAMP_S ->
                {
                    for (int col = 0; col < dbChunkSize; col++)
                    {
                        long micros = duckdb_timestamp.micros(duckdb_timestamp.asSlice(Timestamps, col));
                        ResultArray[col] = LocalDateTime.ofEpochSecond(micros, 0, ZoneOffset.UTC);
                    }
                }
                case DuckDbDatatype.DUCKDB_TYPE_TIMESTAMP_MS ->
                {
                    for (int col = 0; col < dbChunkSize; col++)
                    {
                        long micros = duckdb_timestamp.micros(duckdb_timestamp.asSlice(Timestamps, col));
                        ResultArray[col] = LocalDateTime.ofEpochSecond(micros2seconds(micros * 1000), nanosPartMicros(micros * 1000), ZoneOffset.UTC);
                    }
                }
                case DuckDbDatatype.DUCKDB_TYPE_TIMESTAMP_NS ->
                {
                    for (int col = 0; col < dbChunkSize; col++)
                    {
                        long micros = duckdb_timestamp.micros(duckdb_timestamp.asSlice(Timestamps, col));
                        ResultArray[col] = LocalDateTime.ofEpochSecond(nanos2seconds(micros), nanosPartNanos(micros), ZoneOffset.UTC);
                    }
                }
            }
            setValidityForChunk(ResultVector, dbChunkSize, ResultArray);
        }
        this.VectorArrays.add(ResultArray);
    }
}

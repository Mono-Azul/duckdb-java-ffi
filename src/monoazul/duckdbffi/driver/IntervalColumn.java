package monoazul.duckdbffi.driver;

import monoazul.duckdbffi.jextractffi.duckdb_interval;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.time.Period;

import static monoazul.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class IntervalColumn extends ObjectColumn<Interval>
{
    public IntervalColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = Interval.class;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        Interval[] ResultArray = new Interval[dbChunkSize];

        try (Arena ColumnArena = Arena.ofConfined())
        {
            MemorySegment IntervalStructArray = duckdb_interval.reinterpret(ResultVectorData, dbChunkSize, ColumnArena, null);

            for (int pos = 0; pos < dbChunkSize; pos++)
            {
                MemorySegment IntervalStruct = duckdb_interval.asSlice(IntervalStructArray, pos);
                Period tmpPeriod = Period.of(0, duckdb_interval.months(IntervalStruct), duckdb_interval.days(IntervalStruct));
                Duration tmpDuration = Duration.ofNanos(duckdb_interval.micros(IntervalStruct) * 1000L);
                ResultArray[pos] = new Interval(tmpPeriod, tmpDuration);
            }

            setValidityForChunk(ResultVector, dbChunkSize, ResultArray);
        }
        this.VectorArrays.add(ResultArray);
    }
}

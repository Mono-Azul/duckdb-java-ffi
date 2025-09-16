package monoazul.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.LocalDate;
import java.time.LocalTime;

import static monoazul.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class LocalTimeColumn extends ObjectColumn<LocalTime>
{
    public LocalTimeColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = LocalTime.class;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert Vector into long[] array
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        LocalTime[] ResultArray = new LocalTime[dbChunkSize];
        long[] TmpResultArray = new long[dbChunkSize];
        ResultVectorData.reinterpret(dbChunkSize);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_LONG, 0, TmpResultArray, 0, dbChunkSize);

        // Create LocalDate from int
        for (int pos = 0; pos < dbChunkSize; pos++)
        {
            // Time stored in micros => * 1000
            ResultArray[pos] = LocalTime.ofNanoOfDay(1000 * TmpResultArray[pos]);
        }

        setValidityForChunk(ResultVector, dbChunkSize, ResultArray);
        this.VectorArrays.add(ResultArray);
    }
}

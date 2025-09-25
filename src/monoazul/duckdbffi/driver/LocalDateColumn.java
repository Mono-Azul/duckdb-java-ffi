package monoazul.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.LocalDate;

import static monoazul.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class LocalDateColumn extends ObjectColumn<LocalDate>
{
    public LocalDateColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = LocalDate.class;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert Vector into int[] array
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        LocalDate[] ResultArray = new LocalDate[dbChunkSize];
        int[] TmpResultArray = new int[dbChunkSize];
        ResultVectorData.reinterpret(dbChunkSize);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_INT, 0, TmpResultArray, 0, dbChunkSize);

        // Create LocalDate from int
        for (int pos = 0; pos < dbChunkSize; pos++)
        {
            ResultArray[pos] = LocalDate.ofEpochDay(TmpResultArray[pos]);
        }

        setValidityForChunk(ResultVector, dbChunkSize, ResultArray);
        this.ChunkArrays.add(ResultArray);
    }
}

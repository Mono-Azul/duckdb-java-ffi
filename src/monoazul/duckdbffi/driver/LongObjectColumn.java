package monoazul.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Arrays;

import static monoazul.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class LongObjectColumn extends ObjectColumn<Long>
{
    public LongObjectColumn (String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = Long.class;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        // Switch to unsigned int path
        if (ColumnDuckDbDataype.type == DuckDbDatatype.DUCKDB_TYPE_UINTEGER)
        {
            addVectorChunkUnsignedInt(ResultVector, dbChunkSize);
            return;
        }

        // Convert Vector into long[] array first and then convert to Long[]
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        long[] PrimitiveResultArray = new long[dbChunkSize];
        ResultVectorData.reinterpret((long) dbChunkSize * 8);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_LONG, 0, PrimitiveResultArray, 0, dbChunkSize);

        Long[] ResultArray = Arrays.stream(PrimitiveResultArray).boxed().toArray(Long[]::new);
        setValidityForChunk(ResultVector, dbChunkSize, ResultArray);

        this.VectorArrays.add(ResultArray);
    }

    protected void addVectorChunkUnsignedInt(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert Vector into int[] array first and then convert to Long[]
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        int[] PrimitiveResultArray = new int[dbChunkSize];
        ResultVectorData.reinterpret((long) dbChunkSize * 4);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_INT, 0, PrimitiveResultArray, 0, dbChunkSize);

        Long[] ResultArray = new Long[dbChunkSize];

        for (int pos = 0; pos < dbChunkSize; pos++)
        {
            ResultArray[pos] = Integer.toUnsignedLong(PrimitiveResultArray[pos]);
        }

        setValidityForChunk(ResultVector, dbChunkSize, ResultArray);
        this.VectorArrays.add(ResultArray);
    }
}

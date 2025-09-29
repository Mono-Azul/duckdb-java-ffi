package mau.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

import static mau.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class LongColumn extends PrimitiveColumn<Long>
{
    private final List<long[]> ChunkArrays;
    public long[] VectorArray;

    public LongColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);

        ChunkArrays = new ArrayList<>();
    }

    @Override
    protected void compactChunks()
    {
        VectorArray = new long[ResMetaData.rowCount()];
        int startPos = 0;

        // Concat all Arrays
        for (long[] arr : ChunkArrays)
        {
            System.arraycopy(arr, 0, VectorArray, startPos, arr.length);
            startPos += arr.length;
        }
        // Empty ChunkArrays
        ChunkArrays.clear();
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert Vector into long[] array
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        long[] ResultArray = new long[dbChunkSize];
        ResultVectorData.reinterpret((long)dbChunkSize * 8);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_LONG, 0, ResultArray, 0, dbChunkSize);
        this.ChunkArrays.add(ResultArray);
    }

    @Override
    public Long getValue(int pos)
    {
        if (getValidity(pos))
        {
            return VectorArray[pos];
        }
        // Null value
        return null;
    }

    // Don't forget to check Validity before using the value as it could be null
    public long getPrimitiveValue(int pos)
    {
        return VectorArray[pos];
    }
}

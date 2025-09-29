package mau.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

import static mau.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class FloatColumn extends PrimitiveColumn<Float>
{
    private final List<float[]> ChunkArrays;
    public float[] VectorArray;

    public FloatColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);

        ChunkArrays = new ArrayList<>();
    }

    @Override
    protected void compactChunks()
    {
        VectorArray = new float[ResMetaData.rowCount()];
        int startPos = 0;

        // Concat all Arrays
        for (float[] arr : ChunkArrays)
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
        // Convert Vector into float[] array
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        float[] ResultArray = new float[dbChunkSize];
        ResultVectorData.reinterpret((long)dbChunkSize * 4);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_FLOAT, 0, ResultArray, 0, dbChunkSize);
        this.ChunkArrays.add(ResultArray);
    }

    @Override
    public Float getValue(int pos)
    {
        if (getValidity(pos))
        {
            return VectorArray[pos];
        }
        // Null value
        return null;
    }

    // Don't forget to check Validity before using the value as it could be null
    public float getPrimitiveValue(int pos)
    {
        return VectorArray[pos];
    }
}

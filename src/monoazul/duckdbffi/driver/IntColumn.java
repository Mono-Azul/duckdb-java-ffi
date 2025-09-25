package monoazul.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

import static monoazul.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class IntColumn extends PrimitiveColumn<Integer>
{
    final private List<int[]> ChunkArrays;
    public int[] VectorArray;

    public IntColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);

        ChunkArrays = new ArrayList<>();
    }

    @Override
    protected void compactChunks()
    {
        VectorArray = new int[ResMetaData.rowCount()];
        int startPos = 0;

        // Concat all Arrays
        for (int[] arr : ChunkArrays)
        {
            System.arraycopy(arr, 0, VectorArray, startPos, arr.length);
            startPos += arr.length;
        }
        // Empty ChunkArrays
        ChunkArrays.clear();
    }

    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert Vector into int[] array
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        int[] ResultArray = new int[dbChunkSize];
        ResultVectorData.reinterpret((long)dbChunkSize * 4);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_INT, 0, ResultArray, 0, dbChunkSize);
        this.ChunkArrays.add(ResultArray);
    }

    @Override
    public Integer getValue(int pos)
    {
        if (getValidity(pos))
        {
            return VectorArray[pos];
        }
        // Null value
        return null;
    }

    // Don't forget to check Validity before using the value as it could be null
    public int getPrimitiveValue(int pos)
    {
        return VectorArray[pos];
    }
}

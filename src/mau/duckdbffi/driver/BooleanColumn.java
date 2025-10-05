package mau.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

import static mau.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class BooleanColumn extends PrimitiveColumn<Boolean>
{
    final private List<boolean[]> ChunkArrays;
    public boolean[] VectorArray;

    public BooleanColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);

        ChunkArrays = new ArrayList<>();
    }

    @Override
    protected void compactChunks()
    {
        VectorArray = new boolean[ResMetaData.rowCount()];
        int startPos = 0;

        // Concat all Arrays
        for (boolean[] arr : ChunkArrays)
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
        // Convert Vector into byte[] array
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        boolean[] ResultArray = new boolean[dbChunkSize];
        byte[] TmpResultArray = new byte[dbChunkSize];
        ResultVectorData.reinterpret(dbChunkSize);

        // Copy is not supported for boolean[], so we take byte
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_BYTE, 0, TmpResultArray, 0, dbChunkSize);

        // Also casting primitive arrays is not allowed, so we have to loop
        for (int pos = 0; pos < dbChunkSize; pos++)
        {
            ResultArray[pos] = TmpResultArray[pos] == 1;
        }

        this.ChunkArrays.add(ResultArray);
        buildValidityMask(ResultVector, dbChunkSize);
    }

    @Override
    public Boolean getValue(int pos)
    {
        if (getValidity(pos))
        {
            return VectorArray[pos];
        }
        // Null value
        return null;
    }

    // Don't forget to check Validity before using the value as it could be null
    public boolean getPrimitiveValue(int pos)
    {
        return VectorArray[pos];
    }
}

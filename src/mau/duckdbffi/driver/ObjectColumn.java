package mau.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static mau.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_validity;

abstract public class ObjectColumn<T> extends Column<T>
{
    final protected List<T[]> ChunkArrays;
    public T[] VectorArray;
    Class<T> Clazz;

    public ObjectColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        ChunkArrays = new ArrayList<>();
    }

    public T getValue(int pos)
    {
        return VectorArray[pos];
    }

    protected void compactChunks()
    {
        // Make one great Array from all parts
        //noinspection unchecked
        VectorArray = (T[])Array.newInstance(Clazz, ResMetaData.rowCount());
        int startPos = 0;

        // Concat all Arrays
        for (T[] arr : ChunkArrays)
        {
            System.arraycopy(arr, 0, VectorArray, startPos, arr.length);
            startPos += arr.length;
        }

        // Empty ChunkArrays
        ChunkArrays.clear();
    }

    protected void setValidityForChunk(MemorySegment ResultVector, int dbChunkSize, T[] ResultArray)
    {
        // Create Validity Mask if necessary
        // Size is ChunkSize / 8 (8 results per byte) and rounded up
        long validityMaskSize = Math.ceilDiv(dbChunkSize, 8);
        MemorySegment ValidityPtr = duckdb_vector_get_validity(ResultVector);

        // Null pointer indicates no need for mask => no nulls
        if (ValidityPtr.address() == 0)
        {
            return;
        }

        BitSet VectorValidityMask = BitSet.valueOf(ValidityPtr.reinterpret(validityMaskSize).toArray(ValueLayout.JAVA_BYTE));

        for (int pos = 0; pos < dbChunkSize; pos++)
        {
            if (!VectorValidityMask.get(pos % dbChunkSize))
            {
                ResultArray[pos] = null;
            }
        }
    }
}

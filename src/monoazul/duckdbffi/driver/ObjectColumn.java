package monoazul.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static monoazul.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_validity;

abstract public class ObjectColumn<T> extends Column<T>
{
    final List<T[]> VectorArrays;
    Class<T> Clazz;

    public ObjectColumn(String ColumnName, DuckDbDatatype ColumnDatatype) {
        super(ColumnName, ColumnDatatype);
        VectorArrays = new ArrayList<>();
    }

    public List<T[]> getVectorArrays()
    {
        return VectorArrays;
    }

    public T[] getAsArray()
    {
        @SuppressWarnings("unchecked") T[] retArray = (T[]) Array.newInstance(Clazz, ResMetaData.rowCount());
        int startPos = 0;

        // Concat all Arrays
        for (T[] arr : VectorArrays)
        {
            System.arraycopy(arr, 0, retArray, startPos, arr.length);
            startPos += arr.length;
        }
        return retArray;
    }

    public T getValue(int pos)
    {
        // Division with floor because List is 0 based
        int arrayPosInList = Math.floorDiv(pos, ResMetaData.maxVectorSize());
        return getVectorArrays().get(arrayPosInList)[pos % ResMetaData.maxVectorSize()];
    }

    protected void setValidityForChunk(MemorySegment ResultVector, int dbChunkSize, T[] ResultArray)
    {
        // Create Validity Mask if necessary
        // Size is ChunkSize / 8 (8 results per byte) and rounded up
        long validityMaskSize = Math.ceilDiv(dbChunkSize, 8);
        MemorySegment ValidityPtr = duckdb_vector_get_validity(ResultVector);

        // Null pointer indicates no need for mask => no nulls
        if (ValidityPtr.address() == 0) {
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

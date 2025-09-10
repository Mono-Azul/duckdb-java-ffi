package monoazul.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

import static monoazul.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class IntColumn extends PrimitiveColumn<Integer>
{
    final List<int[]> VectorArrays;

    public IntColumn (String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);

        VectorArrays = new ArrayList<>();
    }

    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert Vector into int[] array
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        int[] ResultArray = new int[dbChunkSize];
        ResultVectorData.reinterpret((long) dbChunkSize * 4);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_INT, 0, ResultArray, 0, dbChunkSize);
        this.VectorArrays.add(ResultArray);
    }

    @Override
    public Integer getValue(int pos)
    {
        // Division with floor because List is 0 based
        int arrayPosInList = Math.floorDiv(pos, ResMetaData.maxVectorSize());
        if (getValidity(pos)) {
            return (Integer) VectorArrays.get(arrayPosInList)[pos % ResMetaData.maxVectorSize()];
        }
        // Null value
        return null;
    }

    public List<int[]> getVectorArrays()
    {
        return VectorArrays;
    }

    // Make one great Array from all parts
    public int[] getAsArray()
    {
        int[] retArray = new int[ResMetaData.columnsCount()];
        int startPos = 0;

        // Concat all Arrays
        for (int[] arr : VectorArrays)
        {
            System.arraycopy(arr, 0, retArray, startPos, arr.length);
            startPos += arr.length;
        }
        return retArray;
    }

    // Check Validity before using the value as it could be null
    public int getPrimitiveValue(int pos)
    {
        // Division with floor because List is 0 based
        int arrayPosInList = Math.floorDiv(pos, ResMetaData.maxVectorSize());
        int[] IntVectorArray = VectorArrays.get(arrayPosInList);
        return IntVectorArray[pos % ResMetaData.maxVectorSize()];
    }
}

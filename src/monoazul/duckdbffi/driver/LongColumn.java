package monoazul.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

import static monoazul.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class LongColumn extends PrimitiveColumn<Long>
{
    final List<long[]> VectorArrays;

    public LongColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);

        VectorArrays = new ArrayList<>();
    }

    @Override
    public List<long[]> getVectorArrays()
    {
        return VectorArrays;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert Vector into long[] array
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        long[] ResultArray = new long[dbChunkSize];
        ResultVectorData.reinterpret((long)dbChunkSize * 8);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_LONG, 0, ResultArray, 0, dbChunkSize);
        this.VectorArrays.add(ResultArray);
    }

    @Override
    public Long getValue(int pos)
    {
        // Division with floor because List is 0 based
        int arrayPosInList = Math.floorDiv(pos, ResMetaData.maxVectorSize());
        if (getValidity(pos))
        {
            return VectorArrays.get(arrayPosInList)[pos % ResMetaData.maxVectorSize()];
        }
        // Null value
        return null;
    }

    // Make one great Array from all parts
    public long[] getAsArray()
    {
        long[] retArray = new long[ResMetaData.columnsCount()];
        int startPos = 0;

        // Concat all Arrays
        for (long[] arr : VectorArrays)
        {
            System.arraycopy(arr, 0, retArray, startPos, arr.length);
            startPos += arr.length;
        }
        return retArray;
    }

    // Don't forget to check Validity before using the value as it could be null
    public long getPrimitiveValue(int pos)
    {
        // Division with floor because List is 0 based
        int arrayPosInList = Math.floorDiv(pos, ResMetaData.maxVectorSize());
        long[] VectorArray = VectorArrays.get(arrayPosInList);
        return VectorArray[pos % ResMetaData.maxVectorSize()];
    }
}

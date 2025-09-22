package monoazul.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

import static monoazul.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class FloatColumn extends PrimitiveColumn<Float>
{
    final List<float[]> VectorArrays;

    public FloatColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);

        VectorArrays = new ArrayList<>();
    }

    @Override
    public List<float[]> getVectorArrays()
    {
        return VectorArrays;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert Vector into float[] array
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        float[] ResultArray = new float[dbChunkSize];
        ResultVectorData.reinterpret((long)dbChunkSize * 4);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_FLOAT, 0, ResultArray, 0, dbChunkSize);
        this.VectorArrays.add(ResultArray);
    }

    @Override
    public Float getValue(int pos)
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
    public float[] getAsArray()
    {
        float[] retArray = new float[ResMetaData.columnsCount()];
        int startPos = 0;

        // Concat all Arrays
        for (float[] arr : VectorArrays)
        {
            System.arraycopy(arr, 0, retArray, startPos, arr.length);
            startPos += arr.length;
        }
        return retArray;
    }

    // Don't forget to check Validity before using the value as it could be null
    public float getPrimitiveValue(int pos)
    {
        // Division with floor because List is 0 based
        int arrayPosInList = Math.floorDiv(pos, ResMetaData.maxVectorSize());
        float[] VectorArray = VectorArrays.get(arrayPosInList);
        return VectorArray[pos % ResMetaData.maxVectorSize()];
    }
}

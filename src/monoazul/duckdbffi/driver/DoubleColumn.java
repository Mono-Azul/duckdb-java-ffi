package monoazul.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

import static monoazul.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class DoubleColumn extends PrimitiveColumn<Double>
{
    final List<double[]> VectorArrays;

    public DoubleColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);

        VectorArrays = new ArrayList<>();
    }

    @Override
    public List<double[]> getVectorArrays()
    {
        return VectorArrays;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert Vector into double[] array
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        double[] ResultArray = new double[dbChunkSize];
        ResultVectorData.reinterpret((long)dbChunkSize * 4);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_DOUBLE, 0, ResultArray, 0, dbChunkSize);
        this.VectorArrays.add(ResultArray);
    }

    @Override
    public Double getValue(int pos)
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
    public double[] getAsArray()
    {
        double[] retArray = new double[ResMetaData.columnsCount()];
        int startPos = 0;

        // Concat all Arrays
        for (double[] arr : VectorArrays)
        {
            System.arraycopy(arr, 0, retArray, startPos, arr.length);
            startPos += arr.length;
        }
        return retArray;
    }

    // Don't forget to check Validity before using the value as it could be null
    public double getPrimitiveValue(int pos)
    {
        // Division with floor because List is 0 based
        int arrayPosInList = Math.floorDiv(pos, ResMetaData.maxVectorSize());
        double[] VectorArray = VectorArrays.get(arrayPosInList);
        return VectorArray[pos % ResMetaData.maxVectorSize()];
    }
}

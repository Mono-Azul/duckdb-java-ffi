package mau.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

import static mau.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class DoubleColumn extends PrimitiveColumn<Double>
{
    private final List<double[]> ChunkArrays;
    public double[] VectorArray;

    public DoubleColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);

        ChunkArrays = new ArrayList<>();
    }

    @Override
    protected void compactChunks()
    {
        VectorArray = new double[ResMetaData.rowCount()];
        int startPos = 0;

        // Concat all Arrays
        for (double[] arr : ChunkArrays)
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
        // Convert Vector into double[] array
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        double[] ResultArray = new double[dbChunkSize];
        ResultVectorData.reinterpret((long)dbChunkSize * 4);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_DOUBLE, 0, ResultArray, 0, dbChunkSize);
        this.ChunkArrays.add(ResultArray);
        buildValidityMask(ResultVector, dbChunkSize);
    }

    @Override
    public Double getValue(int pos)
    {
        if (getValidity(pos))
        {
            return VectorArray[pos];
        }
        // Null value
        return null;
    }

    // Don't forget to check Validity before using the value as it could be null
    public double getPrimitiveValue(int pos)
    {
        return VectorArray[pos];
    }
}

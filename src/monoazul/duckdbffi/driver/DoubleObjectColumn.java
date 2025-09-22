package monoazul.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static monoazul.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class DoubleObjectColumn extends ObjectColumn<Double>
{
    public DoubleObjectColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = Double.class;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert Vector into double[] array first and then convert to Double[]
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        double[] PrimitiveResultArray = new double[dbChunkSize];
        ResultVectorData.reinterpret((long)dbChunkSize * 4);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_DOUBLE, 0, PrimitiveResultArray, 0, dbChunkSize);

        Double[] ResultArray = new Double[dbChunkSize];
        for (int pos = 0; pos < dbChunkSize; pos++)
        {
            ResultArray[pos] = PrimitiveResultArray[pos];
        }

        setValidityForChunk(ResultVector, dbChunkSize, ResultArray);
        this.VectorArrays.add(ResultArray);
    }
}

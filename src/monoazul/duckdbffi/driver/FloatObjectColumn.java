package monoazul.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static monoazul.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class FloatObjectColumn extends ObjectColumn<Float>
{
    public FloatObjectColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = Float.class;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert Vector into float[] array first and then convert to Float[]
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        float[] PrimitiveResultArray = new float[dbChunkSize];
        ResultVectorData.reinterpret((long)dbChunkSize * 4);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_FLOAT, 0, PrimitiveResultArray, 0, dbChunkSize);

        Float[] ResultArray = new Float[dbChunkSize];
        for (int pos = 0; pos < dbChunkSize; pos++)
        {
            ResultArray[pos] = PrimitiveResultArray[pos];
        }

        setValidityForChunk(ResultVector, dbChunkSize, ResultArray);
        this.ChunkArrays.add(ResultArray);
    }
}

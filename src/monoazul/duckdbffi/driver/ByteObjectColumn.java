package monoazul.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static monoazul.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class ByteObjectColumn extends ObjectColumn<Byte>
{
    public ByteObjectColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = Byte.class;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert Vector into byte[] array first and then convert to Byte[]
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        byte[] PrimitiveResultArray = new byte[dbChunkSize];
        ResultVectorData.reinterpret((long)dbChunkSize * 4);
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_BYTE, 0, PrimitiveResultArray, 0, dbChunkSize);

        Byte[] ResultArray = new Byte[dbChunkSize];
        for (int pos = 0; pos < dbChunkSize; pos++)
        {
            ResultArray[pos] = PrimitiveResultArray[pos];
        }

        setValidityForChunk(ResultVector, dbChunkSize, ResultArray);
        this.ChunkArrays.add(ResultArray);
    }
}

package mau.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static mau.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class BooleanObjectColumn extends ObjectColumn<Boolean>
{
    public BooleanObjectColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = Boolean.class;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert Vector into byte[] array
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        Boolean[] ResultArray = new Boolean[dbChunkSize];
        byte[] TmpResultArray = new byte[dbChunkSize];
        ResultVectorData.reinterpret(dbChunkSize);

        // Copy is not supported for boolean[], so we take byte
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_BYTE, 0, TmpResultArray, 0, dbChunkSize);

        // Also casting primitive arrays is not allowed, so we have to loop
        for (int pos = 0; pos < dbChunkSize; pos++)
        {
            ResultArray[pos] = TmpResultArray[pos] == 1;
        }

        setValidityForChunk(ResultVector, dbChunkSize, ResultArray);
        this.ChunkArrays.add(ResultArray);
    }
}

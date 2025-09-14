package monoazul.duckdbffi.driver;

import java.lang.foreign.MemorySegment;

// This column type is used if the datatype is not supported.
public class UnknownColumn extends ObjectColumn<NullValue>
{
    public UnknownColumn (String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = NullValue.class;
    }
    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        NullValue[] ResultArray = new NullValue[dbChunkSize];

        for (int pos = 0; pos < dbChunkSize; pos++)
        {
            ResultArray[pos] = new NullValue();
        }
        this.VectorArrays.add(ResultArray);
    }
}

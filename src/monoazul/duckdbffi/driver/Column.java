package monoazul.duckdbffi.driver;

import java.lang.foreign.MemorySegment;

abstract public class Column<T> {
    public final DuckDbDatatype ColumnDuckDbDataype;
    //public final JavaDatatype ColumnJavaDatatype;
    public final String ColumnName;
    public ResultMetaData ResMetaData;

    public Column (String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        this.ColumnName = ColumnName;
        this.ColumnDuckDbDataype = ColumnDatatype;
    }

    protected  void addResultMetaData(ResultMetaData ResMetaData)
    {
        this.ResMetaData = ResMetaData;
    }
    protected abstract void addVectorChunk(MemorySegment ResultVector, int dbChunkSize);

    abstract public T getValue(int pos);
}

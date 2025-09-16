package monoazul.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

import static monoazul.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class BooleanColumn extends PrimitiveColumn<Boolean>
{
    final List<boolean[]> VectorArrays;

    public BooleanColumn(String ColumnName, DuckDbDatatype ColumnDatatype) {
        super(ColumnName, ColumnDatatype);

        VectorArrays = new ArrayList<>();
    }

    @Override
    public List<boolean[]> getVectorArrays()
    {
        return VectorArrays;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert Vector into byte[] array
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        boolean[] ResultArray = new boolean[dbChunkSize];
        byte[] TmpResultArray = new byte[dbChunkSize];
        ResultVectorData.reinterpret(dbChunkSize);

        // Copy is not supported for boolean[], so we take byte
        MemorySegment.copy(ResultVectorData, ValueLayout.JAVA_BYTE, 0, TmpResultArray, 0, dbChunkSize);

        // Also casting primitive arrays is not allowed, so we have to loop
        for (int pos = 0; pos < dbChunkSize; pos++)
        {
            ResultArray[pos] = TmpResultArray[pos] == 1;
        }

        this.VectorArrays.add(ResultArray);
    }

    @Override
    public Boolean getValue(int pos)
    {
        // Division with floor because List is 0 based
        int arrayPosInList = Math.floorDiv(pos, ResMetaData.maxVectorSize());
        if (getValidity(pos))
        {
            return (Boolean) VectorArrays.get(arrayPosInList)[pos % ResMetaData.maxVectorSize()];
        }
        // Null value
        return null;
    }

    // Make one great Array from all parts
    public boolean[] getAsArray()
    {
        boolean[] retArray = new boolean[ResMetaData.columnsCount()];
        int startPos = 0;

        // Concat all Arrays
        for (boolean[] arr : VectorArrays)
        {
            System.arraycopy(arr, 0, retArray, startPos, arr.length);
            startPos += arr.length;
        }
        return retArray;
    }

    // Don't forget to check Validity before using the value as it could be null
    public boolean getPrimitiveValue(int pos)
    {
        // Division with floor because List is 0 based
        int arrayPosInList = Math.floorDiv(pos, ResMetaData.maxVectorSize());
        boolean[] VectorArray = VectorArrays.get(arrayPosInList);
        return VectorArray[pos % ResMetaData.maxVectorSize()];
    }
}

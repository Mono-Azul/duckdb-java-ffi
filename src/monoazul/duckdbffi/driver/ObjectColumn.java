package monoazul.duckdbffi.driver;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

abstract public class ObjectColumn<T> extends Column<T>
{
    final List<T[]> VectorArrays;
    Class<T> Clazz;

    public ObjectColumn(String ColumnName, DuckDbDatatype ColumnDatatype) {
        super(ColumnName, ColumnDatatype);
        VectorArrays = new ArrayList<>();
    }

    public List<T[]> getVectorArrays()
    {
        return VectorArrays;
    }

    public T[] getAsArray()
    {
        @SuppressWarnings("unchecked") T[] retArray = (T[]) Array.newInstance(Clazz, ResMetaData.rowCount());
        int startPos = 0;

        // Concat all Arrays
        for (T[] arr : VectorArrays)
        {
            System.arraycopy(arr, 0, retArray, startPos, arr.length);
            startPos += arr.length;
        }
        return retArray;
    }

    public T getValue(int pos)
    {
        // Division with floor because List is 0 based
        int arrayPosInList = Math.floorDiv(pos, ResMetaData.maxVectorSize());
        return getVectorArrays().get(arrayPosInList)[pos % ResMetaData.maxVectorSize()];
    }
}

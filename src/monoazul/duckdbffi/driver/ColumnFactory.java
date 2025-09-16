package monoazul.duckdbffi.driver;

public class ColumnFactory
{
    static Column createBooleanColumn(String ColumnName, DuckDbDatatype ColumnDatatype, boolean primitivesAsObject)
    {
        if (primitivesAsObject)
        {
            return new BooleanObjectColumn(ColumnName, ColumnDatatype);
        }

        return new BooleanColumn(ColumnName, ColumnDatatype);
    }

    static Column createByteColumn(String ColumnName, DuckDbDatatype ColumnDatatype, boolean primitivesAsObject)
    {
        if (primitivesAsObject)
        {
            return new ByteObjectColumn(ColumnName, ColumnDatatype);
        }

        return new ByteColumn(ColumnName, ColumnDatatype);
    }

    static Column createShortColumn(String ColumnName, DuckDbDatatype ColumnDatatype, boolean primitivesAsObject)
    {
        if (primitivesAsObject)
        {
            return new ShortObjectColumn(ColumnName, ColumnDatatype);
        }

        return new ShortColumn(ColumnName, ColumnDatatype);
    }

    static Column createIntColumn(String ColumnName, DuckDbDatatype ColumnDatatype, boolean primitivesAsObject)
    {
        if (primitivesAsObject)
        {
            return new IntObjectColumn(ColumnName, ColumnDatatype);
        }

        return new IntColumn(ColumnName, ColumnDatatype);
    }

    static Column createLongColumn(String ColumnName, DuckDbDatatype ColumnDatatype, boolean primitivesAsObject)
    {
        if (primitivesAsObject)
        {
            return new LongObjectColumn(ColumnName, ColumnDatatype);
        }

        return new LongColumn(ColumnName, ColumnDatatype);
    }

    static Column createFloatColumn(String ColumnName, DuckDbDatatype ColumnDatatype, boolean primitivesAsObject)
    {
        if (primitivesAsObject)
        {
            return new FloatObjectColumn(ColumnName, ColumnDatatype);
        }

        return new FloatColumn(ColumnName, ColumnDatatype);
    }

    static Column createDoubleColumn(String ColumnName, DuckDbDatatype ColumnDatatype, boolean primitivesAsObject)
    {
        if (primitivesAsObject)
        {
            return new DoubleObjectColumn(ColumnName, ColumnDatatype);
        }

        return new DoubleColumn(ColumnName, ColumnDatatype);
    }
}

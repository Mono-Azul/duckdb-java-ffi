package monoazul.duckdbffi.driver;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

import static monoazul.duckdbffi.jextractffi.duckdb_h.*;

public class Result
{
    public final List<Column> Columns;
    public final ResultMetaData ResMetaData;
    // Configuration
    public final boolean primitivesAsObject;
    private final String ErrorMessage;
    private final Integer ErrorNumber;

    // Constructor for Error Result
    public Result(String ErrorMessage, Integer ErrorNumber)
    {
        this.ErrorMessage = ErrorMessage;
        this.ErrorNumber = ErrorNumber;
        this.Columns = null;
        this.ResMetaData = new ResultMetaData(0, 0, 0, 0);
        primitivesAsObject = false;
    }

    public Result(MemorySegment DuckDbResultPtr, MemorySegment DuckDbResult) throws Throwable
    {
        this(DuckDbResultPtr, DuckDbResult, false);
    }

    public Result(MemorySegment DuckDbResultPtr, MemorySegment DuckDbResult, boolean primitivesAsObject) throws Throwable
    {
        this.primitivesAsObject = primitivesAsObject;
        this.ErrorMessage = null;
        this.ErrorNumber = null;

        final int columnsCount = (int)duckdb_column_count(DuckDbResultPtr);
        this.Columns = new ArrayList<>();

        // Maximum Vector size
        duckdb_vector_size invoker = duckdb_vector_size.makeInvoker();
        final int maxVectorSize = (int)(long)invoker.handle().invokeExact();

        int chunkCount = 0;
        int rowCount = 0;
        int dbChunkSize;

        try (Arena ResultArena = Arena.ofConfined())
        {
            // Get first DuckDbChunk and check if there are rows
            MemorySegment DuckDbChunk = duckdb_fetch_chunk(DuckDbResult);

            if (DuckDbChunk.address() == 0)
            {
                // Empty result
                ResMetaData = new ResultMetaData(0, columnsCount, 0, maxVectorSize);
                return;
            }

            // Reinterpret for destruction method
            MemorySegment ExistingDbChunk = DuckDbChunk.reinterpret(ResultArena, Result::destroyDuckDbChunk);

            // There is one chunk and now we can ask for the numer of rows in it
            chunkCount = 1;
            dbChunkSize = (int)duckdb_data_chunk_get_size(DuckDbChunk);
            rowCount = dbChunkSize;


            // Create subclasses of Column for all columns
            for (int col = 0; col < columnsCount; col++)
            {
                MemorySegment ResultVector = duckdb_data_chunk_get_vector(ExistingDbChunk, col);
                MemorySegment ResultVectorLogicalType = duckdb_vector_get_column_type(ResultVector);
                DuckDbDatatype DbDatatype = new DuckDbDatatype((short)duckdb_get_type_id(ResultVectorLogicalType));
                String ColumnName = duckdb_column_name(DuckDbResultPtr, col).reinterpret(Integer.MAX_VALUE).getString(0);

                // Destroy column_type, but we need a pointer first
                destroyDuckDbLogicalType(ResultVectorLogicalType);

                // Create and add new column
                this.Columns.add(createColumnByDatatype(ColumnName, DbDatatype, primitivesAsObject));

                // Add first vector as we have the result vector Segment at hand anyway
                Columns.get(col).addVectorChunk(ResultVector, dbChunkSize);
            }
        }

        while (true)
        {
            try (Arena ChunkArena = Arena.ofConfined())
            {
                // Fill Columns chunk-wise
                MemorySegment DuckDbChunkLoop = duckdb_fetch_chunk(DuckDbResult);

                // Leave while loop if there are no more chunks
                if (DuckDbChunkLoop.address() == 0)
                {
                    break;
                }

                // Reinterpret for destruction method
                MemorySegment ExistingDbChunkLoop = DuckDbChunkLoop.reinterpret(ChunkArena, Result::destroyDuckDbChunk);

                dbChunkSize = (int)duckdb_data_chunk_get_size(DuckDbChunkLoop);
                rowCount += dbChunkSize;
                chunkCount++;

                for (int col = 0; col < columnsCount; col++)
                {
                    MemorySegment ResultVector = duckdb_data_chunk_get_vector(DuckDbChunkLoop, col);
                    Columns.get(col).addVectorChunk(ResultVector, dbChunkSize);
                }
            }
        }

        ResMetaData = new ResultMetaData(rowCount, columnsCount, chunkCount, maxVectorSize);
        // Add Result Metadata to Columns
        // Compact Chunks
        for (int col = 0; col < columnsCount; col++)
        {
            Column Col = Columns.get(col);
            Col.addResultMetaData(ResMetaData);
            Col.compactChunks();

            if (Col instanceof PrimitiveColumn)
            {
                PrimitiveColumn PCol = (PrimitiveColumn)Col;
                PCol.compactValidityBitSet();
            }
        }
    }

    private static Column createColumnByDatatype(String ColumnName, DuckDbDatatype DbDatatype, boolean primitivesAsObject)
    {
        return switch (DbDatatype.type)
        {
            case DuckDbDatatype.DUCKDB_TYPE_INTEGER ->
                    ColumnFactory.createIntColumn(ColumnName, DbDatatype, primitivesAsObject);
            case DuckDbDatatype.DUCKDB_TYPE_BIGINT ->
                    ColumnFactory.createLongColumn(ColumnName, DbDatatype, primitivesAsObject);
            case DuckDbDatatype.DUCKDB_TYPE_VARCHAR -> new StringColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_DECIMAL -> new DecimalColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_FLOAT ->
                    ColumnFactory.createFloatColumn(ColumnName, DbDatatype, primitivesAsObject);
            case DuckDbDatatype.DUCKDB_TYPE_DOUBLE ->
                    ColumnFactory.createDoubleColumn(ColumnName, DbDatatype, primitivesAsObject);
            case DuckDbDatatype.DUCKDB_TYPE_SMALLINT ->
                    ColumnFactory.createShortColumn(ColumnName, DbDatatype, primitivesAsObject);
            case DuckDbDatatype.DUCKDB_TYPE_TINYINT ->
                    ColumnFactory.createByteColumn(ColumnName, DbDatatype, primitivesAsObject);
            case DuckDbDatatype.DUCKDB_TYPE_TIMESTAMP,
                 DuckDbDatatype.DUCKDB_TYPE_TIMESTAMP_S,
                 DuckDbDatatype.DUCKDB_TYPE_TIMESTAMP_MS,
                 DuckDbDatatype.DUCKDB_TYPE_TIMESTAMP_NS -> new LocalDateTimeColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_TIMESTAMP_TZ ->
                // It is possible to use a factory in the future to return e.g. ZonedDateTime
                    new InstantColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_USMALLINT -> new IntObjectColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_UINTEGER -> new LongObjectColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_UTINYINT -> new ShortObjectColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_HUGEINT,
                 DuckDbDatatype.DUCKDB_TYPE_UHUGEINT,
                 DuckDbDatatype.DUCKDB_TYPE_UBIGINT -> new BigIntegerColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_BOOLEAN ->
                    ColumnFactory.createBooleanColumn(ColumnName, DbDatatype, primitivesAsObject);
            case DuckDbDatatype.DUCKDB_TYPE_DATE -> new LocalDateColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_TIME,
                 DuckDbDatatype.DUCKDB_TYPE_TIME_NS -> new LocalTimeColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_UUID -> new UuidColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_INTERVAL -> new IntervalColumn(ColumnName, DbDatatype);
            default -> new UnknownColumn(ColumnName, DbDatatype);
        };
    }

    // Separate Consumer method to destroy the DuckDbChunk, because a pointer is needed
    private static void destroyDuckDbChunk(MemorySegment DuckDbChunk)
    {
        try (Arena ClosingArena = Arena.ofConfined())
        {
            MemorySegment DbChunkPtr = ClosingArena.allocate(8);
            DbChunkPtr.set(ValueLayout.JAVA_LONG, 0, DuckDbChunk.address());
            duckdb_destroy_data_chunk(DbChunkPtr);
        } catch (Throwable e)
        {
            throw new RuntimeException(e);
        }
    }

    // Separate Consumer method to destroy the DuckDbLogicalType, because a pointer is needed
    private static void destroyDuckDbLogicalType(MemorySegment DuckDbLogicalType)
    {
        try (Arena ClosingArena = Arena.ofConfined())
        {
            MemorySegment DuckDbLogicalTypePtr = ClosingArena.allocate(8);
            DuckDbLogicalTypePtr.set(ValueLayout.JAVA_LONG, 0, DuckDbLogicalType.address());
            duckdb_destroy_logical_type(DuckDbLogicalTypePtr);
        } catch (Throwable e)
        {
            throw new RuntimeException(e);
        }
    }

    public List<Object> getRow(int row)
    {
        List<Object> Row = new ArrayList<Object>(ResMetaData.columnsCount());
        for (int col = 0; col < ResMetaData.columnsCount(); col++)
        {
            Row.add(Columns.get(col).getValue(row));
        }
        return Row;
    }

    public int getColumnCount()
    {
        return ResMetaData.columnsCount();
    }

    public String[] getColumnNames()
    {
        return Columns.stream().map(c -> c.ColumnName).toArray(String[]::new);
    }

    public int getRowCount()
    {
        return ResMetaData.rowCount();
    }

    public int getMaxVectorSize()
    {
        return ResMetaData.maxVectorSize();
    }

    public boolean hasError()
    {
        return ErrorNumber != null;
    }

    public String getErrorMessage()
    {
        if (!hasError())
        {
            return "No errors!";
        }
        return ErrorMessage;
    }

    public Integer getErrorNumber()
    {
        return ErrorNumber;
    }
}

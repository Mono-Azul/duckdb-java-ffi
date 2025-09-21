package monoazul.duckdbffi.driver;

import monoazul.duckdbffi.jextractffi.duckdb_h;
import monoazul.duckdbffi.jextractffi.duckdb_result;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

// FFI Imports
import static monoazul.duckdbffi.jextractffi.duckdb_h.*;

public class Result
{
    public final List<Column> Columns;
    public final ResultMetaData ResMetaData;

    // Configuration
    public final boolean primitivesAsObject;

    public Result(MemorySegment DuckDbResultPtr) throws Throwable
    {
        this(DuckDbResultPtr, false);
    }

    public Result(MemorySegment DuckDbResultPtr, boolean primitivesAsObject) throws Throwable
    {
        this.primitivesAsObject = primitivesAsObject;

        final int columnsCount = (int)duckdb_column_count(DuckDbResultPtr);
        this.Columns = new ArrayList<>();

        // Maximum Vector size
        duckdb_vector_size invoker = duckdb_vector_size.makeInvoker();
        final int maxVectorSize = (int)(long)invoker.handle().invokeExact();

        try (Arena ResulArena = Arena.ofConfined())
        {
            // Get first DbChunk and check if there are return columns
            MemorySegment DuckDbResult = duckdb_result.reinterpret(DuckDbResultPtr, ResulArena, duckdb_h::duckdb_destroy_result);
            MemorySegment DbChunk = duckdb_fetch_chunk(DuckDbResult);

            if (DbChunk.address() == 0) {
                // Empty result
                ResMetaData = new ResultMetaData(0, columnsCount, 0, maxVectorSize);
                return;
            }
            int chunkCount = 1;
            int dbChunkSize = (int) duckdb_data_chunk_get_size(DbChunk);
            int rowCount = dbChunkSize;

            // Create subclasses of Column for all columns
            for (int col = 0; col < columnsCount; col++) {
                MemorySegment ResultVector = duckdb_data_chunk_get_vector(DbChunk, col);
                MemorySegment ResultVectorType = duckdb_vector_get_column_type(ResultVector);
                DuckDbDatatype DbDatatype = new DuckDbDatatype((short) duckdb_get_type_id(ResultVectorType));
                String ColumnName = duckdb_column_name(DuckDbResultPtr, col).reinterpret(Integer.MAX_VALUE).getString(0);

                // Destroy column_type, but we need a pointer first
                MemorySegment ResultVectorTypePtr = ResulArena.allocate(C_POINTER);
                ResultVectorTypePtr.set(ValueLayout.JAVA_LONG, 0, ResultVectorType.address());
                duckdb_destroy_logical_type(ResultVectorTypePtr);

                // Create and add new column
                this.Columns.add(createColumnByDatatype(ColumnName, DbDatatype, primitivesAsObject));

                // Add first vector as we have the result vector Segment at hand anyway
                Columns.get(col).addVectorChunk(ResultVector, dbChunkSize);
            }

            // We need a pointer to the Chunk in order to destroy it
            MemorySegment DbChunkPtr = ResulArena.allocate(C_POINTER);
            DbChunkPtr.set(ValueLayout.JAVA_LONG, 0, DbChunk.address());
            duckdb_destroy_data_chunk(DbChunkPtr);

            // Fill Columns chunk-wise
            DbChunk = duckdb_fetch_chunk(DuckDbResult);

            while (DbChunk.address() != 0) {
                dbChunkSize = (int) duckdb_data_chunk_get_size(DbChunk);
                rowCount = +dbChunkSize;
                chunkCount++;

                for (int col = 0; col < columnsCount; col++) {
                    MemorySegment ResultVector = duckdb_data_chunk_get_vector(DbChunk, col);
                    Columns.get(col).addVectorChunk(ResultVector, dbChunkSize);
                }

                DbChunkPtr.set(ValueLayout.JAVA_LONG, 0, DbChunk.address());
                duckdb_destroy_data_chunk(DbChunk);
                DbChunk = duckdb_fetch_chunk(DuckDbResult);
            }

            ResMetaData = new ResultMetaData(rowCount, columnsCount, chunkCount, maxVectorSize);
            // Add Result Metadata to Columns
            for (int col = 0; col < columnsCount; col++) {
                Columns.get(col).addResultMetaData(ResMetaData);
            }
        }
    }

    private static Column createColumnByDatatype(String ColumnName, DuckDbDatatype DbDatatype, boolean primitivesAsObject)
    {
        System.out.println(DbDatatype.type);
        return switch (DbDatatype.type) {
            case DuckDbDatatype.DUCKDB_TYPE_INTEGER ->
                    ColumnFactory.createIntColumn(ColumnName, DbDatatype, primitivesAsObject);
            case DuckDbDatatype.DUCKDB_TYPE_BIGINT ->
                    ColumnFactory.createLongColumn(ColumnName, DbDatatype, primitivesAsObject);
            case DuckDbDatatype.DUCKDB_TYPE_VARCHAR ->
                    new StringColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_DECIMAL ->
                    new DecimalColumn(ColumnName, DbDatatype);
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
                 DuckDbDatatype.DUCKDB_TYPE_TIMESTAMP_NS ->
                    new LocalDateTimeColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_TIMESTAMP_TZ ->
                    // It is possible to use a factory in the future to return e.g. ZonedDateTime
                    new InstantColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_USMALLINT ->
                    new IntObjectColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_UINTEGER->
                    new LongObjectColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_UTINYINT ->
                    new ShortObjectColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_HUGEINT,
                 DuckDbDatatype.DUCKDB_TYPE_UHUGEINT,
                 DuckDbDatatype.DUCKDB_TYPE_UBIGINT->
                    new BigIntegerColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_BOOLEAN ->
                    ColumnFactory.createBooleanColumn(ColumnName, DbDatatype, primitivesAsObject);
            case DuckDbDatatype.DUCKDB_TYPE_DATE ->
                    new LocalDateColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_TIME,
                 DuckDbDatatype.DUCKDB_TYPE_TIME_NS ->
                    new LocalTimeColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_UUID ->
                    new UuidColumn(ColumnName, DbDatatype);
            case DuckDbDatatype.DUCKDB_TYPE_INTERVAL ->
                    new IntervalColumn(ColumnName, DbDatatype);
            default -> new UnknownColumn(ColumnName, DbDatatype);
        };
    }

    public List<Object> getRow(int row)
    {
        List<Object> Row = new ArrayList<Object>((int)ResMetaData.columnsCount());
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

    public int getRowCount()
    {
        return ResMetaData.rowCount();
    }

    public int getMaxVectorSize()
    {
        return ResMetaData.maxVectorSize();
    }

    public int getChunkCount()
    {
        return ResMetaData.chunkCount();
    }
}

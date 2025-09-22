package monoazul.duckdbffi.driver;

public class DuckDbDatatype
{
    // Copied from duckdb.h duckdb_type enum
    final static short DUCKDB_TYPE_INVALID = 0;
    // bool
    final static short DUCKDB_TYPE_BOOLEAN = 1;
    // int8_t
    final static short DUCKDB_TYPE_TINYINT = 2;
    // int16_t
    final static short DUCKDB_TYPE_SMALLINT = 3;
    // int32_t
    final static short DUCKDB_TYPE_INTEGER = 4;
    // int64_t
    final static short DUCKDB_TYPE_BIGINT = 5;
    // uint8_t
    final static short DUCKDB_TYPE_UTINYINT = 6;
    // uint16_t
    final static short DUCKDB_TYPE_USMALLINT = 7;
    // uint32_t
    final static short DUCKDB_TYPE_UINTEGER = 8;
    // uint64_t
    final static short DUCKDB_TYPE_UBIGINT = 9;
    // float
    final static short DUCKDB_TYPE_FLOAT = 10;
    // double
    final static short DUCKDB_TYPE_DOUBLE = 11;
    // duckdb_timestamp (microseconds)
    final static short DUCKDB_TYPE_TIMESTAMP = 12;
    // duckdb_date
    final static short DUCKDB_TYPE_DATE = 13;
    // duckdb_time
    final static short DUCKDB_TYPE_TIME = 14;
    // duckdb_interval
    final static short DUCKDB_TYPE_INTERVAL = 15;
    // duckdb_hugeint
    final static short DUCKDB_TYPE_HUGEINT = 16;
    // duckdb_uhugeint
    final static short DUCKDB_TYPE_UHUGEINT = 32;
    // const char*
    final static short DUCKDB_TYPE_VARCHAR = 17;
    // duckdb_blob
    final static short DUCKDB_TYPE_BLOB = 18;
    // duckdb_decimal
    final static short DUCKDB_TYPE_DECIMAL = 19;
    // duckdb_timestamp_s (seconds)
    final static short DUCKDB_TYPE_TIMESTAMP_S = 20;
    // duckdb_timestamp_ms (milliseconds)
    final static short DUCKDB_TYPE_TIMESTAMP_MS = 21;
    // duckdb_timestamp_ns (nanoseconds)
    final static short DUCKDB_TYPE_TIMESTAMP_NS = 22;
    // enum type; only useful as logical type
    final static short DUCKDB_TYPE_ENUM = 23;
    // list type; only useful as logical type
    final static short DUCKDB_TYPE_LIST = 24;
    // struct type; only useful as logical type
    final static short DUCKDB_TYPE_STRUCT = 25;
    // map type; only useful as logical type
    final static short DUCKDB_TYPE_MAP = 26;
    // duckdb_array; only useful as logical type
    final static short DUCKDB_TYPE_ARRAY = 33;
    // duckdb_hugeint
    final static short DUCKDB_TYPE_UUID = 27;
    // union type; only useful as logical type
    final static short DUCKDB_TYPE_UNION = 28;
    // duckdb_bit
    final static short DUCKDB_TYPE_BIT = 29;
    // duckdb_time_tz
    final static short DUCKDB_TYPE_TIME_TZ = 30;
    // duckdb_timestamp (microseconds)
    final static short DUCKDB_TYPE_TIMESTAMP_TZ = 31;
    // ANY type
    final static short DUCKDB_TYPE_ANY = 34;
    // duckdb_bignum
    final static short DUCKDB_TYPE_BIGNUM = 35;
    // SQLNULL type
    final static short DUCKDB_TYPE_SQLNULL = 36;
    // STRING_LITERAL type
    final static short DUCKDB_TYPE_STRING_LITERAL = 37;
    // INTEGER_LITERAL type
    final static short DUCKDB_TYPE_INTEGER_LITERAL = 38;
    // duckdb_time_ns (nanoseconds)
    final static short DUCKDB_TYPE_TIME_NS = 39;

    final short type;

    public DuckDbDatatype(short datatype)
    {
        // Values outside the numbers above are invalid => datatype = 0
        if (datatype >= 0
                && datatype <= 39)
        {
            this.type = datatype;
        }
        else
        {
            this.type = 0;
        }
    }
}

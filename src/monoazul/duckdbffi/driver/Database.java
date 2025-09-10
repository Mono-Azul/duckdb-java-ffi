package monoazul.duckdbffi.driver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

// FFI Imports
import static monoazul.duckdbffi.jextractffi.duckdb_h.*;
import monoazul.duckdbffi.jextractffi.*;

public class Database implements AutoCloseable {

    // Copied from Duckdb JDBC driver class DuckDBNative - refactor!
    // Load shared object file that is included in the .jar.
    // The name depends on the architecture.
    static {
        try {
            String os_name = "";
            String os_arch;
            String os_name_detect = System.getProperty("os.name").toLowerCase().trim();
            String os_arch_detect = System.getProperty("os.arch").toLowerCase().trim();

            os_arch = switch (os_arch_detect) {
                case "x86_64", "amd64" -> "amd64";
                case "aarch64", "arm64" -> "arm64";
                case "i386" -> "i386";
                default -> throw new IllegalStateException("Unsupported system architecture");
            };
            if (os_name_detect.startsWith("windows")) {
                os_name = "windows";
            } else if (os_name_detect.startsWith("mac")) {
                os_name = "osx";
                os_arch = "universal";
            } else if (os_name_detect.startsWith("linux")) {
                os_name = "linux";
            }
            String lib_res_name = "/libduckdb_java.so"
                    + "_" + os_name + "_" + os_arch;

            Path lib_file = Files.createTempFile("libduckdb_java", ".so");
            URL lib_res = Database.class.getResource(lib_res_name);
            if (lib_res == null) {
                System.load(Paths.get("libduckdb-" + os_name  + "-" + os_arch,
                        "libduckdb.so").normalize().toAbsolutePath().toString());
            } else {
                try (final InputStream lib_res_input_stream = lib_res.openStream()) {
                    Files.copy(lib_res_input_stream, lib_file, StandardCopyOption.REPLACE_EXISTING);
                }
                new File(lib_file.toString()).deleteOnExit();
                System.load(lib_file.toAbsolutePath().toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String DatabaseFileName;

    public Arena DatabaseArena;
    private MemorySegment DuckDbDatabase; // _duckdb_database
    private MemorySegment DuckDbConnection; // _duckdb_connection


    public Database (String DatabaseFileName)
    {
        DatabaseArena = Arena.ofShared();

        MemorySegment DatabaseFileNameNative = DatabaseArena.allocateFrom(DatabaseFileName);

        //DuckDbDatabase = _duckdb_database.allocate(DatabaseArena);
        //MemorySegment DuckDbDatabasePtr = DatabaseArena.allocate(duckdb_database);
        MemorySegment DuckDbDatabasePtrPtr = DatabaseArena.allocate(duckdb_database);
        MemorySegment DuckDbConnectionPtrPtr = DatabaseArena.allocate(duckdb_connection);

        //DatabaseArena.allocate(duckdb_connection); //_duckdb_connection.allocate(DatabaseArena);

        this.DatabaseFileName = DatabaseFileName;

        System.out.println(DuckDbDatabasePtrPtr.address());
        System.out.println(DuckDbDatabasePtrPtr.get(ValueLayout.JAVA_LONG, 0));


        int ret = duckdb_open(DatabaseFileNameNative, DuckDbDatabasePtrPtr);


        //int ret = duckdb_open(nullptr, DuckDbDatabase);

        if (ret == DuckDBError())
        {
            System.out.println("Error Open");
        }

//        MemorySegment DuckDbDatabasePtr = MemorySegment.ofAddress(DuckDbDatabasePtrPtr.get(ValueLayout.JAVA_LONG, 0));
//        DuckDbDatabasePtr = DuckDbDatabasePtr.reinterpret(8);

        MemorySegment DuckDbDatabasePtr = DuckDbDatabasePtrPtr.get(C_POINTER, 0);


        //DuckDbDatabase = _duckdb_database.reinterpret(DuckDbDatabase, DatabaseArena, duckdb_h::duckdb_close);
        //DuckDbDatabasePtr.set(ValueLayout.JAVA_LONG, 0, DuckDbDatabase.address());

        //System.out.println(DuckDbDatabase.address());
        System.out.println(DuckDbDatabasePtr.address());
        //System.out.println(DuckDbDatabasePtr.get(ValueLayout.JAVA_LONG, 0));

        ret = duckdb_connect(DuckDbDatabasePtr, DuckDbConnectionPtrPtr);


        if (ret == DuckDBError())
        {
            System.out.println("Error connect");
        }

        MemorySegment DuckDbConnectionPtr = MemorySegment.ofAddress(DuckDbConnectionPtrPtr.get(ValueLayout.JAVA_LONG, 0));
        DuckDbConnectionPtr = DuckDbConnectionPtr.reinterpret(8);

        MemorySegment DuckDbResult = duckdb_result.allocate(DatabaseArena);

        System.out.println("Addresse Result struct: " + DuckDbResult.address());

        MemorySegment DuckDbResultPtr = DatabaseArena.allocate(C_POINTER);
        DuckDbResultPtr.set(ValueLayout.JAVA_LONG, 0, DuckDbResult.address());
        System.out.println("Addresse Result pointer value: " + DuckDbResultPtr.get(ValueLayout.JAVA_LONG, 0));

        duckdb_query(DuckDbConnectionPtr,
                DatabaseArena.allocateFrom("create table t (id int4 null, id2 int4);"),
                DuckDbResultPtr);

        duckdb_query(DuckDbConnectionPtr,
                DatabaseArena.allocateFrom("insert into t values (1,1), (null,2);"),
                DuckDbResultPtr);

        ret = duckdb_query(DuckDbConnectionPtr,
                DatabaseArena.allocateFrom("SELECT * FROM duckdb_memory();"),
                //DatabaseArena.allocateFrom("SELECT 'üüüüüüö', 1001000000001::long, -1.234::decimal(4,3), TIMESTAMP '1902-09-20 11:30:00.123456789';"),
                //DatabaseArena.allocateFrom("SELECT 'üüüüüüö', 1001000000001::long, -1.234::decimal(4,3), TIMESTAMP_MS '1902-09-20 11:30:00.123456789+01:00';"),
                //DatabaseArena.allocateFrom("select * from (SELECT 1 as nullcoc ,1 as intcolumn union select null,2 union select 1,3) order by 2;"),
                //DatabaseArena.allocateFrom("select * from t order by 2;"),
                DuckDbResultPtr);

        if (ret == DuckDBError())
        {
            System.out.println("Error query");
        }

        try {
            Result x = new Result(DuckDbResultPtr);

            System.out.println(x.ResMetaData);

            StringColumn ic = (StringColumn)x.Columns.getFirst();
            List<String[]> y = ic.getVectorArrays();

            //Integer v = (Integer) x.Columns.getFirst().getValue(0);
            //var z = (List<int[]>) x.Columns.getFirst().getVectorArrays();

            System.out.println(ic.ColumnName);
            System.out.println(Arrays.toString(y.getFirst()));
            //System.out.println(ic.getValidity(0));
            //System.out.println(ic.ValidityMasks.getFirst().get(1));
            System.out.println("RowCnt: " + x.ResMetaData.rowCount());

            for (int row = 0; row < x.ResMetaData.rowCount(); row++) {
                var Row = x.getRow(row);
                System.out.println("Row " + row);
                for (Object o : Row) {
                    System.out.println(o);
                }
            }

            System.out.println(Arrays.toString(ic.getAsArray()));


            if (x != null) return;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }



        //DuckDbResult = MemorySegment.ofAddress(DuckDbResultPtr.get(ValueLayout.JAVA_LONG, 0));
        //DuckDbResult = duckdb_result.reinterpret(DuckDbResult, DatabaseArena, duckdb_h::duckdb_destroy_result);

        System.out.println("Addresse Result pointer: " + DuckDbResultPtr.address());
        System.out.println("Addresse Result pointer value: " + DuckDbResultPtr.get(ValueLayout.JAVA_LONG, 0));
        System.out.println("Addresse Result struct: " + DuckDbResult.address());

        duckdb_result.deprecated_column_count(DuckDbResult);

        final MemorySegment ColNameMemSegment = duckdb_column_name(DuckDbResultPtr, 0);
        String ColName = ColNameMemSegment.reinterpret(Integer.MAX_VALUE).getString(0);
        System.out.println("Column name 0: " + ColName);

        final MemorySegment ColName1MemSegment = duckdb_column_name(DuckDbResultPtr, 1);
        String ColName1 = ColName1MemSegment.reinterpret(Integer.MAX_VALUE).getString(0);
        System.out.println("Column name 1: " + ColName1);

        System.out.println("Column count " + duckdb_column_count(DuckDbResultPtr));
        //System.out.println("Row count " + duckdb_row_count(DuckDbResultPtr));
        System.out.println("Rows changed count " + duckdb_rows_changed(DuckDbResultPtr));

        MemorySegment Chunk1 = DatabaseArena.allocate(duckdb_data_chunk);
        MemorySegment Chunk2 = DatabaseArena.allocate(duckdb_data_chunk);

        System.out.println("Chunk1 Address: " + Chunk1.address());

        //Chunk1 = duckdb_result_get_chunk(DuckDbResult, 0);

        DuckDbResult = duckdb_result.reinterpret(DuckDbResultPtr, DatabaseArena, duckdb_h::duckdb_destroy_result);

        Chunk1 = duckdb_fetch_chunk(DuckDbResult);
        System.out.println("Chunk1 Address: " + Chunk1.address());
        //System.out.println("Chunk1 Address: " + Chunk1.get(ValueLayout.JAVA_LONG, 0));

        MemorySegment ResultVector1 = DatabaseArena.allocate((duckdb_vector));

        ResultVector1 = duckdb_data_chunk_get_vector(Chunk1, 0);

        MemorySegment ValidityPtr1 = duckdb_vector_get_validity(ResultVector1);

        if (ValidityPtr1.address() != 0) {
            byte[] ValidityArray = ValidityPtr1.reinterpret(1).toArray(ValueLayout.JAVA_BYTE);
            System.out.println("Validity: " + ValidityArray[0]);
            System.out.println("Validity: " + ValidityArray[1]);
        }



        MemorySegment ResultVector2 = duckdb_data_chunk_get_vector(Chunk1, 1);
        MemorySegment ResultVector2Data = duckdb_vector_get_data(ResultVector2);

        MemorySegment ResultVector3 = duckdb_data_chunk_get_vector(Chunk1, 2);
        MemorySegment ResultVector3Data = duckdb_vector_get_data(ResultVector3);
        System.out.println("Vector2 Data Address: " + ResultVector2Data.address());
        System.out.println("Vector3 Data Address: " + ResultVector3Data.address());

        MemorySegment ResultVector4 = duckdb_data_chunk_get_vector(Chunk1, 3);
        MemorySegment ResultVector4Data = duckdb_vector_get_data(ResultVector4);

        MemorySegment VecColumnType1 = DatabaseArena.allocate(duckdb_logical_type);

        VecColumnType1 = duckdb_vector_get_column_type(ResultVector1);
        MemorySegment VecColumnType2 = duckdb_vector_get_column_type(ResultVector2);
        MemorySegment VecColumnType3 = duckdb_vector_get_column_type(ResultVector3);
        MemorySegment VecColumnLogType3 = duckdb_column_logical_type(DuckDbResult, 2);
        MemorySegment VecColumnType4 = duckdb_vector_get_column_type(ResultVector4);
        MemorySegment VecColumnLogType4 = duckdb_column_logical_type(DuckDbResult, 3);

        int duck_type = duckdb_get_type_id(VecColumnType1);
        long Chunk1Size = duckdb_data_chunk_get_size(Chunk1);
        System.out.println("Vector1 Type: " + duck_type);
        System.out.println("Chunk1 size " + duckdb_data_chunk_get_size(Chunk1));
        MemorySegment ValidityVec1 = duckdb_vector_get_validity(ResultVector1);
        System.out.println("Vector1 Validity Address: " + ValidityVec1.address());

        int duck_type2 = duckdb_get_type_id(VecColumnType2);
        System.out.println("Vector2 Type: " + duck_type2);
        MemorySegment ValidityVec2 = duckdb_vector_get_validity(ResultVector2);
        System.out.println("Vector2 Validity Address: " + ValidityVec2.address());

        int duck_type3 = duckdb_get_type_id(VecColumnType3);
        System.out.println("Vector3 Type: " + duck_type3);
        MemorySegment ValidityVec3 = duckdb_vector_get_validity(ResultVector3);
        System.out.println("Vector3 Validity Address: " + ValidityVec3.address());

        int duck_type4 = duckdb_get_type_id(VecColumnType4);
        System.out.println("Vector4 Type: " + duck_type4);
        MemorySegment ValidityVec4 = duckdb_vector_get_validity(ResultVector4);
        System.out.println("Vector4 Validity Address: " + ValidityVec3.address());

        MemorySegment ResultVector1Data = duckdb_vector_get_data(ResultVector1);
        ResultVector1Data.reinterpret(Chunk1Size * duckdb_string_t.sizeof());
        //ResultVector3Data.reinterpret(Chunk1Size * duckdb_decimal.sizeof());

        for (int i = 0; i < Chunk1Size; i++) {
            MemorySegment String_t = duckdb_string_t.reinterpret(ResultVector1Data, Chunk1Size, DatabaseArena, duckdb_h::duckdb_destroy_vector);
            MemorySegment String_struct = duckdb_string_t.asSlice(String_t, i);
            if (duckdb_string_is_inlined(String_struct))
            {

                var StrValue = duckdb_string_t.value(String_struct);
                var StrInlined = duckdb_string_t.value.inlined(StrValue);
                var InlinedStrArray = duckdb_string_t.value.inlined.inlined(StrInlined);
                int length = duckdb_string_t.value.inlined.length(StrInlined);

                //String FinalString = InlinedStrArray.reinterpret(12).getString(0);
                byte[] InlineStrByteArray = InlinedStrArray.reinterpret(length).toArray(ValueLayout.JAVA_BYTE);
                String FinalString = new String(InlineStrByteArray, StandardCharsets.UTF_8);
                System.out.println("String inlined: " + FinalString);

                // Umwandlung nur mit length und über byte[]
                //String FinalString2 = duckdb_string_t.value.inlined.inlined(duckdb_string_t.value.inlined(duckdb_string_t.value(String_struct))).reinterpret(12).getString(0);
                //System.out.println("String inlined2: " + FinalString2 + " - " + FinalString2.length());
            }
            else
            {
                var StrValue = duckdb_string_t.value(String_struct);
                var StrPtr = duckdb_string_t.value.pointer(StrValue); // Wenn man weiss was es ist, auch direkter Einstieg hier möglich mit Zeiger auf Union
                var StrArrayPtr = duckdb_string_t.value.pointer.ptr(StrPtr);

                System.out.println(duckdb_string_t.value.pointer.length(StrPtr));
                System.out.println(StrValue.address());
                System.out.println(StrPtr.address());
                System.out.println(StrArrayPtr.address());
                //System.out.println(StrArrayPtr.address() + " " + StrArrayPtr.get(ValueLayout.JAVA_CHAR, 0));
                //System.out.println(StrArray.address());
                byte[] byteString = StrArrayPtr.reinterpret(duckdb_string_t.value.pointer.length(StrPtr)).toArray(ValueLayout.JAVA_BYTE);
                String FinalString = new String(byteString, StandardCharsets.UTF_8);
                System.out.println("String Pointer: " + FinalString);

            }
        }

        long[] ResultArray2 = new long[(int)Chunk1Size];
        ResultVector2Data.reinterpret(Chunk1Size * 8);

        MemorySegment.copy(ResultVector2Data, ValueLayout.JAVA_LONG, 0, ResultArray2, 0, (int)Chunk1Size);

        for (int i = 0; i < Chunk1Size; i++) {
            System.out.println(ResultArray2[i]);
        }


        Chunk2 = duckdb_fetch_chunk(DuckDbResult);
        System.out.println("Chunk2 Address: " + Chunk2.address());
        //System.out.println("Chunk2 Address: " + Chunk2.get(ValueLayout.JAVA_LONG, 0));

        System.out.println("Vector2 Data Address: " + ResultVector2Data.address());
        System.out.println("Vector3 Data Address: " + ResultVector3Data.address());

        MemorySegment DecimalStruct = duckdb_decimal.reinterpret(ResultVector3Data, Chunk1Size, DatabaseArena, duckdb_h::duckdb_destroy_vector);
        for (int i = 0; i < Chunk1Size; i++) {
            MemorySegment TmpStruct = duckdb_decimal.asSlice(DecimalStruct, i);

            System.out.println("Vector3 Data Slice Address: " + TmpStruct.address());
            var DecimalValueStruct = duckdb_decimal.value(TmpStruct);
            System.out.println("Vector3 DecimalValueStruct Address: " + DecimalValueStruct.address());
            var DecValueStruct = duckdb_hugeint.reinterpret(DecimalValueStruct, DatabaseArena, duckdb_h::duckdb_destroy_vector);

            long v1 = duckdb_hugeint.upper(DecValueStruct);
            long v2 = duckdb_hugeint.lower(DecValueStruct);

            System.out.println("Decimal values: " + v1 + " " + v2);

            byte[] byteStruct = TmpStruct.reinterpret(8).toArray(ValueLayout.JAVA_BYTE);

            for (int j = 0; j < byteStruct.length; j++) {
                System.out.println("Decimal byte " + j + ": " + byteStruct[j]);
            }
        }

        // Decimal as array/vector
        byte VecDecWidth = duckdb_decimal_width(VecColumnLogType3);
        byte VecDecSize = duckdb_decimal_scale(VecColumnLogType3);
        System.out.println("Decimal Width/Scale " + VecDecWidth + " " + VecDecSize);

        BigDecimal[] DecimalArray = new BigDecimal[(int)Chunk1Size];

        if (VecDecWidth < 5)
        {
            short[] Decimal4Vector = new short[(int)Chunk1Size];
            MemorySegment.copy(ResultVector3Data, ValueLayout.JAVA_SHORT, 0, Decimal4Vector, 0, (int)Chunk1Size);

            for (int i = 0; i < Chunk1Size; i++) {
                System.out.println(Decimal4Vector[i]);
                DecimalArray[i] = BigDecimal.valueOf(Decimal4Vector[i], VecDecSize);
                System.out.println(DecimalArray[i]);
            }
        } else if (VecDecWidth < 10)
        {
            int[] Decimal4Vector = new int[(int)Chunk1Size];
            MemorySegment.copy(ResultVector3Data, ValueLayout.JAVA_INT, 0, Decimal4Vector, 0, (int)Chunk1Size);

            for (int i = 0; i < Chunk1Size; i++) {
                System.out.println(Decimal4Vector[i]);
                DecimalArray[i] = BigDecimal.valueOf(Decimal4Vector[i], VecDecSize);
                System.out.println(DecimalArray[i]);
            }
        } else if (VecDecWidth < 19)
        {
            long[] Decimal4Vector = new long[(int)Chunk1Size];
            MemorySegment.copy(ResultVector3Data, ValueLayout.JAVA_LONG, 0, Decimal4Vector, 0, (int)Chunk1Size);

            for (int i = 0; i < Chunk1Size; i++) {
                System.out.println(Decimal4Vector[i]);
                DecimalArray[i] = BigDecimal.valueOf(Decimal4Vector[i], VecDecSize);
                System.out.println(DecimalArray[i]);
            }
        }
        else
        {
            MemorySegment HughintVector = duckdb_hugeint.reinterpret(ResultVector3Data, Chunk1Size, DatabaseArena, duckdb_h::duckdb_destroy_vector);

            for (int i = 0; i < Chunk1Size; i++) {
                MemorySegment OneHughint = duckdb_hugeint.asSlice(HughintVector, i);
                long lower = duckdb_hugeint.lower(OneHughint);
                long upper = duckdb_hugeint.upper(OneHughint);
                System.out.println(lower + " " + upper);

                MemorySegment HIntString = duckdb_value_to_string(duckdb_create_hugeint(OneHughint));

                System.out.println(HIntString.getString(0));

                DecimalArray[i] = new BigDecimal(HIntString.getString(0)).movePointLeft(VecDecSize);
                System.out.println(DecimalArray[i]);
            }
        }

        MemorySegment Timestamps = duckdb_timestamp.reinterpret(ResultVector4Data, Chunk1Size, DatabaseArena, duckdb_h::duckdb_destroy_vector);

        LocalDateTime[] LocalDateTimeVec = new LocalDateTime[(int)Chunk1Size];

        for (int i = 0; i < Chunk1Size; i++) {
            MemorySegment OneTimestamp = duckdb_timestamp.asSlice(Timestamps, i);
            long micros = duckdb_timestamp.micros(OneTimestamp);

            //LocalDateTime TimestampDt = LocalDateTime.ofEpochSecond(micros2seconds(micros), nanosPartMicros(micros), ZoneOffset.UTC);

            // Für DUCKDB_TYPE_TIMESTAMP_S
            //LocalDateTime TimestampDt = LocalDateTime.ofEpochSecond(micros, 0, ZoneOffset.UTC);

            // TIMESTAMP_MS
            LocalDateTime TimestampDt = LocalDateTime.ofEpochSecond(micros2seconds(micros * 1000), nanosPartMicros(micros * 1000), ZoneOffset.UTC);

            // DUCKDB_TYPE_TIMESTAMP_NS
            //LocalDateTime TimestampDt = LocalDateTime.ofEpochSecond(nanos2seconds(micros), nanosPartNanos(micros), ZoneOffset.UTC);

            LocalDateTimeVec[i] = TimestampDt;
            System.out.println(TimestampDt);
        }

        duckdb_vector_size invoker = duckdb_vector_size.makeInvoker();
        long vsize = 0;
        try {
            vsize = (long)invoker.handle().invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        System.out.println("vsize: " + vsize);
        System.out.println("Chunk2 Address: " + Chunk2.address());
    }

    private static int nanosPartMicros(long micros) {
        int microsMod = (int)(micros % 1000_000);
        if (microsMod >= 0) {
            return microsMod * 1000;
        } else {
            return (1000_000 + microsMod) * 1000;
        }
    }

    private static int nanosPartNanos(long nanos) {
        long nanosMod = nanos % 1_000_000_000L;
        if (nanosMod >= 0) {
            return (int) nanosMod;
        } else {
            return (int) ((1_000_000_000L + nanosMod));
        }
    }

    private static long micros2seconds(long micros) {
        if ((micros % 1000_000L) >= 0) {
            return micros / 1000_000L;
        } else {
            return (micros / 1000_000L) - 1;
        }
    }

    private static long nanos2seconds(long nanos) {
        if ((nanos % 1_000_000_000L) >= 0) {
            return nanos / 1_000_000_000L;
        } else {
            return (nanos / 1_000_000_000L) - 1;
        }
    }

    @Override
    public void close() throws Exception {

        DatabaseArena.close();
    }
}

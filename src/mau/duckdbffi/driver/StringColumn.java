package mau.duckdbffi.driver;

import mau.duckdbffi.jextractffi.duckdb_string_t;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

import static mau.duckdbffi.jextractffi.duckdb_h.duckdb_string_is_inlined;
import static mau.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

public class StringColumn extends ObjectColumn<String>
{
    public StringColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = String.class;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        String[] ResultArray = new String[dbChunkSize];

        try (Arena ColumnArena = Arena.ofConfined())
        {
            MemorySegment String_t = duckdb_string_t.reinterpret(ResultVectorData, dbChunkSize, ColumnArena, null);

            for (int pos = 0; pos < dbChunkSize; pos++)
            {
                MemorySegment String_struct = duckdb_string_t.asSlice(String_t, pos);

                // Short strings can be inlined, longer ones have a pointer => this is a union struct!
                if (duckdb_string_is_inlined(String_struct))
                {
                    //var StrValue = duckdb_string_t.value(String_struct);
                    var StrInlined = duckdb_string_t.value.inlined(String_struct);
                    var InlinedStrArray = duckdb_string_t.value.inlined.inlined(StrInlined);
                    int length = duckdb_string_t.value.inlined.length(StrInlined);
                    byte[] InlineStrByteArray = InlinedStrArray.reinterpret(length).toArray(ValueLayout.JAVA_BYTE);
                    ResultArray[pos] = new String(InlineStrByteArray, StandardCharsets.UTF_8);
                }
                else
                {
                    //var StrValue = duckdb_string_t.value(String_struct);
                    var StrPtr = duckdb_string_t.value.pointer(String_struct);
                    var StrArrayPtr = duckdb_string_t.value.pointer.ptr(StrPtr);
                    byte[] byteString = StrArrayPtr.reinterpret(duckdb_string_t.value.pointer.length(StrPtr)).toArray(ValueLayout.JAVA_BYTE);
                    ResultArray[pos] = new String(byteString, StandardCharsets.UTF_8);
                }
            }
            setValidityForChunk(ResultVector, dbChunkSize, ResultArray);
        }
        this.ChunkArrays.add(ResultArray);
    }
}

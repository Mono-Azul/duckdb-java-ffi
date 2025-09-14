package monoazul.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.math.BigInteger;
import java.util.Arrays;

import static monoazul.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_data;

// For hugeint and unsigned long
public class BigIntegerColumn extends ObjectColumn<BigInteger>
{
    public BigIntegerColumn (String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        Clazz = BigInteger.class;
    }

    @Override
    protected void addVectorChunk(MemorySegment ResultVector, int dbChunkSize)
    {
        switch (ColumnDuckDbDataype.type)
        {
            case DuckDbDatatype.DUCKDB_TYPE_HUGEINT -> addVectorChunkHugeint(ResultVector, dbChunkSize);
            case DuckDbDatatype.DUCKDB_TYPE_UBIGINT -> addVectorChunkUnsignedLong(ResultVector, dbChunkSize);
            case DuckDbDatatype.DUCKDB_TYPE_UHUGEINT -> addVectorChunkUnsignedHugeint(ResultVector, dbChunkSize);
            default -> throw new IllegalStateException("Unexpected value: " + ColumnDuckDbDataype.type);
        }
    }

    protected  void addVectorChunkHugeint(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert whole vector to a byte array, switch endianness and then build BigIntegers step by step
        BigInteger[] ResultArray = new BigInteger[dbChunkSize];
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        byte[] ResultByteArray = ResultVectorData.reinterpret((long)dbChunkSize * 16).toArray(ValueLayout.JAVA_BYTE);

        for (int pos = 0; pos < dbChunkSize; pos+= 16)
        {
            byte[] swappedArray = swapEndianness16(Arrays.copyOfRange(ResultByteArray, pos, pos + 16));
            ResultArray[pos] = new BigInteger(swappedArray, 0, 16);
        }
        this.VectorArrays.add(ResultArray);
    }

    protected void addVectorChunkUnsignedLong(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert whole vector to a byte array, switch endianness and then build BigIntegers step by step
        BigInteger[] ResultArray = new BigInteger[dbChunkSize];
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        byte[] ResultByteArray = ResultVectorData.reinterpret((long)dbChunkSize * 8).toArray(ValueLayout.JAVA_BYTE);

        for (int pos = 0; pos < dbChunkSize; pos+= 8)
        {
            byte[] swappedArray = swapEndianness8(Arrays.copyOfRange(ResultByteArray, pos, pos + 8));
            ResultArray[pos] = new BigInteger(1, swappedArray, 0, 8);
        }
        this.VectorArrays.add(ResultArray);
    }

    protected void addVectorChunkUnsignedHugeint(MemorySegment ResultVector, int dbChunkSize)
    {
        // Convert whole vector to a byte array, switch endianness and then build BigIntegers step by step
        BigInteger[] ResultArray = new BigInteger[dbChunkSize];
        MemorySegment ResultVectorData = duckdb_vector_get_data(ResultVector);
        byte[] ResultByteArray = ResultVectorData.reinterpret((long)dbChunkSize * 16).toArray(ValueLayout.JAVA_BYTE);

        for (int pos = 0; pos < dbChunkSize; pos+= 16)
        {
            byte[] swappedArray = swapEndianness16(Arrays.copyOfRange(ResultByteArray, pos, pos + 16));
            ResultArray[pos] = new BigInteger(1, swappedArray, 0, 16);
        }
        this.VectorArrays.add(ResultArray);
    }

    private byte[] swapEndianness16(byte[] inputArray)
    {
        byte[] outputArray = new byte[16];

        outputArray[15] = inputArray[0];
        outputArray[14] = inputArray[1];
        outputArray[13] = inputArray[2];
        outputArray[12] = inputArray[3];
        outputArray[11] = inputArray[4];
        outputArray[10] = inputArray[5];
        outputArray[9] = inputArray[6];
        outputArray[8] = inputArray[7];
        outputArray[7] = inputArray[8];
        outputArray[6] = inputArray[9];
        outputArray[5] = inputArray[10];
        outputArray[4] = inputArray[11];
        outputArray[3] = inputArray[12];
        outputArray[2] = inputArray[13];
        outputArray[1] = inputArray[14];
        outputArray[0] = inputArray[15];

        return outputArray;
    }

    private byte[] swapEndianness8(byte[] inputArray)
    {
        byte[] outputArray = new byte[16];

        outputArray[7] = inputArray[0];
        outputArray[6] = inputArray[1];
        outputArray[5] = inputArray[2];
        outputArray[4] = inputArray[3];
        outputArray[3] = inputArray[4];
        outputArray[2] = inputArray[5];
        outputArray[1] = inputArray[6];
        outputArray[0] = inputArray[7];

        return outputArray;
    }
}

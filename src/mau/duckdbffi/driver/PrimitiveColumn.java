package mau.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static mau.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_validity;

abstract public class PrimitiveColumn<T> extends Column<T>
{
    public BitSet ValidityMask = new BitSet();

    public PrimitiveColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
    }

    //abstract public List<?> getChunkArrays();
    abstract public T getValue(int pos);

    protected void buildValidityMask(MemorySegment ResultVector, int DbChunkSize)
    {
        // Create Validity Mask if necessary
        // Size is ChunkSize / 8 (8 results per byte) and rounded up
        long validityMaskSize = Math.ceilDiv(DbChunkSize, 8);
        MemorySegment ValidityPtr = duckdb_vector_get_validity(ResultVector);

        int startPos = ValidityMask.length();
        // Null pointer indicates no need for mask => no nulls
        // We still add a Validity Mask with all true
        if (ValidityPtr.address() != 0)
        {
            BitSet tmpValidityMask = BitSet.valueOf(ValidityPtr.reinterpret(validityMaskSize).toArray(ValueLayout.JAVA_BYTE));

            for (int pos = 0; pos < DbChunkSize; pos++)
            {
                ValidityMask.set(startPos + pos, tmpValidityMask.get(pos));
            }
        }
        else
        {
            for (int pos = 0; pos < DbChunkSize; pos++)
            {
                ValidityMask.set(startPos + pos, true);
            }
        }
    }

    void compactValidityBitSet()
    {
        // All rows have a value => avoid checks completely
        if (ValidityMask.length() == ValidityMask.cardinality())
        {
            ValidityMask = null;
        }
    }

    public boolean getValidity(int pos)
    {
        // No Mask => no NULLs
        if (ValidityMask == null)
        {
            return true;
        }
        return ValidityMask.get(pos);
    }
}

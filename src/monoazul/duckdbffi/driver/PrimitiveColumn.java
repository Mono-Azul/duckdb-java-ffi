package monoazul.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static monoazul.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_validity;

abstract public class PrimitiveColumn<T> extends Column<T>
{
    private final List<BitSet> ValidityChunkMasks;
    public BitSet ValidityMask;

    public PrimitiveColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        ValidityChunkMasks = new ArrayList<>();
    }

    //abstract public List<?> getChunkArrays();
    abstract public T getValue(int pos);

    private void buildValidityMask(MemorySegment ResultVector, int DbChunkSize)
    {
        // Create Validity Mask if necessary
        // Size is ChunkSize / 8 (8 results per byte) and rounded up
        long validityMaskSize = Math.ceilDiv(DbChunkSize, 8);
        BitSet tmpValidityMask = null;
        MemorySegment ValidityPtr = duckdb_vector_get_validity(ResultVector);

        // Null pointer indicates no need for mask => no nulls
        if (ValidityPtr.address() != 0)
        {
            tmpValidityMask = BitSet.valueOf(ValidityPtr.reinterpret(validityMaskSize).toArray(ValueLayout.JAVA_BYTE));
            ValidityChunkMasks.add(tmpValidityMask);
        }
    }

    void compactValidityBitSet()
    {
        ValidityMask = new BitSet(ResMetaData.columnsCount());
        byte[] tmpArray = new byte[ResMetaData.columnsCount()];
        int startPos = 0;

        // Concat all Arrays
        for (BitSet bSet : ValidityChunkMasks)
        {
            System.arraycopy(bSet.toByteArray(), 0, tmpArray, startPos, bSet.size());
            startPos += bSet.size();
        }
        ValidityMask = BitSet.valueOf(tmpArray);

        // Empty Validity Masks from Chunks
        ValidityChunkMasks.clear();
    }

    public boolean getValidity(int pos)
    {
        // No Mask => no NULLs
        if (ValidityChunkMasks.isEmpty())
        {
            return true;
        }
        return ValidityMask.get(pos);
    }
}

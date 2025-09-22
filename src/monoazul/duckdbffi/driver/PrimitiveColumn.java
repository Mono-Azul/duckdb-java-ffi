package monoazul.duckdbffi.driver;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static monoazul.duckdbffi.jextractffi.duckdb_h.duckdb_vector_get_validity;

abstract public class PrimitiveColumn<T> extends Column<T>
{
    final List<BitSet> ValidityMasks;

    public PrimitiveColumn(String ColumnName, DuckDbDatatype ColumnDatatype)
    {
        super(ColumnName, ColumnDatatype);
        ValidityMasks = new ArrayList<>();
    }

    abstract public List<?> getVectorArrays();

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
            ValidityMasks.add(tmpValidityMask);
        }
    }

    public boolean getValidity(int pos)
    {
        // No Mask => no NULLs
        if (ValidityMasks.isEmpty())
        {
            return true;
        }

        // Division with floor because List is 0 based
        int maskPosInList = Math.floorDiv(pos, ResMetaData.maxVectorSize());
        var ValidityMaskBitSet = ValidityMasks.get(maskPosInList);
        return ValidityMaskBitSet.get(pos % ResMetaData.maxVectorSize());
    }
}

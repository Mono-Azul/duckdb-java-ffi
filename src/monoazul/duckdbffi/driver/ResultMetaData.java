package monoazul.duckdbffi.driver;

public record ResultMetaData(int rowCount, int columnsCount, int chunkCount, int maxVectorSize)
{}

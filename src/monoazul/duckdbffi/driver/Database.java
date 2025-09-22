package monoazul.duckdbffi.driver;

import monoazul.duckdbffi.jextractffi.duckdb_h;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static monoazul.duckdbffi.jextractffi.duckdb_h.*;

public class Database implements AutoCloseable
{

    // Copied from Duckdb JDBC driver class DuckDBNative - refactor!
    // Load shared object file that is included in the .jar.
    // The name depends on the architecture.
    static
    {
        try
        {
            String os_name = "";
            String os_arch;
            String os_name_detect = System.getProperty("os.name").toLowerCase().trim();
            String os_arch_detect = System.getProperty("os.arch").toLowerCase().trim();

            os_arch = switch (os_arch_detect)
            {
                case "x86_64", "amd64" -> "amd64";
                case "aarch64", "arm64" -> "arm64";
                case "i386" -> "i386";
                default -> throw new IllegalStateException("Unsupported system architecture");
            };
            if (os_name_detect.startsWith("windows"))
            {
                os_name = "windows";
            }
            else if (os_name_detect.startsWith("mac"))
            {
                os_name = "osx";
                os_arch = "universal";
            }
            else if (os_name_detect.startsWith("linux"))
            {
                os_name = "linux";
            }
            String lib_res_name = "/libduckdb_java.so"
                    + "_" + os_name + "_" + os_arch;

            Path lib_file = Files.createTempFile("libduckdb_java", ".so");
            URL lib_res = Database.class.getResource(lib_res_name);
            if (lib_res == null)
            {
                System.load(Paths.get("libduckdb-" + os_name + "-" + os_arch,
                        "libduckdb.so").normalize().toAbsolutePath().toString());
            }
            else
            {
                try (final InputStream lib_res_input_stream = lib_res.openStream())
                {
                    Files.copy(lib_res_input_stream, lib_file, StandardCopyOption.REPLACE_EXISTING);
                }
                new File(lib_file.toString()).deleteOnExit();
                System.load(lib_file.toAbsolutePath().toString());
            }
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private final String DatabaseFileName;
    private final Arena DatabaseArena;
    private final MemorySegment DuckDbDatabase;
    private MemorySegment DuckDbDatabasePtr; // _duckdb_database

    public Database(String DatabaseFileName) throws Throwable
    {
        DatabaseArena = Arena.ofShared();
        //DuckDbDatabasePtr = DatabaseArena.allocate(duckdb_database);

        // There could be problems with double freeing otherwise
        DuckDbDatabasePtr = DatabaseArena.allocate(8);
        DuckDbDatabasePtr = DuckDbDatabasePtr.reinterpret(DatabaseArena, duckdb_h::duckdb_close);

        // Move DB file name into MemorySegment
        MemorySegment DatabaseFileNameNative = DatabaseArena.allocateFrom(DatabaseFileName);
        this.DatabaseFileName = DatabaseFileName;

        // Open DB
        int duckDbState = duckdb_open(DatabaseFileNameNative, DuckDbDatabasePtr);

        if (duckDbState == DuckDBError())
        {
            System.out.println("Error opening DB!");
        }

        DuckDbDatabase = DuckDbDatabasePtr.get(C_POINTER, 0);
    }

    public Connection getConnection() throws Exception
    {
        return new Connection(DuckDbDatabase);
    }

    @Override
    public void close() throws Exception
    {
        DatabaseArena.close();
    }
}

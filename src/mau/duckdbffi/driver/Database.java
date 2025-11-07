/*
 * duckdb-java-ffi - A DuckDB (non JDBC) Java client using FFI
 *
 * Copyright (C) 2025  Jens Hofer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package mau.duckdbffi.driver;

import mau.duckdbffi.jextractffi.duckdb_h;

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

import static mau.duckdbffi.jextractffi.duckdb_h.*;

public class Database implements AutoCloseable
{

    // Copied from Duckdb JDBC driver class DuckDBNative - refactored!
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
            String lib_res_name = "/libduckdb.so"
                    + "_" + os_name + "_" + os_arch;

            Path lib_file = Files.createTempFile("libduckdb", ".so");
            URL lib_res = Database.class.getResource(lib_res_name);

            // Here without jar => development setup with Linux
            if (lib_res == null)
            {
                LibPath = Paths.get("libduckdb-linux-amd64","libduckdb.so").normalize()
                        .toAbsolutePath().toString();
                System.load(Paths.get("libduckdb-linux-amd64","libduckdb.so").normalize()
                        .toAbsolutePath().toString());
            }
            else
            {
                try (final InputStream lib_res_input_stream = lib_res.openStream())
                {
                    Files.copy(lib_res_input_stream, lib_file, StandardCopyOption.REPLACE_EXISTING);
                }
                new File(lib_file.toString()).deleteOnExit();
                LibPath = lib_file.toAbsolutePath().toString();
                System.load(lib_file.toAbsolutePath().toString());
            }
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static final String LibPath;
    private final String DatabaseFileName;
    private final Arena DatabaseArena;
    private final MemorySegment DuckDbDatabase;
    private final MemorySegment DuckDbDatabasePtr; // _duckdb_database

    public Database(String DatabaseFileName) throws DuckDbException
    {
        DatabaseArena = Arena.ofShared();
        //DuckDbDatabasePtr = DatabaseArena.allocate(duckdb_database);

        // There could be problems with double freeing otherwise
        DuckDbDatabasePtr = DatabaseArena.allocate(8).reinterpret(DatabaseArena, duckdb_h::duckdb_close);

        // Move DB file name into MemorySegment
        MemorySegment DbFileNameNative = DatabaseArena.allocateFrom(DatabaseFileName);
        this.DatabaseFileName = DatabaseFileName;

        MemorySegment ErrorMessagePtr = DatabaseArena.allocate(C_POINTER);
        int duckDbState = duckdb_open_ext(DbFileNameNative, DuckDbDatabasePtr, MemorySegment.NULL, ErrorMessagePtr);

        if (duckDbState == DuckDBError())
        {
            String DuckDbErrorMsg = ErrorMessagePtr.get(C_POINTER, 0).getString(0);
            MemorySegment ErrorMessagePtrFree = ErrorMessagePtr.reinterpret(DatabaseArena, duckdb_h::duckdb_free);
            throw new DuckDbException(DuckDbErrorMsg);
        }

        DuckDbDatabase = DuckDbDatabasePtr.get(C_POINTER, 0);
    }

    public Connection getConnection() throws DuckDbException
    {
        return new Connection(DuckDbDatabase);
    }

    @Override
    public void close() throws DuckDbException
    {
        try
        {
            DatabaseArena.close();
        } catch (Exception e)
        {
            throw new DuckDbException(e);
        }
    }
}

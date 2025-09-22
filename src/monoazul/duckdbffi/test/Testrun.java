package monoazul.duckdbffi.test;

import monoazul.duckdbffi.driver.Database;
import monoazul.duckdbffi.driver.Result;

public class Testrun {
    public static void main(String[] args) {
        try
        {
            try (var db = new Database(":memory:"))
            {
                try (var con = db.getConnection())
                {
                    Result res = con.query("SELECT *,-1.234::decimal(4,3), TIMESTAMP '1902-09-20 11:30:00.123456789', 1.1234::long, 2.33445::double, 65535::uint16, 255::uint8, 4294967295::uint32  FROM duckdb_memory();");

/*
 Result res = con.query("SELECT *,-1.234::decimal(4,3), TIMESTAMP '1902-09-20 11:30:00.123456789', 1.1234::long, 2.33445::double, 65535::uint16, 255::uint8, 4294967295::uint32  FROM duckdb_memory();")
 Result res = con.query("SELECT 'üüüüüüö', 1001000000001::long, 1.1234::float, 2.33445::double, 250::int2, -5::int1, 12.9::decimal(3,1) ,-999.123::decimal(6,3), 987654321.555::decimal(18,4), 123456789123456789.999::decimal(30,3);")
 Result res = con.query("SELECT 'üüüüüüö', 1001000000001::long, -1.234::decimal(4,3), TIMESTAMP '1902-09-20 11:30:00.123456789', 1.1234::long, 2.33445::double, 4::uint16")
 Result res = con.query("SELECT 'üüüüüüö', 1001000000001::long, -1.234::decimal(4,3), TIMESTAMP_MS '1902-09-20 11:30:00.123456789+01:00';")
 Result res = con.query("select *, 1::boolean, 0::boolean, 1::int128, -1234567890123456789123456789::int128, 18446744073709551615::uint64, 1234567890123456789123456789::uint128 FROM duckdb_settings();")
 Result res = con.query("SELECT INTERVAL 1 hour,  '2f938db5-3425-43d3-a161-6ad7feaf1c5a'::uuid, * FROM duckdb_settings();")
 Result res = con.query("select '1992-09-20 12:30:00.123456789+01:00'::TIMESTAMPTZ, '1902-09-20 11:30:00.123456789-01:00'::timestamptz, '1901-10-28'::date, TIME_NS '11:30:00.123456', '11:30:00.123456789'::time_ns ;")
 Result res = con.query("SELECT -234::int4, * FROM duckdb_settings();")
 Result res = con.query("select 'aaa' union select null union select 'xxx';")
   */
                    // Check if rows are returned
                    if (res.getColumnCount() == 0) {return;}

                    // Print all columns and rows
                    for (int row = 0; row < res.getRowCount(); row++) {
                        var Row = res.getRow(row);
                        System.out.println("Row " + row);
                        for (Object o : Row) {
                            System.out.println(o);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }


    }
}

package test.java;

import mau.duckdbffi.driver.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestStringColumn
{
    static Database DbForTestRun;

    @BeforeAll
    static void setupDB() throws DuckDbException
    {
        DbForTestRun = new Database(":memory:");
        Connection con = DbForTestRun.getConnection();

        con.query("CREATE TABLE StringTest (ID INT4, NullValue String);");
        // Long insert, but we can use the UUIDa also as fixed Strings
        con.query("""
            INSERT INTO StringTest (ID, NullValue) VALUES
            (0,  'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'),
            (1,  'b8f2d5c6-1d1e-45a7-9e32-2d4e8c1f9b02'),
            (2,  'c4d7f1e0-3f0a-4b21-8c45-5a6b7c8d9e0f'),
            (3,  'd9c3b8a1-5c2d-4e3f-a012-7b8c9d0e1f2a'),
            (4,  'e5b4c9d2-7b4f-4d5c-b123-8c9d0e1f2a3b'),
            (5,  'f0a1d3e4-9a6d-4c7b-c234-9d0e1f2a3b4c'),
            (6,  '01b2e4f5-b87e-4b9a-d345-a0e1f2a3b4c5'),
            (7,  '12c3f506-d98f-4aac-e456-b1f2a3b4c5d6'),
            (8,  '23d40617-e0a0-4a1d-f567-c2a3b4c5d6e7'),
            (9, '34e51728-f1b1-4b2e-0678-d3b4c5d6e7f8'),
            (10, '45f62839-02c2-4c3f-1789-e4c5d6e7f809'),
            (11, '560a394a-13d3-4d40-289a-f5d6e7f8091a'),
            (12, '671b4a5b-24e4-4e51-39ab-06e7f8091a2b'),
            (13, '782c5b6c-35f5-4f62-4abc-17f8091a2b3c'),
            (14, '893d6c7d-4606-5073-5bcd-28091a2b3c4d'),
            (15, '904e7d8e-5717-5184-6cde-391a2b3c4d5e'),
            (16, 'a15f8e9f-6828-5295-7def-4a2b3c4d5e6f'),
            (17, 'b26a9fa0-7939-53a6-8ef0-5b3c4d5e6f70'),
            (18, 'c37b0cb1-8a4a-54b7-9f01-6c4d5e6f7081'),
            (19, 'd48c1dc2-9b5b-55c8-a012-7d5e6f708192'),
            (20, 'e59d2ed3-a0c0-56d9-b123-8e6f708192a3'),
            (21, 'f6a03fe4-b1d1-57e0-c234-9f708192a3b4'),
            (22, '07b14af5-c2e2-58f1-d345-a08192a3b4c5'),
            (23, '18c25bc6-d3f3-5902-e456-b192a3b4c5d6'),
            (24, '29d36'),;""");
        con.query("INSERT INTO StringTest (ID) VALUES (-1);");
    }

    @AfterAll
    static void closeDB() throws DuckDbException
    {
        DbForTestRun.close();
    }

    @Test
    void testStringArray()
    {
        String[] compArray = new String[25];
        compArray[0]  = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11";
        compArray[1]  = "b8f2d5c6-1d1e-45a7-9e32-2d4e8c1f9b02";
        compArray[2]  = "c4d7f1e0-3f0a-4b21-8c45-5a6b7c8d9e0f";
        compArray[3]  = "d9c3b8a1-5c2d-4e3f-a012-7b8c9d0e1f2a";
        compArray[4]  = "e5b4c9d2-7b4f-4d5c-b123-8c9d0e1f2a3b";
        compArray[5]  = "f0a1d3e4-9a6d-4c7b-c234-9d0e1f2a3b4c";
        compArray[6]  = "01b2e4f5-b87e-4b9a-d345-a0e1f2a3b4c5";
        compArray[7]  = "12c3f506-d98f-4aac-e456-b1f2a3b4c5d6";
        compArray[8]  = "23d40617-e0a0-4a1d-f567-c2a3b4c5d6e7";
        compArray[9]  = "34e51728-f1b1-4b2e-0678-d3b4c5d6e7f8";
        compArray[10] = "45f62839-02c2-4c3f-1789-e4c5d6e7f809";
        compArray[11] = "560a394a-13d3-4d40-289a-f5d6e7f8091a";
        compArray[12] = "671b4a5b-24e4-4e51-39ab-06e7f8091a2b";
        compArray[13] = "782c5b6c-35f5-4f62-4abc-17f8091a2b3c";
        compArray[14] = "893d6c7d-4606-5073-5bcd-28091a2b3c4d";
        compArray[15] = "904e7d8e-5717-5184-6cde-391a2b3c4d5e";
        compArray[16] = "a15f8e9f-6828-5295-7def-4a2b3c4d5e6f";
        compArray[17] = "b26a9fa0-7939-53a6-8ef0-5b3c4d5e6f70";
        compArray[18] = "c37b0cb1-8a4a-54b7-9f01-6c4d5e6f7081";
        compArray[19] = "d48c1dc2-9b5b-55c8-a012-7d5e6f708192";
        compArray[20] = "e59d2ed3-a0c0-56d9-b123-8e6f708192a3";
        compArray[21] = "f6a03fe4-b1d1-57e0-c234-9f708192a3b4";
        compArray[22] = "07b14af5-c2e2-58f1-d345-a08192a3b4c5";
        compArray[23] = "18c25bc6-d3f3-5902-e456-b192a3b4c5d6";
        compArray[24] = "29d36";

        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM StringTest WHERE ID >= 0 ORDER BY id;");

            StringColumn Col = (StringColumn)res.Columns.getFirst();

            assertEquals(0, Arrays.compare(compArray, Col.VectorArray));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testStringRows()
    {
        String[] compArray = new String[25];
        compArray[0]  = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11";
        compArray[1]  = "b8f2d5c6-1d1e-45a7-9e32-2d4e8c1f9b02";
        compArray[2]  = "c4d7f1e0-3f0a-4b21-8c45-5a6b7c8d9e0f";
        compArray[3]  = "d9c3b8a1-5c2d-4e3f-a012-7b8c9d0e1f2a";
        compArray[4]  = "e5b4c9d2-7b4f-4d5c-b123-8c9d0e1f2a3b";
        compArray[5]  = "f0a1d3e4-9a6d-4c7b-c234-9d0e1f2a3b4c";
        compArray[6]  = "01b2e4f5-b87e-4b9a-d345-a0e1f2a3b4c5";
        compArray[7]  = "12c3f506-d98f-4aac-e456-b1f2a3b4c5d6";
        compArray[8]  = "23d40617-e0a0-4a1d-f567-c2a3b4c5d6e7";
        compArray[9]  = "34e51728-f1b1-4b2e-0678-d3b4c5d6e7f8";
        compArray[10] = "45f62839-02c2-4c3f-1789-e4c5d6e7f809";
        compArray[11] = "560a394a-13d3-4d40-289a-f5d6e7f8091a";
        compArray[12] = "671b4a5b-24e4-4e51-39ab-06e7f8091a2b";
        compArray[13] = "782c5b6c-35f5-4f62-4abc-17f8091a2b3c";
        compArray[14] = "893d6c7d-4606-5073-5bcd-28091a2b3c4d";
        compArray[15] = "904e7d8e-5717-5184-6cde-391a2b3c4d5e";
        compArray[16] = "a15f8e9f-6828-5295-7def-4a2b3c4d5e6f";
        compArray[17] = "b26a9fa0-7939-53a6-8ef0-5b3c4d5e6f70";
        compArray[18] = "c37b0cb1-8a4a-54b7-9f01-6c4d5e6f7081";
        compArray[19] = "d48c1dc2-9b5b-55c8-a012-7d5e6f708192";
        compArray[20] = "e59d2ed3-a0c0-56d9-b123-8e6f708192a3";
        compArray[21] = "f6a03fe4-b1d1-57e0-c234-9f708192a3b4";
        compArray[22] = "07b14af5-c2e2-58f1-d345-a08192a3b4c5";
        compArray[23] = "18c25bc6-d3f3-5902-e456-b192a3b4c5d6";
        compArray[24] = "29d36";

        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT NullValue, ID FROM StringTest WHERE ID >= 0 ORDER BY id LIMIT 3000;");

            for (int row = 0; row < 25; row++)
            {
                assertEquals((String)res.getRow(row).getFirst(), compArray[row]);
            }
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testStringNull()
    {
        try (Connection con = DbForTestRun.getConnection())
        {
            Result res = con.query("SELECT ID, NullValue FROM StringTest WHERE ID = -1;");

            assertNull(res.getRow(0).get(1));
            assertEquals(-1, res.getRow(0).get(0));
        }
        catch (DuckDbException e)
        {
            throw new RuntimeException(e);
        }
    }
}

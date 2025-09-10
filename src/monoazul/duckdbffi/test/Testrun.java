package monoazul.duckdbffi.test;

import monoazul.duckdbffi.driver.Database;

public class Testrun {
    public static void main(String[] args) {
        var Database = new Database(":memory:");
    }
}

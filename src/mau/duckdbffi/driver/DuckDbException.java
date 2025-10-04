package mau.duckdbffi.driver;

public class DuckDbException extends Exception
{
    public DuckDbException() {}

    public DuckDbException(String s)
    {
        super(s);
    }

    public DuckDbException(Exception e)
    {
        super(e);
    }
}



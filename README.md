# duckdb-java-ffi

A DuckDB (non-JDBC) Java client using FFI.

This is an independent project that is not linked to DuckDB in any way, except that it uses their C API.

Status: ALPHA

Not all data types are supported, especially nested types.
The appender is not yet supported.
The way to use it may change if needed.

## Versioning

Until deemed fit, the version number starts with 0, followed by the full version of the included libduckdb.so. This is followed by the running number of releases.

This should provide a clear way to compare and order versions while also indicating the underlying DuckDB version.

## Java version

Due to FFI, a fairly recent version is necessary. Java 23+

## Rational: Why another client?

The JDBC is great for achieving compatibility with many tools and services that support it. However, it also has a lot of methods that are not strictly necessary. Many of these methods perform actions that can be carried out using simple queries.

With DuckDB, connection handling can be simplified. Creating a new connection does not create a new session on another server. Reuse it for the task at hand, but there is no need to keep a pool of connections open.

The overall goal is to provide methods only for tasks that cannot be done directly with queries.

Another important aspect is that the column database client should have first-class access to result columns, rather than only row-wise access.

From a developer's perspective, avoiding all the JNI/C(++) hassles is also beneficial. If necessary, it is possible to debug much closer to the C API, which is also useful for developers using the client.

## Usage

### Database

Create or open a database

```
var Db = new Database(":memory:");
```

Close database

```
Db.close();
```

### Connection

Create Connection (here with try to auto-close it)
```
try (Connection con = Db.getConnection())
```

### Run query

#### Direct query

```
Result res = con.query("select * from duckdb_settings();");
```

#### Query with parameters

Add a list of parameters to replace the '?' in the query. This is the best practice for avoiding SQL injections.

```
List Params = new ArrayList<>();
Params.add(-222229999);

Result res = con.queryWithParameters("INSERT INTO ExampleTable (myValue) VALUES (?);", Params);
```

### Getting Results

The Result object holds the returned table as a List of Column objects. Each Column has a value array whose type depends on the database column type.

The number of columns and rows is available from the Result object. The order of both depends on the SQL.

#### Getting results as rows

Calling the getRow() method returns a List<Object> containing all values, which then needs to be cast to the appropriate data type.

```
Short col3 = (Short)res.getRow(0).get(2);
```

#### Getting results as columns

Columns can be accessed directly from the Result object. The Column object contains an array called VectorArray that holds all the column values, but a cast to the appropriate data type is necessary beforehand.

```
ShortObjectColumn Col = (ShortObjectColumn)res.Columns.getFirst();
var arr = Col.VectorArray
```




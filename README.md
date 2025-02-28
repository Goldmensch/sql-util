![GitHub Workflow Status](https://img.shields.io/github/workflow/status/RainbowDashLabs/sql-util/Verify%20state?style=for-the-badge&label=Build)
![GitHub Workflow Status](https://img.shields.io/github/workflow/status/RainbowDashLabs/sql-util/Publish%20to%20Nexus?style=for-the-badge&label=Publish)
[![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/de.chojo/sql-util?nexusVersion=3&server=https%3A%2F%2Feldonexus.de&style=for-the-badge&label=Release)][nexus_releases]
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/de.chojo/sql-util?server=https%3A%2F%2Feldonexus.de&style=for-the-badge&color=orange&label=Snapshot)][nexus_snapshots]


# SQL-Util

This project contains various functions I use for sql handing.

It is by far not a replacement for large Frameworks like Hibernate, but a solid ground to reduce boilerplate code when
you work with plain SQL like I do most of the time.

# Dependency

```gradle
maven("https://eldonexus.de/repository/maven-public")

implementation("de.chojo", "sql-util", "version")
```

## DataHolder

The data holder can be used to extend a class which should handle SQL connections.

Each query object is owned by a plugin.

```java
public class MySqlQueries extends QuerObject {
    public MySqlQueries(Plugin plugin, DataSource dataSource) {
        super(plugin, dataSource);
    }
}

```

The data holder is extended by a `QueryFactoryHolder`, which allows to directly creates a QueryBuilderFactory.

### Logging

Logging can be done by calling `QueryObject#logDbError(String, SQLException)`.\
This will log the error with the provided logger of the plugin.

This requires an existing slf4j implementation.

### QueryBuilder

You can obtain a [QueryBuilder](#QueryBuilder) directly inside a QueryObject via `QueryBuilder#queryBuilder(Class<T>)`.\
Read more at the [QueryBuilder](#QueryBuilder) section.

### Connection

You can directly retrieve a connection from the provided data source via `QueryObject#getConnection()`.

# DBUtil

DBUtil proves methods for exception formatting to log them properly.

# QueryBuilder

The `QueryBuilder` class allows you to easily call your database and reduces boilerplate code.\
It uses a builder like pattern to guide you while calling your database.

## QueryBuilderConfig

You will need a `QueryBuilderConfig` to use a `QueryBuilder`. You can create your own configuration or use the default
config via `QueryBuilderConfig#DEFAULT`.

Otherwise you can call `QueryBuilderConfig#builder()` to obtain a builder for the config.

### Throwing

By default the query builder will catch all SQL Exceptions and log them properly.\
If you want to log them by yourself you should call `QueryBuilderConfig.Builder#throwing()` on the builder.

### Atomic Transaction

By default the query builder will execute all queries in one atomic transaction. This has the effect, that the data will
only be changed if all queries were executed succesfully. This is especially usefull, when executing multiple queries.
If you dont want this call `QueryBuilderConfig.Builder#notAtomic()`. Tbh there is no real reason why you would want
this.

## Obtain a QueryBuilder

The query builder is used to write and read data. If you want to read data you have to provide the class of the object
you want to read. It doesn't matter if you want to read multiple entries or get a single entry.

A QueryBuilder can be optained via three different methods.

### QueryObject

If your class extends QueryObject you can simply call `QueryObject#builder(Class<T>)`.\
This will give you a query builder with the plugin and datasource of your QueryObject.

### Static Factory Method
The simple way is to call the static factory methods on the `QueryBuilder`.
The Factory methods all accept your `Plugin` and a `DataSource`.

If you want to read something from the database you should use the method with a class:
`QueryBuilder#builder(Plugin, DataSource, Class<T>)`

If you just want to write data your can use the builder without a class: `QueryBuilder#builder(Plugin, DataSource)`

### QueryBuilderFactory
The `QueryBuilderFactory` has one general advantage. It will apply a configuration for you and skip the [ConfigrationStage](#ConfigurationStage).

To create a `QueryBuilderFactory` call the constructor and cache the Factory where you need it:
`QueryBuilderFactory(QueryBuilderConfig, DataSource, Plugin)`

## Stages
The QueryBuilder uses a stage system to guilde you through the creation of your calls.
If you didnt used the `QueryBuilderFactor` to obtain your builder, you will start in the [ConfigurationStage](#ConfigurationStage) and otherwise in the [QueryStage](#QueryStage)

### ConfigurationStage
The `ConfigurationStage` allows you to set your `QueryBuilderConfig`.

You can apply your configuration here or just use the default config.

### QueryStage
The `QueryStage` allows you to set your query with parameter for a `PreparedStatement`.

If you dont have parameter you can call `QueryStage#queryWithoutParams(String)` to skip the [StagementStage](#StatementStage).

### StatementStage
The `StagementStage` allows you to invoke methods on the PreparedStatement.

### ResultStage
The `ResultStage` allows you to define what your query does.

#### Reading data
If you want to read data you have to call `ResultStage#readRow(ThrowingFunction<T, ResultSet, SQLException> mapper)`.

Now you have to map the current row to a object of your choice. This has to be the class you provided on creation of the QueryBuilder.

**Note: Do only map the current row. Dont ever modify the ResultSet by calling `ResultSet#next()` or something else.**

Calling these functions will return a [RetrievalStage](#RetrievalStage).

#### Update, delete or insert
All these operations are just updates anyway in sql context. It doesn't matter what you call.

Calling these functions will return a [UpdateStage](#UpdateStage).

#### Append
You can also append another query by calling `StatementStage#append()`.
This will return a `QueryStage` and allows you to set another query.\
All queries will be executed in the order they were appended and with a single connection.

### RetrievalStage
The `RetrievalStage` allows you to actually call the queries you entered before.

If you want to retrieve the first or only one result call the `RetrievalStage#first` or `RetrievalStage#firstSync` method.
This will return the rows as a list mapped via the function provided in the [ResultStage](#ResultStage).

If you want to retrieve all entries in the `ResultSet` call `RetrievalStage#all` or `RetrievalStage#allSync`

#### Async retrieval
Methods not suffixed with `Sync` will return a [BukkitFutureResult](Threading#BukkitFutureResult).

These methods will provide the list or single result to you.

#### Sync retrieval
Methods suffixed with `Sync` will block the thread until the result is retrieved from the database.
Do not use blocking methods unless you really know what you ar doing.

### UpdateStage
The update stage allows you to update entries in your table.

#### Async Update
Methods not suffixed with `Sync` will return a [BukkitFutureResult](Threading#BukkitFutureResult).

These methods will provide the amount of changed rows.

#### Sync Update
Methods suffixed with `Sync` will block the thread until the update is done.\
These methods will provide the amount of changed rows.\
Do not use blocking methods unless you really know what you ar doing.

## Examples
Update a table async and get the inserted auto increment.
```java
QueryBuilder.builder(plugin, dataSource, Long.class)
        .defaultConfig()
        .query("INSERT INTO table(first, second) VALUES(?, ?)")
        .params(stmt -> {
            stmt.setString(1, "newVal");
            stmt.setString(2, "some");
        })
        .append()
        .queryWithoutParams("SELECT LAST_INSERT_ID()")
        .readRow(r -> r.getLong(1))
        .first()
        .whenComplete(id -> {
            System.out.println("Inserted new entry with id " + id);
        });

```
Reading multiple rows async.
```java
QueryBuilder.builder(plugin, dataSource, String.class)
        .defaultConfig()
        .query("SELECT something FROM table WHERE key = ?")
        .params(stmt -> stmt.setString(1, "foo"))
        .readRow(rs -> rs.getString("something"))
        .all()
        .whenComplete(results -> {
            List<String> resultList = results;
        });
```

# DataSourceCreator

The DataSourceCreator can be used to build HikariDataScource with a builder pattern.

```java
HikariDataSource build = DataSourceCreator.create(DataSource.class)
        .withAddress("localhost")
        .forDatabase("db")
        .withPort(2000)
        .withUser("root")
        .withPassword("passy")
        .create()
        .setMaximumPoolSize(20)
        .setMinimumIdle(2)
        .build();
```

As an alternative the DbConfig can be used.

```java
HikariDataSource build = DataSourceCreator.create(DataSource.class)
        .withSettings(new DbConfig("localhost", "5432", "root", "passy", "db"))
        .create()
        .setMaximumPoolSize(20)
        .setMinimumIdle(2)
        .build();
```

Every value which is not set will be the default value provided by the database driver.

The data source needs to be replaced with your proper data source implementation.


[nexus_releases]: https://eldonexus.de/#browse/browse:maven-releases:de%2Fchojo%2Fsql-util
[nexus_snapshots]: https://eldonexus.de/#browse/browse:maven-snapshots:de%2Fchojo%2Fsql-util
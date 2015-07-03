# Cloud Demo

## Description

A basic cloud demo that has been hard-coded to work for NuoDB only.
The data model is representative of typical web style workloads and
telco/cloud-internals workloads; it uses a data model similar to
what's found in Swift.

## Dependencies

None

## Database Credentials

A previously created database having the following credentials and name must
exist:

    database-name: cloud
    database-user: dba
    database-password: dba
    database-host: localhost
    database-port: 48004

## Execution

Simple,

    java -jar cloud-demo.jar

The demo supports scale out; simply start more transaction engines and start
more clients. Aggregate client output is measured by taking the sums or averages
of the output rates or latency values, respectively.

## Output

Internally we use the Coda Hale metrics library and output statistics to the
console in the following format:

    Name        Count       Rate        Min      Max      Mean     Std Dev  Median   75%      95%      98%      99%      99.9%     
    OLTP_C1     64213       141         5.81     89.68    14.32    7.63     11.81    15.49    25.66    40.05    48.96    69.48    
    OLTP_C2     215076      472         1.28     108.37   16.46    9.46     13.53    18.14    33.89    47.52    56.96    95.26    
    OLTP_C3     644957      1417        0.27     90.02    14.29    10.29    13.37    18.94    31.38    42.70    48.22    88.40    
    OLTP_R2     160987      353         0.34     53.22    4.26     4.64     2.37     5.71     12.56    19.95    24.51    39.52    

## Configuration

Several properties are available to tailor the execution of the demo:

|      Property     | Description                      | Default              |
|:----------------  |---------------------------------  |:---------------------|
| test.class        | package-qualified class name of the test to run | com.nuodb.field.services.cloud.Namespace |
| test.mix          | comma separated list of mix integer percentages that must add up to 100 |  |
| test.tag          | comma separated list of each mix name, names that correspond to the annotated workload names in the test suite |  |
| test.threads      | the concurrency level            | 4 * num_cores    |
| test.skip.db.init | whether or not to skip the database preload | false |
| db.name           | the database name                | cloud       |
| db.host           | the database host                | localhost  |
| db.port           | the database port                | 48004      |
| db.schema         | the database schema              | cloud       |
| db.user           | the database user                | dba        |
| db.password       | the database password            | dba        |
| db.connections.maxage    | the maximum connection age in ms | 30000      |
| db.connections.default_auto_commit    | whether or not to automatically commit each statement | false      |
| db.connections.validation_query_string    | the validation query to execute when a connection is retrieved | SELECT 1 FROM DUAL |
| db.connections.test_on_return    | whether or not to test a connection on retrieval | true  |
| db.connections.isolation    | connection transaction isolation level | READ_COMMITTED  |

## Things to Do

Add HTAP workload.

## Data Model and Processing

The data model is straight forward.
 
On one side of the equation you have account tables, child-aggregate tables,
and leaf tables. This could represent users who have multiple shopping carts,
and in each cart they have a bunch of items for purchase. This could on the
other hand represent file system metadata, users, directories and files.

And the common operations for these are create, delete, and list the objects
at each of those levels of aggregation.

On the other side of the equation we have stat tables; frequently one wants
to answer a basic question, how many directories or files do I have, or how
many items are in this container or shopping cart. One could go about and
count the items but for large aggregations that's not terribly efficient.
This is where the stat tables come into place; every time we add or remove
an object, the counts are updated to reflect the change. Then the question
is simply answered, just look at the stat table.

But one question the above things do not answer is for any particular day
what was the usage per user. Perhaps this is related to some sort of structure
where users are charged for how many resources they use, or perhaps on the
commerce side the difference between expressed interest (impressions) versus
actual purchase rates. There is an OLAP query that answers this question
and generates metering table content that can be subsequently read.

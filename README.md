# Dash (NG)

## Description

Dash is a performance test framework geared towards benchmarking realistic
operational workloads by allowing one to define a mix of workloads that run
concurrently using specified percentages of each. The latter point here is a
significant differentiator for this framework that distinguishes if from other
performance test frameworks.

## Concepts

There are a few essential concepts that must be understood in order to effectively
use Dash, these are the 'mix', the 'tag', and the 'code'.

The code refers to the logic that expresses a business use case. The logic as
an entity is labeled with a tag that uniquely identifies it from other business
use cases; each tag must be unique within a Java class, they SHOULD NOT contain
whitespace characters, but otherwise can be anything you want. Each Java class
can express several business use cases, one method for each, and each method
with its own tag. The mix is the relative ratio of operations executed expressed
as an integer percentage; the cumulative ratios for all tags declared must add
to one-hundred (100) percent. Both the workload tag and the workload mix must
have equivalent cardinality.

For example:

    dash.workload.mix=90,5,3,2
    dash.workload.tag=search,buy,return,sell

## Configuration

Dash is fully configurable and permits the declaration of concurrency,
workload tags, workload mix, total execution time, limit and burst rates, etc.

### Configuration Sources and Properties Precedence

There are three sources for properties that control Dash behavior, in order
of precedence they are:

- User Configurable System Properties
- User Configurable Application Properties File (or conf.yml)
- Fixed Default Properties

There are a number of user configurable properties within Dash; users may
override their default values by supplying them as system properties, or
declare them in the conf.yml file. Properties declared within the conf.yml
file override the defaults, and those declared as system properties override
those declared by the application.properties file as well as the defaults.

Most properties have reasonable defaults; those properties that do not have
default values MUST BE declared by the user. See the tables below for further
detail.

### Core Dash Properties

There are several core Dash properties, these are:

|        Property       | Description                      | Default              |
| :-------------------- |:---------------------------------| ---------------------|
| dash.workload.mix          | comma separated list of mix integer percentages that must add up to 100 |  |
| dash.workload.tag          | comma separated list of each mix name, names that correspond to the annotated workload names in the test suite |  |
| dash.driver.duration     | the duration for the workload    | 5 seconds            |
| dash.driver.threads      | the concurrency level            | 32                   |
| dash.driver.rates.limit  | the limit rate for the workload  | 2000                 |
| dash.driver.rates.burst  | the burst rate for the workload  | dash.driver.rates.limit |

### Database Properties

Dash natively supports testing databases, as such there are several properties
related to databases that must be configured when running such a test:

|        Property       | Description                      | Default              | Options |
| :-------------------- |:---------------------------------| ---------------------|----------
| dash.db.type      | the database type       | hsqldb      | hsqldb, nuodb, oracle, mysql, postgresql, sqlserver |
| dash.db.pool.type  | the connection pool type to use  | bonecp  | bonecp, hikaricp |
| dash.db.name      | the database name       | test       | |
| dash.db.schema    | the database schema     | test       | |
| dash.db.user      | the database user       | dba        | |
| dash.db.password  | the database password   | dba        | |
| dash.db.host      | the database host       | localhost  | |
| dash.db.port      | the database type       | database specific default port | |
| dash.db.transaction.autocommit | the database transaction default auto-commit setting | true | |
| dash.db.transaction.readonly  | the database transaction read-only flag | false | |
| dash.db.transaction.isolation  | the database transaction default isolation level | TRANSACTION_READ_COMMITTED | Any JDBC transaction isolation level supported by the underlying database. |

n.b. NuoDB only supports TRANSACTION_READ_COMMITTED, TRANSACTION_SERIALIZABLE.
n.b. When using BoneCP do not use the TRANSACTION_ prefixes.

### Performance Monitoring Properties

Dash uses the <a href="https://github.com/dropwizard/metrics">Coda Hale Metrics Library</a>
and related to logging performance metrics, the metrics may be published to one of three
targets: CSV files, the terminal (console) window, and to Elasticsearch. The properties
governing this are:

|        Property       | Description                                    | Default |
| :-------------------- |:-----------------------------------------------|:----------
| dash.metrics.service.reporters  | One or more reporters to publish statistics to, must be in CSV array-form syntax | [csv,console,elasticsearch] |
| dash.metrics.service.elasticsearch.hosts | When using elasticsearch, the set of elasticsearch hosts to publish to, must be in CSV array-form syntax | ['localhost:9200'], or commented out if unused |

### YAML Configuration

The conf directory contains a YAML file; the YAML file provided contains example
property groups, and example use of property references. The syntax has been
extended to support references to system properties and their default values if
the system property is not set.

#### Sample YAML File

Note in this sample the creation and use of YAML anchors (&), and of
references (*). The can reduce repetitive declaration of common properties
among a set of different test plans.

In the example below, the CREATE and DELETE single test plans share 
twelve (12) properties, making their own declarations fit on a single line.

You can also declare non-single tests, such as the mix defined in MIX_1.

    DEFAULTS: &defaults
      dash.driver.rates.limit: 500000
      dash.driver.burst.limit: 800000
      dash.driver.duration: !env $(workload.duration:30)
      dash.driver.threads: !env $(workload.threads:32)
      dash.workload.class: com.github.rbuck.dash.sample.Sample
      dash.db.name: test
      dash.db.schema: test
      dash.db.user: dba
      dash.db.password: dba
      dash.db.type: nuodb
      dash.db.pool.type: hikaricp
      dash.db.transaction.autocommit: true
      dash.db.transaction.readonly: false
      dash.db.transaction.isolation: TRANSACTION_READ_COMMITTED
      dash.metrics.service.reporters: [csv,console,elasticsearch]
      dash.metrics.service.elasticsearch.hosts: ['localhost:9200']

    SINGLE: &single
      <<: *defaults
      dash.workload.mix: 100
      dash.workload.tag: !env $(dash.workload.tag:create)
    
    # traditional style...
    CREATE:
      <<: *single
      dash.workload.tag: create
    
    # or flow style...
    DELETE: {<<: *single, dash.workload.tag: delete}
    
    # etc...
    
    MIX_1:
      <<: *defaults
      dash.workload.tag: [create,delete]
      dash.workload.mix: [50,50]

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
| dash.driver.class        | package-qualified class name of the test to run | com.github.rbuck.dash.services.cloud.BusinessServices |
| dash.driver.threads      | the concurrency level            | 4 * num_cores    |
| dash.workload.mix          | comma separated list of mix integer percentages that must add up to 100 |  |
| dash.workload.tag          | comma separated list of each mix name, names that correspond to the annotated workload names in the test suite |  |
| dash.test.skip.dash.db.init | whether or not to skip the database preload | false |
| dash.db.name           | the database name                | cloud       |
| dash.db.host           | the database host                | localhost  |
| dash.db.port           | the database port                | 48004      |
| dash.db.schema         | the database schema              | cloud       |
| dash.db.user           | the database user                | dba        |
| dash.db.password       | the database password            | dba        |
| dash.db.connections.maxage    | the maximum connection age in ms | 30000      |
| dash.db.connections.default_auto_commit    | whether or not to automatically commit each statement | false      |
| dash.db.connections.validation_query_string    | the validation query to execute when a connection is retrieved | SELECT 1 FROM DUAL |
| dash.db.connections.test_on_return    | whether or not to test a connection on retrieval | true  |
| dash.db.connections.isolation    | connection transaction isolation level | READ_COMMITTED  |

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

# Dash (NG)

## Description

Dash is a performance test framework geared towards benchmarking realistic
operational workloads by allowing one to define a mix of workloads that run
concurrently using specified percentages of each. The latter point here is a
significant differentiator for this framework that distinguishes if from other
performance test frameworks.

## Extension

Dash is also a framework that allows extension to support additional workloads
through the standard Java Service Loader infrastructure, which has been part
of the JRE formally as infrastructure since JDK 1.7, and part of the JRE 
informally since 1.18.

To add your own sample workload, declare a suitably named child package within
the services package, and create a Java class that extends AbstractService.
You must implement two abstract methods:

    protected abstract Context createContext();
    
    protected abstract void execute(Context context);

The purpose of the first method is to define an execution context (state)
to pass to the second method; you define the execution context as either
per thread or as a singleton.

The purpose of the second method is to contain the test code, which itself
typically dispatches into one of several other business use case methods
depending upon the mix. Read the samples provided for more detail.

Read the documentation associated to those methods for full detail on their
purpose and also read the detailed documentation on the Context and Service
class.

By default the execution of test code (above) will be threaded, and if you
want to control the number of threads you do so through a property defined
below.

The final item you need to add is a Service Loader hook, namely, to the
META-INF/services/com.github.rbuck.dash.services.Service file add the name
of the class you created above. If the file does not exist, add it as a
resource.

## Building / Packaging

Simply use Maven 3.x:

    $ mvn clean install

In the target directory will be a .tar.gz distribution. Simply unzip that
somewhere to run. Details on running and configuring the demos follow.  

## Execution

This section covers how to run a demo. You will learn about command line
syntax, options, and options defaults.

### Command Line Interface

The command line accepts the following options:

|        Option        | Description                      | Default              |
| :-------------------- |:--------------------------------| ---------------------|
| -h, --help           | show this help message and exit |  |
| -c CONF, --conf CONF | the config file containing the test specification to run | ../conf/conf.yml |
| -t TEST, --test TEST | the name of the test to run   |             |
| -v, --version        | print the version number           |                    |

### Running the Application

First off, verify your configuration in conf.yml, and noting the test names.

The preferred way to run the application is to run it using the run.sh script.
Here is an example of running the cloud demo:

    $ ./run.sh -t NUODB_MIX

    [2015-07-14T10:19:21.855] created
    [2015-07-14T10:19:23.120] started
    Name        Count       Rate        Min      Max      Mean     Std Dev  Median   75%      95%      98%      99%      99.9%     
    OLTP_C1     3662        729         0.62     50.90    4.61     3.73     3.69     5.44     10.93    15.98    20.60    25.90    
    OLTP_C2     6485        1291        0.50     34.32    5.97     3.96     4.98     7.24     13.50    17.96    21.89    29.66    
    OLTP_C3     9675        1925        0.38     44.88    5.64     4.93     4.67     7.50     14.82    18.86    23.69    40.82    
    OLTP_R2     6589        1310        0.32     28.46    3.57     3.10     2.59     4.30     9.69     13.16    16.00    24.33    
    OLTP_R3     6502        1292        0.39     46.09    4.15     3.69     3.07     5.12     10.93    15.07    17.99    28.72    
    ...
    [2015-07-14T10:19:43.872] stopped
    [2015-07-14T10:19:43.873] destroyed

The Count column is the total number of calls to the test case performed.
The Rate is measured in TPS. The remaining columns are all measured in
milliseconds. The right five columns are the quantiles. The middle three
are measured over a rolling window of time.

## Logging Output

The framework will create a log directory. In that directory will be two
sorts of files: the dash.log (has logging output, errors, etc), and many
CSV files, one CSV for each test case measured. The CSV output is handy
if you want to plot TPS or latency over time in tools such as R or Excel.

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
those declared by either.

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

    NUODB: &nuodb
      dash.db.host: localhost
      dash.db.type: nuodb
    
    DEFAULTS: &defaults
      dash.driver.rates.limit: 500000
      dash.driver.burst.limit: 800000
      dash.driver.duration: !env $(dash.driver.duration:30)
      dash.driver.threads: !env $(dash.driver.threads:32)
      dash.driver.class: com.github.rbuck.dash.services.cloud.BusinessServices
      dash.db.name: cloud
      dash.db.schema: cloud
      dash.db.user: dba
      dash.db.password: dba
      dash.db.pool.type: hikaricp
      dash.db.transaction.autocommit: true
      dash.db.transaction.readonly: false
      # n.b. BoneCP does not use the TRANSACTION_ prefix for isolation level naming!
      dash.db.transaction.isolation: TRANSACTION_READ_COMMITTED
      dash.metrics.service.reporters: [csv,console]
      dash.metrics.service.elasticsearch.hosts: ['localhost:9200']
    
    CLOUD_MIX: &cloud
      <<: *defaults
      dash.workload.tag: [OLTP_C1,OLTP_C2,OLTP_C3,OLTP_R2,OLTP_R3]
      dash.workload.mix: [10,20,30,20,20]
    
    NUODB_MIX:
      <<: *cloud
      <<: *nuodb

Using this simple pattern, switching over to running the test against MySQL
can be accomplished simply (other supported databases are similar):

    # -- conf.yml --
    MYSQL: &mysql
      dash.db.host: 172.16.211.146
      dash.db.type: mysql

    MYSQL_MIX:
      <<: *cloud
      <<: *mysql

    $ run.sh -t MYSQL_MIX

## Why Not JUnit

There are two fundamental issues with JUnit, and one additionally significant
consequence of its first design choice.
 
First issue is that JUnit creates a new test object instance for every test
method contained in a class. Its aim was to keep tests side effect free, but
that is non-sense reasoning as object oriented languages already have a means
to accomplish that, the class.

The consequence of this choice was a decision to introduce static methods
and members for those situations where one-time initializations were required,
for example when creating a reference to a JDBC connection pool. The problems
related to static state are well known and therefore do not need to be repeated
here.

The second issue with JUnit is its runner and related tooling are not capable
of allowing test methods to be run multiple times; for example, what if it were
valid to not only check for correctness but also measure performance? Testing
non-functional requirements is just as valid as functional ones, and JUnit is
terrible for that as one would want multiple data points not just a single one,
and perhaps with concurrent workload (another weakness of JUnit).

All in all, a decision was made to abandon JUnit as the basis for Dash.

## Dependencies

None, entirely self-contained. (TODO: a few jars are still brought in from Maven Central, will be addressed).

# Known Issues

1. The SQL statement splitter that enables running SQL scripts, it's rather
   poorly implemented. I am rewriting a new one and will toss that old rag
   into the trash shortly. Consequently, for databases other than NuoDB you
   need to load the DDL manually.
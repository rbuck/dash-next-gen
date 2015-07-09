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

None, entirely self-contained.

## Execution

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
Here is an example of running a 50/50 test mix for this/that operations:

    $ ./run.sh -t MIX_1
    
    Name        Count       Rate        Min      Max      Mean     Std Dev  Median   75%      95%      98%      99%      99.9%
    that        0           0           0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00
    this        18005596    2793639     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00
    that        0           0           0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00
    this        40948025    3578840     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00
    that        0           0           0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00
    this        63710613    3874956     0.00     0.01     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00
    that        0           0           0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00
    this        85940803    4008354     0.00     0.01     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00
    that        0           0           0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00
    this        108056113   4086774     0.00     0.01     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00
    that        0           0           0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00
    this        127769973   4063944     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00     0.00
    
    Time: 30.929
    
    OK (1 test)

The Count column is the total number of calls to the test case performed.
The Rate is measured in TPS. The remaining columns are all measured in
milliseconds. The right five columns are the quantiles. The middle three
are measured over a rolling window of time.

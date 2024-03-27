Guidelines for contributing to Hibernate Search
====

Contributions from the community are essential in keeping Hibernate Search strong and successful.

This guide focuses on how to contribute back to Hibernate Search using GitHub pull requests.

## Legal

All original contributions to Hibernate Search are licensed under the
[Apache License version 2.0 (Apache-2.0)](https://www.apache.org/licenses/LICENSE-2.0.txt),
or, if another license is specified as governing the file or directory being modified, such other license.
The Apache-2.0 license text is included verbatim in the [`LICENSE.txt`](LICENSE.txt) file
in the root directory of the repository.

Note that Hibernate Search 7.1 and older are distributed under
[a different license](https://github.com/hibernate/hibernate-search/tree/7.1#license).
To allow backports, the Hibernate team may ask contributors to dual-license their contribution.

All contributions are subject to the [Developer Certificate of Origin (DCO)](https://developercertificate.org/).
The DCO text is also included verbatim in the [dco.txt](dco.txt) file in the root directory of the repository.

## Finding something to contribute

Our [JIRA instance](https://hibernate.atlassian.net/browse/HSEARCH) is where all tasks are reported and tracked.

In particular there is a [list of issues suitable for new contributors](https://hibernate.atlassian.net/issues/?filter=21120).

## Contributing a bug report

If you want to see something fixed, but are not comfortable enough to dig into the codebase,
you can help us by providing a well-documented bug report:

* Open a bug report on our [JIRA instance](https://hibernate.atlassian.net/browse/HSEARCH).
Make sure to provide enough information, in particular:
the code you wrote, the expected result, the result you got instead,
the version of your dependencies. 
* Ideally (and this helps a lot), provide a self-contained test case.
We provide [test case templates for all Hibernate projects](https://github.com/hibernate/hibernate-test-case-templates)
to help you get started:
just fork this repository, build your test case and attach it as an archive to a JIRA issue.

## Setting up a development environment

### <a id="setup-build-tools"></a> Build tools

You will need JDK 17 exactly for the build.

A maven wrapper script is provided at the root of the repository (`./mvnw`),
so you can use that and don't need to care about the required version of Maven
(it will be downloaded automatically).

### <a id="setup-develocity"></a> Develocity build cache and build scans

Hibernate Search relies on a [Develocity](https://gradle.com/develocity/) instance
at [https://ge.hibernate.org](https://ge.hibernate.org/scans?search.rootProjectNames=Hibernate%20Search)
to speed up its build through a build cache

By default, only [continuous integration](#ci) builds will write to the remote build cache or publish build scans.

Local builds of Hibernate Search will:

* write to a local build cache;
* read from both a local and a remote build cache to speed up builds;
* not write to the remote build cache;
* not publish any build scans.

To opt out from build caches for a particular build (e.g. to debug flaky tests),
pass `-Dno-build-cache` to Maven.

To publish build scans for your local builds,
[reach out to the team](https://hibernate.org/community/#contribute) to set up an account,
and once you have one, run this from the root of your local clone of Hibernate Search:

```shell
./mvnw gradle-enterprise:provision-access-key
```

To opt out from build scans for a particular build (e.g. when working on a security vulnerability),
pass `-Dscan=false` to Maven.

### <a id="setup-ide"></a> IDE

#### <a id="setup-ide-intellij-idea"></a> IntelliJ IDEA

**WARNING**: Avoid running `./mvnw` while IntelliJ IDEA is importing/building,
and ideally avoid using Maven from the command line at all while IntelliJ IDEA is open.
IntelliJ IDEA's own build might conflict with the Maven build, leaving your working directory in an undetermined state
(some classes being generated twice, ...).
If you already did that, close IntelliJ IDEA, run `./mvnw clean`, and open IntelliJ IDEA again.

You will need to change some settings:

* `Build, Execution, Deployment > Build Tools > Maven`: set `Maven home path` to `Use Maven wrapper`
* In `Project structure`, make sure the project JDK is JDK 17.
* Set up [formatting rules and code style](#setup-ide-formatting).

Then a few steps will initialize your workspace:

* In the "Maven" side panel, click "Reload all Maven projects".
* To check your setup, click `Build > Rebuild Project`.
  You might get a few errors similar to `java: module not found: org.hibernate.search.mapper.orm`;
  those are caused by limitations of IntelliJ IDEA and can be safely ignored.
  If the build has no other error, your workspace is correctly set up.
* If you encounter any problem, that might be caused by the project being half-built before you started.
  Try again from a clean state: close IntelliJ IDEA, run `./mvnw clean`, open IntelliJ IDEA again,
  and go back to the first step.

#### <a id="setup-ide-eclipse"></a> Eclipse

Eclipse shouldn't require any particular setup besides
[formatting rules and code style](#setup-ide-formatting).

#### <a id="setup-ide-formatting"></a> Formatting rules and style conventions

Hibernate Search has a strictly enforced code style. Code formatting is done by the Eclipse code formatter, 
using the config files found in the `build/config/src/main/resources` directory. 
By default, when you run `mvn install`, the code will be formatted automatically. 
When submitting a pull request the CI build will fail if running the formatter results in any code changes,
so it is recommended that you always run a full Maven build before submitting a pull request.

The [Adapter for Eclipse Code Formatter](https://plugins.jetbrains.com/plugin/6546-adapter-for-eclipse-code-formatter) plugin
can be used by IntelliJ IDEA users to apply formatting while within the IDE. Additionally, contributors might need to 
increase import counts to prevent star imports, as this setting is not exportable and star imports will lead to
a build failure.

## Contributing code

### Prerequisites

If you are just getting started with Git, GitHub and/or contributing to Hibernate Search there are a
few prerequisite steps:

* Make sure you have a [Hibernate JIRA account](https://hibernate.atlassian.net)
* Make sure you have a [GitHub account](https://github.com/signup/free)
* Make sure you have [set up git locally](https://help.github.com/articles/set-up-git)
* On Windows, run `git config --global core.longpaths true`
so that you are able to clone the Hibernate Search repository
* [Fork](https://help.github.com/articles/fork-a-repo/) the [Hibernate Search repository](https://github.com/hibernate/hibernate-search)
* Instruct git to ignore certain commits when using `git blame`.
From the directory of your local clone, run this:
`git config blame.ignoreRevsFile .git-blame-ignore-revs`
    
### Development environment

Make sure to [set up your development environment](#setup) correctly.

Be especially careful about setting up the [formatting rules and code style](#setup-ide-formatting).

If you built the project at least once (`./mvnw clean install`),
you can very quickly check that you have respected the formatting rules by running Checkstyle:
```bash
./mvnw checkstyle:check -fn
```

### Create a topic branch

Create a "topic" branch on which you will work.  The convention is to name the branch
using the JIRA issue key.  If there is not already a JIRA issue covering the work you
want to do, create one.  Assuming you will be working from the main branch and working
on the JIRA HSEARCH-123:
```bash
git checkout -b HSEARCH-123 main
```

### Code
  
See [this section](#source-code-structure) for details about the structure of the source code,
and [this section](#building-from-source) for how to build the project.

If you need help, feel free to contact us, be it through comments on your JIRA ticket,
emails on the mailing list, or directly though our chat:
see [here](https://hibernate.org/community/#contribute) for more information.

### Commit

* Make commits of logical units.
* Be sure to start the commit messages with the key of the JIRA issue you are working on.
This is how JIRA will pick up the related commits and display them on the JIRA issue.
* Avoid formatting changes to existing code as much as possible:
they make the intent of your patch less clear.

_Prior to committing, if you want to pull in the latest upstream changes (highly
appreciated by the way), please use rebasing rather than merging (see instructions below).
Merging creates "merge commits" that really muck up the project timeline._

Add the original Hibernate Search repository as a remote repository called upstream:
```bash
git remote add upstream https://github.com/hibernate/hibernate-search.git
```

If you want to rebase your branch on top of the main branch, you can use the following git command:
```bash
git pull --rebase upstream main
```

### Check and test your work

Before submitting a pull requests, check your contribution:

* Make sure you have added the necessary tests for your changes.
* If relevant, make sure you have updated the documentation to match your changes.
* Run the relevant tests once again to check that your changes work as expected.
  No need to run the whole test suite, the Continuous Integration will take care of that.

**Note**: If you want to run specific tests of the `integrationtests/backend/tck` module from the IDE,
you will need to rely on runner classes to run them in the appropriate context:
see `org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchTckTestRunner` for Elasticsearch,
or `org.hibernate.search.integrationtest.backend.lucene.testsupport.util.LuceneTckTestRunner` for Lucene.

### Submit

* Push your changes to a topic branch in your fork of the repository.
* Initiate a [pull request](http://help.github.com/send-pull-requests/).
* Update the JIRA issue, using the "Link to pull request" button to include a link to
the created pull request.

## <a id="source-code-structure"></a> Source code structure

The project is split in several Maven modules:

* `build`: Various modules that are mostly useful for the build itself.
* `backend`: The backends, i.e. the modules that provide integration to actual indexing services.
  * `elasticsearch`: A backend that connects to a remote Elasticsearch cluster.
  * `elasticsearch-aws`: Implementation of AWS authentication using request signing for the Elasticsearch backend.
  * `lucene`: A backend that uses an embedded (same JVM) Lucene instance.
* `build-config`: Code-related artifacts like [checkstyle](https://checkstyle.org/) and
[forbiddenapis](https://github.com/policeman-tools/forbidden-apis) rules.
* `distribution`: Builds the distribution package.
* `documentation`: The project documentation.
* `engine`: The Hibernate Search engine.
This module handles most of the basic integration work
(configuration properties, bean instantiation, ...),
defines APIs common to every mapper/backend (the Search DSL in particular),
and provides the "glue" between mappers and backends.
* `integrationtest`: Integration tests for backends (Elasticsearch, Lucene) and mappers (Hibernate ORM),
as well as any other technology Hibernate Search integrates with.
Here are some notable sub-directories:
  * `performance`: performance tests.
  * `showcase/library`: a sample application using Hibernate Search in a Spring Boot environment.
* `mapper`: The mappers, i.e. the modules that expose APIs to index and search user entities,
and do the work of converting between user entities and documents to be indexed.
  * `pojo-base`: Contains base classes and APIs that are re-used in other POJO-based mapper.
  * `orm`: A mapper for [Hibernate ORM](http://hibernate.org/orm/) entities.
  * `orm-outbox-polling`: An implementation of indexing coordination between nodes
  in the orm mapper (see above) using an outbox, i.e. an event table in the database. 
  * `pojo-standalone`: A mapper for POJOs in standalone mode, i.e. without Hibernate ORM.
    Currently incubating, i.e. backwards-incompatible changes in APIs may happen.
* `util`: Various modules containing util classes, both for runtime and for tests.

## <a id="building-from-source"></a> Building from source

### Basic build from the commandline

First, make sure your [development environment is correctly set up](#setup).

The following command will build Hibernate Search, install it in your local Maven repository,
and run unit tests and integration tests.

```bash
./mvnw clean install
```

Note: on Windows, you will need a Docker install able to run Linux containers.
If you don't have that, you can skip the Elasticsearch tests and only run tests against H2 database (without using DB containers):
`./mvnw clean install -Dtest.elasticsearch.skip=true`.

Note: the produced JARs are compatible with Java 8 and later,
regardless of the JDK used to build Hibernate Search.

**WARNING:** Avoid using other goals unless you know what you're doing, because they may leave your workspace
in an undetermined state and lead to strange errors.
In particular, `./mvnw compile` will not build tests and may skip some post-processing of classes,
and `./mvnw package` will not install the JARs into your local Maven repository
which might be a problem for some of the Maven plugins used in the build.
If you did run those commands and are facing strange errors,
you'll have to close your IDE then use `./mvnw clean` to get back to a clean state.

### Building without running tests

To only build Hibernate Search, without running tests, use the following command:

```bash
./mvnw clean install -DskipTests
```

### Documentation

The documentation is based on [Asciidoctor](http://asciidoctor.org/).

To generate the documentation only, without running tests, use:

```bash
./mvnw clean install -pl documentation -am -DskipTests
```

You can then find the freshly built documentation at the following location:

```
./documentation/target/dist/
```

By default only the HTML output is enabled; to also generate the PDF output, enable the `documentation-pdf` profile:

```bash
./mvnw clean install -pl documentation -am -DskipTests -Pdocumentation-pdf
```

### Distribution

To build the distribution bundle, enable the `documentation-pdf` and `dist` profiles:

```bash
./mvnw clean install -Pdocumentation-pdf,dist
```

Or if you don't want to run tests:

```bash
./mvnw clean install -Pdocumentation-pdf,dist -DskipTests
```

### <a id="other-jdks"></a> Other JDKs

To test Hibernate Search against another JDK
than [the one required for the build](#setup-build-tools),
you will need to have both JDKs installed,
and then you will need to pass additional properties to Maven.

To test Hibernate Search against JDK 11:

```bash
./mvnw clean install -Djava-version.test.release=11 -Djava-version.test.launcher.java_home=/path/to/jdk11
```

To test Hibernate Search against JDKs other than 11 or the default 17:

```bash
./mvnw clean install -Djava-version.test.release=15 -Djava-version.test.compiler.java_home=/path/to/jdk15
```

Or more simply, if the newer JDK you want to test against is newer than 17 and is your default JDK:

```bash
./mvnw clean install -Djava-version.test.release=18
```

### Lucene

The Lucene integration tests do not, by themselves,
require any external setup.

If you are not interested in Lucene integration tests (e.g. you only want to test Elasticsearch),
you can skip all Lucene tests with:

```bash
./mvnw clean install -Dtest.lucene.skip=true
```
### Elasticsearch

The Elasticsearch integration tests run against one single version of Elasticsearch at a time,
launching an Elasticsearch server automatically on port 9200 using Docker.
You may redefine the distribution/version to use by specifying the properties
`test.elasticsearch.distribution`/`test.elasticsearch.version`:

```bash
./mvnw clean install -Dtest.elasticsearch.distribution=elastic -Dtest.elasticsearch.version=6.0.0 
```
The following distribution options are supported:
* `elastic` for Elasticsearch distribution
* `opensearch` for OpenSearch (local or Amazon OpenSearch Service)
* `amazon-opensearch-serverless` for Amazon OpenSearch Serverless

For available versions of Elasticsearch distribution from Elastic see [DockerHub](https://hub.docker.com/r/elastic/elasticsearch/tags).
Please note that Elasticsearch [distributions starting with version 7.11 are not open-source](https://opensource.org/node/1099).

For available versions of [OpenSearch](https://www.opensearch.org/) distribution see [DockerHub](https://hub.docker.com/r/opensearchproject/opensearch/tags).

For Amazon OpenSearch Serverless, the version must be unset (set to an empty string).

When necessary (e.g. you don't have Docker, or are on Windows and can't run Linux containers),
you can skip all Elasticsearch tests (and thus the Elasticsearch container startup) with:

```bash
./mvnw clean install -Dtest.elasticsearch.skip=true
```

Alternatively, you can prevent the build from launching an Elasticsearch server automatically
and run Elasticsearch-related tests against your own server using the
`test.elasticsearch.connection.uris` property:

```bash
./mvnw clean install -Dtest.elasticsearch.connection.uris=http://localhost:9200
```

If you want to use HTTPS:

```bash
./mvnw clean install -Dtest.elasticsearch.connection.uris=https://localhost:9200
```

If you want to run tests against a different Elasticsearch version  (6.x for instance),
you will still have to specify the distribution and version:

```bash
./mvnw clean install -Dtest.elasticsearch.distribution=elastic -Dtest.elasticsearch.version=6.0.0 \
        -Dtest.elasticsearch.connection.uris=http://localhost:9200
```

You may also use authentication:

```bash
./mvnw clean install -Dtest.elasticsearch.connection.uris=http://localhost:9200 \
        -Dtest.elasticsearch.connection.username=ironman \
        -Dtest.elasticsearch.connection.password=j@rV1s
```

Also, the elasticsearch integration tests can be executed
against an Elasticsearch service on AWS.
If your AWS credentials are already stored in `~/.aws/credentials`, just run:

```bash
./mvnw clean install -Dtest.elasticsearch.connection.uris=http://<host:port> \
        -Dtest.elasticsearch.connection.aws.signing.enabled=true \
        -Dtest.elasticsearch.connection.aws.region=<Your AWS region ID>
```

If you want to use statically-provided AWS credentials,
use the following instead:

```bash
export HIBERNATE_SEARCH_AWS_STATIC_CREDENTIALS_ACCESS_KEY_ID=<Your access key ID>
export HIBERNATE_SEARCH_AWS_STATIC_CREDENTIALS_SECRET_ACCESS_KEY=<Your secret access key>
./mvnw clean install -Dtest.elasticsearch.connection.uris=http://<host:port> \
        -Dtest.elasticsearch.connection.aws.signing.enabled=true \
        -Dtest.elasticsearch.connection.aws.region=<Your AWS region ID> \
        -Dtest.elasticsearch.connection.aws.credentials.type=static
```

### Testcontainers

Hibernate Search uses Testcontainers in its integration tests. 
By default, testcontainers are not reusable, i.e. a container is started at the beginning of an executed test suite,
and then stopped; each execution of failsafe plugin will start/stop their own containers as well as each test module.
With [reusable testcontainers](https://java.testcontainers.org/features/reuse/) required containers will start as needed
but will not be terminated, staying available between different failsafe executions and test modules
and even after the maven build is finished.
Reusable containers must be stopped manually, if needed (e.g. with `docker stop <container name>`
or `docker kill <container name>`).

There are a few ways to enable reusable testcontainers:

1. Enable reusable testcontainers in `~/.testcontainers.properties`, by adding `testcontainers.reuse.enable=true`
2. Set the environment variable `TESTCONTAINERS_REUSE_ENABLE` to `true`.

### JQAssistant

You can request static analysis and sanity checks with the `jqassistant` profile.
Tests do not need to be run for these checks.

```bash
./mvnw clean install -Pjqassistant -DskipTests
```

To also check cyclic dependencies between packages, use `-Djqassistant.groups=default,cycles`.
Cyclic dependency analysis is costly and may add significant overhead to the build:
at least 10 seconds, maybe one minute or more depending on your setup.

```bash
./mvnw clean install -Pjqassistant -DskipTests -Djqassistant.groups=default,cycles
```

You can also inspect the created Neo4j datastore after a build,
provided that build had the `jqassistant` profile enabled:

```bash
./mvnw jqassistant:server -Pjqassistant
```

The Neo4j web UI will be accessible from http://localhost:7474/.

## <a id="ci"></a> Continuous integration

Continuous integration happens on a self-hosted Jenkins instance at https://ci.hibernate.org.

Several multi-branch pipelines are available.

### Main pipeline

https://ci.hibernate.org/job/hibernate-search/

See [Jenkinsfile](Jenkinsfile).

This job takes care of:

* Primary branch builds
* Pull request builds

It executes the build in a default environment, at the very least.
For primary branches, it may also re-execute the same build in different environments:

* Newer JDKs
* Different database vendors (PostgreSQL, Oracle, ...)
* Different versions of Elasticsearch/OpenSearch
* AWS Elasticsearch/OpenSearch Service

See [this section](#building-from-source) for information on how to execute similar builds from the commandline.

The job can be triggered manually, which is particularly useful to test more environments on a pull request.

### Release pipeline

https://ci.hibernate.org/job/hibernate-search/

See [Jenkinsfile](Jenkinsfile).

This job takes care of:

* Primary branch builds
* Pull request builds

It executes the build in a default environment, at the very least.
For primary branches, it may also re-execute the same build in different environments:

* Newer JDKs
* Different database vendors (PostgreSQL, Oracle, ...)
* Different versions of Elasticsearch/OpenSearch
* AWS Elasticsearch/OpenSearch Service

See [this section](#building-from-source) for information on how to execute similar builds from the commandline.

The job can be triggered manually, which is particularly useful to test more environments on a pull request.
## More conventions

### Naming and architecture rules

Some rules are not checked by Checkstyle, but will only be checked automatically when you submit a PR.
You will spare yourself some back-and-forth by complying with them from the start.

Naming rules are the easiest. All classes/interfaces should be named according to this pattern:

```
[Abstract][<module-specific keyword>][<some meaningful name>][Impl]
```

* An `Abstract` prefix **must** be used for abstract classes.
Exceptions are allowed for classes that don't implement any meaningful interface
in which case the abstract class is assumed to represent both the interface and part of the implementation.
and for marker classes (only private constructors).
* An `Impl` suffix **must only** be used for non-abstract classes
that are the only implementation of an interface defined in Hibernate Search,
with the part of the name before `Impl` being the name of the interface.
* A module-specific keyword should be used whenever a type extends or implements a type from another module.
The exact keyword differs depending on the module, but is generally fairly obvious:
  * `Elasticsearch` for the Elasticsearch backend
  * `Lucene` for the Lucene backend
  * `Pojo` for the Pojo mapper
  * `HibernateOrm` for the Hibernate ORM mapper
  * etc.

For example:
 
* If you add a non-abstract class in the Lucene backend that implements an interface
defined in the engine module, it should be named `Lucene<something>`
* If you add a class in the Lucene backend that is the only implementation of an interface
that is also in the Lucene backend, it should be named `<name of the interface>Impl`.
* If you add a class in the Lucene backend that is one of multiple implementations
of an interface that is also in the Lucene backend,
its name should not have an `Impl` suffix and should meaningfully describe
what is specific to this implementation.

Architecture rules are a bit more complex;
feel free to ignore them, submit your PR and let the reviewer guide you.

* Types whose package contains an "spi" component (`*.spi.*`) are considered SPI.
* Types whose package contains an "impl" component (`*.impl.*`) are considered internal.
* All other types are considered API.
* API types **must not** expose SPI or internal types, be it through inheritance, public or protected fields,
or the return type or parameter type of public or protected methods.
* SPI types **must not** expose internal types, be it through inheritance, public or protected fields,
or the return type or parameter type of public or protected methods.
* Types from a given module A **must not** depend on an internal type defined in another module B.
There are exceptions, for example if module B is purely internal (named `hibernate-search-*-internal-*`),
like `hibernate-search-util-internal-common`.

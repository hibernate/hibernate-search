Guidelines for contributing to Hibernate Search
====

Contributions from the community are essential in keeping Hibernate Search strong and successful.

This guide focuses on how to contribute back to Hibernate Search using GitHub pull requests.

## Legal

All original contributions to Hibernate Search are licensed under the
[GNU Lesser General Public License (LGPL)](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt),
version 2.1 or later, or, if another license is specified as governing the file or directory being
modified, such other license. The LGPL text is included verbatim in the [lgpl.txt](lgpl.txt) file
in the root directory of the repository.

All contributions are subject to the [Developer Certificate of Origin (DCO)](https://developercertificate.org/).
The DCO text is also included verbatim in the [dco.txt](dco.txt) file in the root directory of the repository.

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

### <a id="setup-ide"></a> IDE

#### <a id="setup-ide-intellij-idea"></a> IntelliJ IDEA

Make sure you use IntelliJ IDEA 2022.1 or later, as previous versions have some
[trouble with generated sources](https://youtrack.jetbrains.com/issue/IDEA-286455).

You will need to change some settings:

* `Build, Execution, Deployment > Build Tools > Maven`: set `Maven home path` to `Use Maven wrapper`
* In `Project structure`, make sure the project JDK is JDK 17.
* Set up [formatting rules and code style](#setup-ide-formatting).

Then a few steps will initialize your workspace:

* In the "Maven" side panel, click "Reload all Maven projects".
* In the "Maven" side panel, click "Generate Sources and Update Folders For All Projects".
  This will take a while.
* To check your setup, click `Build > Rebuild Project`.
  If this completes successfully, your workspace is correctly set up.

#### <a id="setup-ide-eclipse"></a> Eclipse

Eclipse shouldn't require any particular setup besides
[formatting rules and code style](#setup-ide-formatting).

#### <a id="setup-ide-formatting"></a> Formatting rules and style conventions

The Hibernate family projects share the same style conventions,
and we provide settings for some IDEs to help you follow these conventions.
See:

* [here for IntelliJ IDEA](https://hibernate.org/community/contribute/intellij-idea/)
* [here for Eclipse IDE](https://hibernate.org/community/contribute/eclipse-ide/)


## Contributing code

### Prerequisites

If you are just getting started with Git, GitHub and/or contributing to Hibernate Search there are a
few prerequisite steps:

* Make sure you have a [Hibernate JIRA account](https://hibernate.atlassian.net)
* Make sure you have a [GitHub account](https://github.com/signup/free)
* [Fork](https://help.github.com/articles/fork-a-repo/) the Hibernate Search [repository](https://github.com/hibernate/hibernate-search).
As discussed in the linked page, this also includes:
    * [Setting](https://help.github.com/articles/set-up-git) up your local git install
    * Cloning your fork
    
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
* Make sure you have added the necessary tests for your changes.
* If relevant, make sure you have updated the documentation to match your changes.
* Run _all_ the tests to assure nothing else was accidentally broken:

    ```bash
    ./mvnw clean install
    ```

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

### Submit

* Push your changes to a topic branch in your fork of the repository.
* Initiate a [pull request](http://help.github.com/send-pull-requests/).
* Update the JIRA issue, using the "Link to pull request" button to include a link to
the created pull request.

## <a id="source-code-structure"></a> Source code structure

The project is split in several Maven modules:

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
* `jakarta`: Modules that take the source code of other modules (e.g. mapper/orm)
and transform it to use Jakarta EE instead of Java EE. 
* `mapper`: The mappers, i.e. the modules that expose APIs to index and search user entities,
and do the work of converting between user entities and documents to be indexed.
  * `pojo-base`: Contains base classes and APIs that are re-used in other POJO-based mapper.
  * `orm`: A mapper for [Hibernate ORM](http://hibernate.org/orm/) entities.
  * `orm-coordination-outbox-polling`: An implementation of coordination of automatic indexing between nodes
  in the orm mapper (see above) using an outbox, i.e. an event table in the database. 
  * `pojo-standalone`: A mapper for POJOs in standalone mode, i.e. without Hibernate ORM.
    Currently incubating, i.e. backwards-incompatible changes in APIs may happen.
* `orm6`: Modules that take the source code of other modules (e.g. mapper/orm)
and transform it to use Hibernate ORM 6 instead of Hibernate ORM 5.x.
* `reports`: Module built last, producing reports related to test coverage in particular.
* `util`: Various modules containing util classes, both for runtime and for tests.

## <a id="building-from-source"></a> Building from source

### Basic build from the commandline

First, make sure your [development environment is correctly set up](#setup).

The following command will build Hibernate Search, install it in your local Maven repository,
and run unit tests and integration tests.

```bash
./mvnw clean install
```

Note: the produced JARs are compatible with Java 8 and later,
regardless of the JDK used to build Hibernate Search.

### Documentation
The documentation is based on [Asciidoctor](http://asciidoctor.org/). By default only the HTML
output is enabled; to also generate the PDF output use:

```bash
./mvnw clean install -Pdocumentation-pdf
```

You can then find the freshly built documentation at the following location:

```
./documentation/target/dist/
```

### Distribution

To build the distribution bundle run:

```bash
./mvnw clean install -Pdocumentation-pdf,dist
```

### <a id="other-jdks"></a> Other JDKs

To test Hibernate Search against another JDK
than [the one required for the build](#setup-build-tools),
you will need to have both JDKs installed,
and then you will need to pass additional properties to Maven.

To test Hibernate Search against JDK 8:

```bash
./mvnw clean install -Djava-version.test.release=8 -Djava-version.test.launcher.java_home=/path/to/jdk8
```

To test Hibernate Search against JDKs other than 8 or the default 17:

```bash
./mvnw clean install -Djava-version.test.release=11 -Djava-version.test.compiler.java_home=/path/to/jdk11
```

Or more simply, if the newer JDK you want to test against is newer than 17 and is your default JDK:

```bash
./mvnw clean install -Djava-version.test.release=18
```

### Elasticsearch

The Elasticsearch integration tests run against one single version of Elasticsearch at a time,
launching an Elasticsearch server automatically on port 9200 using Docker.
You may redefine the version to use by specifying the right profile and using the
`test.elasticsearch.connection.version` property:

```bash
./mvnw clean install -Pelasticsearch-6.0 -Dtest.elasticsearch.connection.version=6.0.0
```

The following profiles are available:

* Elasticsearch distribution from Elastic
  (see available version on [Maven Central](https://search.maven.org/search?q=g:org.elasticsearch%20AND%20a:elasticsearch&core=gav))
  * `elasticsearch-5.6` for 5.6.x and later 5.x
  * `elasticsearch-6.0` for 6.0.x to 6.2.x
  * `elasticsearch-6.3` for 6.3.x
  * `elasticsearch-6.4` for 6.4.x to 6.6.x
  * `elasticsearch-6.7` for 6.7.x
  * `elasticsearch-6.8` for 6.8 and later 6.x
  * `elasticsearch-7.0` for 7.0 to 7.2
  * `elasticsearch-7.3` for 7.3 to 7.6
  * `elasticsearch-7.7` for 7.7
  * `elasticsearch-7.8` for 7.8 to 7.9
  * `elasticsearch-7.10` for 7.10
  * `elasticsearch-7.11` for 7.11 ([not open-source starting with this version](https://opensource.org/node/1099))
  * `elasticsearch-7.12` for 7.12 to 7.17
  * `elasticsearch-8.0` for 8.0+ (**the default**)
* [OpenSearch](https://www.opensearch.org/)
  * `opensearch-1.0` for 1.0 and later 1.x
  * `opensearch-2.0` for 2.0+

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
you will still have to select a profile among those listed above, and specify the version:

```bash
./mvnw clean install -Pelasticsearch-6.0 -Dtest.elasticsearch.connection.version=6.0.0 \
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
You will need to execute something along the lines of:

```bash
./mvnw clean install -Dtest.elasticsearch.connection.uris=http://<host:port> \
        -Dtest.elasticsearch.connection.aws.signing.enabled=true \
        -Dtest.elasticsearch.connection.aws.region=<Your AWS region ID> \
        -Dtest.elasticsearch.connection.aws.credentials.type=static \
        -Dtest.elasticsearch.connection.aws.credentials.access_key_id=<Your access key ID> \
        -Dtest.elasticsearch.connection.aws.credentials.secret_access_key=<Your secret access key>
```

Or more simply, if your AWS credentials are already stored in `~/.aws/credentials`:

```bash
./mvnw clean install -Dtest.elasticsearch.connection.uris=http://<host:port> \
        -Dtest.elasticsearch.connection.aws.signing.enabled=true \
        -Dtest.elasticsearch.connection.aws.region=<Your AWS region ID>
```

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
* Types from a given module A **must not** depend on a internal type defined in another module B.
There are exceptions, for example if module B is purely internal (named `hibernate-search-*-internal-*`),
like `hibernate-search-util-interal-common`.

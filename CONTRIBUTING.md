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
    
### Create a topic branch

Create a "topic" branch on which you will work.  The convention is to name the branch
using the JIRA issue key.  If there is not already a JIRA issue covering the work you
want to do, create one.  Assuming you will be working from the master branch and working
on the JIRA HSEARCH-123:
```bash
git checkout -b HSEARCH-123 master
```

### Formatting rules and style conventions

The Hibernate family projects share the same style conventions,
and we provide settings for some IDEs to help you follow these conventions.
See:

* [here for IntelliJ IDEA](https://hibernate.org/community/contribute/intellij-idea/)
* [here for Eclipse IDE](https://hibernate.org/community/contribute/eclipse-ide/)

If you built the project at least once (`mvn clean install`),
you can very quickly check that you have respected the formatting rules by running Checkstyle:
```bash
mvn checkstyle:check -fn
```

### Code
  
See the [README](README.md) for details about how to build the project and about the structure of the source code.

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
    mvn clean install
    ```

_Prior to committing, if you want to pull in the latest upstream changes (highly
appreciated by the way), please use rebasing rather than merging (see instructions below).  Merging creates
"merge commits" that really muck up the project timeline._

Add the original Hibernate Search repository as a remote repository called upstream:
```bash
git remote add upstream https://github.com/hibernate/hibernate-search.git
```

If you want to rebase your branch on top of the master branch, you can use the following git command:
```bash
git pull --rebase upstream master
```

### Submit

* Push your changes to a topic branch in your fork of the repository.
* Initiate a [pull request](http://help.github.com/send-pull-requests/).
* Update the JIRA issue, using the "Link to pull request" button to include a link to the created pull request.

## Source code structure

The project is split in several Maven modules:

* `backend`: The backends, i.e. the modules that provide integration to actual indexing services.
  * `elasticsearch`: A backend that connects to a remote Elasticsearch cluster.
  * `lucene`: A backend that uses an embedded (same JVM) Lucene instance.
* `build-config`: Code-related artifacts like [checkstyle](https://checkstyle.org/) and [forbiddenapis](https://github.com/policeman-tools/forbidden-apis) rules.
* `distribution`: Builds the distribution package.
* `documentation`: The project documentation.
* `engine`: The Hibernate Search engine.
This module handles most of the basic integration work (configuration properties, bean instantiation, ...),
defines APIs common to every mapper/backend (the Search DSL in particular),
and provides the "glue" between mappers and backends.
* `integrationtest`: Integration tests for backends (Elasticsearch, Lucene) and mappers (Hibernate ORM),
as well as any other technology Hibernate Search integrates with.
Here are some notable sub-directories:
  *  `performance`: performance tests.
  *  `showcase/library`: a sample application using Hibernate Search in a Spring Boot environment.
* `legacy`: Legacy code from Search 5. This code is not part of the distributed JARs.
Parts of it will progressively be re-integrated into the main (Search 6+) code base.
Note that compilation and tests in this directory are disabled by default
(they are only enabled when the property `legacy.skip` is set to `false`).
When enabled, Elasticsearch integration test in this directory 
are executed against Elasticsearch 5.6 by default.
* `mapper`: The mappers, i.e. the modules that expose APIs to index and search user entities,
and do the work of converting between user entities and documents to be indexed.
  * `javabean`: An experimental (not published) mapper for Java Beans without Hibernate ORM.
  Mostly useful for tests of the `pojo` module.
  * `orm`: A mapper for [Hibernate ORM](http://hibernate.org/orm/) entities.
  * `pojo-base`: Contains base classes and APIs that are re-used in other POJO-based mapper.
* `reports`: Module built last, producing reports related to test coverage in particular.
* `util`: Various modules containing util classes, both for runtime and for tests.

## Building from source

### Basic build

You will need:

* Maven 3.6.2 or later.
* JDK 11 or later.

The following command will build Hibernate Search, install it in your local Maven repository,
and run unit tests and integration tests.

```bash
mvn clean install
```

Note: the produced JARs are compatible with Java 8 and later,
regardless of the JDK used to build Hibernate Search.

### Documentation
The documentation is based on [Asciidoctor](http://asciidoctor.org/). By default only the HTML
output is enabled; to also generate the PDF output use:

```bash
mvn clean install -Pdocumentation-pdf
```

You can then find the freshly built documentation in the following location:

```
./documentation/target/asciidoctor/en-US
```

### Distribution

To build the distribution bundle run:

```bash
mvn clean install -Pdocumentation-pdf,dist
```

### Elasticsearch

The Elasticsearch integration tests run against one single version of Elasticsearch at a time,
launching an Elasticsearch server automatically on port 9200.
You may redefine the version to use by specifying the right profile and using the
`test.elasticsearch.connection.version` property:

```bash
mvn clean install -Pelasticsearch-6.0 -Dtest.elasticsearch.connection.version=6.0.0
```

The following profiles are available:

 * `elasticsearch-5.6` for 5.6.x and later 5.x
 * `elasticsearch-6.0` for 6.0.x to 6.2.x
 * `elasticsearch-6.3` for 6.3.x
 * `elasticsearch-6.4` for 6.4.x to 6.6.x
 * `elasticsearch-6.7` for 6.7 and later 6.x
 * `elasticsearch-7.0` for 7.0 to 7.2
 * `elasticsearch-7.3` for 7.3 to 7.6
 * `elasticsearch-7.7` for 7.7+ (the default)

A list of available versions for `test.elasticsearch.connection.version` can be found on
[Maven Central](https://search.maven.org/search?q=g:org.elasticsearch%20AND%20a:elasticsearch&core=gav).

Alternatively, you can prevent the build from launching an Elasticsearch server automatically
and run Elasticsearch-related tests against your own server using the
`test.elasticsearch.connection.hosts` properties:

```bash
mvn clean install -Dtest.elasticsearch.connection.hosts=http://localhost:9200
```

If you want to run tests against a different Elasticsearch version  (6.x for instance),
you will still have to select a profile among those listed above, and specify the version:

```bash
mvn clean install -Pelasticsearch-6.0 -Dtest.elasticsearch.connection.version=6.0.0 -Dtest.elasticsearch.connection.hosts=http://localhost:9200
```

You may also use authentication:

```bash
mvn clean install -Dtest.elasticsearch.connection.hosts=https://localhost:9200 -Dtest.elasticsearch.connection.username=ironman -Dtest.elasticsearch.connection.password=j@rV1s
```

Also, the elasticsearch integration tests can be executed
against an Elasticsearch service on AWS.
You will need to execute something along the lines of:

```bash
mvn clean install -Dtest.elasticsearch.connection.hosts=<The full URL of your Elasticsearch endpoint> -Dtest.elasticsearch.connection.aws.signing.access_key=<Your access key> -Dtest.elasticsearch.connection.aws.signing.secret_key=<Your secret key> -Dtest.elasticsearch.connection.aws.signing.region=<Your AWS region ID>
```

When building Hibernate Search with new JDKs, you may want to run Elasticsearch with a different JDK than the one used by Maven.
This can be done by setting a property
(**this will only work with the profiles for Elasticsearch 5 and above**):

```bash
mvn clean install -Dtest.elasticsearch.run.java_home=/path/to/my/jdk
```

### JQAssistant

You can request static analysis and sanity checks with the `jqassistant` profile.
Tests do not need to be run for these checks.

```bash
mvn clean install -Pjqassistant -DskipTests
```

To also check cyclic dependencies between packages, use `-Djqassistant.groups=default,cycles`.
Cyclic dependency analysis is costly and may add significant overhead to the build:
at least 10 seconds, maybe one minute or more depending on your setup.

```bash
mvn clean install -Pjqassistant -DskipTests -Djqassistant.groups=default,cycles
```

You can also inspect the created Neo4j datastore after a build,
provided that build had the `jqassistant` profile enabled:

```bash
mvn jqassistant:server -Pjqassistant-server -pl reports
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
 
* If you add a non-abstract class in the Lucene backend that implements an interface defined in the engine module,
it should be named `Lucene<something>`
* If you add a class in the Lucene backend that is the only implementation of an interface that is also in the Lucene backend,
it should be named `<name of the interface>Impl`.
* If you add a class in the Lucene backend that is one of multiple implementations of an interface that is also in the Lucene backend,
its name should not have an `Impl` suffix and should meaningfully describe what is specific to this implementation.

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

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

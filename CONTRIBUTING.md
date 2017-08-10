Guidelines for contributing to Hibernate Search
====

Contributions from the community are essential in keeping Hibernate Search strong and successful.

This guide focuses on how to contribute back to Hibernate Search using GitHub pull requests.
If you need help with cloning, compiling or setting the project up in an IDE please refer to
[this page](http://hibernate.org/search/contribute/).

## Legal

All original contributions to Hibernate Search are licensed under the
[GNU Lesser General Public License (LGPL)](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt),
version 2.1 or later, or, if another license is specified as governing the file or directory being
modified, such other license. The LGPL text is included verbatim in the [lgpl.txt](lgpl.txt) file
in the root directory of the repository.

All contributions are subject to the [Developer Certificate of Origin (DCO)](https://developercertificate.org/).
The DCO text is also included verbatim in the [dco.txt](dco.txt) file in the root directory of the repository.

## Getting Started

If you are just getting started with Git, GitHub and/or contributing to Hibernate Search there are a
few prerequisite steps:

* Make sure you have a [Hibernate JIRA account](https://hibernate.atlassian.net)
* Make sure you have a [GitHub account](https://github.com/signup/free)
* [Fork](https://help.github.com/articles/fork-a-repo/) the Hibernate Search [repository](https://github.com/hibernate/hibernate-search).
As discussed in the linked page, this also includes:
    * [Setting](https://help.github.com/articles/set-up-git) up your local git install
    * Cloning your fork

## Create a test case

If you have opened a JIRA issue but are not comfortable enough to contribute code directly, creating a self
contained test case is a good first step towards contributing.

As part of our efforts to simplify access to new contributors, we provide [test case templates for the Hibernate family
projects](https://github.com/hibernate/hibernate-test-case-templates).

Just fork this repository, build your test case and attach it as an archive to a JIRA issue.

## Create a topic branch

Create a "topic" branch on which you will work.  The convention is to name the branch
using the JIRA issue key.  If there is not already a JIRA issue covering the work you
want to do, create one.  Assuming you will be working from the master branch and working
on the JIRA HSEARCH-123:
```shell
git checkout -b HSEARCH-123 master
```

## Code

Code away...

## Formatting rules and style conventions

The Hibernate family projects share the same style conventions. You can download the appropriate configuration
files for your IDE from [the IDE codestyles GitHub repository](https://github.com/hibernate/hibernate-ide-codestyles).

You can very quickly check that you have respected the formatting rules by running Checkstyle:
```shell
mvn checkstyle:check
```

## Commit

* Make commits of logical units.
* Be sure to start the commit messages with the JIRA issue key you are working on. This is how JIRA will pick
up the related commits and display them on the JIRA issue.
* Avoid formatting changes to existing code as much as possible: they make the intent of your patch less clear.
* Make sure you have added the necessary tests for your changes.
* Run _all_ the tests to assure nothing else was accidentally broken:

```shell
mvn verify
```

_Prior to committing, if you want to pull in the latest upstream changes (highly
appreciated by the way), please use rebasing rather than merging (see instructions below).  Merging creates
"merge commits" that really muck up the project timeline._

Add the original Hibernate Search repository as a remote repository called upstream:
```shell
git remote add upstream https://github.com/hibernate/hibernate-search.git
```

If you want to rebase your branch on top of the master branch, you can use the following git command:
```shell
git pull --rebase upstream master
```

## Submit
* Push your changes to a topic branch in your fork of the repository.
* Initiate a [pull request](http://help.github.com/send-pull-requests/).
* Update the JIRA issue, using the "Link to pull request" button to include a link to the created pull request.

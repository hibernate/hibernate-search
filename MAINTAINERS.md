Guide for maintainers of Hibernate Search
====

This guide is intended for maintainers of Hibernate Search,
i.e. anybody with direct push access to the git repository.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Continuous integration

Continuous integration happens on a self-hosted Jenkins instance at https://ci.hibernate.org.

Several multi-branch pipelines are available.

### Main pipeline

https://ci.hibernate.org/job/hibernate-search/

This job takes care of:

* Primary branch builds
* Pull request builds

It is generally triggered on push,
but can also be triggered manually,
which is particularly useful to test more environments on a pull request.

See [Jenkinsfile](Jenkinsfile) for the job definition.

This job executes the build in a default environment, at the very least.
For primary branches, it may also re-execute the same build in different environments:

* Newer JDKs
* Different database vendors (PostgreSQL, Oracle, ...)
* Different versions of Elasticsearch/OpenSearch
* AWS Elasticsearch/OpenSearch Service

See [CONTRIBUTING.md](CONTRIBUTING.md#building-from-source)
for information about how to execute similar builds from the commandline.

### Release pipeline

https://ci.hibernate.org/job/hibernate-search-release/

This job takes care of releases. It is triggered manually.

See [ci/release/Jenkinsfile](ci/release/Jenkinsfile) for the job definition.

See [Releasing](#releasing) for more information.

### Dependency update pipeline

https://ci.hibernate.org/job/hibernate-search-dependency-update/

This job builds and tests Hibernate Search against updated versions of dependencies,
for example newer versions of Hibernate ORM.

See [ci/dependency-update/Jenkinsfile](ci/dependency-update/Jenkinsfile) for the job definition,
or to configure which dependency updates are tested exactly.

In some cases where the upgrade requires changes to the Hibernate Search source code,
this job may automatically merge a branch before running the build.
By convention, such a branch will be named `wip/<base-branch>-dependency-update-<update-name>`,
e.g. `wip/main-dependency-update-orm6.1`.

### Performance pipeline

https://ci.hibernate.org/job/hibernate-search-performance/

A few jobs are hosted there, but they are not run regularly,
have not been kept up-to-date, and might need a refresh.

## <a id="releasing"></a> Releasing

See https://hibernate.org/search/documentation/maintain/.

TODO: Move the documentation here.

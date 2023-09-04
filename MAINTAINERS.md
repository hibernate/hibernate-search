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

Note you can test dependency updates locally by calling the script `ci/dependency-update/perform-update.sh`.
First, make sure to have all the necessary artifacts in your local Maven repository:

```shell
./mvnw clean install -Pdist -DskipTests
```

Then update the dependencies and apply necessary patches to tests with this command:

```shell
ci/dependency-update/perform-update.sh orm6.2 version.org.hibernate.orm
```

Or if you're on a feature branch and want to apply patches of another, primary branch (here the main branch):

```shell
BRANCH_NAME=main ci/dependency-update/perform-update.sh orm6.2 version.org.hibernate.orm
```

After applying the update, you can easily run all tests that rely on a particular module with this command:

```shell
./mvnw clean verify -Pdependency-update -Pdist -pl $(./ci/list-dependent-integration-tests.sh hibernate-search-mapper-orm)
```

### Performance pipeline

https://ci.hibernate.org/job/hibernate-search-performance/

A few jobs are hosted there, but they are not run regularly,
have not been kept up-to-date, and might need a refresh.

## <a id="releasing"></a> Releasing

### Preparing the release

* Check that everything has been pushed to the upstream repository.
* Check that the CI job for the branch you want to release is green.
* Check Jira:
  * Check there are no outstanding issues assigned to the release.
  * Check there are no resolved/closed issues in the current `*-backlog` "version";
    if there are, you might want to assign them to your release.
* **If it's a `.CR` or `.Final` release**:
  * Check that the [migration guide](documentation/src/main/asciidoc/migration/index.adoc) is up to date.
    In particular, check the git history for API/SPI changes
    and document them in the migration guide.
* If you **added a new Maven module** that should be included in the distribution,
  **check that it has been included in the distribution** (javadoc and ZIP distribution).
  * `mvn clean install -Pdocumentation-pdf,dist -DskipTests`
  * Check the distribution package as built by Maven (`distribution/target/hibernate-search-<version>-dist`).
    In particular, check the jar files in the subdirectories:
    * `lib/required`
    * `lib/optional`
    * `lib/provided`

    They should contain the appropriate dependencies, without duplicates.
    The creation of these directories is driven by the assembly plugin (`distribution/src/main/assembly/dist.xml`)
    which is very specific and might break with the inclusion of new dependencies.

### Performing the release

Once you trigger the CI job, it automatically pushes artifacts to the
[OSSRH repository manager](https://oss.sonatype.org/#stagingRepositories),
the distribution to [SourceForge](https://sourceforge.net/projects/hibernate/files/hibernate-search/)
and the documentation to [docs.jboss.org](https://docs.jboss.org/hibernate/search/).

* Transfer the released issues in JIRA to the "Closed state":
  * Go to [the list of releases](https://hibernate.atlassian.net/projects/HSEARCH?selectedItem=com.atlassian.jira.jira-projects-plugin%3Arelease-page)
  * Select the version to release.
  * Click the link "View in Issue Navigator" on the top right corner of the list.
  * Click the button with three dots on the top right corner of the screen and click "Bulk update all XX issues".
  * Use the "Transition" action to transition your issues from "Resolved" to "Closed".
* Release the version on JIRA:
  * Go to [the list of releases](https://hibernate.atlassian.net/projects/HSEARCH?selectedItem=com.atlassian.jira.jira-projects-plugin%3Arelease-page)
  * Click "Release" next to the version to release.
* Do *not* update the repository (in particular changelog.txt and README.md), 
  the release job does it for you.
* Trigger the release on CI:
  * Go to CI, to [the "hibernate-search-release" CI job](https://ci.hibernate.org/job/hibernate-search-release/).
  * Click the "run" button (the green triangle on top of a clock, to the right) next to the branch you want to release.
  * **Be careful** when filling the form with the build parameters.
  * Note that for new branches where the job has never run, the first run will not ask for parameters and thus will fail:
    that's expected, just run it again.
* Release the artifacts on the [OSSRH repository manager](https://oss.sonatype.org/#stagingRepositories).
  * Log into Nexus. The credentials can be found on Bitwarden; ask a teammate if you don't have access.
  * Click "staging repositories" to the left.
  * Examine your staging repository: check that all expected artifacts are there.
  * If necessary (that's very rare), test the release in the staging repository.
    You can drop the staging repo if there is a problem,
    but you'll need to revert the commits pushed during the release.
  * If everything is ok, select the staging repository and click the "Release" button.
    * For Search 5.x and below, you will also need to "Close" the repository before you can release it.
      A manual refresh may be necessary after closing.

### Announcing the release

* Update [hibernate.org](https://github.com/hibernate/hibernate.org):
  * If it is a new major or minor release, add a `_data/projects/search/releases/series.yml` file
    and a `search/releases/<version>/index.adoc` file.
  * Add a new YAML release file to `_data/projects/search/releases`.
  * Depending on which series you want to have displayed,
    make sure to adjust the `end-of-life`/`displayed` flag of the `series.yml` file of the old series.
  * Push to the production branch.
* Blog about release on [in.relation.to](https://github.com/hibernate/in.relation.to).
  Make sure to use the tags "Hibernate Search" and "Releases" for the blog entry.
* Send an email to hibernate-announce and CC hibernate-dev.
* Tweet about the release via the @Hibernate account.
  Try to engage with the Elasticsearch/Lucene community or other communities depending on the release highlights.

### Updating depending projects

If you just released the latest stable, you will need to update other projects:

* Approve and merge automatic updates that dependabot will send (it might take ~24h):
  * In the [test case templates](https://github.com/hibernate/hibernate-test-case-templates/tree/master/search).
  * In the [demos](https://github.com/hibernate/hibernate-demos/tree/master/hibernate-search).
* **If it's a `.Final` release**, upgrade the Hibernate Search dependency manually:
  * In the [Quarkus BOM](https://github.com/quarkusio/quarkus/blob/main/bom/application/pom.xml).
  * In the [WildFly root POM](https://github.com/wildfly/wildfly/blob/main/pom.xml).
  * In any other relevant project.

### Updating Hibernate Search

* **If it is a new major or minor release**, and if not already done, create a maintenance branch for the previous series:
  * `git branch <x.(y-1)>`
  * `mvn versions:set -DnewVersion=<x.(y-1).z>-SNAPSHOT`
  * `git add`, `commit`, `push upstream` the new branch.
  * Activate GitHub's "branch protection" features on the newly created maintenance branch:
    https://github.com/hibernate/hibernate-search/settings/branches/.
* Make sure to keep the `previous.stable` property in the POM up-to-date
  on all actively developed branches.
  The property must point to the latest micro of the previous minor.
  E.g. let's say you release 5.6.5 while actively working on 5.7,
  then the development branch for 5.7 must have its `previous.stable` property set to 5.6.5.

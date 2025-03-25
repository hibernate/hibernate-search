/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/*
 * See https://github.com/hibernate/hibernate-jenkins-pipeline-helpers
 */
@Library('hibernate-jenkins-pipeline-helpers@1.13') _

import groovy.transform.Field
import org.hibernate.jenkins.pipeline.helpers.job.JobHelper
import org.hibernate.jenkins.pipeline.helpers.alternative.AlternativeMultiMap

/*
 * WARNING: DO NOT IMPORT LOCAL LIBRARIES HERE.
 *
 * By local, I mean libraries whose files are in the same Git repository.
 *
 * The Jenkinsfile is protected and will not be executed if modified in pull requests from external users,
 * but other local library files are not protected.
 * A user could potentially craft a malicious PR by modifying a local library.
 *
 * See https://blog.grdryn.me/blog/jenkins-pipeline-trust.html for a full explanation,
 * and a potential solution if we really need local libraries.
 * Alternatively we might be able to host libraries in a separate GitHub repo and configure
 * them in the GUI: see https://ci.hibernate.org/job/hibernate-search/configure, "Pipeline Libraries".
 */

/*
 * See https://github.com/hibernate/hibernate-jenkins-pipeline-helpers for the documentation
 * of the helpers library used in this Jenkinsfile,
 * and for help writing Jenkinsfiles.
 *
 * ### Jenkins configuration
 *
 * #### Jenkins plugins
 *
 * This file requires the following plugins in particular:
 *
 * - everything required by the helpers library (see the org.hibernate.(...) imports for a link to its documentation)
 * - https://plugins.jenkins.io/ec2 for AWS credentials
 * - https://plugins.jenkins.io/lockable-resources for the lock on the AWS Elasticsearch server
 * - https://plugins.jenkins.io/pipeline-github for the trigger on pull request comments
 *
 * #### Script approval
 *
 * If not already done, you will need to allow the following calls in <jenkinsUrl>/scriptApproval/:
 *
 * - everything required by the helpers library (see the org.hibernate.(...) imports for a link to its documentation)
 *
 * ### Integrations
 *
 * #### AWS
 *
 * This job will trigger integration tests against an Elasticsearch service hosted on AWS.
 *
 * You need to set some environment variables to select the endpoint (see below).
 *
 * Then you will also need to add AWS credentials in Jenkins
 * and reference them from the configuration file (see below).
 *
 * #### Sonarcloud (optional)
 *
 * You need to enable the SonarCloud GitHub app for your repository:
 * see https://github.com/apps/sonarcloud.
 *
 * Then you will also need to add SonarCloud credentials in Jenkins
 * and reference them from the configuration file (see below).
 *
 * #### Develocity (optional)
 *
 * You will need to add Develocity credentials in Jenkins
 * and reference them from the configuration file (see below).
 *
 * #### Gitter (optional)
 *
 * You need to enable the Jenkins integration in your Gitter room first:
 * see https://gitlab.com/gitlab-org/gitter/webapp/blob/master/docs/integrations.md
 *
 * Then you will also need to configure *global* secret text credentials containing the Gitter webhook URL,
 * and list the ID of these credentials in the job configuration file
 * (see https://github.com/hibernate/hibernate-jenkins-pipeline-helpers#job-configuration-file).
 *
 * ### Job configuration
 *
 * This Jenkinsfile gets its configuration from four sources:
 * branch name, environment variables, a configuration file, and credentials.
 * All configuration is optional for the default build (and it should stay that way),
 * but some features require some configuration.
 *
 * #### Branch name
 *
 * See the org.hibernate.(...) imports for a link to the helpers library documentation,
 * which explains the basics.
 *
 * #### Environment variables
 *
 * The following environment variables are necessary for some features:
 *
 * - 'ES_AWS_REGION', containing the name of an AWS region such as 'us-east-1', to test Elasticsearch as a service on AWS.
 * - 'ES_AWS_<majorminor without dot>_ENDPOINT' (e.g. 'ES_AWS_52_ENDPOINT'),
 * containing the URL of an AWS Elasticsearch service endpoint using that exact version,
 * to test Elasticsearch as a service on AWS.
 *
 * #### Job configuration file
 *
 * See the org.hibernate.(...) imports for a link to the helpers library documentation,
 * which explains the basic structure of this file and how to set it up.
 *
 * Below is the additional structure specific to this Jenkinsfile:
 *
 *     aws:
 *       # String containing the ID of aws credentials. Mandatory in order to test against an Elasticsearch service hosted on AWS.
 *       # Expects username/password credentials where the username is the AWS access key
 *       # and the password is the AWS secret key.
 *       credentials: ...
 *     sonar:
 *       # String containing the ID of Sonar credentials. Optional.
 *       # Expects username/password credentials where the username is the organization ID on sonarcloud.io
 *       # and the password is a sonarcloud.io access token with sufficient rights for that organization.
 *       credentials: ...
 *     develocity:
 *       credentials:
 *         # String containing the ID of Develocity credentials used on "main" (non-PR) builds. Optional.
 *         # Expects something valid for the DEVELOCITY_ACCESS_KEY environment variable.
 *         # See https://docs.gradle.com/enterprise/gradle-plugin/#via_environment_variable
 *         main: ...
 *         # String containing the ID of Develocity credentials used on PR builds. Optional.
 *         # Expects something valid for the DEVELOCITY_ACCESS_KEY environment variable.
 *         # See https://docs.gradle.com/enterprise/gradle-plugin/#via_environment_variable
 *         # WARNING: These credentials should not give write access to the build cache!
 *         pr: ...
 */

@Field final String DEFAULT_JDK_TOOL = 'OpenJDK 21 Latest'
@Field final String MAVEN_TOOL = 'Apache Maven 3.9'

// Default node pattern, to be used for resource-intensive stages.
// Should not include the controller node.
@Field final String NODE_PATTERN_BASE = 'Worker&&Containers'
// Quick-use node pattern, to be used for very light, quick, and environment-independent stages,
// such as sending a notification. May include the controller node in particular.
@Field final String QUICK_USE_NODE_PATTERN = 'Controller||Worker'

@Field AlternativeMultiMap<BuildEnvironment> environments
@Field JobHelper helper

@Field boolean enableDefaultBuild = false
@Field boolean enableDefaultBuildIT = false
@Field boolean incrementalBuild = false

this.helper = new JobHelper(this)

helper.runWithNotification {

stage('Configure') {
	// We want to make sure that if we are building a PR that the branch name will not require any escaping of symbols in it.
	// Otherwise, it may lead to cryptic build errors.
	if (helper.scmSource.branch.name && !(helper.scmSource.branch.name ==~ /^[\w\d\/\\_\-\.]+$/)) {
		throw new IllegalArgumentException("""
											Branch name ${helper.scmSource.branch.name} contains unexpected symbols.
											 Only characters, digits and -_.\\/ symbols are allowed in the branch name.
											 Change the branch name and open a new Pull Request.
										   """)
	}

	requireApprovalForPullRequest 'hibernate'

	this.environments = AlternativeMultiMap.create([
			jdk: [
					// This should not include every JDK; in particular let's not care too much about EOL'd JDKs like version 9
					// See http://www.oracle.com/technetwork/java/javase/eol-135779.html
					new JdkBuildEnvironment(version: '11', testCompilerTool: 'OpenJDK 11 Latest',
							condition: TestCondition.AFTER_MERGE),
					new JdkBuildEnvironment(version: '17', testCompilerTool: 'OpenJDK 17 Latest',
							condition: TestCondition.AFTER_MERGE),
					new JdkBuildEnvironment(version: '21', testCompilerTool: 'OpenJDK 21 Latest',
							condition: TestCondition.BEFORE_MERGE,
							isDefault: true),
					// We want to enable preview features when testing newer builds of OpenJDK:
					// even if we don't use these features, just enabling them can cause side effects
					// and it's useful to test that.
					new JdkBuildEnvironment(version: '22', testCompilerTool: 'OpenJDK 22 Latest',
							testLauncherArgs: '--enable-preview',
							condition: TestCondition.AFTER_MERGE),
					// The following JDKs aren't supported by Hibernate ORM out-of-the box yet:
					// they require the use of -Dnet.bytebuddy.experimental=true.
					// Make sure to remove that argument as soon as possible
					// -- generally that requires upgrading bytebuddy in Hibernate ORM after the JDK goes GA.
					new JdkBuildEnvironment(version: '23', testCompilerTool: 'OpenJDK 23 Latest',
							testLauncherArgs: '--enable-preview',
							condition: TestCondition.AFTER_MERGE),
					new JdkBuildEnvironment(version: '24', testCompilerTool: 'OpenJDK 24 Latest',
							testLauncherArgs: '--enable-preview -Dnet.bytebuddy.experimental=true',
							condition: TestCondition.AFTER_MERGE),
					new JdkBuildEnvironment(version: '25', testCompilerTool: 'OpenJDK 25 Latest',
							testLauncherArgs: '--enable-preview -Dnet.bytebuddy.experimental=true',
							condition: TestCondition.AFTER_MERGE)
					// IMPORTANT: Make sure to update the documentation for any newly supported Java versions
					//            See java-version.main.compatible.expected.text in POMs.
			],
			compiler: [
					new CompilerBuildEnvironment(name: 'eclipse', mavenProfile: 'compiler-eclipse',
							condition: TestCondition.BEFORE_MERGE)
			],
			database: [
					new DatabaseBuildEnvironment(dbName: 'h2', mavenProfile: 'h2',
							condition: TestCondition.BEFORE_MERGE,
							isDefault: true),
					new DatabaseBuildEnvironment(dbName: 'postgresql', mavenProfile: 'ci-postgresql',
							condition: TestCondition.BEFORE_MERGE),
                    new DatabaseBuildEnvironment(dbName: 'oracle', mavenProfile: 'ci-oracle',
                            condition: TestCondition.BEFORE_MERGE),
					new DatabaseBuildEnvironment(dbName: 'mariadb', mavenProfile: 'ci-mariadb',
							condition: TestCondition.AFTER_MERGE),
					new DatabaseBuildEnvironment(dbName: 'mysql', mavenProfile: 'ci-mysql',
                    		condition: TestCondition.AFTER_MERGE),
                    new DatabaseBuildEnvironment(dbName: 'db2', mavenProfile: 'ci-db2',
                            slow: true,
                            condition: TestCondition.AFTER_MERGE),
                    new DatabaseBuildEnvironment(dbName: 'mssql', mavenProfile: 'ci-mssql',
                            condition: TestCondition.AFTER_MERGE),
                    new DatabaseBuildEnvironment(dbName: 'cockroachdb', mavenProfile: 'ci-cockroachdb',
                            slow: true,
                            condition: TestCondition.AFTER_MERGE),
			],
			localElasticsearch: [
					// --------------------------------------------
					// Elasticsearch distribution from Elastic
					// Not testing 7.9- since those versions are EOL'ed.
					new LocalElasticsearchBuildEnvironment(version: '7.10.1', condition: TestCondition.AFTER_MERGE),
					// Not testing 7.11 to make the build quicker.
					// The only difference with 7.12+ is that wildcard predicates on analyzed fields get their pattern normalized,
					// and that was deemed a bug: https://github.com/elastic/elasticsearch/pull/53127
					new LocalElasticsearchBuildEnvironment(version: '7.11.2', condition: TestCondition.ON_DEMAND),
					new LocalElasticsearchBuildEnvironment(version: '7.12.1', condition: TestCondition.ON_DEMAND),
					new LocalElasticsearchBuildEnvironment(version: '7.13.2', condition: TestCondition.ON_DEMAND),
					// 7.14 and 7.15 have annoying bugs that make almost all of our test suite fail,
					// so we don't test them
					// See https://hibernate.atlassian.net/browse/HSEARCH-4340
					new LocalElasticsearchBuildEnvironment(version: '7.16.3', condition: TestCondition.ON_DEMAND),
					new LocalElasticsearchBuildEnvironment(version: '7.17.23', condition: TestCondition.AFTER_MERGE),
					// Not testing 8.0 because we know there are problems in 8.0.1 (see https://hibernate.atlassian.net/browse/HSEARCH-4497)
					// Not testing 8.1-8.6 to make the build quicker.
					new LocalElasticsearchBuildEnvironment(version: '8.1.3', condition: TestCondition.ON_DEMAND),
					new LocalElasticsearchBuildEnvironment(version: '8.2.3', condition: TestCondition.ON_DEMAND),
					new LocalElasticsearchBuildEnvironment(version: '8.3.3', condition: TestCondition.ON_DEMAND),
					new LocalElasticsearchBuildEnvironment(version: '8.4.3', condition: TestCondition.ON_DEMAND),
					new LocalElasticsearchBuildEnvironment(version: '8.5.3', condition: TestCondition.ON_DEMAND),
					new LocalElasticsearchBuildEnvironment(version: '8.6.2', condition: TestCondition.ON_DEMAND),
					new LocalElasticsearchBuildEnvironment(version: '8.7.1', condition: TestCondition.ON_DEMAND),
					new LocalElasticsearchBuildEnvironment(version: '8.8.2', condition: TestCondition.ON_DEMAND),
					new LocalElasticsearchBuildEnvironment(version: '8.9.2', condition: TestCondition.ON_DEMAND),
					new LocalElasticsearchBuildEnvironment(version: '8.10.4', condition: TestCondition.ON_DEMAND),
					new LocalElasticsearchBuildEnvironment(version: '8.11.4', condition: TestCondition.ON_DEMAND),
					new LocalElasticsearchBuildEnvironment(version: '8.12.2', condition: TestCondition.ON_DEMAND),
					new LocalElasticsearchBuildEnvironment(version: '8.13.4', condition: TestCondition.ON_DEMAND),
					new LocalElasticsearchBuildEnvironment(version: '8.14.3', condition: TestCondition.ON_DEMAND),
					new LocalElasticsearchBuildEnvironment(version: '8.15.4', condition: TestCondition.BEFORE_MERGE, isDefault: true),
					// IMPORTANT: Make sure to update the documentation for any newly supported Elasticsearch versions
					//            See version.org.elasticsearch.compatible.expected.text
					//            and version.org.elasticsearch.compatible.regularly-tested.text in POMs.

					// --------------------------------------------
					// OpenSearch
					// Not testing 1.0 - 1.2 as these versions are EOL'ed.
					new LocalOpenSearchBuildEnvironment(version: '1.3.18', condition: TestCondition.AFTER_MERGE),
					// See https://opensearch.org/lines/1x.html for a list of all 1.x versions
					new LocalOpenSearchBuildEnvironment(version: '2.0.1', condition: TestCondition.ON_DEMAND),
					new LocalOpenSearchBuildEnvironment(version: '2.1.0', condition: TestCondition.ON_DEMAND),
					new LocalOpenSearchBuildEnvironment(version: '2.2.1', condition: TestCondition.ON_DEMAND),
					new LocalOpenSearchBuildEnvironment(version: '2.3.0', condition: TestCondition.ON_DEMAND),
					new LocalOpenSearchBuildEnvironment(version: '2.4.1', condition: TestCondition.ON_DEMAND),
					new LocalOpenSearchBuildEnvironment(version: '2.5.0', condition: TestCondition.ON_DEMAND),
					new LocalOpenSearchBuildEnvironment(version: '2.6.0', condition: TestCondition.ON_DEMAND),
					new LocalOpenSearchBuildEnvironment(version: '2.7.0', condition: TestCondition.ON_DEMAND),
					new LocalOpenSearchBuildEnvironment(version: '2.8.0', condition: TestCondition.ON_DEMAND),
					new LocalOpenSearchBuildEnvironment(version: '2.9.0', condition: TestCondition.ON_DEMAND),
					new LocalOpenSearchBuildEnvironment(version: '2.10.0', condition: TestCondition.ON_DEMAND),
					new LocalOpenSearchBuildEnvironment(version: '2.11.1', condition: TestCondition.ON_DEMAND),
					new LocalOpenSearchBuildEnvironment(version: '2.12.0', condition: TestCondition.ON_DEMAND),
					new LocalOpenSearchBuildEnvironment(version: '2.13.0', condition: TestCondition.ON_DEMAND),
					new LocalOpenSearchBuildEnvironment(version: '2.14.0', condition: TestCondition.ON_DEMAND),
					new LocalOpenSearchBuildEnvironment(version: '2.15.0', condition: TestCondition.ON_DEMAND),
					new LocalOpenSearchBuildEnvironment(version: '2.16.0', condition: TestCondition.BEFORE_MERGE),
					// See https://opensearch.org/lines/2x.html for a list of all 2.x versions
					// IMPORTANT: Make sure to update the documentation for any newly supported OpenSearch versions
					//            See version.org.opensearch.compatible.expected.text
					//            and version.org.opensearch.compatible.regularly-tested.text in POMs.

					// --------------------------------------------
					// Amazon OpenSearch Serverless dialect running against a local OpenSearch instance
					// WARNING: this does NOT actually run against Amazon OpenSearch Serverless (yet)
					// See https://hibernate.atlassian.net/browse/HSEARCH-4919
					new AmazonOpenSearchServerlessLocalBuildEnvironment(condition: TestCondition.AFTER_MERGE)
			],
			amazonElasticsearch: [
					// --------------------------------------------
					// Amazon Elasticsearch Service (OpenDistro)
					new AmazonElasticsearchServiceBuildEnvironment(version: '7.10', condition: TestCondition.AFTER_MERGE),

					// --------------------------------------------
					// Amazon OpenSearch Service
					new AmazonOpenSearchServiceBuildEnvironment(version: '1.3', condition: TestCondition.AFTER_MERGE),
					new AmazonOpenSearchServiceBuildEnvironment(version: '2.13', condition: TestCondition.AFTER_MERGE),
					// Also test static credentials, but only for the latest version
					new AmazonOpenSearchServiceBuildEnvironment(version: '2.13', condition: TestCondition.AFTER_MERGE, staticCredentials: true)
			]
	])

	helper.configure {
		configurationNodePattern QUICK_USE_NODE_PATTERN
		file 'job-configuration.yaml'
		jdk {
			defaultTool DEFAULT_JDK_TOOL
		}
		maven {
			defaultTool MAVEN_TOOL
			producedArtifactPattern "org/hibernate/search/*"
			producedArtifactPattern "org/hibernate/hibernate-search*"
		}
	}

	properties([
			buildDiscarder(
					logRotator(daysToKeepStr: '30', numToKeepStr: '10')
			),
			disableConcurrentBuilds(abortPrevious: true),
			pipelineTriggers(
					// HSEARCH-3417: do not add snapshotDependencies() here, this was known to cause problems.
					[
							issueCommentTrigger('.*test this please.*')
					]
							+ helper.generateUpstreamTriggers()
			),
			helper.generateNotificationProperty(),
			[$class: 'EnvInjectJobProperty', info: [propertiesContent: 'TESTCONTAINERS_REUSE_ENABLE=true']],
			parameters([
					choice(
							name: 'ENVIRONMENT_SET',
							choices: """AUTOMATIC
DEFAULT
SUPPORTED
ALL""",
							description: """A set of environments that must be checked.
'AUTOMATIC' picks a different set of environments based on the branch name.
'DEFAULT' means a single build with the default environment expected by the Maven configuration,
while other options will trigger multiple Maven executions in different environments."""
					),
					string(
							name: 'ENVIRONMENT_FILTER',
							defaultValue: '',
							trim: true,
							description: """A regex filter to apply to the environments that must be checked.
If this parameter is non-empty, ENVIRONMENT_SET will be ignored and environments whose tag matches the given regex will be checked.
Some useful filters: 'default', 'jdk', 'jdk-10', 'eclipse', 'postgresql', 'elasticsearch-local-[5.6'.
"""
					)
			])
	])

	if (params.ENVIRONMENT_FILTER) {
		keepOnlyEnvironmentsMatchingFilter(params.ENVIRONMENT_FILTER)
	}
	else {
		keepOnlyEnvironmentsFromSet(params.ENVIRONMENT_SET)
	}

	// Determine whether ITs need to be run in the default build
	enableDefaultBuildIT = environments.content.any { key, envSet ->
		return envSet.enabled.contains(envSet.default)
	}
	// No need to re-test default environments separately, they will be tested as part of the default build if needed
	environments.content.each { key, envSet ->
		envSet.enabled.remove(envSet.default)
	}

	enableDefaultBuild =
			enableDefaultBuildIT ||
			environments.content.any { key, envSet -> envSet.enabled.any { buildEnv -> buildEnv.requiresDefaultBuildArtifacts() } }

	if (helper.scmSource.pullRequest) {
		incrementalBuild = true
	}

	echo """Branch: ${helper.scmSource.branch.name}
PR: ${helper.scmSource.pullRequest?.id}
params.ENVIRONMENT_SET: ${params.ENVIRONMENT_SET}
params.ENVIRONMENT_FILTER: ${params.ENVIRONMENT_FILTER}

Resulting execution plan:
    enableDefaultBuild=$enableDefaultBuild
    enableDefaultBuildIT=$enableDefaultBuildIT
    environments=${environments.enabledAsString}
    incrementalBuild=$incrementalBuild
"""
}

stage('Default build') {
	if (!enableDefaultBuild) {
		echo 'Skipping default build and integration tests in the default environment'
		helper.markStageSkipped()
		return
	}
	runBuildOnNode( NODE_PATTERN_BASE, [time: 2, unit: 'HOURS'] ) {
		withMavenWorkspace {
			String commonMavenArgs = """ \
					--fail-at-end \
					-Pcoverage \
					${toTestEnvironmentArgs(environments.content.jdk.default)} \
			"""

			echo "Building code and running unit tests and basic checks."
			mvn """ \
					${commonMavenArgs} \
					-Pdist \
					-Pjqassistant -Pci-build \
					-DskipITs \
					clean install \
			"""
			dir(helper.configuration.maven.localRepositoryPath) {
				stash name:'default-build-result', includes:"org/hibernate/search/**"
				stash name:'default-build-cache', includes:".develocity/**"
			}

			if (!enableDefaultBuildIT) {
				echo "Skipping integration tests in the default environment."
				return
			}

			echo "Running integration tests in the default environment."
			// We want to include all (relevant) modules in this second build,
			// so that their coverage data is taken into account by jacoco:report-aggregate:
			// if some modules are not in the build or classes are not compiled,
			// Jacoco will just not report coverage for these classes.
			// In PRs, "relevant" modules are only those affected by changes.
			// However:
			// - We have the Develocity local build cache to avoid re-compiling/re-running unit tests.
			// - We disable a couple checks that were run above.
			String itMavenArgs = """ \
					${commonMavenArgs} \
					-Pskip-checks \
					-Pci-build \
					${incrementalBuild ? """ \
							-Dincremental \
							-Dgib.referenceBranch=refs/remotes/origin/${helper.scmSource.pullRequest.target.name} \
					""" : '' } \
			"""
			pullContainerImages( itMavenArgs )
			// Note "clean" is necessary here in order to store the result of the build in the remote build cache
			mvn """ \
					${itMavenArgs} \
					clean verify \
			"""

			stash name:'default-build-jacoco-reports', includes:"**/jacoco.exec"
		}
	}
}

stage('Non-default environments') {
	Map<String, Closure> executions = [:]

	Closure addExecution = { String tag, Closure execution ->
		executions.put( tag, {
			// This nested stage is necessary to be able to mark a single parallel execution as skipped
			// See https://stackoverflow.com/a/59210261/6692043
			stage( tag, execution )
		} )
	}

	// Test with multiple JDKs
	environments.content.jdk.enabled.each { JdkBuildEnvironment buildEnv ->
		addExecution(buildEnv.tag, {
			runBuildOnNode {
				withMavenWorkspace {
					// Re-run integration tests against the JARs produced by the default build,
					// but using a different JDK to build and run the tests.
					mavenNonDefaultBuild buildEnv, "-f integrationtest"
				}
			}
		})
	}

	// Build with different compilers
	environments.content.compiler.enabled.each { CompilerBuildEnvironment buildEnv ->
		addExecution(buildEnv.tag, {
			runBuildOnNode {
				withMavenWorkspace {
					// NOTE: we are not relying on incremental build in this case as
					// we'd better recompile everything with the same compiler rather than get some strange errors
					mavenNonDefaultBuild buildEnv, """ \
							-DskipTests -DskipITs \
							-P${buildEnv.mavenProfile},!javaModuleITs \
							-Dgib.buildAll=true \
					"""
				}
			}
		})
	}

	// Test ORM integration with multiple databases
	environments.content.database.enabled.each { DatabaseBuildEnvironment buildEnv ->
		addExecution(buildEnv.tag, {
			runBuildOnNode(NODE_PATTERN_BASE, [time: buildEnv.slow ? 2 : 1, unit: 'HOURS']) {
				withMavenWorkspace {
					def artifactsToTest = ['hibernate-search-mapper-orm']
					// Some modules are likely to fail for reasons unrelated to Hibernate Search anyway
					// (for example because we didn't configure the tests to handle other DBs),
					// so we skip them.
					String mavenBuildAdditionalArgs = ''' \
							-pl !documentation \
							-pl !integrationtest/mapper/orm-spring \
							-pl !integrationtest/v5migrationhelper/orm \
							-pl !integrationtest/java/modules/orm-lucene \
							-pl !integrationtest/java/modules/orm-elasticsearch \
							-pl !integrationtest/java/modules/orm-outbox-polling-elasticsearch \
					'''
					String mavenDockerArgs = ""
					def startedContainers = false
					tryFinally({
						mavenNonDefaultBuild buildEnv, """ \
								-Pdist \
								-P$buildEnv.mavenProfile \
								$mavenBuildAdditionalArgs \
								""",
								artifactsToTest
					}, { // Finally
						if ( startedContainers ) {
							sh "mvn docker:stop $mavenDockerArgs"
						}
					})
				}
			}
		})
	}

	// Test Elasticsearch integration with multiple versions in a local instance
	environments.content.localElasticsearch.enabled.each { LocalElasticsearchBuildEnvironment buildEnv ->
		addExecution(buildEnv.tag, {
			runBuildOnNode {
				withMavenWorkspace {
					mavenNonDefaultBuild buildEnv, """ \
							-Pdist \
							-Dtest.elasticsearch.distribution=$buildEnv.distribution \
							-Dtest.elasticsearch.version=$buildEnv.version \
							-Dtest.lucene.skip=true \
							""",
							['hibernate-search-backend-elasticsearch']
				}
			}
		})
	}

	// Test Elasticsearch integration with multiple versions in an AWS instance
	environments.content.amazonElasticsearch.enabled.each { AmazonElasticsearchServiceBuildEnvironment buildEnv ->
		if (!env.ES_AWS_REGION) {
			throw new IllegalStateException("Environment variable ES_AWS_REGION is not set")
		}
		def awsCredentialsId = null
		if (buildEnv.staticCredentials) {
			awsCredentialsId = helper.configuration.file?.aws?.credentials
			if (!awsCredentialsId) {
				throw new IllegalStateException("Missing AWS credentials")
			}
		}
		addExecution(buildEnv.tag, {
			lock(label: buildEnv.lockedResourcesLabel, variable: 'LOCKED_RESOURCE_URI') {
				runBuildOnNode(NODE_PATTERN_BASE + '&&AWS') {
					if (awsCredentialsId == null) {
						// By default, rely on credentials provided by the EC2 infrastructure

						withMavenWorkspace {
							// Tests may fail because of hourly AWS snapshots,
							// which prevent deleting indexes while they are being executed.
							// Unfortunately, this triggers test failure in @BeforeAll/@AfterAll,
							// which cannot be handled by the maven-failsafe-plugin,
							// which normally re-runs failing tests, but only if
							// the failure occurs in the @Test method.
							// So if this fails, we re-try ALL TESTS, at most twice.
							// Note that because we expect frequent failure and retries,
							// we use --fail-fast here, to make sure we don't waste time.
							retry(count: 3) {
								mavenNonDefaultBuild buildEnv, """ \
										--fail-fast \
										-pl ${[
											'org.hibernate.search:hibernate-search-integrationtest-backend-elasticsearch',
											'org.hibernate.search:hibernate-search-integrationtest-showcase-library'
											 ].join(',')} \
										-Dtest.lucene.skip=true \
										-Dtest.elasticsearch.distribution=$buildEnv.distribution \
										-Dtest.elasticsearch.version=$buildEnv.version \
										-Dtest.elasticsearch.connection.uris=$env.LOCKED_RESOURCE_URI \
										-Dtest.elasticsearch.connection.aws.signing.enabled=true \
										-Dtest.elasticsearch.connection.aws.region=$env.ES_AWS_REGION \
									"""
									// We're not using ./ci/list-dependent-integration-tests.sh here on purpose:
									// testing using AWS services is slow, and requires tweaks in test modules,
									// so we're running only a few specific test modules.
							}
						}
					}
					else {
						// For a few builds only, rely on static credentials provided by Jenkins
						// (just to check that statically-provided credentials work correctly)

						withMavenWorkspace {
							// WARNING: Make sure credentials are evaluated by sh, not Groovy.
							// To that end, escape the '$' when referencing the variables.
							// See https://www.jenkins.io/doc/book/pipeline/jenkinsfile/#string-interpolation
							withCredentials([[$class          : 'AmazonWebServicesCredentialsBinding',
											  credentialsId   : awsCredentialsId,
											  // We use non-standard env variable names because we want the credentials
											  // to be provided through configuration properties.
											  // See how ElasticsearchTestHostConnectionConfiguration does that.
											  accessKeyVariable: 'HIBERNATE_SEARCH_AWS_STATIC_CREDENTIALS_ACCESS_KEY_ID',
											  secretKeyVariable: 'HIBERNATE_SEARCH_AWS_STATIC_CREDENTIALS_SECRET_ACCESS_KEY'
											 ]]) {

								// Tests may fail because of hourly AWS snapshots,
								// which prevent deleting indexes while they are being executed.
								// Unfortunately, this triggers test failure in @BeforeAll/@AfterAll,
								// which cannot be handled by the maven-failsafe-plugin,
								// which normally re-runs failing tests, but only if
								// the failure occurs in the @Test method.
								// So if this fails, we re-try ALL TESTS, at most twice.
								// Note that because we expect frequent failure and retries,
								// we use --fail-fast here, to make sure we don't waste time.
								retry(count: 3) {
									mavenNonDefaultBuild buildEnv, """ \
										--fail-fast \
										-pl org.hibernate.search:hibernate-search-integrationtest-backend-elasticsearch,org.hibernate.search:hibernate-search-integrationtest-showcase-library \
										-Dtest.lucene.skip=true \
										-Dtest.elasticsearch.distribution=$buildEnv.distribution \
										-Dtest.elasticsearch.version=$buildEnv.version \
										-Dtest.elasticsearch.connection.uris=$env.LOCKED_RESOURCE_URI \
										-Dtest.elasticsearch.connection.aws.signing.enabled=true \
										-Dtest.elasticsearch.connection.aws.region=$env.ES_AWS_REGION \
										-Dtest.elasticsearch.connection.aws.credentials.type=static \
										"""
										// We're not using ./ci/list-dependent-integration-tests.sh here on purpose:
										// testing using AWS services is slow, and requires tweaks in test modules,
										// so we're running only a few specific test modules.
								}
							}
						}
					}
				}
			}
		})
	}

	if (executions.isEmpty()) {
		echo 'Skipping builds in non-default environments'
		helper.markStageSkipped()
	}
	else {
		parallel(executions)
	}
}

stage('Sonar analysis') {
	def sonarCredentialsId = helper.configuration.file?.sonar?.credentials
	if (sonarCredentialsId) {
		runBuildOnNode {
			withMavenWorkspace {
				dir(helper.configuration.maven.localRepositoryPath) {
					unstash name: "default-build-cache"
				}
				if (enableDefaultBuild && enableDefaultBuildIT) {
					unstash name: "default-build-jacoco-reports"
				}
				environments.content.jdk.enabled.each { JdkBuildEnvironment buildEnv ->
					unstash name: "${buildEnv.tag}-build-jacoco-reports"
				}
				environments.content.database.enabled.each { JdkBuildEnvironment buildEnv ->
					unstash name: "${buildEnv.tag}-build-jacoco-reports"
				}
				environments.content.localElasticsearch.enabled.each { JdkBuildEnvironment buildEnv ->
					unstash name: "${buildEnv.tag}-build-jacoco-reports"
				}
				environments.content.amazonElasticsearch.enabled.each { JdkBuildEnvironment buildEnv ->
					unstash name: "${buildEnv.tag}-build-jacoco-reports"
				}

				// we don't clean to keep the unstashed jacoco reports:
				sh "mvn package -Pskip-checks -Pci-build -DskipTests -Pcoverage-report ${toTestEnvironmentArgs(environments.content.jdk.default)}"


				// WARNING: Make sure credentials are evaluated by sh, not Groovy.
				// To that end, escape the '$' when referencing the variables.
				// See https://www.jenkins.io/doc/book/pipeline/jenkinsfile/#string-interpolation
				withCredentials([usernamePassword(
						credentialsId: sonarCredentialsId,
						usernameVariable: 'SONARCLOUD_ORGANIZATION',
						// https://docs.sonarsource.com/sonarqube/latest/analyzing-source-code/scanners/sonarscanner-for-maven/#analyzing
						passwordVariable: 'SONAR_TOKEN'
				)]) {
					// We don't want to use the build cache or build scans for this execution
					def miscMavenArgs = '-Dscan=false -Dno-build-cache'
					sh """ \
							mvn sonar:sonar \
							${miscMavenArgs} \
							-Dsonar.organization=\${SONARCLOUD_ORGANIZATION} \
							-Dsonar.host.url=https://sonarcloud.io \
							${helper.scmSource.pullRequest ? """ \
									-Dsonar.pullrequest.branch=${helper.scmSource.branch.name} \
									-Dsonar.pullrequest.key=${helper.scmSource.pullRequest.id} \
									-Dsonar.pullrequest.base=${helper.scmSource.pullRequest.target.name} \
									${helper.scmSource.gitHubRepoId ? """ \
											-Dsonar.pullrequest.provider=GitHub \
											-Dsonar.pullrequest.github.repository=${helper.scmSource.gitHubRepoId} \
									""" : ''} \
							""" : """ \
									-Dsonar.branch.name=${helper.scmSource.branch.name} \
							"""} \
					"""
				}
			}
		}
	} else {
		echo "Skipping Sonar report: no credentials."
	}
}

} // End of helper.runWithNotification

// Job-specific helpers

enum TestCondition {
	// For environments that are expected to work correctly
	// before merging into main or maintenance branches.
	// Tested on main and maintenance branches, on feature branches, and for PRs.
	BEFORE_MERGE,
	// For environments that are expected to work correctly,
	// but are considered too resource-intensive to test them on pull requests.
	// Tested on main and maintenance branches only.
	// Not tested on feature branches or PRs.
	AFTER_MERGE,
	// For environments that may not work correctly.
	// Only tested when explicitly requested through job parameters.
	ON_DEMAND;

	// Work around JENKINS-33023
	// See https://issues.jenkins-ci.org/browse/JENKINS-33023?focusedCommentId=325738&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-325738
	public TestCondition() {}
}

abstract class BuildEnvironment {
	boolean isDefault = false
	TestCondition condition
	String toString() { getTag() }
	abstract String getTag()
	boolean isDefault() { isDefault }
	boolean requiresDefaultBuildArtifacts() { true }
	boolean generatesCoverage() { true }
}

class JdkBuildEnvironment extends BuildEnvironment {
	String version
	String testCompilerTool
	String testLauncherTool
	String testLauncherArgs
	@Override
	String getTag() { "jdk-$version" }
}

class CompilerBuildEnvironment extends BuildEnvironment {
	String name
	String mavenProfile
	String getTag() { "compiler-$name" }
	@Override
	boolean requiresDefaultBuildArtifacts() { false }

	@Override
	boolean generatesCoverage() {
		return false
	}
}

class DatabaseBuildEnvironment extends BuildEnvironment {
	String dbName
	String mavenProfile
	boolean slow
	@Override
	String getTag() { "database-$dbName" }
}

class LocalElasticsearchBuildEnvironment extends BuildEnvironment {
	String version
	String getTagPrefix() { 'elasticsearch-local' }
	@Override
	String getTag() { tagPrefix + (version ? '-' + version : '') }
	String getDistribution() { 'elastic' }
}

class LocalOpenSearchBuildEnvironment extends LocalElasticsearchBuildEnvironment {
	String version
	@Override
	String getTagPrefix() { 'opensearch-local' }
	@Override
	String getDistribution() { 'opensearch' }
}

class AmazonOpenSearchServerlessLocalBuildEnvironment
		extends LocalOpenSearchBuildEnvironment {
	{
		setVersion('')
	}
	@Override
	String getTagPrefix() { 'amazon-opensearch-serverless' }
	@Override
	String getDistribution() { 'amazon-opensearch-serverless' }
}

class AmazonElasticsearchServiceBuildEnvironment extends LocalElasticsearchBuildEnvironment {
	boolean staticCredentials = false
	@Override
	String getTagPrefix() { 'amazon-elasticsearch-service' }
	@Override
	String getTag() {
		tagPrefix + (version ? '-' + version : '') + (staticCredentials ? '-credentials-static' : '')
	}
	String getLockedResourcesPrefix() { 'es' }
	String getLockedResourcesLabel() {
		"$lockedResourcesPrefix-aws-${version.replaceAll('\\.', '-')}"
	}
}

class AmazonOpenSearchServiceBuildEnvironment extends AmazonElasticsearchServiceBuildEnvironment {
	@Override
	String getTagPrefix() { 'amazon-opensearch-service' }
	@Override
	String getDistribution() { 'opensearch' }
	@Override
	String getLockedResourcesPrefix() { 'opensearch' }
}

void keepOnlyEnvironmentsMatchingFilter(String regex) {
	def pattern = /$regex/

	boolean enableDefault = ('default' =~ pattern)

	environments.content.each { key, envSet ->
		envSet.enabled.removeAll { buildEnv ->
			!(buildEnv.tag =~ pattern) && !(envSet.default == buildEnv && enableDefault)
		}
	}
}

void keepOnlyEnvironmentsFromSet(String environmentSetName) {
	boolean enableDefaultEnv = false
	boolean enableBeforeMergeEnvs = false
	boolean enableAfterMergeEnvs = false
	boolean enableOnDemandEnvs = false
	switch (environmentSetName) {
		case 'DEFAULT':
			enableDefaultEnv = true
			break
		case 'SUPPORTED':
			enableDefaultEnv = true
			enableBeforeMergeEnvs = true
			enableAfterMergeEnvs = true
			break
		case 'ALL':
			enableDefaultEnv = true
			enableBeforeMergeEnvs = true
			enableAfterMergeEnvs = true
			enableOnDemandEnvs = true
			break
		case 'AUTOMATIC':
			if (helper.scmSource.pullRequest) {
				echo "Building pull request '$helper.scmSource.pullRequest.id'"
				enableDefaultEnv = true
				enableBeforeMergeEnvs = true
			} else if (helper.scmSource.branch.primary) {
				echo "Building primary branch '$helper.scmSource.branch.name'"
				enableDefaultEnv = true
				enableBeforeMergeEnvs = true
				enableAfterMergeEnvs = true
			} else {
				echo "Building feature branch '$helper.scmSource.branch.name'"
				enableDefaultEnv = true
			}
			break
		default:
			throw new IllegalArgumentException(
					"Unknown value for param 'ENVIRONMENT_SET': '$environmentSetName'."
			)
	}

	// Filter environments

	environments.content.each { key, envSet ->
		envSet.enabled.removeAll { buildEnv -> ! (
				enableDefaultEnv && buildEnv.isDefault ||
				enableBeforeMergeEnvs && buildEnv.condition == TestCondition.BEFORE_MERGE ||
				enableAfterMergeEnvs && buildEnv.condition == TestCondition.AFTER_MERGE ||
						enableOnDemandEnvs && buildEnv.condition == TestCondition.ON_DEMAND ) }
	}
}

void runBuildOnNode(Closure body) {
	runBuildOnNode( NODE_PATTERN_BASE, body )
}

void runBuildOnNode(String label, Closure body) {
	runBuildOnNode( label, [time: 1, unit: 'HOURS'], body )
}

void runBuildOnNode(String label, def timeoutValue, Closure body) {
	node( label ) {
		timeout( timeoutValue, body )
	}
}

void withMavenWorkspace(Closure body) {
	withMavenWorkspace([:], body)
}

void withMavenWorkspace(Map args, Closure body) {
	args.put("options", [
			// Artifacts are not needed and take up disk space
			artifactsPublisher(disabled: true),
			// stdout/stderr for successful tests is not needed and takes up disk space
			// we archive test results and stdout/stderr as part of the build scan anyway,
			// see https://develocity.commonhaus.dev/scans?search.rootProjectNames=Hibernate%20Search
			junitPublisher(disabled: true)
	])
	helper.withMavenWorkspace(args, {
		// The script is in the code repository, so we need the scm checkout
		// to be performed by helper.withMavenWorkspace before we can call the script.
		sh 'ci/docker-cleanup.sh'
		tryFinally(body, { // Finally
			sh 'ci/docker-cleanup.sh'
		})
	})
}

void mvn(String args) {
	def develocityMainCredentialsId = helper.configuration.file?.develocity?.credentials?.main
	def develocityPrCredentialsId = helper.configuration.file?.develocity?.credentials?.pr
	def develocityBaseUrl = helper.configuration.file?.develocity?.url
	if ( !helper.scmSource.pullRequest && develocityMainCredentialsId ) {
		// Not a PR: we can pass credentials to the build, allowing it to populate the build cache
		// and to publish build scans directly.
		withEnv(["DEVELOCITY_BASE_URL=${develocityBaseUrl}"]) {
			withCredentials([string(credentialsId: develocityMainCredentialsId,
					variable: 'DEVELOCITY_ACCESS_KEY')]) {
				withGradle { // withDevelocity, actually: https://plugins.jenkins.io/gradle/#plugin-content-capturing-build-scans-from-jenkins-pipeline
					sh "mvn $args"
				}
			}
		}
	}
	else if ( helper.scmSource.pullRequest && develocityPrCredentialsId ) {
		// Pull request: we can't pass credentials to the build, since we'd be exposing secrets to e.g. tests.
		// We do the build first, then publish the build scan separately.
		tryFinally({
			sh "mvn $args"
		}, { // Finally
			withEnv(["DEVELOCITY_BASE_URL=${develocityBaseUrl}"]) {
				withCredentials([string(credentialsId: develocityPrCredentialsId,
						variable: 'DEVELOCITY_ACCESS_KEY')]) {
					withGradle { // withDevelocity, actually: https://plugins.jenkins.io/gradle/#plugin-content-capturing-build-scans-from-jenkins-pipeline
						sh 'mvn develocity:build-scan-publish-previous || true'
					}
				}
			}
		})
	}
	else {
		// No Develocity credentials.
		sh "mvn $args"
	}
}

// Perform authenticated pulls of container images, to avoid failure due to download throttling on dockerhub.
def pullContainerImages(String mavenArgs) {
	String containerImageRefsString = ((String) sh( script: "./ci/list-container-images.sh ${mavenArgs}", returnStdout: true ) )
	String[] containerImageRefs = containerImageRefsString ? containerImageRefsString.split( '\\s+' ) : new String[0]
	echo 'Container images to be used in tests: ' + Arrays.toString( containerImageRefs )
	if ( containerImageRefs.length == 0 ) {
		return
	}
	// Cannot use a foreach loop because then Jenkins wants to serialize the iterator,
	// and obviously the iterator is not serializable.
	for (int i = 0; i < containerImageRefs.length; i++) {
		containerImageRef = containerImageRefs[i]
		sh "docker pull ${containerImageRef}"
	}
}

void mavenNonDefaultBuild(BuildEnvironment buildEnv, String args, List<String> artifactsToTest = []) {
	if ( buildEnv.requiresDefaultBuildArtifacts() ) {
		dir(helper.configuration.maven.localRepositoryPath) {
			unstash name:'default-build-result'
		}
	}
	dir(helper.configuration.maven.localRepositoryPath) {
		unstash name: "default-build-cache"
	}

	// We want to run relevant integration test modules only (see array of module names)
	// and in PRs we want to run only those affected by changes
	// (see gib.disableSelectedProjectsHandling=true).
	String incrementalProjectsListFile = 'target/.gib-impacted'
	args = """ \
		${incrementalBuild ? """ \
				-Dincremental -Dgib.disableSelectedProjectsHandling=true \
				-Dgib.referenceBranch=refs/remotes/origin/${helper.scmSource.pullRequest.target.name} \
				-Dgib.logImpactedTo='${incrementalProjectsListFile}' \
		""" : ''} \
		${args} \
	"""
	if ( buildEnv.requiresDefaultBuildArtifacts() ) {
		// - We disable a couple checks that were run above.
		// - We activate a profile that fixes the second Maven execution for some modules
		//   by disabling a few misbehaving plugins whose output is already there anyway.
		args += ' -Pskip-checks'
	}
	if ( artifactsToTest ) {
		args += ' -pl ' + sh(script: "./ci/list-dependent-integration-tests.sh ${artifactsToTest.join(',')}", returnStdout: true).trim()
	}
	if ( buildEnv.generatesCoverage() ) {
		args += ' -Pcoverage'
	}

	pullContainerImages( args )

	// Note "clean" is necessary here in order to store the result of the build in the remote build cache
	mvn """ \
			clean install -Pci-build \
					${toTestEnvironmentArgs(buildEnv)} \
					--fail-at-end \
					$args \
	"""

	if ( buildEnv.generatesCoverage() ) {
		// We allow an empty stash here since it can happen that a PR build is triggered
		// but because of incremental build there will be no tests executed and no jacoco files generated:
		stash name: "${buildEnv.tag}-build-jacoco-reports", includes:"**/jacoco.exec", allowEmpty: true
	}

	// In incremental builds, the Maven execution above
	// created a file listing projects relevant to the incremental build.
	// If it is empty, it means the incremental build didn't actually do anything,
	// so make sure to mark the stage as skipped.
	if (incrementalBuild && 0 == sh(script: "test ! -s '${incrementalProjectsListFile}'", returnStatus: true)) {
		echo 'Skipping stage because PR changes do not affect tested modules'
		helper.markStageSkipped()
	}
}

String toTestEnvironmentArgs(BuildEnvironment buildEnv) {
	String args = ''

	// Enable exact matching of the JDK version for Develocity build caches.
	// This is enabled on this build only to avoid problems with
	// Other builds (local, GitHub actions) do not enable this and thus benefit
	// from more lenient cache hits.
	// The behavior is implemented in our Maven extension, see:
	// https://github.com/hibernate/hibernate-search-develocity-extension
	args +=  " -Dbuild-cache.java-version.exact"

	// https://docs.gradle.com/develocity/maven-extension/current/#changing_the_local_cache_directory
	// The local Build Cache is located at ${user.home}/.m2/.develocity/build-cache by default
	args += " -Ddevelocity.cache.local.directory=${helper.configuration.maven.localRepositoryPath}/.develocity/build-cache"

	// Add a suffix to tests to distinguish between different executions
	// of the same test in different environments in reports
	def testSuffix = buildEnv.tag.replaceAll('[^a-zA-Z0-9_\\-+]+', '_')
	args +=  " -Dsurefire.environment=$testSuffix"

	if ( ! (buildEnv instanceof JdkBuildEnvironment) ) {
		return args;
	}

	String testCompilerTool = buildEnv.testCompilerTool
	if ( testCompilerTool && DEFAULT_JDK_TOOL != testCompilerTool ) {
		def testCompilerToolPath = tool(name: testCompilerTool, type: 'jdk')
		args += " -Djava-version.test.compiler.java_home=$testCompilerToolPath"
	}
	// Note: the POM uses the java_home of the test compiler for the test launcher by default.
	String testLauncherTool = buildEnv.testLauncherTool
	if ( testLauncherTool && DEFAULT_JDK_TOOL != testLauncherTool ) {
		def testLauncherToolPath = tool(name: testLauncherTool, type: 'jdk')
		args += " -Djava-version.test.launcher.java_home=$testLauncherToolPath"
	}
	String defaultVersion = environments.content.jdk.default.version
	String version = buildEnv.version
	if ( defaultVersion != version ) {
		args += " -Djava-version.test.release=$version"
	}

	if ( buildEnv.testLauncherArgs ) {
		args += " -Dtest.launcher.args=${buildEnv.testLauncherArgs}"
	}

	return args
}

// try-finally construct that properly suppresses exceptions thrown in the finally block.
def tryFinally(Closure main, Closure ... finallies) {
	def mainFailure = null
	try {
		main()
	}
	catch (Throwable t) {
		mainFailure = t
		throw t
	}
	finally {
		finallies.each {it ->
			try {
				it()
			}
			catch (Throwable t) {
				if ( mainFailure ) {
					mainFailure.addSuppressed( t )
				}
				else {
					mainFailure = t
				}
			}
		}
	}
	if ( mainFailure ) { // We may reach here if only the "finally" failed
		throw mainFailure
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

import groovy.transform.Field

/*
 * See https://github.com/hibernate/hibernate-jenkins-pipeline-helpers
 */
@Library('hibernate-jenkins-pipeline-helpers@1.4')
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
 * #### Nexus deployment
 *
 * This job is only able to deploy snapshot artifacts,
 * for every non-PR build on "primary" branches (main and maintenance branches),
 * but the name of a Maven settings file must be provided in the job configuration file
 * (see below).
 *
 * For actual releases, see jenkins/release.groovy.
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
 *     deployment:
 *       maven:
 *         # String containing the ID of a Maven settings file registered using the config-file-provider Jenkins plugin.
 *         # The settings must provide credentials to the server with ID 'ossrh'.
 *         settingsId: ...
 */

@Field final String DEFAULT_JDK_TOOL = 'OpenJDK 17 Latest'
@Field final String MAVEN_TOOL = 'Apache Maven 3.8'

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
@Field boolean deploySnapshot = false

this.helper = new JobHelper(this)

helper.runWithNotification {

stage('Configure') {
	this.environments = AlternativeMultiMap.create([
			jdk: [
					// This should not include every JDK; in particular let's not care too much about EOL'd JDKs like version 9
					// See http://www.oracle.com/technetwork/java/javase/eol-135779.html
					// We only run the tests with JDK8, but we compile them with JDK17 (with --release 8).
					new JdkBuildEnvironment(version: '8', testLauncherTool: 'OpenJDK 8 Latest',
							condition: TestCondition.AFTER_MERGE),
					new JdkBuildEnvironment(version: '11', testCompilerTool: 'OpenJDK 11 Latest',
							condition: TestCondition.AFTER_MERGE),
					new JdkBuildEnvironment(version: '17', testCompilerTool: 'OpenJDK 17 Latest',
							condition: TestCondition.BEFORE_MERGE,
							isDefault: true),
					new JdkBuildEnvironment(version: '18', testCompilerTool: 'OpenJDK 18 Latest',
							condition: TestCondition.AFTER_MERGE),
					// We want to enable preview features when testing early-access builds of OpenJDK:
					// even if we don't use these features, just enabling them can cause side effects
					// and it's useful to test that.
					new JdkBuildEnvironment(version: '19', testCompilerTool: 'OpenJDK 19 Latest',
							testLauncherArgs: '--enable-preview',
							condition: TestCondition.AFTER_MERGE),
					new JdkBuildEnvironment(version: '20', testCompilerTool: 'OpenJDK 20 Latest',
							testLauncherArgs: '--enable-preview',
							condition: TestCondition.AFTER_MERGE)
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
                            condition: TestCondition.AFTER_MERGE),
                    new DatabaseBuildEnvironment(dbName: 'mssql', mavenProfile: 'ci-mssql',
                            condition: TestCondition.AFTER_MERGE),
                    new DatabaseBuildEnvironment(dbName: 'cockroachdb', mavenProfile: 'ci-cockroachdb',
                            condition: TestCondition.AFTER_MERGE),
			],
			esLocal: [
					// --------------------------------------------
					// Elasticsearch distribution from Elastic
					new EsLocalBuildEnvironment(version: '5.6.16', condition: TestCondition.AFTER_MERGE),
					// ES 6.2, 6.3.0, 6.3.1 and 6.3.2 and below have a bug that prevents double-nested
					// sorts from working: https://github.com/elastic/elasticsearch/issues/32130
					new EsLocalBuildEnvironment(version: '6.2.4', condition: TestCondition.ON_DEMAND),
					// ES 6.3 has a bug that prevents IndexingIT from passing.
					// See https://github.com/elastic/elasticsearch/issues/32395
					new EsLocalBuildEnvironment(version: '6.3.2', condition: TestCondition.ON_DEMAND),
					new EsLocalBuildEnvironment(version: '6.6.2', condition: TestCondition.AFTER_MERGE),
					// Not testing 6.7 to make the build quicker.
					// The only difference with 6.8+ is a bug in field sorts that is already present in earlier versions.
					new EsLocalBuildEnvironment(version: '6.7.2', condition: TestCondition.ON_DEMAND),
					new EsLocalBuildEnvironment(version: '6.8.22', condition: TestCondition.AFTER_MERGE),
					// Not testing 7.0/7.1/7.2 to make the build quicker.
					// The only difference with 7.3+ is they have a bug in their BigInteger support.
					new EsLocalBuildEnvironment(version: '7.2.1', condition: TestCondition.ON_DEMAND),
					new EsLocalBuildEnvironment(version: '7.6.2', condition: TestCondition.AFTER_MERGE),
					// Not testing 7.7 to make the build quicker.
					// The only difference with 7.7+ is how we create templates for tests.
					new EsLocalBuildEnvironment(version: '7.7.1', condition: TestCondition.ON_DEMAND),
					// Not testing 7.9 to make the build quicker.
					// The only difference with 7.10+ is an additional test for exists on null values,
					// which is disabled on 7.10 but enabled on all older versions (not just 7.9).
					new EsLocalBuildEnvironment(version: '7.9.3', condition: TestCondition.AFTER_MERGE),
					new EsLocalBuildEnvironment(version: '7.10.1', condition: TestCondition.AFTER_MERGE),
					// Not testing 7.11 to make the build quicker.
					// The only difference with 7.12+ is that wildcard predicates on analyzed fields get their pattern normalized,
					// and that was deemed a bug: https://github.com/elastic/elasticsearch/pull/53127
					new EsLocalBuildEnvironment(version: '7.11.2', condition: TestCondition.ON_DEMAND),
					new EsLocalBuildEnvironment(version: '7.12.1', condition: TestCondition.ON_DEMAND),
					new EsLocalBuildEnvironment(version: '7.13.2', condition: TestCondition.ON_DEMAND),
					// 7.14 and 7.15 have annoying bugs that make almost all of our test suite fail,
					// so we don't test them
					// See https://hibernate.atlassian.net/browse/HSEARCH-4340
					new EsLocalBuildEnvironment(version: '7.16.3', condition: TestCondition.ON_DEMAND),
					new EsLocalBuildEnvironment(version: '7.17.0', condition: TestCondition.AFTER_MERGE),
					// Not testing 8.0 because we know there are problems in 8.0.1 (see https://hibernate.atlassian.net/browse/HSEARCH-4497)
					// Not testing 8.1-8.4 to make the build quicker.
					new EsLocalBuildEnvironment(version: '8.1.3', condition: TestCondition.ON_DEMAND),
					new EsLocalBuildEnvironment(version: '8.2.3', condition: TestCondition.ON_DEMAND),
					new EsLocalBuildEnvironment(version: '8.3.3', condition: TestCondition.ON_DEMAND),
					new EsLocalBuildEnvironment(version: '8.4.3', condition: TestCondition.ON_DEMAND),
					new EsLocalBuildEnvironment(version: '8.5.3', condition: TestCondition.BEFORE_MERGE, isDefault: true),

					// --------------------------------------------
					// OpenSearch
					// Not testing 1.0 - 1.2 to make the build quicker.
					new OpenSearchLocalBuildEnvironment(version: '1.3.7', condition: TestCondition.AFTER_MERGE),
					// See https://opensearch.org/lines/1x.html for a list of all 1.x versions
					new OpenSearchLocalBuildEnvironment(version: '2.0.1', condition: TestCondition.ON_DEMAND),
					new OpenSearchLocalBuildEnvironment(version: '2.1.0', condition: TestCondition.ON_DEMAND),
					new OpenSearchLocalBuildEnvironment(version: '2.2.1', condition: TestCondition.ON_DEMAND),
					new OpenSearchLocalBuildEnvironment(version: '2.3.0', condition: TestCondition.ON_DEMAND),
					new OpenSearchLocalBuildEnvironment(version: '2.4.1', condition: TestCondition.AFTER_MERGE)
					// See https://opensearch.org/lines/2x.html for a list of all 2.x versions
			],
			esAws: [
					// --------------------------------------------
					// AWS Elasticsearch service (OpenDistro)
					new EsAwsBuildEnvironment(version: '5.6', condition: TestCondition.ON_DEMAND),
					new EsAwsBuildEnvironment(version: '6.0', condition: TestCondition.ON_DEMAND),
					// ES 6.2, 6.3.0, 6.3.1 and 6.3.2 and below have a bug that prevents double-nested
					// sorts from working: https://github.com/elastic/elasticsearch/issues/32130
					new EsAwsBuildEnvironment(version: '6.2', condition: TestCondition.ON_DEMAND),
					// ES 6.3 has a bug that prevents IndexingIT from passing.
					// See https://github.com/elastic/elasticsearch/issues/32395
					new EsAwsBuildEnvironment(version: '6.3', condition: TestCondition.ON_DEMAND),
					new EsAwsBuildEnvironment(version: '6.4', condition: TestCondition.ON_DEMAND),
					new EsAwsBuildEnvironment(version: '6.5', condition: TestCondition.ON_DEMAND),
					new EsAwsBuildEnvironment(version: '6.7', condition: TestCondition.ON_DEMAND),
					new EsAwsBuildEnvironment(version: '6.8', condition: TestCondition.ON_DEMAND),
					new EsAwsBuildEnvironment(version: '7.1', condition: TestCondition.ON_DEMAND),
					new EsAwsBuildEnvironment(version: '7.4', condition: TestCondition.ON_DEMAND),
					new EsAwsBuildEnvironment(version: '7.7', condition: TestCondition.ON_DEMAND),
					new EsAwsBuildEnvironment(version: '7.8', condition: TestCondition.ON_DEMAND),
					new EsAwsBuildEnvironment(version: '7.10', condition: TestCondition.AFTER_MERGE),

					// --------------------------------------------
					// AWS OpenSearch service
					new OpenSearchAwsBuildEnvironment(version: '1.3', condition: TestCondition.AFTER_MERGE),
					// Also test static credentials, but only for the latest version
					new OpenSearchAwsBuildEnvironment(version: '1.3', condition: TestCondition.AFTER_MERGE, staticCredentials: true)
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

	if (helper.scmSource.branch.primary && !helper.scmSource.pullRequest) {
		if (helper.configuration.file?.deployment?.maven?.settingsId) {
			deploySnapshot = true
		}
		else {
			echo "Missing deployment configuration in job configuration file - snapshot deployment will be skipped."
		}
	}

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
			environments.content.any { key, envSet -> envSet.enabled.any { buildEnv -> buildEnv.requiresDefaultBuildArtifacts() } } ||
			deploySnapshot

	echo """Branch: ${helper.scmSource.branch.name}
PR: ${helper.scmSource.pullRequest?.id}
params.ENVIRONMENT_SET: ${params.ENVIRONMENT_SET}
params.ENVIRONMENT_FILTER: ${params.ENVIRONMENT_FILTER}

Resulting execution plan:
    enableDefaultBuild=$enableDefaultBuild
    enableDefaultBuildIT=$enableDefaultBuildIT
    environments=${environments.enabledAsString}
    deploySnapshot=$deploySnapshot
"""
}

stage('Default build') {
	if (!enableDefaultBuild) {
		echo 'Skipping default build and integration tests in the default environment'
		helper.markStageSkipped()
		return
	}
	runBuildOnNode( NODE_PATTERN_BASE, [time: 2, unit: 'HOURS'] ) {
		withMavenWorkspace(mavenSettingsConfig: deploySnapshot ? helper.configuration.file.deployment.maven.settingsId : null) {
			String mavenArgs = """ \
					--fail-at-end \
					-Pdist -Pcoverage -Pjqassistant \
					${enableDefaultBuildIT ? '' : '-DskipITs'} \
					${toTestJdkArg(environments.content.jdk.default)} \
			"""
			pullContainerImages( mavenArgs )
			sh """ \
					mvn clean \
					${deploySnapshot ? "\
							deploy \
					" : "\
							install \
					"} \
					$mavenArgs \
			"""

			// Don't try to report to SonarCloud if coverage data is missing
			if (enableDefaultBuildIT) {
				if (helper.configuration.file?.sonar?.credentials) {
					def sonarCredentialsId = helper.configuration.file.sonar.credentials
					// WARNING: Make sure credentials are evaluated by sh, not Groovy.
					// To that end, escape the '$' when referencing the variables.
					// See https://www.jenkins.io/doc/book/pipeline/jenkinsfile/#string-interpolation
					withCredentials([usernamePassword(
									credentialsId: sonarCredentialsId,
									usernameVariable: 'SONARCLOUD_ORGANIZATION',
									passwordVariable: 'SONARCLOUD_TOKEN'
					)]) {
						sh """ \
								mvn sonar:sonar \
								-Dsonar.organization=\${SONARCLOUD_ORGANIZATION} \
								-Dsonar.host.url=https://sonarcloud.io \
								-Dsonar.login=\${SONARCLOUD_TOKEN} \
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
				else {
					echo "No Sonar organization configured - skipping Sonar report."
				}
			}

			dir(helper.configuration.maven.localRepositoryPath) {
				stash name:'default-build-result', includes:"org/hibernate/search/**"
			}
		}
	}
}

stage('Non-default environments') {
	Map<String, Closure> executions = [:]

	// Test with multiple JDKs
	environments.content.jdk.enabled.each { JdkBuildEnvironment buildEnv ->
		executions.put(buildEnv.tag, {
			runBuildOnNode {
				withMavenWorkspace {
					// Re-run integration tests against the JARs produced by the default build,
					// but using a different JDK to build and run the tests.
					mavenNonDefaultBuild buildEnv, "", 'integrationtest'
				}
			}
		})
	}

	// Build with different compilers
	environments.content.compiler.enabled.each { CompilerBuildEnvironment buildEnv ->
		executions.put(buildEnv.tag, {
			runBuildOnNode {
				withMavenWorkspace {
					mavenNonDefaultBuild buildEnv, """ \
							-DskipTests -DskipITs \
							-P${buildEnv.mavenProfile},!javaModuleITs \
					"""
				}
			}
		})
	}

	// Test ORM integration with multiple databases
	environments.content.database.enabled.each { DatabaseBuildEnvironment buildEnv ->
		executions.put(buildEnv.tag, {
			// Some databases, e.g. DB2 or CockroachDB, can be really slow, so we need to raise the timeout.
			runBuildOnNode(NODE_PATTERN_BASE, [time: 2, unit: 'HOURS']) {
				withMavenWorkspace {
					String mavenBuildAdditionalArgs = ""
					String mavenDockerArgs = ""
					def startedContainers = false
					// DB2 setup is super slow (~5 to 15 minutes).
					// We can't afford to do that once per module,
					// so we start DB2 here, once and for all.
					if ( buildEnv.dbName == 'db2' ) {
						// Prevent the actual build from starting the DB container
						mavenBuildAdditionalArgs += " -Dtest.database.run.db2.skip=true"
						// Pick a module that doesn't normally execute the docker-maven-plugin,
						// but that has the configuration necessary to start the DB container:
						// that way, the maven-docker-plugin won't try to remove our container
						// when we execute maven another time to run the tests.
						// (This works because maven-docker-plugin filters containers to stop
						// based on the Maven GAV coordinates of the Maven project that started that container,
						// which are attached to the container thanks to a container label).
						mavenDockerArgs = """ \
								-pl build/parents/integrationtest \
								-P$buildEnv.mavenProfile \
								-Dtest.database.run.db2.skip=false \
						"""
						// Cleanup just in case some containers were left over from a previous build.
						sh "mvn docker:stop $mavenDockerArgs"
						pullContainerImages mavenDockerArgs
						sh "mvn docker:start $mavenDockerArgs"
						startedContainers = true
					}
					try {
						mavenNonDefaultBuild buildEnv, """ \
								-pl ${[
									'org.hibernate.search:hibernate-search-integrationtest-mapper-orm',
									'org.hibernate.search:hibernate-search-integrationtest-mapper-orm-coordination-outbox-polling',
									'org.hibernate.search:hibernate-search-integrationtest-mapper-orm-envers',
									'org.hibernate.search:hibernate-search-integrationtest-showcase-library',
									'org.hibernate.search:hibernate-search-integrationtest-mapper-orm-realbackend'
									 ].join(',')} \
								-P$buildEnv.mavenProfile \
								$mavenBuildAdditionalArgs \
						"""
					}
					finally {
						if ( startedContainers ) {
							sh "mvn docker:stop $mavenDockerArgs"
						}
					}
				}
			}
		})
	}

	// Test Elasticsearch integration with multiple versions in a local instance
	environments.content.esLocal.enabled.each { EsLocalBuildEnvironment buildEnv ->
		executions.put(buildEnv.tag, {
			runBuildOnNode {
				withMavenWorkspace {
					mavenNonDefaultBuild buildEnv, """ \
							-Dtest.elasticsearch.distribution=$buildEnv.distribution \
							-Dtest.elasticsearch.version=$buildEnv.version \
							-pl ${[
								'org.hibernate.search:hibernate-search-integrationtest-backend-elasticsearch',
								'org.hibernate.search:hibernate-search-integrationtest-showcase-library',
								'org.hibernate.search:hibernate-search-integrationtest-mapper-orm-realbackend'
								 ].join(',')} \
					"""
				}
			}
		})
	}

	// Test Elasticsearch integration with multiple versions in an AWS instance
	environments.content.esAws.enabled.each { EsAwsBuildEnvironment buildEnv ->
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
		executions.put(buildEnv.tag, {
			lock(label: buildEnv.lockedResourcesLabel, variable: 'LOCKED_RESOURCE_URI') {
				runBuildOnNode(NODE_PATTERN_BASE + '&&AWS') {
					if (awsCredentialsId == null) {
						// By default, rely on credentials provided by the EC2 infrastructure

						withMavenWorkspace {
							// Tests may fail because of hourly AWS snapshots,
							// which prevent deleting indexes while they are being executed.
							// Unfortunately, this triggers test failure in @BeforeClass/@AfterClass,
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
										-Dtest.elasticsearch.distribution=$buildEnv.distribution \
										-Dtest.elasticsearch.version=$buildEnv.version \
										-Dtest.elasticsearch.connection.uris=$env.LOCKED_RESOURCE_URI \
										-Dtest.elasticsearch.connection.aws.signing.enabled=true \
										-Dtest.elasticsearch.connection.aws.region=$env.ES_AWS_REGION \
									"""
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
											  usernameVariable: 'AWS_ACCESS_KEY_ID',
											  passwordVariable: 'AWS_SECRET_ACCESS_KEY'
											 ]]) {

								// Tests may fail because of hourly AWS snapshots,
								// which prevent deleting indexes while they are being executed.
								// Unfortunately, this triggers test failure in @BeforeClass/@AfterClass,
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
										-Dtest.elasticsearch.distribution=$buildEnv.distribution \
										-Dtest.elasticsearch.version=$buildEnv.version \
										-Dtest.elasticsearch.connection.uris=$env.LOCKED_RESOURCE_URI \
										-Dtest.elasticsearch.connection.aws.signing.enabled=true \
										-Dtest.elasticsearch.connection.aws.region=$env.ES_AWS_REGION \
										-Dtest.elasticsearch.connection.aws.credentials.type=static \
										-Dtest.elasticsearch.connection.aws.credentials.access_key_id=\${AWS_ACCESS_KEY_ID} \
										-Dtest.elasticsearch.connection.aws.credentials.secret_access_key=\${AWS_SECRET_ACCESS_KEY} \
									"""
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
}

class DatabaseBuildEnvironment extends BuildEnvironment {
	String dbName
	String mavenProfile
	@Override
	String getTag() { "database-$dbName" }
}

class EsLocalBuildEnvironment extends BuildEnvironment {
	String version
	@Override
    String getTag() { "elasticsearch-local-$version" }
    String getDistribution() { "elastic" }
}

class OpenSearchLocalBuildEnvironment extends EsLocalBuildEnvironment {
	String version
	@Override
	String getTag() { "opensearch-local-$version" }
	@Override
	String getDistribution() { "opensearch" }
}

class EsAwsBuildEnvironment extends EsLocalBuildEnvironment {
	boolean staticCredentials = false
	@Override
	String getTag() { "elasticsearch-aws-$version" + (staticCredentials ? "-credentials-static" : "") }
	String getNameEmbeddableVersion() {
		version.replaceAll('\\.', '-')
	}
	String getLockedResourcesLabel() {
		"es-aws-${nameEmbeddableVersion}"
	}
}

class OpenSearchAwsBuildEnvironment extends EsAwsBuildEnvironment {
	@Override
	String getTag() { "opensearch-aws-$version" + (staticCredentials ? "-credentials-static" : "") }
	@Override
	String getDistribution() { "opensearch" }
	@Override
	String getLockedResourcesLabel() {
		"opensearch-aws-${nameEmbeddableVersion}"
	}
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
			enableOptional = true
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
	helper.withMavenWorkspace(args, {
		// The script is in the code repository, so we need the scm checkout
		// to be performed by helper.withMavenWorkspace before we can call the script.
		sh 'ci/docker-cleanup.sh'
		try {
			body()
		}
		finally {
			sh 'ci/docker-cleanup.sh'
		}
	})
}

// Perform authenticated pulls of container images, to avoid failure due to download throttling on dockerhub.
def pullContainerImages(String mavenArgs) {
	String containerImageRefsString = ((String) sh( script: "./ci/list-container-images.sh ${mavenArgs}", returnStdout: true ) )
	String[] containerImageRefs = containerImageRefsString ? containerImageRefsString.split( '\\s+' ) : new String[0]
	echo 'Container images to be used in tests: ' + Arrays.toString( containerImageRefs )
	if ( containerImageRefs.length == 0 ) {
		return
	}
	docker.withRegistry('https://index.docker.io/v1/', 'hibernateci.hub.docker.com') {
		// Cannot use a foreach loop because then Jenkins wants to serialize the iterator,
		// and obviously the iterator is not serializable.
		for (int i = 0; i < containerImageRefs.length; i++) {
			containerImageRef = containerImageRefs[i]
			docker.image( containerImageRef ).pull()
		}
	}
}

void mavenNonDefaultBuild(BuildEnvironment buildEnv, String args, String projectPath = '.') {
	if ( buildEnv.requiresDefaultBuildArtifacts() ) {
		dir(helper.configuration.maven.localRepositoryPath) {
			unstash name:'default-build-result'
		}
	}

	pullContainerImages( args )

	// Add a suffix to tests to distinguish between different executions
	// of the same test in different environments in reports
	def testSuffix = buildEnv.tag.replaceAll('[^a-zA-Z0-9_\\-+]+', '_')

	dir(projectPath) {
		sh """ \
				mvn clean install -Dsurefire.environment=$testSuffix \
						${toTestJdkArg(buildEnv)} \
						--fail-at-end \
						$args \
		"""
	}
}

String toTestJdkArg(BuildEnvironment buildEnv) {
	String args = ''

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
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
@Library('hibernate-jenkins-pipeline-helpers@1.3')
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
 * them in the GUI: see https://ci.hibernate.org/job/hibernate-search-6-poc/configure, "Pipeline Libraries".
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
 * #### Coveralls (optional)
 *
 * You need to enable your repository in Coveralls first: see https://coveralls.io/repos/new.
 *
 * Then you will also need to add the Coveralls repository token as credentials in Jenkins
 * and reference it from the configuration file (see below).
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
 *     coveralls:
 *       # String containing the ID of coveralls credentials. Optional.
 *       # Expects secret text credentials containing the repository token.
 *       # Note these credentials should be registered at the job level, not system-wide.
 *       credentials: ...
 */

@Field final String MAVEN_TOOL = 'Apache Maven 3.5'

// Default node pattern, to be used for resource-intensive stages.
// Should not include the controller node.
@Field final String NODE_PATTERN_BASE = 'Worker'
// Quick-use node pattern, to be used for very light, quick, and environment-independent stages,
// such as sending a notification. May include the controller node in particular.
@Field final String QUICK_USE_NODE_PATTERN = 'Controller||Worker'

@Field AlternativeMultiMap<BuildEnvironment> environments
@Field JobHelper helper

@Field boolean enableDefaultBuild = false
@Field boolean enableDefaultBuildIT = false

this.helper = new JobHelper(this)

helper.runWithNotification {

stage('Configure') {
	this.environments = AlternativeMultiMap.create([
			jdk: [
					// This should not include every JDK; in particular let's not care too much about EOL'd JDKs like version 9
					// See http://www.oracle.com/technetwork/java/javase/eol-135779.html
					new JdkBuildEnvironment(version: '8', tool: 'OpenJDK 8 Latest',
							condition: TestCondition.BEFORE_MERGE,
							isDefault: true),
					new JdkBuildEnvironment(version: '11', tool: 'OpenJDK 11 Latest',
							condition: TestCondition.AFTER_MERGE),
					new JdkBuildEnvironment(version: '13', tool: 'OpenJDK 13 Latest',
							condition: TestCondition.ON_DEMAND),
					new JdkBuildEnvironment(version: '14', tool: 'OpenJDK 14 Latest',
							condition: TestCondition.ON_DEMAND),
					new JdkBuildEnvironment(version: '15', tool: 'OpenJDK 15 Latest',
							condition: TestCondition.ON_DEMAND),
					new JdkBuildEnvironment(version: '16', tool: 'OpenJDK 16 Latest',
							condition: TestCondition.ON_DEMAND),
					new JdkBuildEnvironment(version: '17', tool: 'OpenJDK 17 Latest',
							condition: TestCondition.AFTER_MERGE)
			],
			database: [
					new DatabaseBuildEnvironment(dbName: 'h2', mavenProfile: 'h2',
							condition: TestCondition.BEFORE_MERGE,
							isDefault: true),
					new DatabaseBuildEnvironment(dbName: 'mariadb', mavenProfile: 'ci-mariadb',
							condition: TestCondition.AFTER_MERGE),
					new DatabaseBuildEnvironment(dbName: 'postgresql', mavenProfile: 'ci-postgresql',
							condition: TestCondition.AFTER_MERGE)
			],
			esLocal: [
					new EsLocalBuildEnvironment(versionRange: '[2.0,2.2)', mavenProfile: 'elasticsearch-2.0',
							jdkTool: 'OpenJDK 8 Latest', condition: TestCondition.AFTER_MERGE),
					new EsLocalBuildEnvironment(versionRange: '[2.2,5.0)', mavenProfile: 'elasticsearch-2.2',
							jdkTool: 'OpenJDK 8 Latest', condition: TestCondition.AFTER_MERGE),
					// Use Elasticsearch 5.0.2 instead of the default 5.1.2, because a bug crashes ES on startup in our environment
					// See https://github.com/elastic/elasticsearch/issues/23218
					new EsLocalBuildEnvironment(versionRange: '[5.0,5.2)', version: '5.0.2', mavenProfile: 'elasticsearch-5.0',
							jdkTool: 'OpenJDK 8 Latest', condition: TestCondition.AFTER_MERGE),
					new EsLocalBuildEnvironment(versionRange: '[5.2,6.0)', mavenProfile: 'elasticsearch-5.2',
							jdkTool: 'OpenJDK 11 Latest', condition: TestCondition.BEFORE_MERGE,
							isDefault: true)
			],
			esAws: [
					new EsAwsBuildEnvironment(version: '2.3', mavenProfile: 'elasticsearch-2.2',
							condition: TestCondition.ON_DEMAND),
					new EsAwsBuildEnvironment(version: '5.1', mavenProfile: 'elasticsearch-5.0',
							condition: TestCondition.ON_DEMAND),
					new EsAwsBuildEnvironment(version: '5.3', mavenProfile: 'elasticsearch-5.2',
							condition: TestCondition.ON_DEMAND),
					new EsAwsBuildEnvironment(version: '5.5', mavenProfile: 'elasticsearch-5.2',
							condition: TestCondition.ON_DEMAND),
					new EsAwsBuildEnvironment(version: '5.6', mavenProfile: 'elasticsearch-5.2',
							condition: TestCondition.ON_DEMAND)
			]
	])

	helper.configure {
		configurationNodePattern QUICK_USE_NODE_PATTERN
		file 'job-configuration.yaml'
		jdk {
			defaultTool environments.content.jdk.default.tool
		}
		maven {
			defaultTool MAVEN_TOOL
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

	echo """Branch: ${helper.scmSource.branch.name}
PR: ${helper.scmSource.pullRequest?.id}
params.ENVIRONMENT_SET: ${params.ENVIRONMENT_SET}
params.ENVIRONMENT_FILTER: ${params.ENVIRONMENT_FILTER}

Resulting execution plan:
    enableDefaultBuild=$enableDefaultBuild
    enableDefaultBuildIT=$enableDefaultBuildIT
    environments=${environments.enabledAsString}
"""
}

stage('Default build') {
	if (!enableDefaultBuild) {
		echo 'Skipping default build and integration tests in the default environment'
		helper.markStageSkipped()
		return
	}
	runBuildOnNode {
		helper.withMavenWorkspace {
			sh """ \
					mvn clean install \
					-Pdist -Pcoverage -Pjqassistant \
					${enableDefaultBuildIT ? '' : '-DskipITs'} \
					${toElasticsearchJdkArg(environments.content.jdk.default)} \
			"""

			// Don't try to report to Coveralls.io or SonarCloud if coverage data is missing
			if (enableDefaultBuildIT) {
				if (helper.configuration.file?.coveralls?.credentials) {
					def coverallsCredentialsId = helper.configuration.file.coveralls.credentials
					// WARNING: Make sure credentials are evaluated by sh, not Groovy.
					// To that end, escape the '$' when referencing the variables.
					// See https://www.jenkins.io/doc/book/pipeline/jenkinsfile/#string-interpolation
					withCredentials([string(credentialsId: coverallsCredentialsId, variable: 'COVERALLS_TOKEN')]) {
						sh """ \
								mvn coveralls:report \
								-DrepoToken=\${COVERALLS_TOKEN} \
								${helper.scmSource.pullRequest ? """ \
										-DpullRequest=${helper.scmSource.pullRequest.id} \
								""" : """ \
										-Dbranch=${helper.scmSource.branch.name} \
								"""} \
						"""
					}
				}
				else {
					echo "No Coveralls token configured - skipping Coveralls report."
				}
			}

			dir(helper.configuration.maven.localRepositoryPath) {
				stash name:'main-build', includes:"org/hibernate/hibernate-search*/**"
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
				helper.withMavenWorkspace(jdk: buildEnv.tool) {
					mavenNonDefaultBuild buildEnv, """ \
							clean install --fail-at-end \
					"""
				}
			}
		})
	}

	// Test ORM integration with multiple databases
	environments.content.database.enabled.each { DatabaseBuildEnvironment buildEnv ->
		executions.put(buildEnv.tag, {
			runBuildOnNode('FedoraLegacyDBInstall') {
				helper.withMavenWorkspace {
					mavenNonDefaultBuild buildEnv, """ \
							clean install -pl org.hibernate:hibernate-search-orm -P$buildEnv.mavenProfile \
					"""
				}
			}
		})
	}

	// Test Elasticsearch integration with multiple versions in a local instance
	environments.content.esLocal.enabled.each { EsLocalBuildEnvironment buildEnv ->
		executions.put(buildEnv.tag, {
			runBuildOnNode {
				helper.withMavenWorkspace {
					mavenNonDefaultBuild buildEnv, """ \
							clean install -pl org.hibernate:hibernate-search-integrationtest-elasticsearch \
							${toElasticsearchVersionArgs(buildEnv.mavenProfile, null)} \
							${buildEnv.version ? "-Dtest.elasticsearch.host.version=$buildEnv.version" : ''} \
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
		def awsCredentialsId = helper.configuration.file?.aws?.credentials
		if (!awsCredentialsId) {
			throw new IllegalStateException("Missing AWS credentials")
		}
		executions.put(buildEnv.tag, {
			lock(label: buildEnv.lockedResourcesLabel, variable: 'LOCKED_RESOURCE_URI') {
				runBuildOnNode(NODE_PATTERN_BASE + '&&AWS') {
					helper.withMavenWorkspace {
						// WARNING: Make sure credentials are evaluated by sh, not Groovy.
						// To that end, escape the '$' when referencing the variables.
						// See https://www.jenkins.io/doc/book/pipeline/jenkinsfile/#string-interpolation
						withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
										 credentialsId   : awsCredentialsId,
										 usernameVariable: 'AWS_ACCESS_KEY_ID',
										 passwordVariable: 'AWS_SECRET_ACCESS_KEY'
						]]) {
							// Tests may fail because of hourly AWS snapshots,
							// which prevent deleting indexes while they are being executed.
							// So if this fails, we re-try twice.
							retry(count: 3) {
								mavenNonDefaultBuild buildEnv, """ \
									clean install -pl org.hibernate:hibernate-search-integrationtest-elasticsearch \
									${toElasticsearchVersionArgs(buildEnv.mavenProfile, buildEnv.version)} \
									-Dtest.elasticsearch.host.url=$env.LOCKED_RESOURCE_URI \
									-Dtest.elasticsearch.host.aws.signing.enabled=true \
									-Dtest.elasticsearch.host.aws.access_key=\${AWS_ACCESS_KEY_ID} \
									-Dtest.elasticsearch.host.aws.secret_key=\${AWS_SECRET_ACCESS_KEY} \
									-Dtest.elasticsearch.host.aws.region=$env.ES_AWS_REGION \
								"""
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

	String getMavenJdkTool(def allEnvironments) {
		allEnvironments.content.jdk.default.tool
	}

	String getElasticsearchJdkTool(def allEnvironments) {
		allEnvironments.content.esLocal.default.jdkTool
	}
}

class JdkBuildEnvironment extends BuildEnvironment {
	String version
	String tool
	String getTag() { "jdk-$version" }
	@Override
	boolean requiresDefaultBuildArtifacts() { false }
	@Override
	String getMavenJdkTool(def allEnvironments) {
		tool
	}
}

class DatabaseBuildEnvironment extends BuildEnvironment {
	String dbName
	String mavenProfile
	@Override
	String getTag() { "database-$dbName" }
}

class EsLocalBuildEnvironment extends BuildEnvironment {
	String versionRange
	String version
	String mavenProfile
	String jdkTool
	@Override
	String getTag() { "elasticsearch-local-$versionRange${version ? "-as-$version" : ''}" }
	@Override
	String getElasticsearchJdkTool(def allEnvironments) {
		jdkTool
	}
}

class EsAwsBuildEnvironment extends BuildEnvironment {
	String version
	String mavenProfile
	@Override
	String getTag() { "elasticsearch-aws-$version" }
	@Override
	String getElasticsearchJdkTool(def allEnvironments) {
		null // No JDK needed for Elasticsearch: the Elasticsearch instance is remote.
	}
	String getNameEmbeddableVersion() {
		version.replaceAll('\\.', '-')
	}
	String getLockedResourcesLabel() {
		"es-aws-${nameEmbeddableVersion}"
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
		case 'EXPERIMENTAL':
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
				enableBeforeMergeEnvs = true
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
	node( label ) {
		timeout( [time: 1, unit: 'HOURS'], body )
	}
}

void mavenNonDefaultBuild(BuildEnvironment buildEnv, String args) {
	if ( buildEnv.requiresDefaultBuildArtifacts() ) {
		dir(helper.configuration.maven.localRepositoryPath) {
			unstash name:'main-build'
		}
	}

	// Add a suffix to tests to distinguish between different executions
	// of the same test in different environments in reports
	def testSuffix = buildEnv.tag.replaceAll('[^a-zA-Z0-9_\\-+]+', '_')
	sh """ \
			mvn -Dsurefire.environment=$testSuffix \
					${toElasticsearchJdkArg(buildEnv)} \
					$args \
	"""
}

String toElasticsearchVersionArgs(String mavenEsProfile, String version) {
	String defaultEsProfile = environments.content.esLocal.default.mavenProfile
	if ( version ) {
		// The default profile is disabled, because a version is passed explicitly
		// We just need to set the correct profile and pass the version
		"-P$mavenEsProfile -Dtest.elasticsearch.host.version=$version"
	}
	else if ( mavenEsProfile != defaultEsProfile) {
		// Disable the default profile to avoid conflicting configurations
		"-P!$defaultEsProfile,$mavenEsProfile"
	}
	else {
		// Do not do as above, as we would tell Maven "disable the default profile, but enable it"
		// and Maven would end up disabling it.
		''
	}
}

String toElasticsearchJdkArg(BuildEnvironment buildEnv) {
	String elasticsearchJdkTool = buildEnv.getElasticsearchJdkTool(environments)

	if (elasticsearchJdkTool == null || buildEnv.getMavenJdkTool(environments) == elasticsearchJdkTool) {
		return '' // No specific JDK needed
	}

	def elasticsearchJdkToolPath = tool(name: elasticsearchJdkTool, type: 'jdk')
	return "-Dtest.elasticsearch.java_home=$elasticsearchJdkToolPath"
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

import groovy.transform.Field
import org.jenkinsci.plugins.credentialsbinding.impl.CredentialNotFoundException

/*
 * See https://github.com/hibernate/hibernate-jenkins-pipeline-helpers
 */
@Library('hibernate-jenkins-pipeline-helpers@1.2')
import org.hibernate.jenkins.pipeline.helpers.job.JobHelper
import org.hibernate.jenkins.pipeline.helpers.alternative.AlternativeMultiMap
import org.hibernate.jenkins.pipeline.helpers.version.Version

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
 * them in the GUI: see http://ci.hibernate.org/job/hibernate-search/configure, "Pipeline Libraries".
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
 * This job includes two deployment modes:
 *
 * - A deployment of snapshot artifacts for every non-PR build on "primary" branches (master and maintenance branches).
 * - A full release when starting the job with specific parameters.
 *
 * In the first case, the name of a Maven settings file must be provided in the job configuration file
 * (see below).
 *
 * #### Coveralls (optional)
 *
 * You need to enable your repository in Coveralls first: see https://coveralls.io/repos/new.
 *
 * Then you will also need to add the Coveralls repository token as credentials in Jenkins
 * (see "Credentials" section below).
 *
 * #### Sonarcloud (optional)
 *
 * You need to enable the SonarCloud GitHub app for your repository:
 * see https://github.com/apps/sonarcloud.
 *
 * Then you will also need to add SonarCloud credentials in Jenkins (see below)
 * and to configure the SonarCloud organization in the job configuration file (see below).
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
 *     sonar:
 *       # String containing the sonar organization. Mandatory in order to enable Sonar analysis.
 *       organization: ...
 *     deployment:
 *       maven:
 *         # String containing the ID of a Maven settings file registered using the config-file-provider Jenkins plugin.
 *         # The settings must provide credentials to the servers with ID
 *         # 'jboss-releases-repository' and 'jboss-snapshots-repository'.
 *         settingsId: ...
 *
 * #### Credentials
 *
 * The following credentials are necessary for some features:
 *
 * - 'aws-elasticsearch' AWS credentials, to test Elasticsearch as a service on AWS
 * - 'coveralls-repository-token' secret text credentials containing the repository token,
 * to send coverage reports to coveralls.io. Note these credentials should be registered at the job level, not system-wide.
 * - 'sonarcloud-hibernate-token' secret text credentials containing a Sonar access token for the configured organization
 * (see "Configuration file" above) to send Sonar analysis input to sonarcloud.io.
 */

@Field final String MAVEN_TOOL = 'Apache Maven 3.6.0'

// Default node pattern, to be used for resource-intensive stages.
// Should not include the master node.
@Field final String NODE_PATTERN_BASE = 'Slave'
// Quick-use node pattern, to be used for very light, quick, and environment-independent stages,
// such as sending a notification. May include the master node in particular.
@Field final String QUICK_USE_NODE_PATTERN = 'Master||Slave'

@Field AlternativeMultiMap<BuildEnvironment> environments
@Field JobHelper helper

@Field boolean enableDefaultBuild = false
@Field boolean enableDefaultBuildIT = false
@Field boolean enableDefaultBuildLegacyIT = false
@Field boolean enableNonDefaultSupportedBuildEnv = false
@Field boolean enableExperimentalBuildEnv = false
@Field boolean performRelease = false
@Field boolean deploySnapshot = false

@Field Version releaseVersion
@Field Version afterReleaseDevelopmentVersion

this.helper = new JobHelper(this)

helper.runWithNotification {

stage('Configure') {
	this.environments = AlternativeMultiMap.create([
			jdk: [
					// This should not include every JDK; in particular let's not care too much about EOL'd JDKs like version 9
					// See http://www.oracle.com/technetwork/java/javase/eol-135779.html
					new JdkBuildEnvironment(version: '8', tool: 'Oracle JDK 8', status: BuildEnvironmentStatus.USED_IN_DEFAULT_BUILD),
					new JdkBuildEnvironment(version: '11', tool: 'OpenJDK 11 Latest', status: BuildEnvironmentStatus.SUPPORTED),
					new JdkBuildEnvironment(version: '12', tool: 'OpenJDK 12 Latest', status: BuildEnvironmentStatus.SUPPORTED),
					new JdkBuildEnvironment(version: '13', tool: 'OpenJDK 13 Latest', status: BuildEnvironmentStatus.EXPERIMENTAL,
							// Elasticsearch won't run on JDK13
							elasticsearchTool: 'OpenJDK 11 Latest')
			],
			compiler: [
					new CompilerBuildEnvironment(name: 'eclipse', mavenProfile: 'compiler-eclipse', status: BuildEnvironmentStatus.SUPPORTED),
			],
			database: [
					new DatabaseBuildEnvironment(dbName: 'h2', mavenProfile: 'h2', status: BuildEnvironmentStatus.USED_IN_DEFAULT_BUILD),
					new DatabaseBuildEnvironment(dbName: 'mariadb', mavenProfile: 'ci-mariadb', status: BuildEnvironmentStatus.SUPPORTED),
					new DatabaseBuildEnvironment(dbName: 'postgresql', mavenProfile: 'ci-postgresql', status: BuildEnvironmentStatus.SUPPORTED)
			],
			esLocal: [
					new EsLocalBuildEnvironment(versionRange: '[5.6,6.0)', mavenProfile: 'elasticsearch-5.6', status: BuildEnvironmentStatus.SUPPORTED),
					new EsLocalBuildEnvironment(versionRange: '[6.0,6.7)', mavenProfile: 'elasticsearch-6.0', status: BuildEnvironmentStatus.SUPPORTED),
					new EsLocalBuildEnvironment(versionRange: '[6.7,7.0)', mavenProfile: 'elasticsearch-6.7', status: BuildEnvironmentStatus.SUPPORTED),
					new EsLocalBuildEnvironment(versionRange: '[7.0,7.x)', mavenProfile: 'elasticsearch-7.0', status: BuildEnvironmentStatus.USED_IN_DEFAULT_BUILD)
			],
			esAws: [
					new EsAwsBuildEnvironment(version: '5.6', mavenProfile: 'elasticsearch-5.6', status: BuildEnvironmentStatus.SUPPORTED),
					new EsAwsBuildEnvironment(version: '6.0', mavenProfile: 'elasticsearch-6.0', status: BuildEnvironmentStatus.SUPPORTED),
					new EsAwsBuildEnvironment(version: '6.2', mavenProfile: 'elasticsearch-6.0', status: BuildEnvironmentStatus.SUPPORTED),
					new EsAwsBuildEnvironment(version: '6.3', mavenProfile: 'elasticsearch-6.0', status: BuildEnvironmentStatus.SUPPORTED),
					new EsAwsBuildEnvironment(version: '6.4', mavenProfile: 'elasticsearch-6.0', status: BuildEnvironmentStatus.SUPPORTED)
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
			producedArtifactPattern "org/hibernate/search/*"
			producedArtifactPattern "org/hibernate/hibernate-search*"
		}
	}

	properties([
			buildDiscarder(
					logRotator(daysToKeepStr: '90')
			),
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
EXPERIMENTAL
ALL""",
							defaultValue: 'AUTOMATIC',
							description: """A set of environments that must be checked.
'AUTOMATIC' picks a different set of environments based on the branch name and whether a release is being performed.
'DEFAULT' means a single build with the default environment expected by the Maven configuration,
while other options will trigger multiple Maven executions in different environments."""
					),
					booleanParam(
							name: 'LEGACY_IT',
							defaultValue: false,
							description: 'If true, also enable tests of the legacy code (Search 5) in the default environment.'
					),
					string(
							name: 'RELEASE_VERSION',
							defaultValue: '',
							description: 'The version to be released, e.g. 5.10.0.Final. Setting this triggers a release.',
							trim: true
					),
					string(
							name: 'RELEASE_DEVELOPMENT_VERSION',
							defaultValue: '',
							description: 'The next version to be used after the release, e.g. 5.10.0-SNAPSHOT.',
							trim: true
					),
					booleanParam(
							name: 'RELEASE_DRY_RUN',
							defaultValue: false,
							description: 'If true, just simulate the release, without pushing any commits or tags, and without uploading any artifacts or documentation.'
					)
			])
	])

	performRelease = (params.RELEASE_VERSION ? true : false)

	if (!performRelease && helper.scmSource.branch.primary && !helper.scmSource.pullRequest) {
		if (helper.configuration.file?.deployment?.maven?.settingsId) {
			deploySnapshot = true
		}
		else {
			echo "Missing deployment configuration in job configuration file - snapshot deployment will be skipped."
		}
	}

	switch (params.ENVIRONMENT_SET) {
		case 'DEFAULT':
			enableDefaultBuildIT = true
			break
		case 'SUPPORTED':
			enableDefaultBuildIT = true
			enableNonDefaultSupportedBuildEnv = true
			break
		case 'ALL':
			enableDefaultBuildIT = true
			enableNonDefaultSupportedBuildEnv = true
			enableExperimentalBuildEnv = true
			break
		case 'EXPERIMENTAL':
			enableExperimentalBuildEnv = true
			break
		case 'AUTOMATIC':
			if (params.RELEASE_VERSION) {
				echo "Skipping default build and integration tests to speed up the release of version $params.RELEASE_VERSION"
			} else if (helper.scmSource.pullRequest) {
				echo "Enabling only the default build with integration tests in the default environment for pull request $helper.scmSource.pullRequest.id"
				enableDefaultBuildIT = true
			} else if (helper.scmSource.branch.primary) {
				echo "Enabling builds on all supported environments for primary branch '$helper.scmSource.branch.name'"
				enableDefaultBuildIT = true
				enableNonDefaultSupportedBuildEnv = true
				echo "Enabling legacy integration tests in the default environment for primary branch '$helper.scmSource.branch.name'"
				enableDefaultBuildLegacyIT = true
			} else {
				echo "Enabling only the default build with integration tests in the default environment for feature branch $helper.scmSource.branch.name"
				enableDefaultBuildIT = true
			}
			break
		default:
			throw new IllegalArgumentException(
					"Unknown value for param 'ENVIRONMENT_SET': '$params.ENVIRONMENT_SET'."
			)
	}

	if ( enableDefaultBuildIT && params.LEGACY_IT ) {
		echo "Enabling legacy integration tests in default environment due to explicit request"
		enableDefaultBuildLegacyIT = true
	}

	enableDefaultBuild =
			enableDefaultBuildIT || enableNonDefaultSupportedBuildEnv || enableExperimentalBuildEnv || deploySnapshot

	echo """Branch: ${helper.scmSource.branch.name}, PR: ${helper.scmSource.pullRequest?.id}, environment setting: $params.ENVIRONMENT_SET, resulting execution plan:
enableDefaultBuild=$enableDefaultBuild
enableDefaultBuildIT=$enableDefaultBuildIT
enableDefaultBuildLegacyIT=$enableDefaultBuildLegacyIT
enableNonDefaultSupportedBuildEnv=$enableNonDefaultSupportedBuildEnv
enableExperimentalBuildEnv=$enableExperimentalBuildEnv
performRelease=$performRelease
deploySnapshot=$deploySnapshot"""

	// Filter environments

	environments.content.each { key, envSet ->
		// No need to re-test default environments, they are already tested as part of the default build
		envSet.enabled.remove(envSet.default)

		if (!enableNonDefaultSupportedBuildEnv) {
			envSet.enabled.removeAll { buildEnv -> buildEnv.status == BuildEnvironmentStatus.SUPPORTED }
		}
		if (!enableExperimentalBuildEnv) {
			envSet.enabled.removeAll { buildEnv -> buildEnv.status == BuildEnvironmentStatus.EXPERIMENTAL }
		}
	}

	environments.content.esAws.enabled.removeAll { buildEnv ->
		buildEnv.endpointUrl = env.getProperty(buildEnv.endpointVariableName)
		if (!buildEnv.endpointUrl) {
			echo "Skipping test ${buildEnv.tag} because environment variable '${buildEnv.endpointVariableName}' is not defined."
			return true
		}
		buildEnv.awsRegion = env.ES_AWS_REGION
		if (!buildEnv.awsRegion) {
			echo "Skipping test ${buildEnv.tag} because environment variable 'ES_AWS_REGION' is not defined."
			return true
		}
		return false // Environment is fully defined, do not remove
	}

	if (environments.isAnyEnabled()) {
		echo "Enabled non-default environments: ${environments.enabledAsString}"
	}
	else {
		echo "Non-default environments are completely disabled."
	}

	if (performRelease) {
		releaseVersion = Version.parseReleaseVersion(params.RELEASE_VERSION)
		echo "Inferred version family for the release to '$releaseVersion.family'"

		// Check that all the necessary parameters are set
		if (!params.RELEASE_DEVELOPMENT_VERSION) {
			throw new IllegalArgumentException(
					"Missing value for parameter RELEASE_DEVELOPMENT_VERSION." +
							" This parameter must be set when RELEASE_VERSION is set."
			)
		}
		if (!params.RELEASE_DRY_RUN && !helper.configuration.file?.deployment?.maven?.settingsId) {
			throw new IllegalArgumentException(
					"Missing deployment configuration in job configuration file." +
							" Cannot deploy artifacts during the release."
			)
		}
	}

	if (params.RELEASE_DEVELOPMENT_VERSION) {
		afterReleaseDevelopmentVersion = Version.parseDevelopmentVersion(params.RELEASE_DEVELOPMENT_VERSION)
	}
}

stage('Default build') {
	if (!enableDefaultBuild) {
		echo 'Skipping default build and integration tests in the default environment'
		helper.markStageSkipped()
		return
	}
	node(NODE_PATTERN_BASE) {
		helper.withMavenWorkspace(mavenSettingsConfig: deploySnapshot ? helper.configuration.file.deployment.maven.settingsId : null) {
			sh """ \
					mvn clean \
					${deploySnapshot ? "\
							deploy \
					" : "\
							install \
					"} \
					-Pdist -Pcoverage -Pjqassistant \
					${enableDefaultBuildIT ? '' : '-DskipITs'} \
					${enableDefaultBuildLegacyIT ? '-Dlegacy.skip=false' : ''} \
			"""

			// Don't try to report to Coveralls.io or SonarCloud if coverage data is missing
			if (enableDefaultBuildIT) {
				try {
					withCredentials([string(credentialsId: 'coveralls-repository-token', variable: 'COVERALLS_TOKEN')]) {
						sh """ \
								mvn coveralls:report \
								-DrepoToken=${COVERALLS_TOKEN} \
								${helper.scmSource.pullRequest ? """ \
										-DpullRequest=${helper.scmSource.pullRequest.id} \
								""" : """ \
										-Dbranch=${helper.scmSource.branch.name} \
								"""} \
						"""
					}
				}
				catch (CredentialNotFoundException e) {
					echo "No Coveralls token configured - skipping Coveralls report. Error was: ${e}"
				}

				if (helper.configuration.file?.sonar?.organization) {
					def sonarOrganization = helper.configuration.file.sonar.organization
					withCredentials([string(credentialsId: 'sonarcloud-hibernate-token', variable: 'SONARCLOUD_TOKEN')]) {
						sh """ \
								mvn sonar:sonar \
								-Dsonar.organization=${sonarOrganization} \
								-Dsonar.host.url=https://sonarcloud.io \
								-Dsonar.login=${SONARCLOUD_TOKEN} \
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

			dir(helper.configuration.maven.localRepositoryPath) {
				stash name:'main-build', includes:"org/hibernate/search/**"
			}
		}
	}
}

stage('Non-default environments') {
	Map<String, Closure> executions = [:]

	// Test with multiple JDKs
	environments.content.jdk.enabled.each { JdkBuildEnvironment buildEnv ->
		executions.put(buildEnv.tag, {
			node(NODE_PATTERN_BASE) {
				def elasticsearchJdkTool = buildEnv.elasticsearchTool ? tool(name: buildEnv.elasticsearchTool, type: 'jdk') : null
				helper.withMavenWorkspace(jdk: buildEnv.tool) {
					mavenNonDefaultBuild buildEnv, """ \
							clean install --fail-at-end \
							${elasticsearchJdkTool ? "-Dtest.elasticsearch.java_home=$elasticsearchJdkTool" : ""} \
					"""
				}
			}
		})
	}

	// Build with different compilers
	environments.content.compiler.enabled.each { CompilerBuildEnvironment buildEnv ->
		executions.put(buildEnv.tag, {
			node(NODE_PATTERN_BASE) {
				helper.withMavenWorkspace {
					mavenNonDefaultBuild buildEnv, """ \
							clean install --fail-at-end \
							-DskipTests \
							-P${buildEnv.mavenProfile} \
					"""
				}
			}
		})
	}

	// Test ORM integration with multiple databases
	environments.content.database.enabled.each { DatabaseBuildEnvironment buildEnv ->
		executions.put(buildEnv.tag, {
			node(NODE_PATTERN_BASE) {
				helper.withMavenWorkspace {
					resumeFromDefaultBuild()
					mavenNonDefaultBuild buildEnv, """ \
							clean install \
							-pl org.hibernate.search:hibernate-search-integrationtest-orm,org.hibernate.search:hibernate-search-integrationtest-showcase-library \
							-P$buildEnv.mavenProfile \
					"""
				}
			}
		})
	}

	// Test Elasticsearch integration with multiple versions in a local instance
	environments.content.esLocal.enabled.each { EsLocalBuildEnvironment buildEnv ->
		executions.put(buildEnv.tag, {
			node(NODE_PATTERN_BASE) {
				helper.withMavenWorkspace {
					resumeFromDefaultBuild()
					mavenNonDefaultBuild buildEnv, """ \
							clean install \
							-pl org.hibernate.search:hibernate-search-integrationtest-backend-elasticsearch,org.hibernate.search:hibernate-search-integrationtest-showcase-library \
							${toMavenElasticsearchProfileArg(buildEnv.mavenProfile)} \
					"""
				}
			}
		})
	}

	// Test Elasticsearch integration with multiple versions in an AWS instance
	environments.content.esAws.enabled.each { EsAwsBuildEnvironment buildEnv ->
		if (!buildEnv.endpointUrl) {
			throw new IllegalStateException("Unexpected empty endpoint URL")
		}
		if (!buildEnv.awsRegion) {
			throw new IllegalStateException("Unexpected empty AWS region")
		}
		executions.put(buildEnv.tag, {
			lock(label: buildEnv.lockedResourcesLabel) {
				node(NODE_PATTERN_BASE + '&&AWS') {
					helper.withMavenWorkspace {
						resumeFromDefaultBuild()
						withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
										 credentialsId   : 'aws-elasticsearch',
										 usernameVariable: 'AWS_ACCESS_KEY_ID',
										 passwordVariable: 'AWS_SECRET_ACCESS_KEY'
						]]) {
							mavenNonDefaultBuild buildEnv, """ \
								clean install \
								-pl org.hibernate.search:hibernate-search-integrationtest-backend-elasticsearch,org.hibernate.search:hibernate-search-integrationtest-showcase-library \
								${toMavenElasticsearchProfileArg(buildEnv.mavenProfile)} \
								-Dtest.elasticsearch.host.provided=true \
								-Dtest.elasticsearch.host.url=$buildEnv.endpointUrl \
								-Dtest.elasticsearch.host.version=$buildEnv.version \
								-Dtest.elasticsearch.host.aws.signing.enabled=true \
								-Dtest.elasticsearch.host.aws.signing.access_key=$AWS_ACCESS_KEY_ID \
								-Dtest.elasticsearch.host.aws.signing.secret_key=$AWS_SECRET_ACCESS_KEY \
								-Dtest.elasticsearch.host.aws.signing.region=$buildEnv.awsRegion \
							"""
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

stage('Deploy') {
	if (deploySnapshot) {
		// TODO delay the release to this stage? This would require to use staging repositories for snapshots, not sure it's possible.
		echo "Already deployed snapshot as part of the 'Default build' stage."
	}
	else if (performRelease) {
		echo "Performing full release for version ${releaseVersion.toString()}"
		node(NODE_PATTERN_BASE) {
			helper.withMavenWorkspace(mavenSettingsConfig: params.RELEASE_DRY_RUN ? null : helper.configuration.file.deployment.maven.settingsId) {
				sh "git clone https://github.com/hibernate/hibernate-noorm-release-scripts.git"
				sh "bash -xe hibernate-noorm-release-scripts/prepare-release.sh search ${releaseVersion.toString()}"

				String deployCommand = "bash -xe hibernate-noorm-release-scripts/deploy.sh search"
				if (!params.RELEASE_DRY_RUN) {
					sh deployCommand
				} else {
					echo "WARNING: Not deploying. Would have executed:"
					echo deployCommand
				}

				String uploadDistributionCommand = "bash -xe hibernate-noorm-release-scripts/upload-distribution.sh search ${releaseVersion.toString()}"
				String uploadDocumentationCommand = "bash -xe hibernate-noorm-release-scripts/upload-documentation.sh search ${releaseVersion.toString()} ${releaseVersion.family}"
				if (!params.RELEASE_DRY_RUN) {
					sh uploadDistributionCommand
					sh uploadDocumentationCommand
				}
				else {
					echo "WARNING: Not uploading anything. Would have executed:"
					echo uploadDistributionCommand
					echo uploadDocumentationCommand
				}

				sh "bash -xe hibernate-noorm-release-scripts/update-version.sh search ${afterReleaseDevelopmentVersion.toString()}"
				sh "bash -xe hibernate-noorm-release-scripts/push-upstream.sh search ${releaseVersion.toString()} ${helper.scmSource.branch.name} ${!params.RELEASE_DRY_RUN}"
			}
		}
	}
	else {
		echo "Skipping deployment"
		helper.markStageSkipped()
		return
	}
}

} // End of helper.runWithNotification

// Job-specific helpers

enum BuildEnvironmentStatus {
	// For environments used as part of the default build (tested on all branches)
	USED_IN_DEFAULT_BUILD,
	// For environments that are expected to work correctly (tested on master and maintenance branches)
	SUPPORTED,
	// For environments that may not work correctly (only tested when explicitly requested through job parameters)
	EXPERIMENTAL;

	// Work around JENKINS-33023
	// See https://issues.jenkins-ci.org/browse/JENKINS-33023?focusedCommentId=325738&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-325738
	public BuildEnvironmentStatus() {}
}

abstract class BuildEnvironment {
	BuildEnvironmentStatus status
	String toString() { getTag() }
	abstract String getTag()
	boolean isDefault() { status == BuildEnvironmentStatus.USED_IN_DEFAULT_BUILD }
}

class JdkBuildEnvironment extends BuildEnvironment {
	String version
	String tool
	String elasticsearchTool
	String getTag() { "jdk-$version" }
}

class CompilerBuildEnvironment extends BuildEnvironment {
	String name
	String mavenProfile
	String getTag() { "compiler-$name" }
}

class DatabaseBuildEnvironment extends BuildEnvironment {
	String dbName
	String mavenProfile
	String getTag() { "database-$dbName" }
}

class EsLocalBuildEnvironment extends BuildEnvironment {
	String versionRange
	String mavenProfile
	String getTag() { "elasticsearch-local-$versionRange" }
}

class EsAwsBuildEnvironment extends BuildEnvironment {
	String version
	String mavenProfile
	String endpointUrl = null
	String awsRegion = null
	String getTag() { "elasticsearch-aws-$version" }
	String getNameEmbeddableVersion() {
		version.replaceAll('\\.', '')
	}
	String getEndpointVariableName() {
		"ES_AWS_${nameEmbeddableVersion}_ENDPOINT"
	}
	String getLockedResourcesLabel() {
		"es-aws-${nameEmbeddableVersion}"
	}
}

void resumeFromDefaultBuild() {
	dir(helper.configuration.maven.localRepositoryPath) {
		unstash name:'main-build'
	}
}

void mavenNonDefaultBuild(BuildEnvironment buildEnv, String args) {
	// Add a suffix to tests to distinguish between different executions
	// of the same test in different environments in reports
	def testSuffix = buildEnv.tag.replaceAll('[^a-zA-Z0-9_\\-+]+', '_')
	sh "mvn -Dsurefire.environment=$testSuffix $args"
}

String toMavenElasticsearchProfileArg(String mavenEsProfile) {
	String defaultEsProfile = environments.content.esLocal.default.mavenProfile
	if (mavenEsProfile != defaultEsProfile) {
		// Disable the default profile to avoid conflicting configurations
		"-P!$defaultEsProfile,$mavenEsProfile"
	}
	else {
		// Do not do as above, as we would tell Maven "disable the default profile, but enable it"
		// and Maven would end up disabling it.
		''
	}
}

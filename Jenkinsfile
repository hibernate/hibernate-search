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
 * them in the GUI: see http://ci.hibernate.org/job/hibernate-search-6-poc/configure, "Pipeline Libraries".
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
 * This Jenkinsfile gets its configuration from two sources:
 * branch name, and a configuration file.
 * All configuration is optional for the default build (and it should stay that way),
 * but some features require some configuration.
 *
 * #### Branch name
 *
 * See the org.hibernate.(...) imports for a link to the helpers library documentation,
 * which explains the basics.
 *
 * #### Job configuration file
 *
 * See the org.hibernate.(...) imports for a link to the helpers library documentation,
 * which explains the basic structure of this file and how to set it up.
 *
 * Below is the additional structure specific to this Jenkinsfile:
 *
 */

@Field final String MAVEN_TOOL = 'Apache Maven 3.5'

// Default node pattern, to be used for resource-intensive stages.
// Should not include the master node.
@Field final String NODE_PATTERN_BASE = 'Slave'
// Quick-use node pattern, to be used for very light, quick, and environment-independent stages,
// such as sending a notification. May include the master node in particular.
@Field final String QUICK_USE_NODE_PATTERN = 'Master||Slave'

@Field AlternativeMultiMap<ITEnvironment> environments
@Field JobHelper helper

@Field boolean enableDefaultBuild = false
@Field boolean enableDefaultEnvIT = false
@Field boolean enableNonDefaultSupportedEnvIT = false
@Field boolean enableExperimentalEnvIT = false
@Field boolean performRelease = false

@Field Version releaseVersion
@Field Version afterReleaseDevelopmentVersion

this.helper = new JobHelper(this)

helper.runWithNotification {

stage('Configure') {
	this.environments = AlternativeMultiMap.create([
			jdk: [
					new JdkITEnvironment(version: '8', tool: 'OpenJDK 8 Latest', status: ITEnvironmentStatus.USED_IN_DEFAULT_BUILD),
					new JdkITEnvironment(version: '9', tool: 'OpenJDK 9 Latest', status: ITEnvironmentStatus.EXPERIMENTAL)
			],
			database: [
					new DatabaseITEnvironment(dbName: 'h2', mavenProfile: 'h2', status: ITEnvironmentStatus.USED_IN_DEFAULT_BUILD),
					new DatabaseITEnvironment(dbName: 'mariadb', mavenProfile: 'ci-mariadb', status: ITEnvironmentStatus.SUPPORTED),
					new DatabaseITEnvironment(dbName: 'postgresql', mavenProfile: 'ci-postgresql', status: ITEnvironmentStatus.SUPPORTED)
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
							name: 'INTEGRATION_TESTS',
							choices: """AUTOMATIC
DEFAULT_ENV_ONLY
SUPPORTED_ENV_ONLY
EXPERIMENTAL_ENV_ONLY
ALL_ENV""",
							defaultValue: 'AUTOMATIC',
							description: """Which integration tests to run.
'AUTOMATIC' chooses based on the branch name and whether a release is being performed.
'DEFAULT_ENV_ONLY' means a single build, while other options will trigger multiple Maven executions in different environments."""
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

	switch (params.INTEGRATION_TESTS) {
		case 'DEFAULT_ENV_ONLY':
			enableDefaultEnvIT = true
			break
		case 'SUPPORTED_ENV_ONLY':
			enableDefaultEnvIT = true
			enableNonDefaultSupportedEnvIT = true
			break
		case 'ALL_ENV':
			enableDefaultEnvIT = true
			enableNonDefaultSupportedEnvIT = true
			enableExperimentalEnvIT = true
			break
		case 'EXPERIMENTAL_ENV_ONLY':
			enableExperimentalEnvIT = true
			break
		case 'AUTOMATIC':
			if (params.RELEASE_VERSION) {
				echo "Skipping default build and integration tests to speed up the release of version $params.RELEASE_VERSION"
			} else if (helper.scmSource.pullRequest) {
				echo "Enabling only the default build and integration tests in the default environment for pull request $helper.scmSource.pullRequest.id"
				enableDefaultEnvIT = true
			} else if (helper.scmSource.branch.primary) {
				echo "Enabling integration tests on all supported environments for primary branch '$helper.scmSource.branch.name'"
				enableDefaultBuild = true
				enableDefaultEnvIT = true
				enableNonDefaultSupportedEnvIT = true
			} else {
				echo "Enabling only the default build and integration tests in the default environment for feature branch $helper.scmSource.branch.name"
				enableDefaultBuild = true
				enableDefaultEnvIT = true
			}
			break
		default:
			throw new IllegalArgumentException(
					"Unknown value for param 'INTEGRATION_TESTS': '$params.INTEGRATION_TESTS'."
			)
	}

	enableDefaultBuild =
			enableDefaultEnvIT || enableNonDefaultSupportedEnvIT || enableExperimentalEnvIT

	echo """Branch: ${helper.scmSource.branch.name}, PR: ${helper.scmSource.pullRequest?.id}, integration test setting: $params.INTEGRATION_TESTS, resulting execution plan:
enableDefaultBuild=$enableDefaultBuild
enableDefaultEnvIT=$enableDefaultEnvIT
enableNonDefaultSupportedEnvIT=$enableNonDefaultSupportedEnvIT
enableExperimentalEnvIT=$enableExperimentalEnvIT
performRelease=$performRelease"""

	// Filter environments

	environments.content.each { key, envSet ->
		// No need to re-test default environments, they are already tested as part of the default build
		envSet.enabled.remove(envSet.default)

		if (!enableNonDefaultSupportedEnvIT) {
			envSet.enabled.removeAll { itEnv -> itEnv.status == ITEnvironmentStatus.SUPPORTED }
		}
		if (!enableExperimentalEnvIT) {
			envSet.enabled.removeAll { itEnv -> itEnv.status == ITEnvironmentStatus.EXPERIMENTAL }
		}
	}

	if (environments.isAnyEnabled()) {
		echo "Enabled non-default environment ITs: ${environments.enabledAsString}"
	}
	else {
		echo "Non-default environment ITs are completely disabled."
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
	runBuildOnNode {
		helper.withMavenWorkspace {
			sh """ \
					mvn clean install \
					-Pdocs,docbook,dist \
					${enableDefaultEnvIT ? '' : '-DskipTests'} \
			"""

			dir(helper.configuration.maven.localRepositoryPath) {
				stash name:'main-build', includes:"org/hibernate/hibernate-search*/**"
			}
		}
	}
}

stage('Non-default environment ITs') {
	Map<String, Closure> executions = [:]

	// Test with multiple JDKs
	environments.content.jdk.enabled.each { JdkITEnvironment itEnv ->
		executions.put(itEnv.tag, {
			runBuildOnNode {
				helper.withMavenWorkspace(jdk: itEnv.tool) {
					mavenNonDefaultIT itEnv, """ \
							clean install --fail-at-end \
					"""
				}
			}
		})
	}

	// Test ORM integration with multiple databases
	environments.content.database.enabled.each { DatabaseITEnvironment itEnv ->
		executions.put(itEnv.tag, {
			runBuildOnNode('FedoraLegacyDBInstall') {
				helper.withMavenWorkspace {
					resumeFromDefaultBuild()
					mavenNonDefaultIT itEnv, """ \
							clean install -pl org.hibernate:hibernate-search-orm -P$itEnv.mavenProfile \
					"""
				}
			}
		})
	}

	if (executions.isEmpty()) {
		echo 'Skipping integration tests in non-default environments'
		helper.markStageSkipped()
	}
	else {
		parallel(executions)
	}
}

stage('Deploy') {
	if (performRelease) {
		echo "Performing full release for version ${releaseVersion.toString()}"
		runBuildOnNode {
			helper.withMavenWorkspace(mavenSettingsConfig: params.RELEASE_DRY_RUN ? null : helper.configuration.file.deployment.maven.settingsId) {
				configFileProvider([configFile(fileId: 'release.config.ssh', targetLocation: env.HOME + '/.ssh/config')]) {
					sshagent(['ed25519.Hibernate-CI.github.com', 'hibernate.filemgmt.jboss.org', 'hibernate-ci.frs.sourceforge.net']) {
						sh 'cat $HOME/.ssh/config'
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

enum ITEnvironmentStatus {
	// For environments used as part of the integration tests in the default build (tested on all branches)
	USED_IN_DEFAULT_BUILD,
	// For environments that are expected to work correctly (tested on master and maintenance branches)
	SUPPORTED,
	// For environments that may not work correctly (only tested when explicitly requested through job parameters)
	EXPERIMENTAL;

	// Work around JENKINS-33023
	// See https://issues.jenkins-ci.org/browse/JENKINS-33023?focusedCommentId=325738&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-325738
	public ITEnvironmentStatus() {}
}

abstract class ITEnvironment {
	ITEnvironmentStatus status
	String toString() { getTag() }
	abstract String getTag()
	boolean isDefault() { status == ITEnvironmentStatus.USED_IN_DEFAULT_BUILD }
}

class JdkITEnvironment extends ITEnvironment {
	String version
	String tool
	String getTag() { "jdk-$version" }
}

class DatabaseITEnvironment extends ITEnvironment {
	String dbName
	String mavenProfile
	String getTag() { "database-$dbName" }
}

void resumeFromDefaultBuild() {
	dir(helper.configuration.maven.localRepositoryPath) {
		unstash name:'main-build'
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

void mavenNonDefaultIT(ITEnvironment itEnv, String args) {
	// Add a suffix to tests to distinguish between different executions
	// of the same test in different environments in reports
	def testSuffix = itEnv.tag.replaceAll('[^a-zA-Z0-9_\\-+]+', '_')
	sh "mvn -Dsurefire.environment=$testSuffix $args"
}

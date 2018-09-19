/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

import groovy.transform.Field
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils
import org.jenkinsci.plugins.credentialsbinding.impl.CredentialNotFoundException

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
 * See http://ci.hibernate.org/pipeline-syntax/ for help writing Jenkins pipeline steps.
 *
 * ### Jenkins configuration
 *
 * #### Jenkins plugins
 *
 * This file requires the following plugins in particular:
 *
 * - https://plugins.jenkins.io/pipeline-maven
 * - https://plugins.jenkins.io/ec2
 * - https://plugins.jenkins.io/lockable-resources
 * - https://plugins.jenkins.io/pipeline-github
 * - https://plugins.jenkins.io/email-ext
 * - https://plugins.jenkins.io/config-file-provider
 * - https://plugins.jenkins.io/pipeline-utility-steps
 *
 * #### Script approval
 *
 * Also you might need to allow the following calls in <jenkinsUrl>/scriptApproval/:
 *
 * - method java.util.Map putIfAbsent java.lang.Object java.lang.Object
 * - staticMethod org.jenkinsci.plugins.pipeline.modeldefinition.Utils markStageSkippedForConditional java.lang.String
 * - new java.lang.IllegalArgumentException java.lang.String
 * - method hudson.plugins.git.GitSCM getUserRemoteConfigs
 * - method hudson.plugins.git.UserRemoteConfig getUrl
 * - method java.lang.Throwable addSuppressed java.lang.Throwable
 *
 * Just run the script a few times, it will fail and display a link to allow these calls.
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
 * ### Job configuration
 *
 * This file gets its configuration from three sources: environment variables, a configuration file, and credentials.
 * All configuration is optional for the default build (and it should stay that way),
 * but some features require some configuration.
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
 * The job configuration file is optional. Its purpose is to host job-specific configuration, such as notification recipients.
 *
 * The file is named 'job-configuration.yaml', and it should be set up using the config file provider plugin
 * (https://plugins.jenkins.io/config-file-provider).
 * Expected structure of this file:
 *
 *     notification:
 *       email:
 *         # String containing a space-separated list of email addresses to notify in case of failing non-PR builds.
 *         recipients: ...
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

@Field final String MAVEN_LOCAL_REPOSITORY_RELATIVE = '.repository'
@Field final String MAVEN_TOOL = 'Apache Maven 3.5.4'

// Default node pattern, to be used for resource-intensive stages.
// Should not include the master node.
@Field final String NODE_PATTERN_BASE = 'Slave'
// Quick-use node pattern, to be used for very light, quick, and environment-independent stages,
// such as sending a notification. May include the master node in particular.
@Field final String QUICK_USE_NODE_PATTERN = 'Master||Slave'

@Field final List<JdkITEnvironment> jdkEnvs = [
		// This should not include every JDK; in particular let's not care too much about EOL'd JDKs like version 9
		// See http://www.oracle.com/technetwork/java/javase/eol-135779.html
		// TODO add support for JDK10/JDK11
		new JdkITEnvironment(version: '8', tool: 'Oracle JDK 8', status: ITEnvironmentStatus.USED_IN_DEFAULT_BUILD),
		new JdkITEnvironment(version: '10', tool: 'Oracle JDK 10.0.1', status: ITEnvironmentStatus.EXPERIMENTAL),
		new JdkITEnvironment(version: '11', tool: 'OpenJDK 11 Latest', status: ITEnvironmentStatus.EXPERIMENTAL)
]
@Field JdkITEnvironment defaultJdkEnv
@Field final List<DatabaseITEnvironment> databaseEnvs = [
		new DatabaseITEnvironment(dbName: 'h2', mavenProfile: 'h2', status: ITEnvironmentStatus.USED_IN_DEFAULT_BUILD),
		new DatabaseITEnvironment(dbName: 'mariadb', mavenProfile: 'ci-mariadb', status: ITEnvironmentStatus.SUPPORTED),
		new DatabaseITEnvironment(dbName: 'postgresql', mavenProfile: 'ci-postgresql', status: ITEnvironmentStatus.SUPPORTED)
]
@Field DatabaseITEnvironment defaultDatabaseEnv
@Field final List<EsLocalITEnvironment> esLocalEnvs = [
		// TODO add support for Elasticsearch 2? 5.0? 5.1?
		new EsLocalITEnvironment(versionRange: '[2.0,2.2)', mavenProfile: 'elasticsearch-2.0', status: ITEnvironmentStatus.EXPERIMENTAL),
		new EsLocalITEnvironment(versionRange: '[2.2,5.0)', mavenProfile: 'elasticsearch-2.2', status: ITEnvironmentStatus.EXPERIMENTAL),
		new EsLocalITEnvironment(versionRange: '[5.0,5.2)', mavenProfile: 'elasticsearch-5.0', status: ITEnvironmentStatus.EXPERIMENTAL),
		new EsLocalITEnvironment(versionRange: '[5.2,6.0)', mavenProfile: 'elasticsearch-5.2', status: ITEnvironmentStatus.USED_IN_DEFAULT_BUILD),
		new EsLocalITEnvironment(versionRange: '[6.0,6.x)', mavenProfile: 'elasticsearch-6.0', status: ITEnvironmentStatus.SUPPORTED)
]
@Field EsLocalITEnvironment defaultEsLocalEnv
@Field final List<EsAwsITEnvironment> esAwsEnvs = [
		// TODO add support for AWS (needs the plugin currently only available in Search 5)
		new EsAwsITEnvironment(version: '2.3', mavenProfile: 'elasticsearch-2.0', status: ITEnvironmentStatus.EXPERIMENTAL),
		new EsAwsITEnvironment(version: '5.1', mavenProfile: 'elasticsearch-5.0', status: ITEnvironmentStatus.EXPERIMENTAL),
		new EsAwsITEnvironment(version: '5.3', mavenProfile: 'elasticsearch-5.2', status: ITEnvironmentStatus.EXPERIMENTAL),
		new EsAwsITEnvironment(version: '5.5', mavenProfile: 'elasticsearch-5.2', status: ITEnvironmentStatus.EXPERIMENTAL),
		new EsAwsITEnvironment(version: '6.0', mavenProfile: 'elasticsearch-6.0', status: ITEnvironmentStatus.EXPERIMENTAL),
		new EsAwsITEnvironment(version: '6.2', mavenProfile: 'elasticsearch-6.0', status: ITEnvironmentStatus.EXPERIMENTAL)
]
@Field final List<List<? extends ITEnvironment>> allEnvironmentLists = [
		jdkEnvs, databaseEnvs, esLocalEnvs, esAwsEnvs
]

@Field boolean enableDefaultBuild = false
@Field boolean enableDefaultEnvIT = false
@Field boolean enableDefaultEnvLegacyIT = false
@Field boolean enableNonDefaultSupportedEnvIT = false
@Field boolean enableExperimentalEnvIT = false
@Field boolean performRelease = false
@Field boolean deploySnapshot = false

@Field def jobConfiguration = null
@Field ScmSource scmSource = null

@Field String releaseVersionFamily

Throwable mainScriptException = null
try { // Start of the code triggering notifications, see below. Not indenting for readability.

stage('Configure') {
	defaultJdkEnv = getDefaultEnv( jdkEnvs )
	defaultDatabaseEnv = getDefaultEnv( databaseEnvs )
	defaultEsLocalEnv = getDefaultEnv( esLocalEnvs )

	// Load the configuration specific to each job set up in Jenkins
	node(QUICK_USE_NODE_PATTERN) {
		jobConfiguration = loadYamlConfiguration('job-configuration.yaml')
		echo "Job configuration: $jobConfiguration"
	}

	scmSource = new ScmSource(env, scm)
	echo "SCM source: $scmSource"

	properties([
			pipelineTriggers([
					issueCommentTrigger('.*test this please.*')
			]),
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

	if (!performRelease && scmSource.branch.primary && !scmSource.pullRequest) {
		if (jobConfiguration?.deployment?.maven?.settingsId) {
			deploySnapshot = true
		}
		else {
			echo "Missing deployment configuration in job configuration file - snapshot deployment will be skipped."
		}
	}

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
			} else if (scmSource.pullRequest) {
				echo "Enabling only the default build and integration tests in the default environment for pull request $scmSource.pullRequest.id"
				enableDefaultEnvIT = true
			} else if (scmSource.branch.primary) {
				echo "Enabling integration tests on all supported environments for primary branch '$scmSource.branch.name'"
				enableDefaultBuild = true
				enableDefaultEnvIT = true
				enableNonDefaultSupportedEnvIT = true
				echo "Enabling legacy integration tests for primary branch '$scmSource.branch.name'"
				enableDefaultEnvLegacyIT = true
			} else {
				echo "Enabling only the default build and integration tests in the default environment for feature branch $scmSource.branch.name"
				enableDefaultBuild = true
				enableDefaultEnvIT = true
			}
			break
		default:
			throw new IllegalArgumentException(
					"Unknown value for param 'INTEGRATION_TESTS': '$params.INTEGRATION_TESTS'."
			)
	}

	if ( enableDefaultEnvIT && params.LEGACY_IT ) {
		echo "Enabling legacy integration tests in default environment due to explicit request"
		enableDefaultEnvLegacyIT = true
	}

	enableDefaultBuild =
			enableDefaultEnvIT || enableNonDefaultSupportedEnvIT || enableExperimentalEnvIT || deploySnapshot

	echo """Branch: ${scmSource.branch.name}, PR: ${scmSource.pullRequest?.id}, integration test setting: $params.INTEGRATION_TESTS, resulting execution plan:
enableDefaultBuild=$enableDefaultBuild
enableDefaultEnvIT=$enableDefaultEnvIT
enableNonDefaultSupportedEnvIT=$enableNonDefaultSupportedEnvIT
enableExperimentalEnvIT=$enableExperimentalEnvIT
enableDefaultEnvLegacyIT=$enableDefaultEnvLegacyIT
performRelease=$performRelease
deploySnapshot=$deploySnapshot"""

	allEnvironmentLists.each { envList ->
		// No need to re-test these environments, they are already tested as part of the default build
		envList.removeAll { itEnv -> itEnv.status == ITEnvironmentStatus.USED_IN_DEFAULT_BUILD }

		if (!enableNonDefaultSupportedEnvIT) {
			envList.removeAll { itEnv -> itEnv.status == ITEnvironmentStatus.SUPPORTED }
		}
		if (!enableExperimentalEnvIT) {
			envList.removeAll { itEnv -> itEnv.status == ITEnvironmentStatus.EXPERIMENTAL }
		}
	}

	esAwsEnvs.removeAll { itEnv ->
		itEnv.endpointUrl = env.getProperty(itEnv.endpointVariableName)
		if (!itEnv.endpointUrl) {
			echo "Skipping test ${itEnv.tag} because environment variable '${itEnv.endpointVariableName}' is not defined."
			return true
		}
		itEnv.awsRegion = env.ES_AWS_REGION
		if (!itEnv.awsRegion) {
			echo "Skipping test ${itEnv.tag} because environment variable 'ES_AWS_REGION' is not defined."
			return true
		}
		return false // Environment is fully defined, do not remove
	}

	/*
	 * There is something deeply wrong with collections in pipelines:
	 * - referencing a collection directly with interpolation ("$foo") sometimes results
	 *   in the echo statement not being executed at all, without even an exception
	 * - calling collection.toString() only prints the first element in the collection
	 * - calling flatten().join(', ') only prints the first element
	 * Thus the workaround below...
	 */
	String stringToPrint = ''
	allEnvironmentLists.each { envList ->
		envList.each {
			stringToPrint += ' ' + it.toString()
		}
	}
	if (stringToPrint) {
		echo "Enabled non-default environment ITs:$stringToPrint"
	}
	else {
		echo "Non-default environment ITs are completely disabled."
	}

	def versionPattern = ~/^(\d+)\.(\d+)(\.0\.Alpha\d+|\.0\.Beta\d+|\.0\.CR\d+|\.\d+\.Final)$/

	// Compute the version truncated to the minor component, a.k.a. the version family
	// To be used for the documentation upload in particular
	if (performRelease) {
		def matcher = (params.RELEASE_VERSION =~ versionPattern)
		if (!matcher.matches()) {
			throw new IllegalArgumentException(
					"Invalid version number: '$params.RELEASE_VERSION'. Version numbers must match /$versionPattern/"
			)
		}

		String major = matcher.group(1)
		String minor = matcher.group(2)
		releaseVersionFamily = "$major.$minor"
		echo "Inferred version family for the release to '$releaseVersionFamily'"

		// Check that all the necessary parameters are set
		if (!params.RELEASE_DEVELOPMENT_VERSION) {
			throw new IllegalArgumentException(
					"Missing value for parameter RELEASE_DEVELOPMENT_VERSION." +
							" This parameter must be set when RELEASE_VERSION is set."
			)
		}
	}

	def developmentVersionPattern = ~/^\d+\.\d+\.\d+-SNAPSHOT$/

	if (params.RELEASE_DEVELOPMENT_VERSION && !(params.RELEASE_DEVELOPMENT_VERSION ==~ developmentVersionPattern)) {
		throw new IllegalArgumentException(
				"Invalid development version number: '$params.RELEASE_DEVELOPMENT_VERSION'." +
						" Development version numbers must match /$developmentVersionPattern/"
		)
	}
}

stage('Default build') {
	if (!enableDefaultBuild) {
		echo 'Skipping default build and integration tests in the default environment'
		Utils.markStageSkippedForConditional(STAGE_NAME)
		return
	}
	node(NODE_PATTERN_BASE) {
		cleanWs()
		checkout scm
		withDefaultedMaven(mavenSettingsConfig: deploySnapshot ? jobConfiguration.deployment.maven.settingsId : null) {
			sh """ \\
					mvn clean \\
					${deploySnapshot ? """ \\
							deploy \\
					""" : """ \\
							install \\
					"""} \\
					-Pdist -Pcoverage -Pjqassistant \\
					${enableDefaultEnvIT ? '' : '-DskipITs'} \\
					${enableDefaultEnvLegacyIT ? '-Dsurefire.legacy.skip=false -Dfailsafe.legacy.skip=false' : ''}
			"""

			// Don't try to report to Coveralls.io or SonarCloud if coverage data is missing
			if ( enableDefaultEnvIT ) {
				try {
					withCredentials([string(credentialsId: 'coveralls-repository-token', variable: 'COVERALLS_TOKEN')]) {
						sh """ \\
								mvn coveralls:report \\
								-DrepoToken=${COVERALLS_TOKEN} \\
								${scmSource.pullRequest ? """ \\
										-DpullRequest=${scmSource.pullRequest.id} \\
								""" : """ \\
										-Dbranch=${scmSource.branch.name} \\
								"""} \\
						"""
					}
				}
				catch (CredentialNotFoundException e) {
					echo "No Coveralls token configured - skipping Coveralls report. Error was: ${e}"
				}

				if ( jobConfiguration?.sonar?.organization ) {
					def sonarOrganization = jobConfiguration.sonar.organization
					withCredentials([string(credentialsId: 'sonarcloud-hibernate-token', variable: 'SONARCLOUD_TOKEN')]) {
						sh """ \\
								mvn sonar:sonar \\
								-Dsonar.organization=${sonarOrganization} \\
								-Dsonar.host.url=https://sonarcloud.io \\
								-Dsonar.login=${SONARCLOUD_TOKEN} \\
								${scmSource.pullRequest ? """ \\
										-Dsonar.pullrequest.branch=${scmSource.branch.name} \\
										-Dsonar.pullrequest.key=${scmSource.pullRequest.id} \\
										-Dsonar.pullrequest.base=${scmSource.pullRequest.target.name} \\
										${scmSource.gitHubRepoId ? """ \\
												-Dsonar.pullrequest.provider=GitHub \\
												-Dsonar.pullrequest.github.repository=${scmSource.gitHubRepoId} \\
										""" : ''} \\
								""" : """ \\
										-Dsonar.branch.name=${scmSource.branch.name} \\
								"""} \\
						"""
					}
				}
			}

			dir("$env.WORKSPACE/$MAVEN_LOCAL_REPOSITORY_RELATIVE") {
				stash name:'main-build', includes:"org/hibernate/search/**"
			}
		}
	}
}

stage('Non-default environment ITs') {
	Map<String, Closure> executions = [:]

	// Test with multiple JDKs
	jdkEnvs.each { itEnv ->
		executions.put(itEnv.tag, {
			node(NODE_PATTERN_BASE) {
				cleanWs()
				withDefaultedMaven(jdk: itEnv.tool) {
					checkout scm
					mavenNonDefaultIT itEnv,
							"clean install --fail-at-end"
				}
			}
		})
	}

	// Test ORM integration with multiple databases
	databaseEnvs.each { itEnv ->
		executions.put(itEnv.tag, {
			node(NODE_PATTERN_BASE) {
				cleanWs()
				withDefaultedMaven {
					resumeFromDefaultBuild()
					mavenNonDefaultIT itEnv, """ \\
							clean install -pl org.hibernate.search:hibernate-search-integrationtest-orm -P$itEnv.mavenProfile
					"""
				}
			}
		})
	}

	// Test Elasticsearch integration with multiple versions in a local instance
	esLocalEnvs.each { itEnv ->
		executions.put(itEnv.tag, {
			node(NODE_PATTERN_BASE) {
				cleanWs()
				withDefaultedMaven {
					resumeFromDefaultBuild()
					mavenNonDefaultIT itEnv, """ \\
							clean install -pl org.hibernate.search:hibernate-search-integrationtest-backend-elasticsearch \\
							${toMavenElasticsearchProfileArg(itEnv.mavenProfile)} \\
					"""
				}
			}
		})
	}

	// Test Elasticsearch integration with multiple versions in an AWS instance
	esAwsEnvs.each { itEnv ->
		if (!itEnv.endpointUrl) {
			throw new IllegalStateException("Unexpected empty endpoint URL")
		}
		if (!itEnv.awsRegion) {
			throw new IllegalStateException("Unexpected empty AWS region")
		}
		executions.put(itEnv.tag, {
			lock(label: itEnv.lockedResourcesLabel) {
				node(NODE_PATTERN_BASE + '&&AWS') {
					cleanWs()
					withDefaultedMaven {
						resumeFromDefaultBuild()
						withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
										 credentialsId   : 'aws-elasticsearch',
										 usernameVariable: 'AWS_ACCESS_KEY_ID',
										 passwordVariable: 'AWS_SECRET_ACCESS_KEY'
						]]) {
							mavenNonDefaultIT itEnv, """ \\
								clean install -pl org.hibernate.search:hibernate-search-integrationtest-backend-elasticsearch \\
								${toMavenElasticsearchProfileArg(itEnv.mavenProfile)} \\
								-Dtest.elasticsearch.host.provided=true \\
								-Dtest.elasticsearch.host.url=$itEnv.endpointUrl \\
								-Dtest.elasticsearch.host.aws.access_key=$AWS_ACCESS_KEY_ID \\
								-Dtest.elasticsearch.host.aws.secret_key=$AWS_SECRET_ACCESS_KEY \\
								-Dtest.elasticsearch.host.aws.region=$itEnv.awsRegion
"""
						}
					}
				}
			}
		})
	}

	if (executions.isEmpty()) {
		echo 'Skipping integration tests in non-default environments'
		Utils.markStageSkippedForConditional(STAGE_NAME)
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
		echo "Performing full release for version ${params.RELEASE_VERSION}"
		node(NODE_PATTERN_BASE) {
			cleanWs()
			withDefaultedMaven {
				checkout scm

				sh "git clone https://github.com/hibernate/hibernate-noorm-release-scripts.git"
				sh "bash -xe hibernate-noorm-release-scripts/prepare-release.sh search ${params.RELEASE_VERSION}"

				if (!params.RELEASE_DRY_RUN) {
					sh "bash -xe hibernate-noorm-release-scripts/deploy.sh search"
				} else {
					echo "WARNING: Not deploying"
				}

				if (!params.RELEASE_DRY_RUN) {
					sh "bash -xe hibernate-noorm-release-scripts/upload-distribution.sh search ${params.RELEASE_VERSION}"
					sh "bash -xe hibernate-noorm-release-scripts/upload-documentation.sh search ${params.RELEASE_VERSION} ${releaseVersionFamily}"
				}
				else {
					echo "WARNING: Not uploading anything"
				}

				sh "bash -xe hibernate-noorm-release-scripts/update-version.sh search ${params.RELEASE_DEVELOPMENT_VERSION}"
				sh "bash -xe hibernate-noorm-release-scripts/push-upstream.sh search ${params.RELEASE_VERSION} ${scmSource.branch.name} ${!params.RELEASE_DRY_RUN}"
			}
		}
	}
	else {
		echo "Skipping deployment"
		Utils.markStageSkippedForConditional(STAGE_NAME)
		return
	}
}

} // End of the code triggering notifications
catch (any) {
	mainScriptException = any
	throw any
}
finally {
	/*
	 * Set the build result so that the email notifications correctly report it.
	 * We have to do it manually because Jenkins only does that automatically *after* the build.
	 */
	if (mainScriptException) {
		currentBuild.result = 'FAILURE'
	}
	else {
		currentBuild.result = 'SUCCESS'
	}
	
	try {
		notifyBuildEnd()
	}
	catch (notifyException) {
		if (mainScriptException != null) {
			// We are already throwing an exception, just register the new one as suppressed
			mainScriptException.addSuppressed(notifyException)
		}
		else {
			// We are not already throwing an exception, we can rethrow the new one
			throw notifyException
		}
	}
}

// Helpers

class ScmSource {
	final String remoteUrl
	final String gitHubRepoId
	final ScmBranch branch
	final ScmPullRequest pullRequest

	ScmSource(env, scm) {
		// See https://stackoverflow.com/a/38255364/6692043
		remoteUrl = scm.getUserRemoteConfigs()[0].getUrl()
		def gitHubUrlMatcher = (remoteUrl =~ /^(?:git@github.com:|https:\/\/github\.com\/)([^\/]+)\/([^.]+)\.git$/)
		if (gitHubUrlMatcher.matches()) {
			String owner = gitHubUrlMatcher.group(1)
			String name = gitHubUrlMatcher.group(2)
			gitHubRepoId = owner + '/' + name
		}
		else {
			gitHubRepoId = null
		}
		if (env.CHANGE_ID) {
			def source = new ScmBranch(name: env.CHANGE_BRANCH)
			def target = new ScmBranch(name: env.CHANGE_TARGET)
			pullRequest = new ScmPullRequest(id: env.CHANGE_ID, source: source, target: target)
			branch = source
		}
		else {
			branch = new ScmBranch(name: env.BRANCH_NAME)
			pullRequest = null
		}
	}

	String toString() {
		"ScmSource(remoteUrl: $remoteUrl, gitHubRepoId: $gitHubRepoId, branch: $branch, pullRequest: $pullRequest)"
	}
}

class ScmBranch {
	String name

	String toString() { "ScmBranch(name:$name)" }

	/**
	 * @return Whether this branch is "primary", i.e. it's either "master" or a maintenance branch.
	 * The purpose of primary branches is to hold the history of a major version of the code,
	 * whereas the only purpose  of "feature" branches is to eventually be merged into a primary branch.
	 */
	boolean isPrimary() {
		(name ==~ /master|\d+\.\d+/)
	}
}

class ScmPullRequest {
	String id
	ScmBranch source
	ScmBranch target

	String toString() { "ScmPullRequest(id: $id, source: $source, target: $target)" }
}

enum ITEnvironmentStatus {
	// For environments used as part of the integration tests in the default build (tested on all branches)
	USED_IN_DEFAULT_BUILD,
	// For environments that are expected to work correctly (tested on master and maintenance branches)
	SUPPORTED,
	// For environments that may not work correctly (only tested when explicitly requested through job parameters)
	EXPERIMENTAL
}

abstract class ITEnvironment {
	ITEnvironmentStatus status
	String toString() { getTag() }
	abstract String getTag()
}

class JdkITEnvironment extends ITEnvironment {
	ITEnvironmentStatus status
	String version
	String tool
	String getTag() { "jdk-$version" }
}

class DatabaseITEnvironment extends ITEnvironment {
	String dbName
	String mavenProfile
	String getTag() { "database-$dbName" }
}

class EsLocalITEnvironment extends ITEnvironment {
	String versionRange
	String mavenProfile
	String getTag() { "elasticsearch-local-$versionRange" }
}

class EsAwsITEnvironment extends ITEnvironment {
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

static <T extends ITEnvironment> T getDefaultEnv(List<T> envs) {
	return envs.find { it.status == ITEnvironmentStatus.USED_IN_DEFAULT_BUILD }
}

void withDefaultedMaven(Closure body) {
	withDefaultedMaven([:], body)
}

void withDefaultedMaven(Map args, Closure body) {
	args.putIfAbsent('jdk', defaultJdkEnv.tool)
	args.putIfAbsent('maven', MAVEN_TOOL)
	args.putIfAbsent('options', [artifactsPublisher(disabled: true)])
	args.putIfAbsent('mavenLocalRepo', "$env.WORKSPACE/$MAVEN_LOCAL_REPOSITORY_RELATIVE")
	withMaven(args, body)
}

void resumeFromDefaultBuild() {
	checkout scm
	dir("$env.WORKSPACE/$MAVEN_LOCAL_REPOSITORY_RELATIVE") {
		unstash name:'main-build'
	}
}

void mavenNonDefaultIT(ITEnvironment itEnv, String args) {
	// Add a suffix to tests to distinguish between different executions
	// of the same test in different environments in reports
	def testSuffix = itEnv.tag.replaceAll('[^a-zA-Z0-9_\\-+]+', '_')
	sh "mvn -Dsurefire.environment=$testSuffix $args"
}

String toMavenElasticsearchProfileArg(String mavenEsProfile) {
	if (mavenEsProfile != defaultEsLocalEnv.mavenProfile) {
		// Disable the default profile to avoid conflicting configurations
		"-P!$defaultEsLocalEnv.mavenProfile,$mavenEsProfile"
	}
	else {
		// Do not do as above, as we would tell Maven "disable the default profile, but enable it"
		// and Maven would end up disabling it.
		''
	}
}

def loadYamlConfiguration(String yamlConfigFileId) {
	try {
		configFileProvider([configFile(fileId: yamlConfigFileId, variable: "FILE_PATH")]) {
			return readYaml(file: FILE_PATH)
		}
	}
	catch (Exception e) {
		echo "Failed to load configuration file '$yamlConfigFileId'; assuming empty file. Exception was: $e"
		return [:]
	}
}

def notifyBuildEnd() {
	boolean success = currentBuild.result == 'SUCCESS'
	boolean successAfterSuccess = success &&
			currentBuild.previousBuild != null && currentBuild.previousBuild.result == 'SUCCESS'

	String explicitRecipients = null

	// Always notify people who explicitly requested a build
	def recipientProviders = [requestor()]

	// In case of failure, notify all the people who committed a change since the last non-broken build
	if (!success) {
		echo "Notification recipients: adding culprits()"
		recipientProviders.add(culprits())
	}
	// Always notify the author of the changeset, except in the case of a "success after a success"
	if (!successAfterSuccess) {
		echo "Notification recipients: adding developers()"
		recipientProviders.add(developers())
	}

	// Notify the notification recipients configured on the job,
	// except in the case of a PR build or of a "success after a success"
	if (!scmSource.pullRequest && !successAfterSuccess) {
		explicitRecipients = jobConfiguration?.notification?.email?.recipients
	}

	// See https://plugins.jenkins.io/email-ext#Email-extplugin-PipelineExamples
	emailext(
			subject: '${DEFAULT_SUBJECT}',
			body: '${DEFAULT_CONTENT}',
			recipientProviders: recipientProviders,
			to: explicitRecipients
	)
}
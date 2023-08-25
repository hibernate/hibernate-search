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
 * #### Job configuration file
 *
 * See the org.hibernate.(...) imports for a link to the helpers library documentation,
 * which explains the basic structure of this file and how to set it up.
 *
 * Below is the additional structure specific to this Jenkinsfile:
 *
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
@Field final String MAVEN_TOOL = 'Apache Maven 3.9'

// Default node pattern, to be used for resource-intensive stages.
// Should not include the controller node.
@Field final String NODE_PATTERN_BASE = 'Worker&&Containers'
// Quick-use node pattern, to be used for very light, quick, and environment-independent stages,
// such as sending a notification. May include the controller node in particular.
@Field final String QUICK_USE_NODE_PATTERN = 'Controller||Worker'

@Field JobHelper helper

@Field boolean deploySnapshot = false
@Field boolean incrementalBuild = false

this.helper = new JobHelper(this)

helper.runWithNotification {

stage('Configure') {
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
			helper.generateNotificationProperty()
	])

	if (helper.scmSource.branch.primary && !helper.scmSource.pullRequest) {
		if (helper.configuration.file?.deployment?.maven?.settingsId) {
			deploySnapshot = true
		}
		else {
			echo "Missing deployment configuration in job configuration file - snapshot deployment will be skipped."
		}
	}

	if (helper.scmSource.pullRequest) {
		incrementalBuild = true
	}

	echo """Branch: ${helper.scmSource.branch.name}
                PR: ${helper.scmSource.pullRequest?.id}
                
                Resulting execution plan:
                    jdk=$DEFAULT_JDK_TOOL
                    deploySnapshot=$deploySnapshot
                    incrementalBuild=$incrementalBuild
                """
}

stage('Pre-build sources') {
	// we want this stage to only be executed when we are planning to use incremental build
	if (!incrementalBuild) {
		echo 'Skipping pre-building sources for non-pull request builds.'
		helper.markStageSkipped()
		return
	}
	runBuildOnNode( NODE_PATTERN_BASE ) {
		withMavenWorkspace {
			sh """ \
					mvn clean install \
					--fail-at-end \
					-Pdist -Pjqassistant -Pci-sources-check \
					-DskipITs -DskipTests \
			"""

			dir(helper.configuration.maven.localRepositoryPath) {
				stash name:'default-build-result', includes:"org/hibernate/search/**"
			}
		}
	}
}

stage('Default build') {
	runBuildOnNode( NODE_PATTERN_BASE, [time: 2, unit: 'HOURS'] ) {
		withMavenWorkspace(mavenSettingsConfig: deploySnapshot ? helper.configuration.file.deployment.maven.settingsId : null) {
			if ( incrementalBuild ) {
				dir(helper.configuration.maven.localRepositoryPath) {
					unstash name:'default-build-result'
				}
			}
			// If we are in the pull request we've already run JQAssistant and source formatting checks
			String mavenArgs = """ \
					--fail-at-end \
					-Pdist -Pcoverage \
					${incrementalBuild ? ('-Dincremental -Dgib.referenceBranch=refs/remotes/origin/'+helper.scmSource.pullRequest.target.name) : '-Pjqassistant -Pci-sources-check'} \
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

			dir(helper.configuration.maven.localRepositoryPath) {
				stash name:'default-build-result', includes:"org/hibernate/search/**"
			}
		}
	}
}
} // End of helper.runWithNotification

// Job-specific helpers
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

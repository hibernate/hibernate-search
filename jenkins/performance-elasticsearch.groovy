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
@Library('hibernate-jenkins-pipeline-helpers@1.2')
import org.hibernate.jenkins.pipeline.helpers.job.JobHelper

@Field final String MAVEN_TOOL = 'Apache Maven 3.6'
@Field final String JDK_TOOL = 'OpenJDK 8 Latest'

// Performance node pattern, to be used for stages involving performance tests.
@Field final String PERFORMANCE_NODE_PATTERN = 'Performance'
// Quick-use node pattern, to be used for very light, quick, and environment-independent stages,
// such as sending a notification. May include the master node in particular.
@Field final String QUICK_USE_NODE_PATTERN = 'Master||Slave||Performance'

@Field JobHelper helper

@Field EsAwsBuildEnvironment esAwsBuildEnv = new EsAwsBuildEnvironment(version: "7.1")

this.helper = new JobHelper(this)

helper.runWithNotification {

stage('Configure') {
	helper.configure {
		configurationNodePattern QUICK_USE_NODE_PATTERN
		file 'job-configuration.yaml'
		jdk {
			defaultTool JDK_TOOL
		}
		maven {
			defaultTool MAVEN_TOOL
			producedArtifactPattern "org/hibernate/search/*"
		}
	}

	properties([
			pipelineTriggers(
					[
							issueCommentTrigger('.*test Elasticsearch performance please.*')
					]
			),
			helper.generateNotificationProperty()
	])

	esAwsBuildEnv.endpointUrl = env.getProperty(esAwsBuildEnv.endpointVariableName)
	if (!esAwsBuildEnv.endpointUrl) {
		throw new IllegalStateException(
				"Cannot run performance test because environment variable '$esAwsBuildEnv.endpointVariableName' is not defined."
		)
	}
	esAwsBuildEnv.awsRegion = env.ES_AWS_REGION
	if (!esAwsBuildEnv.awsRegion) {
		throw new IllegalStateException(
				"Cannot run performance test because environment variable 'ES_AWS_REGION' is not defined."
		)
	}
}

lock(label: esAwsBuildEnv.lockedResourcesLabel) {
	node ('Performance') {
		stage ('Checkout') {
			checkout scm
		}

		stage ('Build') {
			helper.withMavenWorkspace {
				sh """ \
						mvn clean install \
						-U -am -pl :hibernate-search-integrationtest-performance-backend-elasticsearch \
						-DskipTests \
				"""
				dir ('integrationtest/performance/backend/elasticsearch/target') {
					stash name:'jar', includes:'benchmarks.jar'
				}
			}
		}

		stage ('Performance test') {
			def awsCredentialsId = helper.configuration.file?.aws?.credentials
			if (!awsCredentialsId) {
				throw new IllegalStateException("Missing AWS credentials")
			}
			withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
							  credentialsId   : awsCredentialsId,
							  usernameVariable: 'AWS_ACCESS_KEY_ID',
							  passwordVariable: 'AWS_SECRET_ACCESS_KEY'
			]]) {
				helper.withMavenWorkspace { // Mainly to set the default JDK
					unstash name:'jar'
					sh 'mkdir output'
					sh """ \
							java \
							-jar benchmarks.jar \
							-jvmArgsAppend -Dhosts=$esAwsBuildEnv.endpointHostAndPort \
							-jvmArgsAppend -Dprotocol=$esAwsBuildEnv.endpointProtocol \
							-jvmArgsAppend -Daws.signing.enabled=true \
							-jvmArgsAppend -Daws.signing.access_key=$AWS_ACCESS_KEY_ID \
							-jvmArgsAppend -Daws.signing.secret_key=$AWS_SECRET_ACCESS_KEY \
							-jvmArgsAppend -Daws.signing.region=$esAwsBuildEnv.awsRegion \
							-wi 1 -i 10 \
							-rff output/benchmark-results-elasticsearch.csv \
					"""
				}
			}
			archiveArtifacts artifacts: 'output/**'
		}
	}
}

} // End of helper.runWithNotification

class EsAwsBuildEnvironment {
	String version
	String endpointUrl = null
	String endpointHostAndPort = null
	String endpointProtocol = null
	String awsRegion = null
	String getNameEmbeddableVersion() {
		version.replaceAll('\\.', '')
	}
	String getEndpointVariableName() {
		"ES_AWS_${nameEmbeddableVersion}_ENDPOINT"
	}
	String getLockedResourcesLabel() {
		"es-aws-${nameEmbeddableVersion}"
	}
	String setEndpointUrl(String url) {
		this.endpointUrl = url
		if ( endpointUrl ) {
			def matcher = endpointUrl =~ /^(?:(https?):\/\/)?(.*)$/
			endpointProtocol = matcher[0][1]
			endpointHostAndPort = matcher[0][2]
		}
		else {
			endpointProtocol = null
			endpointHostAndPort = null
		}
	}
}
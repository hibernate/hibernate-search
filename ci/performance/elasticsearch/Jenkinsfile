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

@Field final String MAVEN_TOOL = 'Apache Maven 3.8'
@Field final String JDK_TOOL = 'OpenJDK 11 Latest'

// Performance node pattern, to be used for stages involving performance tests.
@Field final String PERFORMANCE_NODE_PATTERN = 'Performance'
// Quick-use node pattern, to be used for very light, quick, and environment-independent stages,
// such as sending a notification. May include the controller node in particular.
@Field final String QUICK_USE_NODE_PATTERN = 'Controller||Worker||Performance'

@Field JobHelper helper

@Field EsAwsBuildEnvironment esAwsBuildEnv = new EsAwsBuildEnvironment(version: "7.10")

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

	if (!env.ES_AWS_REGION) {
		throw new IllegalStateException("Environment variable ES_AWS_REGION is not set")
	}
}

lock(label: esAwsBuildEnv.lockedResourcesLabel, variable: 'LOCKED_RESOURCE_URI') {
	node (PERFORMANCE_NODE_PATTERN) {
		stage ('Checkout') {
			checkout scm
		}

		stage ('Build') {
			helper.withMavenWorkspace {
				sh """ \
						mvn clean install \
						-U -am -pl :hibernate-search-integrationtest-performance-backend-elasticsearch \
						-DskipTests -DskipITs \
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
							-jvmArgsAppend -Duris=$env.LOCKED_RESOURCE_URI \
							-jvmArgsAppend -Daws.signing.enabled=true \
							-jvmArgsAppend -Daws.region=$env.ES_AWS_REGION \
							-jvmArgsAppend -Daws.credentials.type=static \
							-jvmArgsAppend -Daws.credentials.access_key_id=$AWS_ACCESS_KEY_ID \
							-jvmArgsAppend -Daws.credentials.secret_access_key=$AWS_SECRET_ACCESS_KEY \
							-e OnTheFlyIndexingBenchmarks.concurrentReadWrite \
							-e OnTheFlyIndexingBenchmarks.query \
							-wi 1 -i 10 \
							-prof org.hibernate.search.integrationtest.performance.backend.base.profiler.JfrProfiler:outputDir=output \
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
	String getNameEmbeddableVersion() {
		version.replaceAll('\\.', '-')
	}
	String getLockedResourcesLabel() {
		"es-aws-${nameEmbeddableVersion}"
	}
}
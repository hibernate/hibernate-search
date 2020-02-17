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
							issueCommentTrigger('.*test Lucene performance please.*')
					]
			),
			helper.generateNotificationProperty()
	])
}

node (PERFORMANCE_NODE_PATTERN) {
	stage ('Checkout') {
		checkout scm
	}

	stage ('Build') {
		helper.withMavenWorkspace {
			sh """ \
					mvn clean install \
					-U -am -pl :hibernate-search-integrationtest-performance-backend-lucene \
					-DskipTests \
			"""
			dir ('integrationtest/performance/backend/lucene/target') {
				stash name:'jar', includes:'benchmarks.jar'
			}
		}
	}

	stage ('Performance test') {
		helper.withMavenWorkspace { // Mainly to set the default JDK
			unstash name:'jar'
			sh 'mkdir output'
			sh """ \
					java \
					-jar benchmarks.jar \
					-wi 1 -i 10 \
					-rff output/benchmark-results-lucene.csv \
			"""
		}
		archiveArtifacts artifacts: 'output/**'
	}
}

} // End of helper.runWithNotification

node ('Slave') {
	stage ('Checkout') {
		checkout scm
	}
	
	stage ('Build') {
		sh "mvn clean package" +
				" -U -am -pl :hibernate-search-performance-engine-lucene,:hibernate-search-performance-engine-elasticsearch" +
				" -DskipTests -Dtest.elasticsearch.host.provided=true"
		dir('integrationtest/performance/engine-lucene/target/') {
			stash includes: 'benchmarks.jar', name: 'benchmarks-lucene'
		}
		dir('integrationtest/performance/engine-elasticsearch/target/') {
			stash includes: 'benchmarks.jar', name: 'benchmarks-elasticsearch'
		}
	}
}

stage ('Performance test') {
	parallel (
		'Lucene': {
			node ('!AWS&&Slave') {
				unstash 'benchmarks-lucene'
				sh "java -jar benchmarks.jar" +
						" -p directorytype=fs -p indexsize=100 -p maxResults=10"
			}
		},
		
		'Elasticsearch': {
			node ('AWS&&Slave') {
				unstash 'benchmarks-elasticsearch'
				withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-elasticsearch',
						usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY']]) {
					sh "java " +
							" -Dhost=${env.ES_ENDPOINT} -Daws.access-key=${AWS_ACCESS_KEY_ID}" +
							" -Daws.secret-key=${AWS_SECRET_ACCESS_KEY} -Daws.region=${env.AWS_REGION}" +
							" -jar benchmarks.jar " +
							" -p indexSize=1000 -p maxResults=100 -p changesetsPerFlush=500"
				}
			}
		}
	)
}

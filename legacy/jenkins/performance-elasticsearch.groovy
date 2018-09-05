node ('Performance') {
	stage ('Checkout') {
		checkout scm
	}
	
	stage ('Build') {
		sh "mvn clean install" +
				" -U -am -pl :hibernate-search-performance-engine-elasticsearch" +
				" -DskipTests -Dtest.elasticsearch.host.provided=true"
	}
	
	stage ('Performance test') {
		sh 'mkdir output'
		withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-elasticsearch',
				usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY']]) {
			sh "java " +
					" -Dhost=${env.ES_ENDPOINT} -Daws.access-key=${AWS_ACCESS_KEY_ID}" +
					" -Daws.secret-key=${AWS_SECRET_ACCESS_KEY} -Daws.region=${env.AWS_REGION}" +
					" -jar integrationtest/performance/engine-elasticsearch/target/benchmarks.jar " +
					" -wi 1 -i 10" +
					" -rff output/elasticsearch-benchmark-results.csv" +
					" -p indexSize=1000 -p maxResults=100 -p changesetsPerFlush=50 -p streamedAddsPerFlush=5000"
		}
		archiveArtifacts artifacts: 'output/*'
	}
}

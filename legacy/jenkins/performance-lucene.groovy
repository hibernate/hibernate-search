node ('Performance') {
	stage ('Checkout') {
		checkout scm
	}
	
	stage ('Build') {
		sh "mvn clean install" +
				" -U -am -pl :hibernate-search-performance-engine-lucene" +
				" -DskipTests"
	}
	
	stage ('Performance test') {
		unstash 'benchmarks-lucene'
		sh 'mkdir output'
		sh "java " +
				" -jar integrationtest/performance/engine-lucene/target/benchmarks.jar" +
				" -wi 1 -i 10" +
				" -rff output/lucene-benchmark-results.csv" +
				" -p directorytype=fs -p indexsize=100 -p maxResults=10"
		archiveArtifacts artifacts: 'output/*'
	}
}

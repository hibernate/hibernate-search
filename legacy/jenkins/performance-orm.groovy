node ('Performance') {
	stage ('Checkout') {
		checkout scm
	}
	
	stage ('Build') {
		sh "mvn clean install" +
				" -U -am -pl :hibernate-search-performance-orm" +
				" -DskipTests -Dtest.elasticsearch.host.provided=true"
	}
	
	def scenarios
	if (env.SCENARIOS) {
		scenarios = env.SCENARIOS.split(',')
	}
	else {
		scenarios = [
			'org.hibernate.search.test.performance.scenario.FileSystemReadWriteTestScenario',
			'org.hibernate.search.test.performance.scenario.FileSystemNearRealTimeReadWriteTestScenario',
			'org.hibernate.search.test.performance.scenario.FileSystemSessionMassIndexerTestScenario',
			'org.hibernate.search.test.performance.scenario.FileSystemJsr352MassIndexerTestScenario'
		] 
	}

	for (scenario in scenarios) {
		def simpleName = scenario.replaceFirst('^.*\\.', '').replaceFirst('TestScenario$', '')
		stage (simpleName) {
			// Do NOT clean, we want to keep all reports
			sh "mvn test" +
					" -pl :hibernate-search-performance-orm" +
					" -Pperf" +
					" -P${env.DB_PROFILE ?: 'ci-postgresql'}" +
					" -Dtest=${env.RUNNER ?: 'TestRunnerArquillian'}" +
					" -Dscenario=${scenario}"
		}
	}
	archiveArtifacts artifacts: 'integrationtest/performance/orm/target/report-*.txt'
}

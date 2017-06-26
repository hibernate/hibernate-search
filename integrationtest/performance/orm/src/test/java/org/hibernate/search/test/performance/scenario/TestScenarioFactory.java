/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.scenario;

import static org.hibernate.search.test.performance.util.Util.log;

/**
 * @author Tomas Hradec
 */
public class TestScenarioFactory {

	private TestScenarioFactory() {
	}

	public static TestScenario create() {
		String scenarioClassName = System.getProperty( "scenario" );
		if ( scenarioClassName == null ) {
			scenarioClassName = SmokeTestScenario.class.getName();
			printTestScenarioNames( "No test scenario was set, please use one of the following (via -Dscenario=...):" );
		}

		try {
			Class<?> scenarioClass = Class.forName( scenarioClassName );
			return TestScenario.class.cast( scenarioClass.newInstance() );
		}
		catch (Exception e) {
			printTestScenarioNames( "Unable to create specified test scenario with name " + scenarioClassName + ", please use one of the following:" );
			throw new RuntimeException( e );
		}
	}

	private static void printTestScenarioNames(String msg) {
		log( msg );
		log( "    " + FileSystemDefaultTestScenario.class.getName() );
		log( "    " + FileSystemNearRealTimeTestScenario.class.getName() );
	}

}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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

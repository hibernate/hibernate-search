/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.scenario;

import java.util.Properties;

/**
 * @author Tomas Hradec
 */
public class SmokeTestScenario extends ReadWriteTestScenario {

	@Override
	public Properties getHibernateProperties() {
		Properties properties = super.getHibernateProperties();
		properties.setProperty( "hibernate.search.default.directory_provider", "local-heap" );
		return properties;
	}

	@Override
	protected void initContext(TestContext testContext) {
		super.initContext( testContext );
		testContext.initialBookCount = 10;
		testContext.initialAuthorCount = 10;
	}
}

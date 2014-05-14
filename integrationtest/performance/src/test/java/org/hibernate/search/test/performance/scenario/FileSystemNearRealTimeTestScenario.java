/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.scenario;

import java.util.Properties;

import org.hibernate.search.testsupport.TestConstants;

/**
 * @author Tomas Hradec
 */
public class FileSystemNearRealTimeTestScenario extends TestScenario {

	@Override
	public Properties getHibernateProperties() {
		Properties properties = super.getHibernateProperties();
		properties.setProperty( "hibernate.search.default.directory_provider", "filesystem" );
		properties.setProperty( "hibernate.search.default.indexBase", TestConstants.getIndexDirectory( FileSystemNearRealTimeTestScenario.class ) );
		properties.setProperty( "hibernate.search.default.indexmanager", "near-real-time" );
		return properties;
	}

}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jmx;

import java.io.File;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.test.util.HibernateManualConfiguration;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;

import org.junit.Test;

/**
 * @author Hardy Ferentschik
 */
public class MutableSearchFactoryAndJMXTest {

	@Test
	public void testRebuildFactory() {
		File targetDir = TestConstants.getTargetDir( MutableSearchFactoryAndJMXTest.class );
		File simpleJndiDir = new File( targetDir, "simpleJndi" );
		simpleJndiDir.mkdir();

		SearchConfigurationForTest configuration = new HibernateManualConfiguration()
				.addProperty( "hibernate.session_factory_name", "java:comp/SessionFactory" )
				.addProperty( "hibernate.jndi.class", "org.osjava.sj.SimpleContextFactory" )
				.addProperty( "hibernate.jndi.org.osjava.sj.root", simpleJndiDir.getAbsolutePath() )
				.addProperty( "hibernate.jndi.org.osjava.sj.jndi.shared", "true" )
				.addProperty( Environment.JMX_ENABLED, "true" );

		new SearchIntegratorBuilder().configuration( configuration ).buildSearchIntegrator();

		// if there are problems with the JMX registration there will be an exception when the new factory is build
		new SearchIntegratorBuilder().configuration( configuration ).buildSearchIntegrator();
	}
}



/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jmx;

import java.io.File;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.jmx.IndexControlMBean;
import org.hibernate.search.jmx.StatisticsInfoMBean;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * @author Hardy Ferentschik
 */
public class NoMBeansEnabledTest extends SearchTestBase {
	MBeanServer mbeanServer;

	@Test
	public void testMBeanNotRegisteredWithoutExplicitProperty() throws Exception {
		mbeanServer = ManagementFactory.getPlatformMBeanServer();

		ObjectName name = new ObjectName( StatisticsInfoMBean.STATISTICS_MBEAN_OBJECT_NAME );
		assertFalse(
				"Without '" + Environment.JMX_ENABLED + "' set the configuration info MBean should not be registered",
				mbeanServer.isRegistered( name )
		);

		name = new ObjectName( IndexControlMBean.INDEX_CTRL_MBEAN_OBJECT_NAME );
		assertFalse(
				"Without '" + Environment.JMX_ENABLED + "' set the index control MBean should not be registered",
				mbeanServer.isRegistered( name )
		);
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		File targetDir = TestConstants.getTargetDir( NoMBeansEnabledTest.class );
		File simpleJndiDir = new File( targetDir, "simpleJndi" );
		simpleJndiDir.mkdir();

		cfg.setProperty( "hibernate.session_factory_name", "java:comp/SessionFactory" );
		cfg.setProperty( "hibernate.jndi.class", "org.osjava.sj.SimpleContextFactory" );
		cfg.setProperty( "hibernate.jndi.org.osjava.sj.root", simpleJndiDir.getAbsolutePath() );
		cfg.setProperty( "hibernate.jndi.org.osjava.sj.jndi.shared", "true" );
		// not setting the property is effectively the same as setting is explicitly to false
		// cfg.setProperty( Environment.JMX_ENABLED, "false" );
	}

	@Override
	@Before
	public void setUp() throws Exception {
		// make sure that no MBean is registered before the test runs
		mbeanServer = ManagementFactory.getPlatformMBeanServer();
		ObjectName statisticsBeanObjectName = new ObjectName( StatisticsInfoMBean.STATISTICS_MBEAN_OBJECT_NAME );
		if ( mbeanServer.isRegistered( statisticsBeanObjectName ) ) {
			mbeanServer.unregisterMBean( statisticsBeanObjectName );
		}
		ObjectName indexBeanObjectName = new ObjectName( IndexControlMBean.INDEX_CTRL_MBEAN_OBJECT_NAME );
		if ( mbeanServer.isRegistered( indexBeanObjectName ) ) {
			mbeanServer.unregisterMBean( indexBeanObjectName );
		}

		// build the new configuration
		forceConfigurationRebuild();
		super.setUp();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { };
	}
}

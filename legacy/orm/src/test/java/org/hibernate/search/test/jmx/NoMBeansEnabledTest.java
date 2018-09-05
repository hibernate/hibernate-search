/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jmx;

import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.jmx.IndexControlMBean;
import org.hibernate.search.jmx.StatisticsInfoMBean;
import org.hibernate.search.test.SearchTestBase;
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
	public void configure(Map<String,Object> cfg) {
		Path simpleJndiDir = SimpleJNDIHelper.makeTestingJndiDirectory( NoMBeansEnabledTest.class );
		SimpleJNDIHelper.enableSimpleJndi( cfg, simpleJndiDir );
		cfg.put( "hibernate.session_factory_name", "java:comp/SessionFactory" );
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

		super.setUp();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { };
	}
}

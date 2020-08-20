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
import org.hibernate.search.util.jmx.impl.JMXRegistrar;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-1026")
public class IndexControlMBeanWithSuffixTest extends SearchTestBase {

	private static final String JNDI_APP_SUFFIX = "myapp";

	MBeanServer mbeanServer;
	ObjectName indexBeanObjectName;

	@Test
	public void testIndexCtrlMBeanRegistered() throws Exception {
		assertTrue(
				"With the right property set the Search MBean should be registered",
				mbeanServer.isRegistered( indexBeanObjectName )
		);
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		mbeanServer = ManagementFactory.getPlatformMBeanServer();
		indexBeanObjectName = new ObjectName(
				JMXRegistrar.buildMBeanName(
						IndexControlMBean.INDEX_CTRL_MBEAN_OBJECT_NAME,
						JNDI_APP_SUFFIX
				)
		);
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		assertFalse(
				"The MBean should be unregistered",
				mbeanServer.isRegistered( indexBeanObjectName )
		);
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		Path jndiStorage = SimpleJNDIHelper.makeTestingJndiDirectory( IndexControlMBeanWithSuffixTest.class );
		SimpleJNDIHelper.enableSimpleJndi( cfg, jndiStorage );
		cfg.put( "hibernate.session_factory_name", "java:comp/SessionFactory" );
		cfg.put( "hibernate.jndi.org.osjava.sj.factory", "org.hibernate.search.test.jmx.IndexControlMBeanTest$CustomContextFactory" );
		cfg.put( "hibernate.search.indexing_strategy", "manual" );
		cfg.put( Environment.JMX_ENABLED, "true" );
		cfg.put( Environment.JMX_BEAN_SUFFIX, JNDI_APP_SUFFIX );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Counter.class };
	}
}

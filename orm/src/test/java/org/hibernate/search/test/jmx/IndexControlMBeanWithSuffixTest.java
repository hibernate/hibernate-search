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
import org.hibernate.search.jmx.impl.JMXRegistrar;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
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
		forceConfigurationRebuild();
		super.setUp();
		String suffix = getCfg().getProperty( Environment.JMX_BEAN_SUFFIX );
		mbeanServer = ManagementFactory.getPlatformMBeanServer();
		indexBeanObjectName = new ObjectName(
				JMXRegistrar.buildMBeanName(
						IndexControlMBean.INDEX_CTRL_MBEAN_OBJECT_NAME,
						suffix
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
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		File targetDir = TestConstants.getTargetDir( IndexControlMBeanWithSuffixTest.class );
		File simpleJndiDir = new File( targetDir, "simpleJndi" );
		simpleJndiDir.mkdir();

		cfg.setProperty( "hibernate.session_factory_name", "java:comp/SessionFactory" );
		cfg.setProperty( "hibernate.jndi.class", "org.osjava.sj.SimpleContextFactory" );
		cfg.setProperty(
				"hibernate.jndi.org.osjava.sj.factory",
				"org.hibernate.search.test.jmx.IndexControlMBeanTest$CustomContextFactory"
		);
		cfg.setProperty( "hibernate.jndi.org.osjava.sj.root", simpleJndiDir.getAbsolutePath() );
		cfg.setProperty( "hibernate.jndi.org.osjava.sj.jndi.shared", "true" );

		cfg.setProperty( "hibernate.search.indexing_strategy", "manual" );
		cfg.setProperty( Environment.JMX_ENABLED, "true" );
		cfg.setProperty( Environment.JMX_BEAN_SUFFIX, "myapp" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Counter.class };
	}
}

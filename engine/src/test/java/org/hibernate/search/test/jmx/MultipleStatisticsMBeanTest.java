/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jmx;

import java.io.File;
import java.lang.management.ManagementFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.jmx.StatisticsInfoMBean;
import org.hibernate.search.jmx.impl.JMXRegistrar;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.TestForIssue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-1026")
public class MultipleStatisticsMBeanTest {
	private static File simpleJndiDir;
	private static MBeanServer mbeanServer;

	@BeforeClass
	public static void beforeClass() {
		File targetDir = TestConstants.getTargetDir( MultipleStatisticsMBeanTest.class );
		simpleJndiDir = new File( targetDir, "simpleJndi" );
		simpleJndiDir.mkdir();
		mbeanServer = ManagementFactory.getPlatformMBeanServer();
	}

	@AfterClass
	public static void afterClass() {
		simpleJndiDir.delete();
	}

	@Test
	public void testDefaultRegistration() throws Exception {
		String suffix = null;
		try ( SearchIntegrator searchFactory = createSearchIntegratorUsingJndiPrefix( suffix ) ) {
			testStatisticsMBeanRegistered( suffix );
		}
	}

	@Test
	public void testRegistrationWithSuffix() throws Exception {
		String suffix = "myapp";
		try ( SearchIntegrator searchFactory = createSearchIntegratorUsingJndiPrefix( suffix ) ) {
			testStatisticsMBeanRegistered( suffix );
		}
	}

	@Test
	public void testMultipleMbeanRegistration() throws Exception {
		String suffixApp1 = "app-1";
		try ( SearchIntegrator factory1 = createSearchIntegratorUsingJndiPrefix( suffixApp1 ) ) {
			testStatisticsMBeanRegistered( suffixApp1 );

			String suffixApp2 = "app-2";
			try ( SearchIntegrator factory2 = createSearchIntegratorUsingJndiPrefix( suffixApp2 ) ) {
				testStatisticsMBeanRegistered( suffixApp2 );
			}
		}
	}

	@Test
	public void testStatisticMBeanGetsUnregistered() throws Exception {
		String suffix = "myapp";
		try ( SearchIntegrator searchFactory = createSearchIntegratorUsingJndiPrefix( suffix ) ) {
			testStatisticsMBeanRegistered( suffix );
		}

		testStatisticsMBeanUnregistered( suffix );
	}

	private void testStatisticsMBeanUnregistered(String suffix) throws Exception {
		String objectName = JMXRegistrar.buildMBeanName( StatisticsInfoMBean.STATISTICS_MBEAN_OBJECT_NAME, suffix );
		ObjectName statisticsBeanObjectName = new ObjectName( objectName );

		assertFalse( "The MBean should be unregistered", mbeanServer.isRegistered( statisticsBeanObjectName ) );
	}

	private void testStatisticsMBeanRegistered(String suffix) throws Exception {
		String objectName = JMXRegistrar.buildMBeanName( StatisticsInfoMBean.STATISTICS_MBEAN_OBJECT_NAME, suffix );
		ObjectName statisticsBeanObjectName = new ObjectName( objectName );


		ObjectInstance mBean = null;
		try {
			mBean = mbeanServer.getObjectInstance( statisticsBeanObjectName );
		}
		catch (InstanceNotFoundException e) {
			fail( "The mbean " + statisticsBeanObjectName.getCanonicalName() + " should be registered with the MBean server " );
		}
		assertEquals( JMXRegistrar.StatisticsInfo.class.getName(), mBean.getClassName() );
	}

	private SearchIntegrator createSearchIntegratorUsingJndiPrefix(String suffix) {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addProperty( "hibernate.session_factory_name", "java:comp/SessionFactory" )
				.addProperty( "hibernate.jndi.class", "org.osjava.sj.SimpleContextFactory" )
				.addProperty( "hibernate.jndi.org.osjava.sj.root", simpleJndiDir.getAbsolutePath() )
				.addProperty( "hibernate.jndi.org.osjava.sj.jndi.shared", "true" )
				.addProperty( Environment.JMX_ENABLED, "true" );

		if ( suffix != null ) {
			configuration.addProperty( Environment.JMX_BEAN_SUFFIX, suffix );
		}

		return new SearchIntegratorBuilder().configuration( configuration ).buildSearchIntegrator();
	}
}



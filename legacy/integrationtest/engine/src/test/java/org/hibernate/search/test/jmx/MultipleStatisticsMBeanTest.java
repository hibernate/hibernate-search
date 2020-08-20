/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.jmx.StatisticsInfoMBean;
import org.hibernate.search.util.jmx.impl.JMXRegistrar;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-1026")
public class MultipleStatisticsMBeanTest {
	private static Path simpleJndiDir;
	private static MBeanServer mbeanServer;

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	@BeforeClass
	public static void beforeClass() {
		simpleJndiDir = SimpleJNDIHelper.makeTestingJndiDirectory( MultipleStatisticsMBeanTest.class );
		mbeanServer = ManagementFactory.getPlatformMBeanServer();
	}

	@AfterClass
	public static void afterClass() {
		try {
			Files.delete( simpleJndiDir );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
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
				.addProperty( Environment.JMX_ENABLED, "true" )
				.addProperty( "hibernate.session_factory_name", "java:comp/SessionFactory" );
		SimpleJNDIHelper.enableSimpleJndi( configuration, simpleJndiDir );

		if ( suffix != null ) {
			configuration.addProperty( Environment.JMX_BEAN_SUFFIX, suffix );
		}

		return integratorResource.create( configuration );
	}
}



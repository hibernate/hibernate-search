/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.jmx;

import java.io.File;
import java.lang.management.ManagementFactory;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.hibernate.search.Environment;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.jmx.StatisticsInfo;
import org.hibernate.search.jmx.StatisticsInfoMBean;
import org.hibernate.search.jmx.impl.JMXRegistrar;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.test.util.ManualConfiguration;
import org.hibernate.search.test.util.TestForIssue;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;

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
		SearchFactoryImplementor searchFactory = createSearchFactoryUsingJndiPrefix( suffix );
		testStatisticsMBeanRegistered( suffix );
		searchFactory.close();
	}

	@Test
	public void testRegistrationWithSuffix() throws Exception {
		String suffix = "myapp";
		SearchFactoryImplementor searchFactory = createSearchFactoryUsingJndiPrefix( suffix );
		testStatisticsMBeanRegistered( suffix );
		searchFactory.close();
	}

	@Test
	public void testMultipleMbeanRegistration() throws Exception {
		String suffixApp1 = "app-1";
		SearchFactoryImplementor factory1 = createSearchFactoryUsingJndiPrefix( suffixApp1 );
		testStatisticsMBeanRegistered( suffixApp1 );

		String suffixApp2 = "app-2";
		SearchFactoryImplementor factory2 = createSearchFactoryUsingJndiPrefix( suffixApp2 );
		testStatisticsMBeanRegistered( suffixApp2 );

		factory1.close();
		factory2.close();
	}

	@Test
	public void testStatisticMBeanGetsUnregistered() throws Exception {
		String suffix = "myapp";

		SearchFactoryImplementor searchFactory = createSearchFactoryUsingJndiPrefix( suffix );
		testStatisticsMBeanRegistered( suffix );

		searchFactory.close();
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
		assertEquals( StatisticsInfo.class.getName(), mBean.getClassName() );
	}

	private SearchFactoryImplementor createSearchFactoryUsingJndiPrefix(String suffix) {
		ManualConfiguration configuration = new ManualConfiguration()
				.addProperty( "hibernate.search.default.directory_provider", "ram" )
				.addProperty( "hibernate.session_factory_name", "java:comp/SessionFactory" )
				.addProperty( "hibernate.jndi.class", "org.osjava.sj.SimpleContextFactory" )
				.addProperty( "hibernate.jndi.org.osjava.sj.root", simpleJndiDir.getAbsolutePath() )
				.addProperty( "hibernate.jndi.org.osjava.sj.jndi.shared", "true" )
				.addProperty( Environment.JMX_ENABLED, "true" );

		if ( suffix != null ) {
			configuration.addProperty( Environment.JMX_BEAN_SUFFIX, suffix );
		}

		return new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
	}
}



/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.jmx.IndexControlMBean;
import org.hibernate.search.jmx.StatisticsInfoMBean;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;

/**
 * @author Hardy Ferentschik
 */
public class NoMBeansEnabledTest extends SearchTestCase {
	MBeanServer mbeanServer;

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
	public void tearDown() throws Exception {
		super.tearDown();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { };
	}
}

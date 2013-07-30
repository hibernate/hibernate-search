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
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.jmx.IndexControlMBean;
import org.hibernate.search.jmx.impl.JMXRegistrar;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.test.util.TestForIssue;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-1026")
public class IndexControlMBeanWithSuffixTest extends SearchTestCase {
	MBeanServer mbeanServer;
	ObjectName indexBeanObjectName;

	public void testIndexCtrlMBeanRegistered() throws Exception {
		assertTrue(
				"With the right property set the Search MBean should be registered",
				mbeanServer.isRegistered( indexBeanObjectName )
		);
	}

	@Override
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

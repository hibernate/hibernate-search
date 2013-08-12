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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.osjava.sj.memory.MemoryContext;

import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.jmx.IndexControlMBean;
import org.hibernate.search.jmx.StatisticsInfoMBean;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;

/**
 * @author Hardy Ferentschik
 */
public class IndexControlMBeanTest extends SearchTestCase {
	MBeanServer mbeanServer;
	ObjectName statisticsBeanObjectName;
	ObjectName indexBeanObjectName;

	public void testIndexCtrlMBeanRegistered() throws Exception {
		assertTrue(
				"With the right property set the Search MBean should be registered",
				mbeanServer.isRegistered( indexBeanObjectName )
		);
	}

	public void testAttributesAndOperations() throws Exception {
		MBeanInfo info = mbeanServer.getMBeanInfo( indexBeanObjectName );
		MBeanAttributeInfo[] attributes = info.getAttributes();
		assertEquals( "Wrong number of attributes", 3, attributes.length );
		Set<String> attributeNames = new HashSet<String>();
		attributeNames.add( "NumberOfObjectLoadingThreads" );
		attributeNames.add( "NumberOfFetchingThreads" );
		attributeNames.add( "BatchSize" );
		for ( MBeanAttributeInfo attribute : attributes ) {
			assertTrue( attributeNames.contains( attribute.getName() ) );
		}

		MBeanOperationInfo[] operations = info.getOperations();
		assertEquals( "Wrong number of operations", 3, operations.length );
		Set<String> operationNames = new HashSet<String>();
		operationNames.add( "index" );
		operationNames.add( "purge" );
		operationNames.add( "optimize" );
		for ( MBeanOperationInfo operation : operations ) {
			assertTrue( operationNames.contains( operation.getName() ) );
		}
	}

	public void testIndexAndPurge() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		Counter counter = new Counter();
		s.save( counter );
		tx.commit();
		s.close();

		assertNumberOfIndexedEntities( Counter.class.getName(), 0 ); // manual indexing!

		mbeanServer.invoke(
				indexBeanObjectName,
				"index",
				new String[] { Counter.class.getName() },
				new String[] { String.class.getName() }
		);

		assertNumberOfIndexedEntities( Counter.class.getName(), 1 );

		mbeanServer.invoke(
				indexBeanObjectName,
				"purge",
				new String[] { Counter.class.getName() },
				new String[] { String.class.getName() }
		);

		assertNumberOfIndexedEntities( Counter.class.getName(), 0 );
	}

	@Override
	public void setUp() throws Exception {
		forceConfigurationRebuild();
		super.setUp();
		mbeanServer = ManagementFactory.getPlatformMBeanServer();
		statisticsBeanObjectName = new ObjectName( StatisticsInfoMBean.STATISTICS_MBEAN_OBJECT_NAME );
		indexBeanObjectName = new ObjectName( IndexControlMBean.INDEX_CTRL_MBEAN_OBJECT_NAME );
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		if ( mbeanServer.isRegistered( statisticsBeanObjectName ) ) {
			mbeanServer.unregisterMBean( statisticsBeanObjectName );
		}
		if ( mbeanServer.isRegistered( indexBeanObjectName ) ) {
			mbeanServer.unregisterMBean( indexBeanObjectName );
		}
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		File targetDir = TestConstants.getTargetDir( IndexControlMBeanTest.class );
		File simpleJndiDir = new File( targetDir, "simpleJndi" );
		simpleJndiDir.mkdir();

		cfg.setProperty( "hibernate.session_factory_name", "java:comp/SessionFactory" );
		cfg.setProperty( "hibernate.jndi.class", "org.osjava.sj.SimpleContextFactory" );
		cfg.setProperty( "hibernate.jndi.org.osjava.sj.factory", "org.hibernate.search.test.jmx.IndexControlMBeanTest$CustomContextFactory" );
		cfg.setProperty( "hibernate.jndi.org.osjava.sj.root", simpleJndiDir.getAbsolutePath() );
		cfg.setProperty( "hibernate.jndi.org.osjava.sj.jndi.shared", "true" );

		cfg.setProperty( "hibernate.search.indexing_strategy", "manual" );
		cfg.setProperty( Environment.JMX_ENABLED, "true" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Counter.class };
	}

	private void assertNumberOfIndexedEntities(String entity, int count)
			throws InstanceNotFoundException, MBeanException, ReflectionException {
		assertEquals(
				"wrong number of indexed entities", count,
				mbeanServer.invoke(
						statisticsBeanObjectName,
						"getNumberOfIndexedEntities",
						new String[] { entity },
						new String[] { String.class.getName() }
				)
		);
	}

	public static class CustomContextFactory implements InitialContextFactory {
		public CustomContextFactory() {
			super();
		}

		@Override
		public Context getInitialContext(Hashtable environment) throws NamingException {
			return new CloseNoOpMemoryContext( environment );
		}
	}

	public static class CloseNoOpMemoryContext extends MemoryContext {
		public CloseNoOpMemoryContext(Hashtable env) {
			super( env );
		}

		// see HSEARCH-802
		// see http://code.google.com/p/osjava/issues/detail?id=12
		// What is the intended semantics of Context#close()
		@Override
		public void close() {
			// noop
		}
	}
}

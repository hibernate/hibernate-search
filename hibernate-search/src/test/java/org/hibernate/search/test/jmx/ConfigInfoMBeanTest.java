/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 *  Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 *  indicated by the @author tags or express copyright attribution
 *  statements applied by the authors.  All third-party contributions are
 *  distributed under license by Red Hat, Inc.
 *
 *  This copyrighted material is made available to anyone wishing to use, modify,
 *  copy, or redistribute it subject to the terms and conditions of the GNU
 *  Lesser General Public License, as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this distribution; if not, write to:
 *  Free Software Foundation, Inc.
 *  51 Franklin Street, Fifth Floor
 *  Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.jmx;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.jmx.ConfigInfoMBean;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Hardy Ferentschik
 */
public class ConfigInfoMBeanTest extends SearchTestCase {
	MBeanServer mbeanServer;
	ObjectName configBeanObjectName;

	public void testConfigInfoMBeanRegistered() throws Exception {
		assertTrue(
				"With the right property set the Search MBean should be registered",
				mbeanServer.isRegistered( configBeanObjectName )
		);
	}

	public void testAttributesAndOperations() throws Exception {
		MBeanInfo info = mbeanServer.getMBeanInfo( configBeanObjectName );
		MBeanAttributeInfo[] attributes = info.getAttributes();
		assertEquals( "Wrong number of attributes", 3, attributes.length );
		Set<String> attributeNames = new HashSet<String>();
		attributeNames.add( "IndexedClassNames" );
		attributeNames.add( "IndexingStrategy" );
		attributeNames.add( "SearchVersion" );
		for ( MBeanAttributeInfo attribute : attributes ) {
			assertTrue( attributeNames.contains( attribute.getName() ) );
		}

		MBeanOperationInfo[] operations = info.getOperations();
		assertEquals( "Wrong number of operations", 3, operations.length );
		Set<String> operationNames = new HashSet<String>();
		operationNames.add( "getNumberOfIndexedEntities" );
		operationNames.add( "indexedEntitiesCount" );
		operationNames.add( "getIndexingParameters" );
		for ( MBeanOperationInfo operation : operations ) {
			assertTrue( operationNames.contains( operation.getName() ) );
		}
	}

	public void testIndexingStrategy() throws Exception {
		assertEquals(
				"wrong even type", "event", mbeanServer.getAttribute( configBeanObjectName, "IndexingStrategy" )
		);
	}

	public void testIndexedClassNames() throws Exception {
		assertEquals(
				"wrong class name",
				Counter.class.getName(),
				( ( Set ) mbeanServer.getAttribute( configBeanObjectName, "IndexedClassNames" ) ).iterator().next()
		);
	}


	public void testNumberOfIndexedEntities() throws Exception {
		assertNumberOfIndexedEntities( Counter.class.getName(), 0 );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		Counter counter = new Counter();
		s.save( counter );
		tx.commit();
		s.close();

		assertNumberOfIndexedEntities( Counter.class.getName(), 1 );
	}

	private void assertNumberOfIndexedEntities(String entity, int count)
			throws InstanceNotFoundException, MBeanException, ReflectionException {
		assertEquals(
				"wrong number of indexed entities", count,
				mbeanServer.invoke(
						configBeanObjectName,
						"getNumberOfIndexedEntities",
						new String[] { entity },
						new String[] { String.class.getName() }
				)
		);
	}

	protected void setUp() throws Exception {
		super.setUp();
		mbeanServer = ManagementFactory.getPlatformMBeanServer();
		configBeanObjectName = new ObjectName( ConfigInfoMBean.CONFIG_MBEAN_OBJECT_NAME );
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		if ( mbeanServer.isRegistered( configBeanObjectName ) ) {
			mbeanServer.unregisterMBean( configBeanObjectName );
		}
		setCfg( null ); // force a rebuild of the configuration
	}

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.JMX_ENABLED, "true" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Counter.class };
	}
}



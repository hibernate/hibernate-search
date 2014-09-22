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
package org.hibernate.search.infinispan.sharedIndex;

import static org.hibernate.search.infinispan.ClusterTestHelper.createClusterNode;
import static org.hibernate.search.infinispan.ClusterTestHelper.waitMembersCount;
import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.infinispan.ClusterSharedConnectionProvider;
import org.hibernate.search.infinispan.impl.InfinispanDirectoryProvider;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.spi.SearchFactoryIntegrator;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test to verify HSEARCH-926
 *
 * @author Zach Kurey
 */
public class SharedIndexTest {
	FullTextSessionBuilder node;
	HashSet<Class<?>> entityTypes;

	@Test
	public void testSingleResultFromDeviceIndex() {
		assertEquals( 1, clusterSize( node, Toaster.class ) );
		// index an entity:
		{
			FullTextSession fullTextSession = node.openFullTextSession();
			Transaction transaction = fullTextSession.beginTransaction();
			Toaster toaster = new Toaster( "A1" );
			fullTextSession.save( toaster );
			transaction.commit();
			fullTextSession.close();
			verifyResult( node );
		}
	}

	private void verifyResult(FullTextSessionBuilder node) {
		FullTextSession fullTextSession = node.openFullTextSession();
		try {
			Transaction transaction = fullTextSession.beginTransaction();
			QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder()
					.forEntity( Toaster.class ).get();
			Query query = queryBuilder.keyword().onField( "serialNumber" ).matching( "A1" ).createQuery();
			List list = fullTextSession.createFullTextQuery( query ).list();
			assertEquals( 1, list.size() );
			Device device = (Device) list.get( 0 );

			assertEquals( "GE", device.manufacturer );
			transaction.commit();
		}
		finally {
			fullTextSession.close();
		}
	}

	@Before
	public void setUp() throws Exception {
		entityTypes = new HashSet<Class<?>>();
		entityTypes.add( Device.class );
		entityTypes.add( Robot.class );
		entityTypes.add( Toaster.class );
		node = createClusterNode( entityTypes, true );
		waitMembersCount( node, Toaster.class, 1 );
	}

	@After
	public void tearDown() throws Exception {
		if ( node != null ) {
			node.close();
		}
	}

	@BeforeClass
	public static void prepareConnectionPool() {
		ClusterSharedConnectionProvider.realStart();
	}

	@AfterClass
	public static void shutdownConnectionPool() {
		ClusterSharedConnectionProvider.realStop();
	}

	/**
	 * Counts the number of nodes in the cluster on this node
	 *
	 * @param node
	 *            the FullTextSessionBuilder representing the current node
	 * @return
	 */
	protected int clusterSize(FullTextSessionBuilder node, Class<?> entityType) {
		SearchFactoryIntegrator searchFactory = (SearchFactoryIntegrator) node.getSearchFactory();
		EntityIndexBinding indexBinding = searchFactory.getIndexBinding( Toaster.class );
		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) indexBinding.getIndexManagers()[0];
		InfinispanDirectoryProvider directoryProvider = (InfinispanDirectoryProvider) indexManager.getDirectoryProvider();
		EmbeddedCacheManager cacheManager = directoryProvider.getCacheManager();
		List<Address> members = cacheManager.getMembers();
		return members.size();
	}
}

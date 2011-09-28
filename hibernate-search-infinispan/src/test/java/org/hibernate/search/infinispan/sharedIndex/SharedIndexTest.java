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
 * 
 */
package org.hibernate.search.infinispan.sharedIndex;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.AssertionFailedError;

import org.apache.lucene.search.Query;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.infinispan.CacheManagerServiceProvider;
import org.hibernate.search.infinispan.InfinispanDirectoryProvider;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests to verify HSEARCH-926
 * 
 * @author Zach Kurey
 */
public class SharedIndexTest {
	FullTextSessionBuilder node;

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

		Set<Class<?>> annotatedTypes = new HashSet<Class<?>>();
		annotatedTypes.add( Device.class );
		annotatedTypes.add( Robot.class );
		annotatedTypes.add( Toaster.class );

		node = new FullTextSessionBuilder();
		prepareCommonConfiguration( node, annotatedTypes );
		node.build();
		waitMembersCount( node, Toaster.class, 1 );
	}

	@After
	public void tearDown() throws Exception {
		node.close();
	}

	protected void prepareCommonConfiguration(FullTextSessionBuilder cfg, Set<Class<?>> entityTypes) {
		cfg.setProperty( "hibernate.search.default.directory_provider", "infinispan" );
		cfg.setProperty( CacheManagerServiceProvider.INFINISPAN_CONFIGURATION_RESOURCENAME,
				"testing-hibernatesearch-infinispan.xml" );
		for ( Class<?> type : entityTypes ) {
			cfg.addAnnotatedClass( type );
		}
	}

	/**
	 * Wait some time for the cluster to form
	 */
	protected void waitMembersCount(FullTextSessionBuilder node, Class<?> entityType, int expectedSize)
			throws InterruptedException {
		int currentSize = 0;
		int loopCounter = 0;
		while ( currentSize < expectedSize ) {
			Thread.sleep( 10 );
			currentSize = clusterSize( node, entityType );
			if ( loopCounter > 200 ) {
				throw new AssertionFailedError( "timeout while waiting for all nodes to join in cluster" );
			}
		}
	}

	/**
	 * Counts the number of nodes in the cluster on this node
	 * 
	 * @param node
	 *            the FullTextSessionBuilder representing the current node
	 * @return
	 */
	protected int clusterSize(FullTextSessionBuilder node, Class<?> entityType) {
		SearchFactory searchFactory = node.getSearchFactory();
		DirectoryProvider[] directoryProviders = searchFactory.getDirectoryProviders( entityType );
		assertEquals( 1, directoryProviders.length );
		InfinispanDirectoryProvider directoryProvider = (InfinispanDirectoryProvider) directoryProviders[0];
		EmbeddedCacheManager cacheManager = directoryProvider.getCacheManager();
		List<Address> members = cacheManager.getMembers();
		return members.size();
	}
}

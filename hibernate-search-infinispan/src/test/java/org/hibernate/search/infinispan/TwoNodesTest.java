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
package org.hibernate.search.infinispan;

import java.util.List;

import junit.framework.AssertionFailedError;
import org.apache.lucene.search.Query;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.test.util.FullTextSessionBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * We start two different Hibernate Search instances, both using
 * an InfinispanDirectoryProvider as the default DirectoryProvider
 * for all entities.
 * Then after some indexing was done we verify the index contains expected data
 * both on the other node and on a third just started node.
 * 
 * Set <code>-Djava.net.preferIPv4Stack=true</code> as this is required by JGroups.
 *
 * @author Sanne Grinovero
 */
public class TwoNodesTest {

	private final String to = "spam@hibernate.org";
	private final String messageText = "to get started as a real spam expert, search for 'getting an iphone' on Hibernate forums";

	FullTextSessionBuilder nodea;
	FullTextSessionBuilder nodeb;

	@Test
	public void testSomething() {
		assertEquals( 2, clusterSize( nodea ) );
		// index an entity:
		{
			FullTextSession fullTextSession = nodea.openFullTextSession();
			Transaction transaction = fullTextSession.beginTransaction();
			SimpleEmail mail = new SimpleEmail();
			mail.to = to;
			mail.message = messageText;
			fullTextSession.save( mail );
			transaction.commit();
			fullTextSession.close();
		}
		// verify nodeb is able to find it:
		verifyNodeSeesUpdatedIndex( nodeb );
		// now start a new node, it will join the cluster and receive the current index state:
		FullTextSessionBuilder nodeC = new FullTextSessionBuilder();
		prepareCommonConfiguration( nodeC );
		nodeC.build();
		assertEquals( 3, clusterSize( nodea ) );
		try {
			// verify the new node is able to perform the same searches:
			verifyNodeSeesUpdatedIndex(nodeC);
		}
		finally {
			nodeC.close();
		}
		assertEquals( 2, clusterSize( nodea ) );
		verifyNodeSeesUpdatedIndex( nodea );
		verifyNodeSeesUpdatedIndex( nodeb );
	}

	private void verifyNodeSeesUpdatedIndex(FullTextSessionBuilder node) {
		FullTextSession fullTextSession = node.openFullTextSession();
		try {
			Transaction transaction = fullTextSession.beginTransaction();
			QueryBuilder queryBuilder = fullTextSession.getSearchFactory()
					.buildQueryBuilder()
					.forEntity( SimpleEmail.class )
					.get();
			Query query = queryBuilder.keyword()
					.onField( "message" )
					.matching( "Hibernate Getting Started" )
					.createQuery();
			List list = fullTextSession.createFullTextQuery( query ).setProjection( "message" ).list();
			assertEquals( 1, list.size() );
			Object[] result = (Object[]) list.get( 0 );
			assertEquals( messageText, result[0] );
			transaction.commit();
		}
		finally {
			fullTextSession.close();
		}
	}

	@Before
	public void setUp() throws Exception {
		nodea = new FullTextSessionBuilder();
		nodeb = new FullTextSessionBuilder();
		prepareCommonConfiguration( nodea );
		nodea.build();
		prepareCommonConfiguration( nodeb );
		nodeb.build();

		waitMembersCount( nodea, 2 );
	}

	/**
	 * Wait some time for the cluster to form
	 */
	private void waitMembersCount(FullTextSessionBuilder node, int expectedSize) throws InterruptedException {
		int currentSize = 0;
		int loopCounter = 0;
		while ( currentSize < expectedSize ) {
			Thread.sleep( 10 );
			currentSize = clusterSize( node );
			if ( loopCounter > 200 ) {
				throw new AssertionFailedError( "timeout while waiting for all nodes to join in cluster" );
			}
		}
	}

	/**
	 * Counts the number of nodes in the cluster on this node
	 * @param node the FullTextSessionBuilder representing the current node
	 * @return
	 */
	private int clusterSize(FullTextSessionBuilder node) {
		SearchFactory searchFactory = node.getSearchFactory();
		DirectoryProvider[] directoryProviders = searchFactory.getDirectoryProviders( SimpleEmail.class );
		InfinispanDirectoryProvider directoryProvider = (InfinispanDirectoryProvider) directoryProviders[0];
		EmbeddedCacheManager cacheManager = directoryProvider.getCacheManager();
		List<Address> members = cacheManager.getMembers();
		return members.size();
	}

	private void prepareCommonConfiguration(FullTextSessionBuilder cfg) {
		cfg.setProperty( "hibernate.search.default.directory_provider", "infinispan" );
		cfg.setProperty(
				CacheManagerServiceProvider.INFINISPAN_CONFIGURATION_RESOURCENAME,
				"testing-hibernatesearch-infinispan.xml"
		);
		cfg.addAnnotatedClass( SimpleEmail.class );
	}

	@After
	public void tearDown() throws Exception {
		nodea.close();
		nodeb.close();
	}
}

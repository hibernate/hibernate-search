/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.infinispan;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.search.infinispan.ClusterTestHelper.clusterSize;
import static org.hibernate.search.infinispan.ClusterTestHelper.createClusterNode;
import static org.hibernate.search.infinispan.ClusterTestHelper.waitMembersCount;

import java.util.HashSet;
import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * We start two different Hibernate Search instances, both using
 * an InfinispanDirectoryProvider as the default DirectoryProvider
 * for all entities.
 * Then after some indexing was done we verify the index contains expected data
 * both on the other node and on a third just started node.
 *
 * @author Sanne Grinovero
 */
public class TwoNodesTest {

	private final String to = "spam@hibernate.org";
	private final String messageText = "to get started as a real spam expert, search for 'getting an iphone' on Hibernate forums";

	FullTextSessionBuilder nodea;
	FullTextSessionBuilder nodeb;
	HashSet<Class<?>> entityTypes;

	@Test
	public void testSomething() {
		assertEquals( 2, clusterSize( nodea, SimpleEmail.class ) );
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
		FullTextSessionBuilder nodeC = createClusterNode( entityTypes, true );
		assertEquals( 3, clusterSize( nodea, SimpleEmail.class ) );
		try {
			// verify the new node is able to perform the same searches:
			verifyNodeSeesUpdatedIndex( nodeC );
		}
		finally {
			nodeC.close();
		}
		assertEquals( 2, clusterSize( nodea, SimpleEmail.class ) );
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
		entityTypes = new HashSet<Class<?>>();
		entityTypes.add( SimpleEmail.class );
		nodea = createClusterNode( entityTypes, true );
		nodeb = createClusterNode( entityTypes, true );
		waitMembersCount( nodea, SimpleEmail.class, 2 );
	}

	@After
	public void tearDown() throws Exception {
		if ( nodea != null ) {
			nodea.close();
		}
		if ( nodeb != null ) {
			nodeb.close();
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
}

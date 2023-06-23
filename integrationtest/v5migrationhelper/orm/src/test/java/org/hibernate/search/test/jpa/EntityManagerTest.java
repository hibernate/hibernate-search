/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jpa;

import static org.junit.Assert.assertEquals;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;

import org.junit.Before;
import org.junit.Test;

import org.apache.lucene.document.IntPoint;
import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
public class EntityManagerTest extends JPATestCase {
	private FullTextEntityManager em;
	private Bretzel bretzel;

	@Before
	@Override
	public void setUp() {
		super.setUp();

		em = Search.getFullTextEntityManager( factory.createEntityManager() );
		em.getTransaction().begin();
		bretzel = new Bretzel( 23, 34 );
		em.persist( bretzel );
		em.getTransaction().commit();
		em.clear();
	}

	@Test
	public void testMassIndexer() throws Exception {
		// verify against index
		assertEquals( "At the beginning of the test there should be an indexed Bretzel", 1, countBretzelsViaIndex( em ) );

		// clear index
		em.purgeAll( Bretzel.class );
		em.flushToIndexes();

		// verify Bretzel removed from index
		assertEquals( "The index should be empty after an purge and flush", 0, countBretzelsViaIndex( em ) );

		// re-index
		em.createIndexer( Bretzel.class ).startAndWait();

		assertEquals( "After re-indexing the bretzel should be indexed again", 1, countBretzelsViaIndex( em ) );
	}

	@Test
	public void testNonMatchingQueryDoesReturnEmptyResults() throws Exception {
		em.getTransaction().begin();

		Query query = IntPoint.newExactQuery( "saltQty", 0 );
		assertEquals( 0, em.createFullTextQuery( query ).getResultList().size() );

		em.getTransaction().commit();
	}

	@Test
	public void testGetResultList() throws Exception {
		em.getTransaction().begin();

		Query query = IntPoint.newExactQuery( "saltQty", 23 );
		assertEquals( "getResultList should return a result", 1, em.createFullTextQuery( query ).getResultList().size() );

		em.getTransaction().commit();
	}

	@Test
	public void testGetSingleResult() throws Exception {
		em.getTransaction().begin();

		Query query = IntPoint.newExactQuery( "saltQty", 23 );
		assertEquals(
				"getSingleResult should return a result", 23,
				( (Bretzel) em.createFullTextQuery( query ).getSingleResult() ).getSaltQty()
		);
		em.getTransaction().commit();
	}

	@Test
	public void testGetResultSize() throws Exception {
		em.getTransaction().begin();

		Query query = IntPoint.newExactQuery( "saltQty", 23 );
		assertEquals( "Wrong result size", 1, em.createFullTextQuery( query ).getResultSize() );

		em.getTransaction().commit();
	}

	@Test
	public void testIndex() {
		em.getTransaction().begin();

		// verify against index
		assertEquals( "At the beginning of the test there should be an indexed Bretzel", 1, countBretzelsViaIndex( em ) );

		// clear index
		em.purgeAll( Bretzel.class );
		em.flushToIndexes();

		// verify Bretzel removed from index
		assertEquals( "The index should be empty after an purge and flush", 0, countBretzelsViaIndex( em ) );

		// re-index manually
		em.index( em.find( Bretzel.class, bretzel.getId() ) );
		em.getTransaction().commit();

		assertEquals( "After re-indexing the bretzel should be indexed again", 1, countBretzelsViaIndex( em ) );
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Bretzel.class
		};
	}

	private int countBretzelsViaIndex(FullTextEntityManager em) {
		QueryBuilder queryBuilder = em.getSearchFactory().buildQueryBuilder().forEntity( Bretzel.class ).get();
		Query allQuery = queryBuilder.all().createQuery();
		FullTextQuery fullTextQuery = em.createFullTextQuery( allQuery, Bretzel.class );
		return fullTextQuery.getResultSize();
	}
}

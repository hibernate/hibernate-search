/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jpa;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.junit.Test;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.test.SerializationTestHelper;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.TestForIssue;

import static org.junit.Assert.assertEquals;

/**
 * Serialization test for entity manager.
 *
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-117")
public class EntityManagerSerializationTest extends JPATestCase {

	/**
	 * Test that a entity manager can successfully be serialized and deserialized.
	 *
	 * @throws Exception in case the test fails.
	 */
	@Test
	public void testSerialization() throws Exception {
		FullTextEntityManager em = Search.getFullTextEntityManager( factory.createEntityManager() );

		indexSearchAssert( em );

		FullTextEntityManager clone = SerializationTestHelper.duplicateBySerialization( em );

		indexSearchAssert( clone );

		clone.close();
		em.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { Bretzel.class };
	}

	/**
	 * Helper method for testing the entity manager before and after
	 * serialization.
	 *
	 * @param em Entity manager used for indexing and searching
	 *
	 * @throws Exception
	 */
	private static void indexSearchAssert(FullTextEntityManager em) throws Exception {
		// index a Bretzel
		em.getTransaction().begin();
		Bretzel bretzel = new Bretzel( 23, 34 );
		em.persist( bretzel );
		em.getTransaction().commit();
		em.clear();
		em.getTransaction().begin();

		// execute a non matching query
		QueryParser parser = new QueryParser(
				TestConstants.getTargetLuceneVersion(),
				"title",
				TestConstants.stopAnalyzer
		);
		Query query = NumericRangeQuery.newIntRange( "saltQty", 0, 0, true, true );
		assertEquals( 0, em.createFullTextQuery( query ).getResultList().size() );

		// execute a matching query
		query = NumericRangeQuery.newIntRange( "saltQty", 23, 23, true, true );
		assertEquals(
				"getResultList", 1, em.createFullTextQuery( query )
				.getResultList().size()
		);
		assertEquals(
				"getSingleResult and object retrieval", 23, ( (Bretzel) em
				.createFullTextQuery( query )
				.getSingleResult() )
				.getSaltQty()
		);
		assertEquals( 1, em.createFullTextQuery( query ).getResultSize() );
		em.getTransaction().commit();

		em.clear();

		em.getTransaction().begin();
		em.remove( em.find( Bretzel.class, bretzel.getId() ) );
		em.getTransaction().commit();
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.lucene.document.IntPoint;
import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
class EntityManagerTest extends JPATestCase {
	private FullTextEntityManager em;
	private Bretzel bretzel;

	@BeforeEach
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
	void testMassIndexer() throws Exception {
		// verify against index
		assertThat( countBretzelsViaIndex( em ) ).as( "At the beginning of the test there should be an indexed Bretzel" )
				.isEqualTo( 1 );

		// clear index
		em.purgeAll( Bretzel.class );
		em.flushToIndexes();

		// verify Bretzel removed from index
		assertThat( countBretzelsViaIndex( em ) ).as( "The index should be empty after an purge and flush" ).isZero();

		// re-index
		em.createIndexer( Bretzel.class ).startAndWait();

		assertThat( countBretzelsViaIndex( em ) ).as( "After re-indexing the bretzel should be indexed again" ).isEqualTo( 1 );
	}

	@Test
	void testNonMatchingQueryDoesReturnEmptyResults() {
		em.getTransaction().begin();

		Query query = IntPoint.newExactQuery( "saltQty", 0 );
		assertThat( em.createFullTextQuery( query ).getResultList() ).isEmpty();

		em.getTransaction().commit();
	}

	@Test
	void testGetResultList() {
		em.getTransaction().begin();

		Query query = IntPoint.newExactQuery( "saltQty", 23 );
		assertThat( em.createFullTextQuery( query ).getResultList() ).as( "getResultList should return a result" ).hasSize( 1 );

		em.getTransaction().commit();
	}

	@Test
	void testGetSingleResult() {
		em.getTransaction().begin();

		Query query = IntPoint.newExactQuery( "saltQty", 23 );
		assertThat(
				( (Bretzel) em.createFullTextQuery( query ).getSingleResult() ).getSaltQty()
		).as( "getSingleResult should return a result" ).isEqualTo( 23 );
		em.getTransaction().commit();
	}

	@Test
	void testGetResultSize() {
		em.getTransaction().begin();

		Query query = IntPoint.newExactQuery( "saltQty", 23 );
		assertThat( em.createFullTextQuery( query ).getResultSize() ).as( "Wrong result size" ).isEqualTo( 1 );

		em.getTransaction().commit();
	}

	@Test
	void testIndex() {
		em.getTransaction().begin();

		// verify against index
		assertThat( countBretzelsViaIndex( em ) ).as( "At the beginning of the test there should be an indexed Bretzel" )
				.isEqualTo( 1 );

		// clear index
		em.purgeAll( Bretzel.class );
		em.flushToIndexes();

		// verify Bretzel removed from index
		assertThat( countBretzelsViaIndex( em ) ).as( "The index should be empty after an purge and flush" ).isZero();

		// re-index manually
		em.index( em.find( Bretzel.class, bretzel.getId() ) );
		em.getTransaction().commit();

		assertThat( countBretzelsViaIndex( em ) ).as( "After re-indexing the bretzel should be indexed again" ).isEqualTo( 1 );
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

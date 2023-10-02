/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded.sorting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.Tags;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

/**
 * @author Davide D'Alto
 */
@TestForIssue(jiraKey = "HSEARCH-2069")
class EmbeddedSortableIdFieldTest extends SearchTestBase {

	private static final String LEX = "Lex Luthor";
	private static final String DARKSEID = "Darkseid";
	private static final String CLARK = "Clark Kent";

	@BeforeEach
	void before() {
		try ( Session session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			Villain darkseid = new Villain( 100, DARKSEID );
			Villain lex = new Villain( Integer.MIN_VALUE, LEX );
			Hero superman = new Hero( Integer.MAX_VALUE, CLARK );

			superman.setVillain( lex );
			lex.setHero( superman );

			superman.setSortableVillain( darkseid );
			darkseid.setHero( superman );

			session.save( superman );
			session.save( lex );

			transaction.commit();
		}
	}

	@Test
	void testSortingOnSortableFieldIncludedByIndexEmbedded() {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			QueryBuilder qb = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( Hero.class ).get();
			Transaction transaction = fullTextSession.beginTransaction();

			// This should be of type Integer
			Sort sort = qb.sort().byField( "sortableVillain.id_sort" ).createSort();

			QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( Hero.class ).get();
			Query q = queryBuilder.keyword().onField( "villain.name" ).matching( LEX ).createQuery();
			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( q, Hero.class );
			fullTextQuery.setSort( sort );
			List list = fullTextQuery.list();
			assertThat( list ).hasSize( 1 );

			Hero actual = (Hero) list.get( 0 );
			assertThat( actual.getSecretIdentity() ).isEqualTo( CLARK );
			transaction.commit();
		}
	}

	@Test
	@Tag(Tags.ELASTICSEARCH_SUPPORT_IN_PROGRESS) // HSEARCH-2398 Improve field name/type validation when querying the Elasticsearch backend
	void testSortingOnSortableFieldNotIncludedByIndexEmbeddedException() {
		assertThatThrownBy( () -> {
			try ( Session session = openSession() ) {
				FullTextSession fullTextSession = Search.getFullTextSession( session );
				QueryBuilder qb = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( Hero.class ).get();
				Transaction transaction = fullTextSession.beginTransaction();

				Sort sort = qb.sort().byField( "villain.id_sort" ).createSort();

				QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity(
						Hero.class ).get();
				Query q = queryBuilder.keyword().onField( "villain.name" ).matching( LEX ).createQuery();
				FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( q, Hero.class );
				fullTextQuery.setSort( sort );
				fullTextQuery.list();
				transaction.commit();
			}
		} ).isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unknown field 'villain.id_sort'" );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Hero.class, Villain.class };
	}

}

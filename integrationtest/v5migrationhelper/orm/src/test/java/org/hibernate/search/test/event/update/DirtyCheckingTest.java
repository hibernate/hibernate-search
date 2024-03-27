/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.event.update;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.jupiter.api.Test;

import org.apache.lucene.search.Query;

/**
 * @author Davide D'Alto
 * @author Sanne Grinovero
 */
@TestForIssue(jiraKey = "HSEARCH-1999")
class DirtyCheckingTest extends SearchTestBase {

	@Test
	void testName() {
		try ( FullTextSession s = Search.getFullTextSession( openSession() ) ) {
			Transaction tx = s.beginTransaction();
			{
				CheeseRollingCompetitor cheeser = new CheeseRollingCompetitor();
				cheeser.id = 1;
				cheeser.nickname = "Jimmy Fontina";
				s.persist( cheeser );
			}
			tx.commit();
			s.clear();

			assertExists( s, "Jimmy", "Jimmy Fontina" );

			tx = s.beginTransaction();
			CheeseRollingCompetitor johnny = s.get( CheeseRollingCompetitor.class, 1 );
			johnny.nickname = "Johnny Fontina";
			tx.commit();
			s.clear();

			assertExists( s, "Johnny", "Johnny Fontina" );

			tx = s.beginTransaction();
			s.delete( johnny );
			tx.commit();
		}
	}

	private void assertExists(FullTextSession s, String keyword, String expectedProjection) {
		QueryBuilder queryBuilder = s.getSearchFactory().buildQueryBuilder().forEntity( CheeseRollingCompetitor.class ).get();
		Query q = queryBuilder.keyword().onField( "Nickname" ).matching( keyword ).createQuery();

		FullTextQuery fullTextQuery = s.createFullTextQuery( q, CheeseRollingCompetitor.class ).setProjection( "Nickname" );
		List list = fullTextQuery.list();
		assertThat( list ).hasSize( 1 );
		assertThat( ( (Object[]) list.get( 0 ) )[0] ).isEqualTo( expectedProjection );

		s.clear();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { CheeseRollingCompetitor.class };
	}
}

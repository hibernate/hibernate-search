/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.event.update;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.Test;

import org.apache.lucene.search.Query;

/**
 * @author Davide D'Alto
 * @author Sanne Grinovero
 */
@TestForIssue(jiraKey = "HSEARCH-1999")
public class DirtyCheckingTest extends SearchTestBase {

	@Test
	public void testName() throws Exception {
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
		assertEquals( 1, list.size() );
		assertEquals( expectedProjection, ( (Object[]) list.get( 0 ) )[0] );

		s.clear();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { CheeseRollingCompetitor.class };
	}
}

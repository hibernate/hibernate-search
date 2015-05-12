/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.boost.embeddable;

import org.apache.lucene.search.Query;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Tests around boosting of embeddable fields.
 *
 * @author Gunnar Morling
 */
public class EmbeddedFieldBoostTest extends SearchTestBase {

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1804")
	public void testBoostedIndexEmbeddedEntity() {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		// Given
		Transaction tx = fullTextSession.beginTransaction();

		Magazine highPerfComputing = new Magazine( 1L, "High-perf trends", new Title( "High Performance Computing" ) );
		fullTextSession.persist( highPerfComputing );

		Magazine roseGrowers = new Magazine( 2L, null, new Title( "Rose Grower's Weekly" ) );
		fullTextSession.persist( roseGrowers );

		Magazine astronautDigest = new Magazine( 3L, null, new Title( "Astronaut Digest", "Tips for astronauts" ) );
		fullTextSession.persist( astronautDigest );

		tx.commit();
		fullTextSession.clear();

		tx = fullTextSession.beginTransaction();

		final QueryBuilder queryBuilder = fullTextSession.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Magazine.class )
				.get();

		// When
		Query query = queryBuilder.bool()
				.should(
						queryBuilder.keyword()
								.onField( "title.value" )
								.matching( "rose" )
								.createQuery()
				)
				.should(
						queryBuilder.keyword()
								.onField( "description" )
								.matching( "trends" )
								.createQuery()
				)
				.should(
						queryBuilder.keyword()
								.onField( "title.subTitle.value" )
								.matching( "tips" )
								.createQuery()
				)
				.createQuery();

		// Then
		assertThat( fullTextSession.createFullTextQuery( query, Magazine.class ).list() )
				.describedAs( "Query results are not in the order expected as per configured field boosts" )
				.onProperty( "id" )
				.containsExactly( 2L, 3L, 1L );

		tx.commit();

		fullTextSession.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Magazine.class };
	}
}

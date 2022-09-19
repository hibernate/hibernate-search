/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jpa;

import static org.junit.Assert.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.Before;
import org.junit.Test;

import org.apache.lucene.search.Query;
import org.hamcrest.CoreMatchers;

/**
 * @author Hardy Ferentschik
 */
public class ToStringTest extends JPATestCase {
	FullTextEntityManager entityManager;
	FullTextSession fullTextSession;
	Query luceneQuery;


	@Before
	public void setup() {
		entityManager = Search.getFullTextEntityManager( factory.createEntityManager() );
		Session session = entityManager.unwrap( Session.class );
		fullTextSession = org.hibernate.search.Search.getFullTextSession( session );

		QueryBuilder builder = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( Foo.class ).get();
		luceneQuery = builder.keyword()
				.onField( "fubar" )
				.matching( "snafu" )
				.createQuery();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1173")
	public void testToStringContainsQueryInformationForSessionUseCase() throws Exception {
		org.hibernate.search.FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(
				luceneQuery, Foo.class
		);
		assertToStringContainsLuceneQueryInformation( fullTextQuery.toString() );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1173")
	public void testToStringContainsQueryInformationForJPAUseCase() throws Exception {
		org.hibernate.search.jpa.FullTextQuery fullTextQuery = entityManager.createFullTextQuery(
				luceneQuery, Foo.class
		);
		assertToStringContainsLuceneQueryInformation( fullTextQuery.toString() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Foo.class };
	}

	private void assertToStringContainsLuceneQueryInformation(String fullTextQueryToString) {
		assertThat(
				"Unexpected toString implementation. The string should contain a string representation of the internal query.",
				fullTextQueryToString, CoreMatchers.containsString( luceneQuery.toString() ) );
	}

	@Entity
	@Indexed
	public static class Foo {
		@Id
		@GeneratedValue
		private long id;

		@Field
		private String fubar;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.jpa;

import static org.assertj.core.api.Assertions.assertThat;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.lucene.search.Query;

/**
 * @author Hardy Ferentschik
 */
class ToStringTest extends JPATestCase {
	FullTextEntityManager entityManager;
	FullTextSession fullTextSession;
	Query luceneQuery;


	@BeforeEach
	void setup() {
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
	void testToStringContainsQueryInformationForSessionUseCase() {
		org.hibernate.search.FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(
				luceneQuery, Foo.class
		);
		assertToStringContainsLuceneQueryInformation( fullTextQuery.toString() );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1173")
	void testToStringContainsQueryInformationForJPAUseCase() {
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
				fullTextQueryToString )
				.as( "Unexpected toString implementation. The string should contain a string representation of the internal query." )
				.contains( luceneQuery.toString() );
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

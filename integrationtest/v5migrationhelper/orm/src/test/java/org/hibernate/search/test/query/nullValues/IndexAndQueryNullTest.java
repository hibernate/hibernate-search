/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.query.nullValues;

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

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * Tests for indexing and querying {@code null} values.
 *
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-115")
class IndexAndQueryNullTest extends SearchTestBase {

	@Test
	void testIndexAndSearchNull() {
		Value fooValue = new Value( "foo" );
		Value nullValue = new Value( null );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		getSession().save( fooValue );
		getSession().save( nullValue );
		tx.commit();

		fullTextSession.clear();
		tx = fullTextSession.beginTransaction();

		searchKeywordWithExpectedNumberOfResults( fullTextSession, "value", "foo", 1 );
		searchKeywordWithExpectedNumberOfResults( fullTextSession, "value", "_custom_token_", 1 );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testNullIndexingWithDSLQuery() {
		Value fooValue = new Value( "foo" );
		Value nullValue = new Value( null );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		getSession().save( fooValue );
		getSession().save( nullValue );
		tx.commit();

		fullTextSession.clear();
		tx = fullTextSession.beginTransaction();

		QueryBuilder queryBuilder = getSearchFactory().buildQueryBuilder().forEntity( Value.class ).get();
		Query query = queryBuilder.keyword().onField( "value" ).matching( "_custom_token_" ).createQuery();
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Value.class );

		@SuppressWarnings("unchecked")
		List<Value> valueList = fullTextQuery.list();
		assertThat( valueList ).as( "Wrong number of results" ).hasSize( 1 );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testIndexAndSearchConfiguredDefaultNullToken() {
		Value fooValue = new Value( "foo" );
		fooValue.setFallback( "foo" );
		Value nullValue = new Value( "bar" );
		nullValue.setFallback( null );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		getSession().save( fooValue );
		getSession().save( nullValue );
		tx.commit();

		fullTextSession.clear();
		tx = fullTextSession.beginTransaction();

		searchKeywordWithExpectedNumberOfResults( fullTextSession, "fallback", "foo", 1 );
		searchKeywordWithExpectedNumberOfResults( fullTextSession, "fallback", "fubar", 1 );

		tx.commit();
		fullTextSession.close();
	}

	private void searchKeywordWithExpectedNumberOfResults(FullTextSession fullTextSession, String fieldName, String termValue,
			int expectedNumberOfResults) {
		TermQuery query = new TermQuery( new Term( fieldName, termValue ) );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Value.class );
		@SuppressWarnings("unchecked")
		List<Value> valueList = fullTextQuery.list();
		assertThat( valueList ).as( "Wrong number of results" ).hasSize( expectedNumberOfResults );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Value.class,
		};
	}

	enum QueryType {
		USE_LUCENE_QUERY,
		USE_DSL_QUERY
	}
}

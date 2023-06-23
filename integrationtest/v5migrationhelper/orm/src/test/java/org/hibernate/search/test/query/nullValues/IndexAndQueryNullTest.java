/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.query.nullValues;

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

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * Tests for indexing and querying {@code null} values.
 *
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-115")
public class IndexAndQueryNullTest extends SearchTestBase {

	@Test
	public void testIndexAndSearchNull() throws Exception {
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
	public void testNullIndexingWithDSLQuery() throws Exception {
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
		assertEquals( "Wrong number of results", 1, valueList.size() );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testIndexAndSearchConfiguredDefaultNullToken() throws Exception {
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
			int expectedNumberOfResults)
			throws Exception {
		TermQuery query = new TermQuery( new Term( fieldName, termValue ) );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Value.class );
		@SuppressWarnings("unchecked")
		List<Value> valueList = fullTextQuery.list();
		assertEquals( "Wrong number of results", expectedNumberOfResults, valueList.size() );
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

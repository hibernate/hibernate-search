/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.query.nullValues;

import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Transaction;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.query.ProjectionToMapResultTransformer;
import org.hibernate.search.testsupport.TestConstants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for indexing and querying {@code null} values. See HSEARCH-115
 *
 * @author Hardy Ferentschik
 */
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

		searchKeywordWithExpectedNumberOfResults( fullTextSession, "foo", 1 );
		searchKeywordWithExpectedNumberOfResults( fullTextSession, "_custom_token_", 1 );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testLuceneDocumentContainsNullToken() throws Exception {
		Value nullValue = new Value( null );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		getSession().save( nullValue );
		tx.commit();

		fullTextSession.clear();
		tx = fullTextSession.beginTransaction();

		Document document = getSingleIndexedDocument( fullTextSession, QueryType.USE_LUCENE_QUERY );

		String indexedNullString = document.get( "value" );
		String expectedString = "_custom_token_";
		assertEquals( "The null value should be indexed as " + expectedString, expectedString, indexedNullString );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testNullIndexingWithDSLQuery() throws Exception {
		Value nullValue = new Value( null );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		getSession().save( nullValue );
		tx.commit();

		fullTextSession.clear();
		tx = fullTextSession.beginTransaction();

		Document document = getSingleIndexedDocument( fullTextSession, QueryType.USE_DSL_QUERY );

		String indexedNullString = document.get( "value" );

		String expectedString = "_custom_token_";
		assertEquals( "The null value should be indexed as " + expectedString, expectedString, indexedNullString );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testNullIndexingWithDSLQueryIgnoringFieldBridge() throws Exception {
		try {
			QueryBuilder queryBuilder = getSearchFactory().buildQueryBuilder().forEntity( Value.class ).get();
			queryBuilder.keyword().onField( "value" ).ignoreFieldBridge().matching( null ).createQuery();
			fail();
		}
		catch (SearchException e) {
			// success
		}
	}

	@Test
	public void testProjectedValueGetsConvertedToNull() throws Exception {
		Value nullValue = new Value( null );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		getSession().save( nullValue );
		tx.commit();

		fullTextSession.clear();
		tx = fullTextSession.beginTransaction();

		Query query = createLuceneQuery();
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Value.class );
		fullTextQuery.setProjection(
				"id",
				"value"
		);
		fullTextQuery.setResultTransformer( new ProjectionToMapResultTransformer() );
		List mappedResults = fullTextQuery.list();
		assertTrue( "Wrong result size", mappedResults.size() == 1 );

		Map map = (Map) mappedResults.get( 0 );
		Integer id = (Integer) map.get( "id" );
		assertNotNull( id );

		String value = (String) map.get( "value" );
		assertEquals( "The null token should be converted back to null", null, value );

		tx.commit();
		fullTextSession.close();
	}


	@Test
	public void testConfiguredDefaultNullToken() throws Exception {
		Value nullValue = new Value( null );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		getSession().save( nullValue );
		tx.commit();

		fullTextSession.clear();
		tx = fullTextSession.beginTransaction();

		Document document = getSingleIndexedDocument( fullTextSession, QueryType.USE_LUCENE_QUERY );

		String indexedNullString = document.get( "fallback" );

		String expectedString = "fubar";
		assertEquals( "The null value should be indexed as " + expectedString, expectedString, indexedNullString );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testNullIndexingWithCustomFieldBridge() throws Exception {
		Value nullValue = new Value( null );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		getSession().save( nullValue );
		tx.commit();

		fullTextSession.clear();
		tx = fullTextSession.beginTransaction();

		Document document = getSingleIndexedDocument( fullTextSession, QueryType.USE_LUCENE_QUERY );

		String indexedNullString = document.get( "dummy" );

		String expectedString = "_dummy_";
		assertEquals( "The null value should be indexed as " + expectedString, expectedString, indexedNullString );

		tx.commit();
		fullTextSession.close();
	}

	private Document getSingleIndexedDocument(FullTextSession fullTextSession, QueryType type) throws ParseException {
		Query query;
		if ( QueryType.USE_LUCENE_QUERY.equals( type ) ) {
			query = createLuceneQuery();
		}
		else {
			query = createDSLQuery();
		}

		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Value.class );
		fullTextQuery.setProjection(
				FullTextQuery.DOCUMENT
		);
		fullTextQuery.setResultTransformer( new ProjectionToMapResultTransformer() );
		List mappedResults = fullTextQuery.list();
		assertTrue( "Wrong result size", mappedResults.size() == 1 );

		Map map = (Map) mappedResults.get( 0 );
		Document document = (Document) map.get( FullTextQuery.DOCUMENT );
		assertNotNull( document );
		return document;
	}

	private Query createLuceneQuery() throws ParseException {
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
		parser.setAllowLeadingWildcard( true );
		return parser.parse( "*" );
	}

	private Query createDSLQuery() {
		QueryBuilder queryBuilder = getSearchFactory().buildQueryBuilder().forEntity( Value.class ).get();
		return queryBuilder.keyword().onField( "value" ).matching( null ).createQuery();
	}

	private void searchKeywordWithExpectedNumberOfResults(FullTextSession fullTextSession, String queryString, int expectedNumberOfResults)
			throws Exception {
		TermQuery query = new TermQuery( new Term( "value", queryString ) );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Value.class );
		@SuppressWarnings("unchecked")
		List<Value> valueList = fullTextQuery.list();
		assertEquals( "Wrong number of results", expectedNumberOfResults, valueList.size() );
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default_null_token", "fubar" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Value.class,
		};
	}

	enum QueryType {
		USE_LUCENE_QUERY,
		USE_DSL_QUERY
	}
}

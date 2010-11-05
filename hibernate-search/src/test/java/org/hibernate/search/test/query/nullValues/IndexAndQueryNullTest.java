/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.test.query.nullValues;

import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.query.ProjectionToMapResultTransformer;

/**
 * Tests for indexing and querying {@code null} values. See HSEARCh-115
 *
 * @author Hardy Ferentschik
 */
public class IndexAndQueryNullTest extends SearchTestCase {

	public void testIndexAndSearchNull() throws Exception {
		Value fooValue = new Value( "foo" );
		Value nullValue = new Value( null );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		session.save( fooValue );
		session.save( nullValue );
		tx.commit();

		fullTextSession.clear();
		tx = fullTextSession.beginTransaction();

		searchKeywordWithExpectedNumberOfResults( fullTextSession, "foo", 1 );
		searchKeywordWithExpectedNumberOfResults( fullTextSession, "_null_", 1 );

		tx.commit();
		fullTextSession.close();
	}

	public void testLuceneDocumentContainsNullToken() throws Exception {
		Value nullValue = new Value( null );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		session.save( nullValue );
		tx.commit();

		fullTextSession.clear();
		tx = fullTextSession.beginTransaction();

		Document document = getSingleIndexedDocument( fullTextSession );

		String indexedNullString = document.get( "value" );
		assertEquals( "The null value should be indexed as _null_", "_null_", indexedNullString );

		tx.commit();
		fullTextSession.close();
	}

	public void testProjectedValueGetsConvertedToNull() throws Exception {
		Value nullValue = new Value( null );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		session.save( nullValue );
		tx.commit();

		fullTextSession.clear();
		tx = fullTextSession.beginTransaction();

		QueryParser parser = new QueryParser( getTargetLuceneVersion(), "id", SearchTestCase.standardAnalyzer );
		parser.setAllowLeadingWildcard( true );
		Query query = parser.parse( "*" );
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


	public void testConfiguredDefaultNullToken() throws Exception {
		Value nullValue = new Value( null );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		session.save( nullValue );
		tx.commit();

		fullTextSession.clear();
		tx = fullTextSession.beginTransaction();

		Document document = getSingleIndexedDocument( fullTextSession );

		String indexedNullString = document.get( "fallback" );
		assertEquals( "The null value should be indexed as 'fubar'", "fubar", indexedNullString );

		tx.commit();
		fullTextSession.close();
	}

	public void testNullIndexingWithCustomFieldBridge() throws Exception {
		Value nullValue = new Value( null );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		session.save( nullValue );
		tx.commit();

		fullTextSession.clear();
		tx = fullTextSession.beginTransaction();

		Document document = getSingleIndexedDocument( fullTextSession );

		String indexedNullString = document.get( "dummy" );
		assertEquals( "The null value should be indexed as '_dummy_'", "_dummy_", indexedNullString );

		tx.commit();
		fullTextSession.close();
	}

	private Document getSingleIndexedDocument(FullTextSession fullTextSession) throws ParseException {
		QueryParser parser = new QueryParser( getTargetLuceneVersion(), "id", SearchTestCase.standardAnalyzer );
		parser.setAllowLeadingWildcard( true );
		Query query = parser.parse( "*" );
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


	private void searchKeywordWithExpectedNumberOfResults(FullTextSession fullTextSession, String queryString, int expectedNumberOfResults)
			throws Exception {
		QueryParser parser = new QueryParser( getTargetLuceneVersion(), "value", SearchTestCase.standardAnalyzer );
		Query query = parser.parse( queryString );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Value.class );
		@SuppressWarnings("unchecked")
		List<Value> valueList = fullTextQuery.list();
		assertEquals( "Wrong number of results", expectedNumberOfResults, valueList.size() );
	}

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default_null_token", "fubar" );
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Value.class,
		};
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.query.nullValues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.query.ProjectionToMapResultTransformer;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests for indexing {@code null} values, asserting the actual document content.
 *
 * This is just for extra safety, there already are index/query tests in {@link IndexAndQueryNullTest}.
 *
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-115")
@Category(SkipOnElasticsearch.class) // Lucene documents cannot be accessed on Elasticsearch
public class IndexNullLuceneDocumentContentTest extends SearchTestBase {

	@Test
	public void testLuceneDocumentContainsNullToken() throws Exception {
		Value nullValue = new Value( null );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		getSession().save( nullValue );
		tx.commit();

		fullTextSession.clear();
		tx = fullTextSession.beginTransaction();

		Document document = getSingleIndexedDocument( fullTextSession );

		String indexedNullString = document.get( "value" );
		String expectedString = "_custom_token_";
		assertEquals( "The null value should be indexed as " + expectedString, expectedString, indexedNullString );

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

		Document document = getSingleIndexedDocument( fullTextSession );

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

		Document document = getSingleIndexedDocument( fullTextSession );

		String indexedNullString = document.get( "dummy" );

		String expectedString = "_dummy_";
		assertEquals( "The null value should be indexed as " + expectedString, expectedString, indexedNullString );

		tx.commit();
		fullTextSession.close();
	}

	private Document getSingleIndexedDocument(FullTextSession fullTextSession) throws ParseException {
		Query query = createLuceneQuery();

		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Value.class );
		fullTextQuery.setProjection(
				FullTextQuery.DOCUMENT
		);
		fullTextQuery.setResultTransformer( new ProjectionToMapResultTransformer() );
		List<?> mappedResults = fullTextQuery.list();
		assertTrue( "Wrong result size", mappedResults.size() == 1 );

		Map<?, ?> map = (Map<?, ?>) mappedResults.get( 0 );
		Document document = (Document) map.get( FullTextQuery.DOCUMENT );
		assertNotNull( document );
		return document;
	}

	private Query createLuceneQuery() throws ParseException {
		QueryParser parser = new QueryParser( "id", TestConstants.standardAnalyzer );
		parser.setAllowLeadingWildcard( true );
		return parser.parse( "*" );
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( "hibernate.search.default_null_token", "fubar" );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Value.class,
		};
	}
}

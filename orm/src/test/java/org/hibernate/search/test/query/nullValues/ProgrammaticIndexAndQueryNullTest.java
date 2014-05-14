/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.query.nullValues;

import java.lang.annotation.ElementType;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.Transaction;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.query.ProjectionToMapResultTransformer;
import org.hibernate.search.testsupport.TestConstants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for indexing and querying {@code null} values. See HSEARCh-115
 *
 * @author Hardy Ferentschik
 */
public class ProgrammaticIndexAndQueryNullTest extends SearchTestBase {

	@Test
	public void testProjectedValueGetsConvertedToNull() throws Exception {
		ProgrammaticConfiguredValue nullValue = new ProgrammaticConfiguredValue( null );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		getSession().save( nullValue );
		tx.commit();

		fullTextSession.clear();
		tx = fullTextSession.beginTransaction();

		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
		parser.setAllowLeadingWildcard( true );
		Query query = parser.parse( "*" );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, ProgrammaticConfiguredValue.class );
		fullTextQuery.setProjection(
				"id",
				"value",
				FullTextQuery.DOCUMENT
		);
		fullTextQuery.setResultTransformer( new ProjectionToMapResultTransformer() );
		List mappedResults = fullTextQuery.list();
		assertTrue( "Wrong result size", mappedResults.size() == 1 );

		Map map = (Map) mappedResults.get( 0 );
		Integer id = (Integer) map.get( "id" );
		assertNotNull( id );

		String value = (String) map.get( "value" );
		assertEquals( "The null token should be converted back to null", null, value );

		Document doc = (Document) map.get( FullTextQuery.DOCUMENT );
		assertEquals(
				"The programmatically configured null value should be in the document",
				"@null@",
				doc.getField( "value" ).stringValue()
		);

		tx.commit();
		fullTextSession.close();
	}

	private SearchMapping createSearchMapping() {
		SearchMapping mapping = new SearchMapping();

		mapping.entity( ProgrammaticConfiguredValue.class )
				.indexed()
				.property( "id", ElementType.FIELD ).documentId().name( "id" )
				.property( "value", ElementType.FIELD ).field().store( Store.YES ).indexNullAs( "@null@" );
		return mapping;
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.getProperties().put( Environment.MODEL_MAPPING, createSearchMapping() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				ProgrammaticConfiguredValue.class
		};
	}
}

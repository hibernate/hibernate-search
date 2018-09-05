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

import java.lang.annotation.ElementType;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.query.ProjectionToMapResultTransformer;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Test;

/**
 * Tests for indexing and querying programmatically mapped {@code null} values.
 *
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-115")
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

		Query query = new MatchAllDocsQuery();
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, ProgrammaticConfiguredValue.class );
		fullTextQuery.setProjection(
				"id",
				"value"
		);
		fullTextQuery.setResultTransformer( new ProjectionToMapResultTransformer() );
		List<?> mappedResults = fullTextQuery.list();
		assertTrue( "Wrong result size", mappedResults.size() == 1 );

		Map<?, ?> map = (Map<?, ?>) mappedResults.get( 0 );
		Integer id = (Integer) map.get( "id" );
		assertNotNull( id );

		String value = (String) map.get( "value" );
		assertEquals( "The null token should be converted back to null", null, value );

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
	public void configure(Map<String,Object> cfg) {
		cfg.put( Environment.MODEL_MAPPING, createSearchMapping() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				ProgrammaticConfiguredValue.class
		};
	}
}

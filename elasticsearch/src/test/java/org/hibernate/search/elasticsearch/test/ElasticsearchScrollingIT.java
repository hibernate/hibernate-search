/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SearchITHelper.EntityInstanceWorkContext;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
@TestForIssue( jiraKey = "HSEARCH-2189" )
public class ElasticsearchScrollingIT {

	/**
	 * The default value for the index.max_result_window setting in Elasticsearch.
	 * @see https://www.elastic.co/guide/en/elasticsearch/reference/2.4/breaking_21_search_changes.html#_from_size_limits
	 */
	private static final int DEFAULT_MAX_RESULT_WINDOW = 10000;

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( IndexedObject.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Test
	public void searchBeforeMaxResultWindow() throws Exception {
		generateData( 0, DEFAULT_MAX_RESULT_WINDOW + 10 );

		Query query = builder().all().createQuery();
		List<EntityInfo> results = getQuery( query )
				.firstResult( DEFAULT_MAX_RESULT_WINDOW - 5 ).maxResults( 5 )
				.queryEntityInfos();
		assertEquals( 5, results.size() );
		assertEquals( DEFAULT_MAX_RESULT_WINDOW - 5, results.get( 0 ).getId() );
	}

	@Test(expected = SearchException.class)
	public void searchBeyondMaxResultWindow() throws Exception {
		generateData( 0, DEFAULT_MAX_RESULT_WINDOW + 10 );

		Query query = builder().all().createQuery();
		getQuery( query )
				.firstResult( DEFAULT_MAX_RESULT_WINDOW + 1 ).maxResults( 5 )
				.queryEntityInfos();
	}

	@Test
	public void scrollBeyondMaxResultWindow() throws Exception {
		generateData( 0, DEFAULT_MAX_RESULT_WINDOW + 10 );

		Query query = builder().all().createQuery();
		try ( DocumentExtractor extractor = getQuery( query )
				.queryDocumentExtractor() ) {
			for ( int i = 0; i < DEFAULT_MAX_RESULT_WINDOW + 10; ++i ) {
				EntityInfo info = extractor.extract(i);
				assertNotNull( info );
				assertEquals( i, info.getId() );
			}
		}
	}

	@Test
	public void scrollForwardToArbitraryPosition() throws Exception {
		generateData( 0, 1000 );

		Query query = builder().all().createQuery();
		try ( DocumentExtractor extractor = getQuery( query )
				.queryDocumentExtractor() ) {
			EntityInfo info = extractor.extract( 1 );
			assertNotNull( info );
			assertEquals( 1, info.getId() );

			info = extractor.extract( 500 );
			assertNotNull( info );
			assertEquals( 500, info.getId() );
		}
	}

	@Test
	public void scrollBackward() throws Exception {
		generateData( 0, 1001 );

		Query query = builder().all().createQuery();
		try ( DocumentExtractor extractor = getQuery( query )
				.queryDocumentExtractor() ) {
			EntityInfo info = extractor.extract( 1000 );
			assertNotNull( info );
			assertEquals( 1000, info.getId() );

			// Backtrack exactly 1000 positions
			info = extractor.extract( 0 );
			assertNotNull( info );
			assertEquals( 0, info.getId() );
		}
	}

	private QueryBuilder builder() {
		return helper.queryBuilder( IndexedObject.class );
	}

	private HSQuery getQuery(Query luceneQuery) {
		ExtendedSearchIntegrator sf = sfHolder.getSearchFactory();
		HSQuery hsQuery = sf.createHSQuery( luceneQuery, IndexedObject.class );
		return hsQuery
				.projection( "id" )
				.sort( new Sort( new SortField( "idSort", SortField.Type.INT ) ) );
	}

	private void generateData(int firstId, int count) throws Exception {
		EntityInstanceWorkContext executor = helper.add();
		for ( int id = firstId; id < firstId + count; ++id ) {
			executor.push( new IndexedObject( id ), id );
		}
		executor.execute();
	}

	@Indexed
	public static class IndexedObject {
		@DocumentId
		@Field(name = "idSort")
		@SortableField(forField = "idSort")
		private Integer id;

		public IndexedObject(Integer id) {
			super();
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

	}
}

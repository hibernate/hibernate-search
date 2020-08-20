/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import java.util.List;

import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.TermMatchingContext;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.junit.ElasticsearchSupportInProgress;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hibernate.search.test.bridge.MapBridgeNullEmbeddedTestEntity.Language.ENGLISH;
import static org.hibernate.search.test.bridge.MapBridgeNullEmbeddedTestEntity.Language.ITALIAN;
import static org.hibernate.search.test.bridge.MapBridgeNullEmbeddedTestEntity.Language.KLINGON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test indexing of {@link javax.persistence.ElementCollection} annotated maps when
 * there also is an @IndexedEmbedded annotation using {@code indexNullAs} on the same property.
 *
 * @author Davide D'Alto
 */
@Category(ElasticsearchSupportInProgress.class) // HSEARCH-2389 Support indexNullAs for @IndexedEmbedded applied on objects with Elasticsearch
public class MapBridgeNullEmbeddedTest extends SearchTestBase {

	private FullTextSession fullTextSession;
	private MapBridgeNullEmbeddedTestEntity withoutNull;
	private MapBridgeNullEmbeddedTestEntity withNullEntry;
	private MapBridgeNullEmbeddedTestEntity withNullEmbedded;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		Session session = openSession();
		fullTextSession = Search.getFullTextSession( session );
		prepareData();
	}

	private void prepareData() {
		Transaction tx = fullTextSession.beginTransaction();

		withoutNull = persistEntity( fullTextSession, "Davide D'Alto" );
		withoutNull.addNullIndexed( 1, ITALIAN );
		withoutNull.addNullIndexed( 2, ENGLISH );
		withoutNull.addNumericNullIndexed( 1, 1 );
		withoutNull.addNumericNullIndexed( 2, 2 );

		withNullEntry = persistEntity( fullTextSession, "Worf" );
		withNullEntry.addNullIndexed( 1, KLINGON );
		withNullEntry.addNullIndexed( 2, ENGLISH );
		withNullEntry.addNullIndexed( 3, null );
		withNullEntry.addNumericNullIndexed( 1, 11 );
		withNullEntry.addNumericNullIndexed( 2, null );

		withNullEmbedded = persistEntity( fullTextSession, "Mime" );
		withNullEmbedded.setNumericNullIndexed( null );
		withNullEmbedded.setNullIndexed( null );

		tx.commit();
	}

	@Test
	public void testSearchNullEntry() throws Exception {
		List<MapBridgeNullEmbeddedTestEntity> results = findResults( "nullIndexed", MapBridgeNullEmbeddedTestEntity.NULL_TOKEN, true );

		assertNotNull( "No result found for an indexed collection", results );
		assertEquals( "Unexpected number of results in a collection", 1, results.size() );
		assertEquals( "Wrong result returned looking for a null in a collection", withNullEntry.getName(), results.get( 0 ).getName() );
	}

	@Test
	@Category(ElasticsearchSupportInProgress.class) // HSEARCH-2389 Support indexNullAs for @IndexedEmbedded applied on objects with Elasticsearch
	public void testSearchNullEmbedded() throws Exception {
		List<MapBridgeNullEmbeddedTestEntity> results = findEmbeddedNullResults( "nullIndexed", MapBridgeNullEmbeddedTestEntity.NULL_EMBEDDED, true );

		assertNotNull( "No result found for an indexed collection", results );
		assertEquals( "Unexpected number of results in a collection", 1, results.size() );
		assertEquals( "Wrong result returned looking for a null in a collection", withNullEmbedded.getName(), results.get( 0 ).getName() );
	}

	@Test
	@Category(ElasticsearchSupportInProgress.class) // HSEARCH-2389 Support indexNullAs for @IndexedEmbedded applied on objects with Elasticsearch
	public void testSearchNullNumericEmbedded() throws Exception {
		List<MapBridgeNullEmbeddedTestEntity> results =
				findEmbeddedNullResults( "embeddedNum", MapBridgeNullEmbeddedTestEntity.NULL_EMBEDDED_NUMERIC, true );

		assertNotNull( "No result found for an indexed collection", results );
		assertEquals( "Unexpected number of results in a collection", 1, results.size() );
		assertEquals( "Wrong result returned looking for a null in a collection of numeric", withNullEmbedded.getName(), results.get( 0 ).getName() );
	}

	@Test
	public void testSearchNullNumericEntry() throws Exception {
		List<MapBridgeNullEmbeddedTestEntity> results =
				findResults( "numericNullIndexed", MapBridgeNullEmbeddedTestEntity.NULL_NUMERIC_TOKEN, true );

		assertNotNull( "No result found for an indexed collection", results );
		assertEquals( "Unexpected number of results in a collection", 1, results.size() );
		assertEquals( "Wrong result returned looking for a null in a collection of numeric", withNullEntry.getName(), results.get( 0 ).getName() );
	}

	@Test
	public void testSearchNotNullEntry() throws Exception {
		{
			List<MapBridgeNullEmbeddedTestEntity> results = findResults( "nullIndexed", KLINGON, false );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed collection", withNullEntry.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<MapBridgeNullEmbeddedTestEntity> results = findResults( "nullIndexed", ITALIAN, false );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed collection", withoutNull.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<MapBridgeNullEmbeddedTestEntity> results = findResults( "nullIndexed", ENGLISH, false );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 2, results.size() );
		}
	}

	@Test
	public void testSearchNotNullNumeric() throws Exception {
		{
			List<MapBridgeNullEmbeddedTestEntity> results = findNumericResults( "numericNullIndexed", 1 );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed collection", withoutNull.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<MapBridgeNullEmbeddedTestEntity> results = findNumericResults( "numericNullIndexed", 11 );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed collection", withNullEntry.getName(), results.get( 0 )
					.getName() );
		}
	}

	@SuppressWarnings("unchecked")
	private List<MapBridgeNullEmbeddedTestEntity> findEmbeddedNullResults(String fieldName, Object value, boolean checkNullToken) {
		QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder()
				.forEntity( MapBridgeNullEmbeddedTestEntity.class ).get();
		TermMatchingContext termMatchingContext = queryBuilder.keyword().onField( fieldName );
		if ( checkNullToken ) {
			termMatchingContext.ignoreFieldBridge();
		}
		Query query = termMatchingContext.ignoreAnalyzer().matching( value ).createQuery();
		return fullTextSession.createFullTextQuery( query, MapBridgeNullEmbeddedTestEntity.class ).list();
	}

	@SuppressWarnings("unchecked")
	private List<MapBridgeNullEmbeddedTestEntity> findResults(String fieldName, Object value, boolean checkNullToken) {
		QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder()
				.forEntity( MapBridgeNullEmbeddedTestEntity.class ).get();
		TermMatchingContext termMatchingContext = queryBuilder.keyword().onField( fieldName );
		if ( checkNullToken ) {
			termMatchingContext.ignoreFieldBridge();
			termMatchingContext.ignoreAnalyzer();
		}
		Query query = termMatchingContext.matching( value ).createQuery();
		return fullTextSession.createFullTextQuery( query, MapBridgeNullEmbeddedTestEntity.class ).list();
	}

	@SuppressWarnings("unchecked")
	private List<MapBridgeNullEmbeddedTestEntity> findNumericResults(String fieldName, Object number) {
		Query query = NumericFieldUtils.createNumericRangeQuery( fieldName, number, number, true, true );
		return fullTextSession.createFullTextQuery( query, MapBridgeNullEmbeddedTestEntity.class ).list();
	}

	private MapBridgeNullEmbeddedTestEntity persistEntity(Session s, String name) {
		MapBridgeNullEmbeddedTestEntity boy = new MapBridgeNullEmbeddedTestEntity();
		boy.setName( name );
		s.persist( boy );
		return boy;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { MapBridgeNullEmbeddedTestEntity.class, };
	}
}

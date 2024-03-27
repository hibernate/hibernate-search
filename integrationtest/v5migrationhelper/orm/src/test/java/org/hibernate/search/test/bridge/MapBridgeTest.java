/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.test.bridge.MapBridgeTestEntity.Language.ENGLISH;
import static org.hibernate.search.test.bridge.MapBridgeTestEntity.Language.ITALIAN;
import static org.hibernate.search.test.bridge.MapBridgeTestEntity.Language.KLINGON;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.TermMatchingContext;
import org.hibernate.search.test.SearchTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.search.Query;

/**
 * Test indexing of {@link jakarta.persistence.ElementCollection} annotated maps.
 *
 * @author Davide D'Alto
 */
class MapBridgeTest extends SearchTestBase {

	private FullTextSession fullTextSession;
	private MapBridgeTestEntity withoutNull;
	private MapBridgeTestEntity withNullEntry;
	private MapBridgeTestEntity withNullEmbedded;
	private Date indexedDate;

	@Override
	@BeforeEach
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
		withoutNull.addNullNotIndexed( 1, "DaltoValue" );
		withoutNull.addNullNotIndexed( 2, "DavideValue" );
		withoutNull.addNumericNullNotIndexed( 1, 3L );
		withoutNull.addNumericNullNotIndexed( 2, 4L );
		indexedDate = new Date();
		withoutNull.addDate( 1, indexedDate );

		withNullEntry = persistEntity( fullTextSession, "Worf" );
		withNullEntry.addNullIndexed( 1, KLINGON );
		withNullEntry.addNullIndexed( 2, ENGLISH );
		withNullEntry.addNullIndexed( 3, null );
		withNullEntry.addNumericNullIndexed( 1, 11 );
		withNullEntry.addNumericNullIndexed( 2, null );
		withNullEntry.addNullNotIndexed( 1, "WorfValue" );
		withNullEntry.addNullNotIndexed( 2, null );
		withNullEntry.addNumericNullNotIndexed( 1, 33L );
		withNullEntry.addNumericNullNotIndexed( 2, null );
		withNullEntry.addDate( 1, null );

		withNullEmbedded = persistEntity( fullTextSession, "Mime" );
		withNullEmbedded.setDates( null );
		withNullEmbedded.setNumericNullIndexed( null );
		withNullEmbedded.setNumericNullNotIndexed( null );
		withNullEmbedded.setNullIndexed( null );
		withNullEmbedded.setNullNotIndexed( null );
		withNullEmbedded.setDates( null );

		tx.commit();
	}

	@Test
	void testSearchNullEntry() {
		List<MapBridgeTestEntity> results = findResults( "nullIndexed", MapBridgeTestEntity.NULL_LANGUAGE_TOKEN, true );

		assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
		assertThat( results ).as( "Unexpected number of results in a collection" ).hasSize( 1 );
		assertThat( results.get( 0 ).getName() ).as( "Wrong result returned looking for a null in a collection" )
				.isEqualTo( withNullEntry.getName() );
	}

	@Test
	void testSearchNullNumericEntry() {
		List<MapBridgeTestEntity> results =
				findResults( "numericNullIndexed", MapBridgeTestEntity.NULL_NUMERIC_TOKEN_INT, false );

		assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
		assertThat( results ).as( "Unexpected number of results in a collection" ).hasSize( 1 );
		assertThat( results.get( 0 ).getName() ).as( "Wrong result returned looking for a null in a collection of numeric" )
				.isEqualTo( withNullEntry.getName() );
	}

	@Test
	void testSearchNotNullEntry() {
		{
			List<MapBridgeTestEntity> results = findResults( "nullIndexed", KLINGON, false );

			assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed collection" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed collection" )
					.isEqualTo( withNullEntry.getName() );
		}
		{
			List<MapBridgeTestEntity> results = findResults( "nullIndexed", ITALIAN, false );

			assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed collection" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed collection" )
					.isEqualTo( withoutNull.getName() );
		}
		{
			List<MapBridgeTestEntity> results = findResults( "nullIndexed", ENGLISH, false );

			assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed collection" ).hasSize( 2 );
		}
	}

	@Test
	void testSearchEntryWhenNullEntryNotIndexed() {
		{
			List<MapBridgeTestEntity> results = findResults( "nullNotIndexed", "DaltoValue", false );

			assertThat( results ).as( "No result found for an indexed array" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed array" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed array" )
					.isEqualTo( withoutNull.getName() );
		}
		{
			List<MapBridgeTestEntity> results = findResults( "nullNotIndexed", "WorfValue", false );

			assertThat( results ).as( "No result found for an indexed array" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed array" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed array" )
					.isEqualTo( withNullEntry.getName() );
		}
	}

	@Test
	void testSearchNotNullNumeric() {
		{
			List<MapBridgeTestEntity> results = findNumericResults( "numericNullIndexed", 1 );

			assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed collection" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed collection" )
					.isEqualTo( withoutNull.getName() );
		}
		{
			List<MapBridgeTestEntity> results = findNumericResults( "numericNullIndexed", 11 );

			assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed collection" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed collection" )
					.isEqualTo( withNullEntry.getName() );
		}
	}

	@Test
	void testSearchNotNullNumericEntryWhenNullEntryNotIndexed() {
		{
			List<MapBridgeTestEntity> results = findNumericResults( "numericNullNotIndexed", 3L );

			assertThat( results ).as( "No result found for an indexed array" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed array" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed array" )
					.isEqualTo( withoutNull.getName() );
		}
		{
			List<MapBridgeTestEntity> results = findNumericResults( "numericNullNotIndexed", 33L );

			assertThat( results ).as( "No result found for an indexed array" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed array" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed array" )
					.isEqualTo( withNullEntry.getName() );
		}
	}

	@Test
	void testDateIndexing() {
		List<MapBridgeTestEntity> results = findResultsWithRangeQuery(
				"dates",
				DateTools.round( indexedDate, DateTools.Resolution.SECOND )
		);

		assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
		assertThat( results ).as( "Wrong number of results returned for an indexed collection" ).hasSize( 1 );
		assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from a collection of Date" )
				.isEqualTo( withoutNull.getName() );
	}

	@SuppressWarnings("unchecked")
	private List<MapBridgeTestEntity> findResults(String fieldName, Object value, boolean checkNullToken) {
		QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder()
				.forEntity( MapBridgeTestEntity.class ).get();
		TermMatchingContext termMatchingContext = queryBuilder.keyword().onField( fieldName );
		if ( checkNullToken ) {
			termMatchingContext.ignoreFieldBridge();
			termMatchingContext.ignoreAnalyzer();
		}
		Query query = termMatchingContext.matching( value ).createQuery();
		return fullTextSession.createFullTextQuery( query, MapBridgeTestEntity.class ).list();
	}

	@SuppressWarnings("unchecked")
	private List<MapBridgeTestEntity> findResultsWithRangeQuery(String fieldName, Object start) {
		QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder()
				.forEntity( MapBridgeTestEntity.class ).get();
		Query query = queryBuilder.range().onField( fieldName ).above( start ).createQuery();
		return fullTextSession.createFullTextQuery( query, MapBridgeTestEntity.class ).list();
	}

	@SuppressWarnings("unchecked")
	private List<MapBridgeTestEntity> findNumericResults(String fieldName, Object number) {
		Query query = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( MapBridgeTestEntity.class )
				.get()
				.range().onField( fieldName )
				.from( number ).to( number )
				.createQuery();
		return fullTextSession.createFullTextQuery( query, MapBridgeTestEntity.class ).list();
	}

	private MapBridgeTestEntity persistEntity(Session s, String name) {
		MapBridgeTestEntity boy = new MapBridgeTestEntity();
		boy.setName( name );
		s.persist( boy );
		return boy;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { MapBridgeTestEntity.class, };
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.test.bridge.IterableBridgeTestEntity.Language.ENGLISH;
import static org.hibernate.search.test.bridge.IterableBridgeTestEntity.Language.ITALIAN;
import static org.hibernate.search.test.bridge.IterableBridgeTestEntity.Language.KLINGON;

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
 * Test indexing of {@link jakarta.persistence.ElementCollection} annotated iterables.
 *
 * @author Davide D'Alto
 */
class IterableBridgeTest extends SearchTestBase {

	private FullTextSession fullTextSession;
	private IterableBridgeTestEntity withoutNull;
	private IterableBridgeTestEntity withNullEntry;
	private IterableBridgeTestEntity withNullEmbedded;
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
		withoutNull.addNullIndexed( ITALIAN );
		withoutNull.addNullIndexed( ENGLISH );
		withoutNull.addNumericNullIndexed( 1 );
		withoutNull.addNumericNullIndexed( 2 );
		withoutNull.addNullNotIndexed( "DaltoValue" );
		withoutNull.addNullNotIndexed( "DavideValue" );
		withoutNull.addNumericNullNotIndexed( 3L );
		withoutNull.addNumericNullNotIndexed( 4L );
		indexedDate = new Date();
		withoutNull.addDate( indexedDate );

		withNullEntry = persistEntity( fullTextSession, "Worf" );
		withNullEntry.addNullIndexed( KLINGON );
		withNullEntry.addNullIndexed( ENGLISH );
		withNullEntry.addNullIndexed( null );
		withNullEntry.addNumericNullIndexed( 11 );
		withNullEntry.addNumericNullIndexed( null );
		withNullEntry.addNullNotIndexed( "WorfValue" );
		withNullEntry.addNullNotIndexed( null );
		withNullEntry.addNumericNullNotIndexed( 33L );
		withNullEntry.addNumericNullNotIndexed( null );
		withNullEntry.addDate( null );

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
		{
			List<IterableBridgeTestEntity> results =
					findResults( "nullIndexed", IterableBridgeTestEntity.NULL_LANGUAGE_TOKEN, true );

			assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
			assertThat( results ).as( "Unexpected number of results in a collection" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned looking for a null in a collection" )
					.isEqualTo( withNullEntry.getName() );
		}
	}

	@Test
	void testSearchNullNumericEntry() {
		List<IterableBridgeTestEntity> results =
				findResults( "numericNullIndexed", IterableBridgeTestEntity.NULL_NUMERIC_TOKEN_INT, false );

		assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
		assertThat( results ).as( "Unexpected number of results in a collection" ).hasSize( 1 );
		assertThat( results.get( 0 ).getName() ).as( "Wrong result returned looking for a null in a collection" )
				.isEqualTo( withNullEntry.getName() );
	}

	@Test
	void testSearchNotNullEntry() {
		{
			List<IterableBridgeTestEntity> results = findResults( "nullIndexed", KLINGON, false );

			assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed collection" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed collection" )
					.isEqualTo( withNullEntry.getName() );
		}
		{
			List<IterableBridgeTestEntity> results = findResults( "nullIndexed", ITALIAN, false );

			assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed collection" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed collection" )
					.isEqualTo( withoutNull.getName() );
		}
		{
			List<IterableBridgeTestEntity> results = findResults( "nullIndexed", ENGLISH, false );

			assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed collection" ).hasSize( 2 );
		}
	}

	@Test
	void testSearchEntryWhenNullEntryNotIndexed() {
		{
			List<IterableBridgeTestEntity> results = findResults( "nullNotIndexed", "DaltoValue", false );

			assertThat( results ).as( "No result found for an indexed array" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed array" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed array" )
					.isEqualTo( withoutNull.getName() );
		}
		{
			List<IterableBridgeTestEntity> results = findResults( "nullNotIndexed", "WorfValue", false );

			assertThat( results ).as( "No result found for an indexed array" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed array" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed array" )
					.isEqualTo( withNullEntry.getName() );
		}
	}

	@Test
	void testSearchNotNullNumeric() {
		{
			List<IterableBridgeTestEntity> results = findNumericResults( "numericNullIndexed", 1 );

			assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed collection" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed collection" )
					.isEqualTo( withoutNull.getName() );
		}
		{
			List<IterableBridgeTestEntity> results = findNumericResults( "numericNullIndexed", 11 );

			assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed collection" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed collection" )
					.isEqualTo( withNullEntry.getName() );
		}
	}

	@Test
	void testSearchNotNullNumericEntryWhenNullEntryNotIndexed() {
		{
			List<IterableBridgeTestEntity> results = findNumericResults( "numericNullNotIndexed", 3L );

			assertThat( results ).as( "No result found for an indexed array" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed array" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed array" )
					.isEqualTo( withoutNull.getName() );
		}
		{
			List<IterableBridgeTestEntity> results = findNumericResults( "numericNullNotIndexed", 33L );

			assertThat( results ).as( "No result found for an indexed array" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed array" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed array" )
					.isEqualTo( withNullEntry.getName() );
		}
	}

	@Test
	void testDateIndexing() {
		List<IterableBridgeTestEntity> results = findResultsWithRangeQuery(
				"dates",
				DateTools.round( indexedDate, DateTools.Resolution.SECOND )
		);

		assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
		assertThat( results ).as( "Wrong number of results returned for an indexed collection" ).hasSize( 1 );
		assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from a collection of Date" )
				.isEqualTo( withoutNull.getName() );
	}

	@SuppressWarnings("unchecked")
	private List<IterableBridgeTestEntity> findResults(String fieldName, Object value, boolean checkRawValue) {
		QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder()
				.forEntity( IterableBridgeTestEntity.class ).get();
		TermMatchingContext termMatchingContext = queryBuilder.keyword().onField( fieldName );
		if ( checkRawValue ) {
			termMatchingContext.ignoreFieldBridge();
			termMatchingContext.ignoreAnalyzer();
		}
		Query query = termMatchingContext.matching( value ).createQuery();
		return fullTextSession.createFullTextQuery( query, IterableBridgeTestEntity.class ).list();
	}

	@SuppressWarnings("unchecked")
	private List<IterableBridgeTestEntity> findResultsWithRangeQuery(String fieldName, Object start) {
		QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder()
				.forEntity( IterableBridgeTestEntity.class ).get();
		Query query = queryBuilder.range().onField( fieldName ).above( start ).createQuery();
		return fullTextSession.createFullTextQuery( query, IterableBridgeTestEntity.class ).list();
	}

	@SuppressWarnings("unchecked")
	private List<IterableBridgeTestEntity> findNumericResults(String fieldName, Object number) {
		Query query = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( IterableBridgeTestEntity.class )
				.get()
				.range().onField( fieldName )
				.from( number ).to( number )
				.createQuery();
		return fullTextSession.createFullTextQuery( query, IterableBridgeTestEntity.class ).list();
	}

	private IterableBridgeTestEntity persistEntity(Session s, String name) {
		IterableBridgeTestEntity boy = new IterableBridgeTestEntity();
		boy.setName( name );
		s.persist( boy );
		return boy;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { IterableBridgeTestEntity.class, };
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.test.bridge.ArrayBridgeTestEntity.Language.ENGLISH;
import static org.hibernate.search.test.bridge.ArrayBridgeTestEntity.Language.ITALIAN;
import static org.hibernate.search.test.bridge.ArrayBridgeTestEntity.Language.KLINGON;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.TermMatchingContext;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.bridge.ArrayBridgeTestEntity.Language;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.search.Query;

/**
 * Test indexing of {@link jakarta.persistence.ElementCollection} annotated arrays.
 *
 * @author Davide D'Alto
 */
class ArrayBridgeTest extends SearchTestBase {

	private FullTextSession fullTextSession;
	private ArrayBridgeTestEntity withoutNull;
	private ArrayBridgeTestEntity withNullEntry;
	private ArrayBridgeTestEntity withNullEmbedded;
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
		indexedDate = new Date();

		Transaction tx = fullTextSession.beginTransaction();

		withoutNull = persistEntity( fullTextSession, "Davide D'Alto" );
		withoutNull.setNullIndexed( new Language[] { ITALIAN, ENGLISH } );
		withoutNull.setNumericNullIndexed( new Integer[] { 1, 2 } );
		withoutNull.setNullNotIndexed( new String[] { "DaltoValue", "DavideValue" } );
		withoutNull.setNumericNullNotIndexed( new Long[] { 3L, 4L } );
		withoutNull.setPrimitive( new float[] { 3.2f, 452.2f } );
		withoutNull.setDates( new Date[] { indexedDate } );

		withNullEntry = persistEntity( fullTextSession, "Worf" );
		withNullEntry.setNullIndexed( new Language[] { KLINGON, ENGLISH, null } );
		withNullEntry.setNumericNullIndexed( new Integer[] { 11, null } );
		withNullEntry.setNullNotIndexed( new String[] { "WorfValue", null } );
		withNullEntry.setNumericNullNotIndexed( new Long[] { 33L, null } );
		withNullEntry.setPrimitive( new float[] { 2.0f } );
		withNullEntry.setDates( new Date[] { null } );

		withNullEmbedded = persistEntity( fullTextSession, "Mime" );
		withNullEmbedded.setDates( null );
		withNullEmbedded.setNumericNullIndexed( null );
		withNullEmbedded.setNumericNullNotIndexed( null );
		withNullEmbedded.setPrimitive( null );
		withNullEmbedded.setNullIndexed( null );
		withNullEmbedded.setNullNotIndexed( null );
		withNullEmbedded.setDates( null );

		tx.commit();
	}

	@Test
	void testSearchNullEntry() {
		List<ArrayBridgeTestEntity> results = findResults( "nullIndexed", ArrayBridgeTestEntity.NULL_LANGUAGE_TOKEN, true );

		assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
		assertThat( results ).as( "Unexpected number of results in a collection" ).hasSize( 1 );
		assertThat( results.get( 0 ).getName() ).as( "Wrong result returned looking for a null in a collection" )
				.isEqualTo( withNullEntry.getName() );
	}

	@Test
	void testSearchNullNumericEntry() {
		List<ArrayBridgeTestEntity> results =
				findResults( "numericNullIndexed", ArrayBridgeTestEntity.NULL_NUMERIC_TOKEN_INT, false );

		assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
		assertThat( results ).as( "Unexpected number of results in a collection" ).hasSize( 1 );
		assertThat( results.get( 0 ).getName() ).as( "Wrong result returned looking for a null in a collection of numeric" )
				.isEqualTo( withNullEntry.getName() );
	}

	@Test
	void testSearchNotNullEntry() {
		{
			List<ArrayBridgeTestEntity> results = findResults( "nullIndexed", KLINGON, false );

			assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed collection" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed collection" )
					.isEqualTo( withNullEntry.getName() );
		}
		{
			List<ArrayBridgeTestEntity> results = findResults( "nullIndexed", ITALIAN, false );

			assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed collection" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed collection" )
					.isEqualTo( withoutNull.getName() );
		}
		{
			List<ArrayBridgeTestEntity> results = findResults( "nullIndexed", ENGLISH, false );

			assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed collection" ).hasSize( 2 );
		}
	}

	@Test
	void testSearchEntryWhenNullEntryNotIndexed() {
		{
			List<ArrayBridgeTestEntity> results = findResults( "nullNotIndexed", "DaltoValue", false );

			assertThat( results ).as( "No result found for an indexed array" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed array" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed array" )
					.isEqualTo( withoutNull.getName() );
		}
		{
			List<ArrayBridgeTestEntity> results = findResults( "nullNotIndexed", "WorfValue", false );

			assertThat( results ).as( "No result found for an indexed array" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed array" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed array" )
					.isEqualTo( withNullEntry.getName() );
		}
	}

	@Test
	void testSearchNotNullNumeric() {
		{
			List<ArrayBridgeTestEntity> results = findNumericResults( "numericNullIndexed", 1 );

			assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed collection" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed collection" )
					.isEqualTo( withoutNull.getName() );
		}
		{
			List<ArrayBridgeTestEntity> results = findNumericResults( "numericNullIndexed", 11 );

			assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed collection" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed collection" )
					.isEqualTo( withNullEntry.getName() );
		}
	}

	@Test
	void testSearchNotNullNumericEntryWhenNullEntryNotIndexed() {
		{
			List<ArrayBridgeTestEntity> results = findNumericResults( "numericNullNotIndexed", 3L );

			assertThat( results ).as( "No result found for an indexed array" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed array" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed array" )
					.isEqualTo( withoutNull.getName() );
		}
		{
			List<ArrayBridgeTestEntity> results = findNumericResults( "numericNullNotIndexed", 33L );

			assertThat( results ).as( "No result found for an indexed array" ).isNotNull();
			assertThat( results ).as( "Wrong number of results returned for an indexed array" ).hasSize( 1 );
			assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from an indexed array" )
					.isEqualTo( withNullEntry.getName() );
		}
	}

	@Test
	void testPrimitiveIndexing() {
		List<ArrayBridgeTestEntity> results = findResultsWithRangeQuery(
				"primitive",
				100.0f
		);

		assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
		assertThat( results ).as( "Wrong number of results returned for an indexed collection" ).hasSize( 1 );
		assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from a collection of primitive numbers" )
				.isEqualTo( withoutNull.getName() );
	}

	@Test
	void testDateIndexing() {
		List<ArrayBridgeTestEntity> results = findResultsWithRangeQuery(
				"dates",
				DateTools.round( indexedDate, DateTools.Resolution.SECOND )
		);

		assertThat( results ).as( "No result found for an indexed collection" ).isNotNull();
		assertThat( results ).as( "Wrong number of results returned for an indexed collection" ).hasSize( 1 );
		assertThat( results.get( 0 ).getName() ).as( "Wrong result returned from a collection of Date" )
				.isEqualTo( withoutNull.getName() );
	}

	@SuppressWarnings("unchecked")
	private List<ArrayBridgeTestEntity> findResults(String fieldName, Object value, boolean checkRawValue) {
		QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder()
				.forEntity( ArrayBridgeTestEntity.class ).get();
		TermMatchingContext termMatchingContext = queryBuilder.keyword().onField( fieldName );
		if ( checkRawValue ) {
			termMatchingContext.ignoreFieldBridge();
			termMatchingContext.ignoreAnalyzer();
		}
		Query query = termMatchingContext.matching( value ).createQuery();
		return fullTextSession.createFullTextQuery( query, ArrayBridgeTestEntity.class ).list();
	}

	@SuppressWarnings("unchecked")
	private List<ArrayBridgeTestEntity> findResultsWithRangeQuery(String fieldName, Object start) {
		QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder()
				.forEntity( ArrayBridgeTestEntity.class ).get();
		Query query = queryBuilder.range().onField( fieldName ).above( start ).createQuery();
		return fullTextSession.createFullTextQuery( query, ArrayBridgeTestEntity.class ).list();
	}

	@SuppressWarnings("unchecked")
	private List<ArrayBridgeTestEntity> findNumericResults(String fieldName, Object number) {
		QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder()
				.forEntity( ArrayBridgeTestEntity.class ).get();
		Query query = queryBuilder.range().onField( fieldName ).from( number ).to( number ).createQuery();
		return fullTextSession.createFullTextQuery( query, ArrayBridgeTestEntity.class ).list();
	}

	private ArrayBridgeTestEntity persistEntity(Session s, String name) {
		ArrayBridgeTestEntity boy = new ArrayBridgeTestEntity();
		boy.setName( name );
		s.persist( boy );
		return boy;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ArrayBridgeTestEntity.class, };
	}
}

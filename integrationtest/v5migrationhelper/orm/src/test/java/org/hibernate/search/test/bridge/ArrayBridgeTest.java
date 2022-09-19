/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import java.util.Date;
import java.util.List;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.search.Query;
import org.junit.Before;
import org.junit.Test;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.TermMatchingContext;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.bridge.ArrayBridgeTestEntity.Language;

import static org.hibernate.search.test.bridge.ArrayBridgeTestEntity.Language.ENGLISH;
import static org.hibernate.search.test.bridge.ArrayBridgeTestEntity.Language.ITALIAN;
import static org.hibernate.search.test.bridge.ArrayBridgeTestEntity.Language.KLINGON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test indexing of {@link jakarta.persistence.ElementCollection} annotated arrays.
 *
 * @author Davide D'Alto
 */
public class ArrayBridgeTest extends SearchTestBase {

	private FullTextSession fullTextSession;
	private ArrayBridgeTestEntity withoutNull;
	private ArrayBridgeTestEntity withNullEntry;
	private ArrayBridgeTestEntity withNullEmbedded;
	private Date indexedDate;

	@Override
	@Before
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
	public void testSearchNullEntry() throws Exception {
		List<ArrayBridgeTestEntity> results = findResults( "nullIndexed", ArrayBridgeTestEntity.NULL_LANGUAGE_TOKEN, true );

		assertNotNull( "No result found for an indexed collection", results );
		assertEquals( "Unexpected number of results in a collection", 1, results.size() );
		assertEquals( "Wrong result returned looking for a null in a collection", withNullEntry.getName(), results.get( 0 ).getName() );
	}

	@Test
	public void testSearchNullNumericEntry() throws Exception {
		List<ArrayBridgeTestEntity> results =
				findResults( "numericNullIndexed", ArrayBridgeTestEntity.NULL_NUMERIC_TOKEN_INT, false );

		assertNotNull( "No result found for an indexed collection", results );
		assertEquals( "Unexpected number of results in a collection", 1, results.size() );
		assertEquals( "Wrong result returned looking for a null in a collection of numeric", withNullEntry.getName(), results.get( 0 ).getName() );
	}

	@Test
	public void testSearchNotNullEntry() throws Exception {
		{
			List<ArrayBridgeTestEntity> results = findResults( "nullIndexed", KLINGON, false );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed collection", withNullEntry.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<ArrayBridgeTestEntity> results = findResults( "nullIndexed", ITALIAN, false );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed collection", withoutNull.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<ArrayBridgeTestEntity> results = findResults( "nullIndexed", ENGLISH, false );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 2, results.size() );
		}
	}

	@Test
	public void testSearchEntryWhenNullEntryNotIndexed() throws Exception {
		{
			List<ArrayBridgeTestEntity> results = findResults( "nullNotIndexed", "DaltoValue", false );

			assertNotNull( "No result found for an indexed array", results );
			assertEquals( "Wrong number of results returned for an indexed array", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed array", withoutNull.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<ArrayBridgeTestEntity> results = findResults( "nullNotIndexed", "WorfValue", false );

			assertNotNull( "No result found for an indexed array", results );
			assertEquals( "Wrong number of results returned for an indexed array", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed array", withNullEntry.getName(), results.get( 0 )
					.getName() );
		}
	}

	@Test
	public void testSearchNotNullNumeric() throws Exception {
		{
			List<ArrayBridgeTestEntity> results = findNumericResults( "numericNullIndexed", 1 );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed collection", withoutNull.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<ArrayBridgeTestEntity> results = findNumericResults( "numericNullIndexed", 11 );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed collection", withNullEntry.getName(), results.get( 0 )
					.getName() );
		}
	}

	@Test
	public void testSearchNotNullNumericEntryWhenNullEntryNotIndexed() throws Exception {
		{
			List<ArrayBridgeTestEntity> results = findNumericResults( "numericNullNotIndexed", 3L );

			assertNotNull( "No result found for an indexed array", results );
			assertEquals( "Wrong number of results returned for an indexed array", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed array", withoutNull.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<ArrayBridgeTestEntity> results = findNumericResults( "numericNullNotIndexed", 33L );

			assertNotNull( "No result found for an indexed array", results );
			assertEquals( "Wrong number of results returned for an indexed array", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed array", withNullEntry.getName(), results.get( 0 )
					.getName() );
		}
	}

	@Test
	public void testPrimitiveIndexing() throws Exception {
		List<ArrayBridgeTestEntity> results = findResultsWithRangeQuery(
				"primitive",
				100.0f
		);

		assertNotNull( "No result found for an indexed collection", results );
		assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
		assertEquals(
				"Wrong result returned from a collection of primitive numbers", withoutNull.getName(), results.get( 0 )
						.getName()
		);
	}

	@Test
	public void testDateIndexing() throws Exception {
		List<ArrayBridgeTestEntity> results = findResultsWithRangeQuery(
				"dates",
				DateTools.round( indexedDate, DateTools.Resolution.SECOND )
		);

		assertNotNull( "No result found for an indexed collection", results );
		assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
		assertEquals(
				"Wrong result returned from a collection of Date", withoutNull.getName(), results.get( 0 )
						.getName()
		);
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

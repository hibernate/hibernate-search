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

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.TermMatchingContext;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.search.test.bridge.IterableBridgeTestEntity.Language.ENGLISH;
import static org.hibernate.search.test.bridge.IterableBridgeTestEntity.Language.ITALIAN;
import static org.hibernate.search.test.bridge.IterableBridgeTestEntity.Language.KLINGON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test indexing of {@link jakarta.persistence.ElementCollection} annotated iterables.
 *
 * @author Davide D'Alto
 */
public class IterableBridgeTest extends SearchTestBase {

	private FullTextSession fullTextSession;
	private IterableBridgeTestEntity withoutNull;
	private IterableBridgeTestEntity withNullEntry;
	private IterableBridgeTestEntity withNullEmbedded;
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
	public void testSearchNullEntry() throws Exception {
		{
			List<IterableBridgeTestEntity> results = findResults( "nullIndexed", IterableBridgeTestEntity.NULL_LANGUAGE_TOKEN, true );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Unexpected number of results in a collection", 1, results.size() );
			assertEquals( "Wrong result returned looking for a null in a collection", withNullEntry.getName(), results.get( 0 ).getName() );
		}
	}

	@Test
	public void testSearchNullNumericEntry() throws Exception {
		List<IterableBridgeTestEntity> results = findResults( "numericNullIndexed", IterableBridgeTestEntity.NULL_NUMERIC_TOKEN_INT, false );

		assertNotNull( "No result found for an indexed collection", results );
		assertEquals( "Unexpected number of results in a collection", 1, results.size() );
		assertEquals( "Wrong result returned looking for a null in a collection of numeric", withNullEntry.getName(), results.get( 0 ).getName() );
	}

	@Test
	public void testSearchNotNullEntry() throws Exception {
		{
			List<IterableBridgeTestEntity> results = findResults( "nullIndexed", KLINGON, false );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed collection", withNullEntry.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<IterableBridgeTestEntity> results = findResults( "nullIndexed", ITALIAN, false );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed collection", withoutNull.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<IterableBridgeTestEntity> results = findResults( "nullIndexed", ENGLISH, false );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 2, results.size() );
		}
	}

	@Test
	public void testSearchEntryWhenNullEntryNotIndexed() throws Exception {
		{
			List<IterableBridgeTestEntity> results = findResults( "nullNotIndexed", "DaltoValue", false );

			assertNotNull( "No result found for an indexed array", results );
			assertEquals( "Wrong number of results returned for an indexed array", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed array", withoutNull.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<IterableBridgeTestEntity> results = findResults( "nullNotIndexed", "WorfValue", false );

			assertNotNull( "No result found for an indexed array", results );
			assertEquals( "Wrong number of results returned for an indexed array", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed array", withNullEntry.getName(), results.get( 0 )
					.getName() );
		}
	}

	@Test
	public void testSearchNotNullNumeric() throws Exception {
		{
			List<IterableBridgeTestEntity> results = findNumericResults( "numericNullIndexed", 1 );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed collection", withoutNull.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<IterableBridgeTestEntity> results = findNumericResults( "numericNullIndexed", 11 );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed collection", withNullEntry.getName(), results.get( 0 )
					.getName() );
		}
	}

	@Test
	public void testSearchNotNullNumericEntryWhenNullEntryNotIndexed() throws Exception {
		{
			List<IterableBridgeTestEntity> results = findNumericResults( "numericNullNotIndexed", 3L );

			assertNotNull( "No result found for an indexed array", results );
			assertEquals( "Wrong number of results returned for an indexed array", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed array", withoutNull.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<IterableBridgeTestEntity> results = findNumericResults( "numericNullNotIndexed", 33L );

			assertNotNull( "No result found for an indexed array", results );
			assertEquals( "Wrong number of results returned for an indexed array", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed array", withNullEntry.getName(), results.get( 0 )
					.getName() );
		}
	}

	@Test
	public void testDateIndexing() throws Exception {
		List<IterableBridgeTestEntity> results = findResultsWithRangeQuery(
				"dates",
				DateTools.round( indexedDate, DateTools.Resolution.SECOND )
		);

		assertNotNull( "No result found for an indexed collection", results );
		assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
		assertEquals( "Wrong result returned from a collection of Date", withoutNull.getName(), results.get( 0 )
				.getName() );
	}

	@SuppressWarnings("unchecked")
	private List<IterableBridgeTestEntity> findResults( String fieldName, Object value, boolean checkRawValue) {
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

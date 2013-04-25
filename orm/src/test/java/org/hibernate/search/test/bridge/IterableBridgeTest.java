/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.bridge;

import static org.hibernate.search.test.bridge.IterableBridgeTestEntity.Language.ENGLISH;
import static org.hibernate.search.test.bridge.IterableBridgeTestEntity.Language.ITALIAN;
import static org.hibernate.search.test.bridge.IterableBridgeTestEntity.Language.KLINGON;

import java.util.Date;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestCase;

/**
 * Test indexing of {@link javax.persistence.ElementCollection} annotated elements.
 *
 * @author Davide D'Alto
 */
public class IterableBridgeTest extends SearchTestCase {

	private FullTextSession fullTextSession;
	private IterableBridgeTestEntity withoutNull;
	private IterableBridgeTestEntity withNullEntry;
	private IterableBridgeTestEntity withNullEmbedded;
	private Date indexedDate;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		Session session = openSession();
		fullTextSession = Search.getFullTextSession( session );
		prepareData();
	}

	@Override
	public void tearDown() throws Exception {
		cleanData();
		assertTrue( indexIsEmpty() );
		super.tearDown();
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

	public void testSearchNullEntry() throws Exception {
		{
			List<IterableBridgeTestEntity> results = findResults( "nullIndexed", IterableBridgeTestEntity.NULL_TOKEN );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Unexpected number of results in a collection", 1, results.size() );
			assertEquals( "Wrong result returned looking for a null in a collection", withNullEntry.getName(), results.get( 0 ).getName() );
		}
	}

	public void testSearchNullEmbedded() throws Exception {
		List<IterableBridgeTestEntity> results = findEmbeddedNullResults( "nullIndexed", IterableBridgeTestEntity.NULL_EMBEDDED );

		assertNotNull( "No result found for an indexed collection", results );
		assertEquals( "Unexpected number of results in a collection", 1, results.size() );
		assertEquals( "Wrong result returned looking for a null in a collection", withNullEmbedded.getName(), results.get( 0 ).getName() );
	}

	public void testSearchNullNumericEmbedded() throws Exception {
		List<IterableBridgeTestEntity> results = findEmbeddedNullResults( "embeddedNum", IterableBridgeTestEntity.NULL_EMBEDDED_NUMERIC );

		assertNotNull( "No result found for an indexed collection", results );
		assertEquals( "Unexpected number of results in a collection", 1, results.size() );
		assertEquals( "Wrong result returned looking for a null in a collection of numeric", withNullEmbedded.getName(), results.get( 0 ).getName() );
	}

	public void testSearchNullNumericEntry() throws Exception {
		List<IterableBridgeTestEntity> results = findResults( "numericNullIndexed", IterableBridgeTestEntity.NULL_NUMERIC_TOKEN );

		assertNotNull( "No result found for an indexed collection", results );
		assertEquals( "Unexpected number of results in a collection", 1, results.size() );
		assertEquals( "Wrong result returned looking for a null in a collection of numeric", withNullEntry.getName(), results.get( 0 ).getName() );
	}

	public void testSearchNotNullEntry() throws Exception {
		{
			List<IterableBridgeTestEntity> results = findResults( "nullIndexed", KLINGON );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed collection", withNullEntry.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<IterableBridgeTestEntity> results = findResults( "nullIndexed", ITALIAN );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed collection", withoutNull.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<IterableBridgeTestEntity> results = findResults( "nullIndexed", ENGLISH );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 2, results.size() );
		}
	}

	public void testSearchEntryWhenNullEntryNotIndexed() throws Exception {
		{
			List<IterableBridgeTestEntity> results = findResults( "nullNotIndexed", "DaltoValue" );

			assertNotNull( "No result found for an indexed array", results );
			assertEquals( "Wrong number of results returned for an indexed array", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed array", withoutNull.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<IterableBridgeTestEntity> results = findResults( "nullNotIndexed", "WorfValue" );

			assertNotNull( "No result found for an indexed array", results );
			assertEquals( "Wrong number of results returned for an indexed array", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed array", withNullEntry.getName(), results.get( 0 )
					.getName() );
		}
	}

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

	public void testDateIndexing() throws Exception {
		List<IterableBridgeTestEntity> results = findResults( "dates", indexedDate );

		assertNotNull( "No result found for an indexed collection", results );
		assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
		assertEquals( "Wrong result returned from a collection of Date", withoutNull.getName(), results.get( 0 )
				.getName() );
	}

	@SuppressWarnings("unchecked")
	private List<IterableBridgeTestEntity> findEmbeddedNullResults( String fieldName, Object value) {
		QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder()
				.forEntity( IterableBridgeTestEntity.class ).get();
		Query query = queryBuilder.keyword().onField( fieldName )
				.ignoreAnalyzer()
				.matching( value ).createQuery();
		return fullTextSession.createFullTextQuery( query, IterableBridgeTestEntity.class ).list();
	}

	@SuppressWarnings("unchecked")
	private List<IterableBridgeTestEntity> findResults( String fieldName, Object value) {
		QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder()
				.forEntity( IterableBridgeTestEntity.class ).get();
		Query query = queryBuilder.keyword().onField( fieldName )
				.matching( value ).createQuery();
		return fullTextSession.createFullTextQuery( query, IterableBridgeTestEntity.class ).list();
	}

	@SuppressWarnings("unchecked")
	private List<IterableBridgeTestEntity> findNumericResults( String fieldName, Object number) {
		Query query = NumericFieldUtils.createNumericRangeQuery( fieldName, number, number, true, true );
		return fullTextSession.createFullTextQuery( query, IterableBridgeTestEntity.class ).list();
	}

	private IterableBridgeTestEntity persistEntity(Session s, String name) {
		IterableBridgeTestEntity boy = new IterableBridgeTestEntity();
		boy.setName( name );
		s.persist( boy );
		return boy;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { IterableBridgeTestEntity.class, };
	}

	private void cleanData() {
		Transaction tx = fullTextSession.beginTransaction();
		@SuppressWarnings("unchecked")
		List<IterableBridgeTestEntity> locations = fullTextSession.createCriteria( IterableBridgeTestEntity.class ).list();
		for ( IterableBridgeTestEntity location : locations ) {
			fullTextSession.delete( location );
		}
		tx.commit();
		fullTextSession.close();
	}

	private boolean indexIsEmpty() {
		int numDocsForeigner = countSizeForType( IterableBridgeTestEntity.class );
		return numDocsForeigner == 0;
	}

	private int countSizeForType(Class<?> type) {
		SearchFactory searchFactory = fullTextSession.getSearchFactory();
		int numDocs = -1; // to have it fail in case of errors
		IndexReader locationIndexReader = searchFactory.getIndexReaderAccessor().open( type );
		try {
			numDocs = locationIndexReader.numDocs();
		}
		finally {
			searchFactory.getIndexReaderAccessor().close( locationIndexReader );
		}
		return numDocs;
	}
}

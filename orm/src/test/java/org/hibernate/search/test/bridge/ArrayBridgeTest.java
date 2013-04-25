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

import static org.hibernate.search.test.bridge.ArrayBridgeTestEntity.Language.ENGLISH;
import static org.hibernate.search.test.bridge.ArrayBridgeTestEntity.Language.ITALIAN;
import static org.hibernate.search.test.bridge.ArrayBridgeTestEntity.Language.KLINGON;

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
import org.hibernate.search.test.bridge.ArrayBridgeTestEntity.Language;

/**
 * Test indexing of {@link javax.persistence.ElementCollection} annotated elements.
 *
 * @author Davide D'Alto
 */
public class ArrayBridgeTest extends SearchTestCase {

	private FullTextSession fullTextSession;
	private ArrayBridgeTestEntity withoutNull;
	private ArrayBridgeTestEntity withNullEntry;
	private ArrayBridgeTestEntity withNullEmbedded;
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
		indexedDate = new Date();

		Transaction tx = fullTextSession.beginTransaction();

		withoutNull = persistEntity( fullTextSession, "Davide D'Alto" );
		withoutNull.setNullIndexed( new Language[] { ITALIAN, ENGLISH } );
		withoutNull.setNumericNullIndexed( new Integer[] { 1, 2 } );
		withoutNull.setNullNotIndexed( new String[] { "DaltoValue", "DavideValue" } );
		withoutNull.setNumericNullNotIndexed( new Long[] { 3L, 4L } );
		withoutNull.setDates( new Date[] { indexedDate } );

		withNullEntry = persistEntity( fullTextSession, "Worf" );
		withNullEntry.setNullIndexed( new Language[] { KLINGON, ENGLISH, null } );
		withNullEntry.setNumericNullIndexed( new Integer[] { 11, null } );
		withNullEntry.setNullNotIndexed( new String[] { "WorfValue", null } );
		withNullEntry.setNumericNullNotIndexed( new Long[] { 33L, null } );
		withNullEntry.setDates( new Date[] { null } );

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
		List<ArrayBridgeTestEntity> results = findResults( "nullIndexed", ArrayBridgeTestEntity.NULL_TOKEN );

		assertNotNull( "No result found for an indexed collection", results );
		assertEquals( "Unexpected number of results in a collection", 1, results.size() );
		assertEquals( "Wrong result returned looking for a null in a collection", withNullEntry.getName(), results.get( 0 ).getName() );
	}

	public void testSearchNullEmbedded() throws Exception {
		List<ArrayBridgeTestEntity> results = findEmbeddedNullResults( "nullIndexed", ArrayBridgeTestEntity.NULL_EMBEDDED );

		assertNotNull( "No result found for an indexed collection", results );
		assertEquals( "Unexpected number of results in a collection", 1, results.size() );
		assertEquals( "Wrong result returned looking for a null in a collection", withNullEmbedded.getName(), results.get( 0 ).getName() );
	}

	public void testSearchNullNumericEmbedded() throws Exception {
		List<ArrayBridgeTestEntity> results = findEmbeddedNullResults( "embeddedNum", ArrayBridgeTestEntity.NULL_EMBEDDED_NUMERIC );

		assertNotNull( "No result found for an indexed collection", results );
		assertEquals( "Unexpected number of results in a collection", 1, results.size() );
		assertEquals( "Wrong result returned looking for a null in a collection of numeric", withNullEmbedded.getName(), results.get( 0 ).getName() );
	}

	public void testSearchNullNumericEntry() throws Exception {
		List<ArrayBridgeTestEntity> results = findResults( "numericNullIndexed", ArrayBridgeTestEntity.NULL_NUMERIC_TOKEN );

		assertNotNull( "No result found for an indexed collection", results );
		assertEquals( "Unexpected number of results in a collection", 1, results.size() );
		assertEquals( "Wrong result returned looking for a null in a collection of numeric", withNullEntry.getName(), results.get( 0 ).getName() );
	}

	public void testSearchNotNullEntry() throws Exception {
		{
			List<ArrayBridgeTestEntity> results = findResults( "nullIndexed", KLINGON );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed collection", withNullEntry.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<ArrayBridgeTestEntity> results = findResults( "nullIndexed", ITALIAN );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed collection", withoutNull.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<ArrayBridgeTestEntity> results = findResults( "nullIndexed", ENGLISH );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 2, results.size() );
		}
	}

	public void testSearchEntryWhenNullEntryNotIndexed() throws Exception {
		{
			List<ArrayBridgeTestEntity> results = findResults( "nullNotIndexed", "DaltoValue" );

			assertNotNull( "No result found for an indexed array", results );
			assertEquals( "Wrong number of results returned for an indexed array", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed array", withoutNull.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<ArrayBridgeTestEntity> results = findResults( "nullNotIndexed", "WorfValue" );

			assertNotNull( "No result found for an indexed array", results );
			assertEquals( "Wrong number of results returned for an indexed array", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed array", withNullEntry.getName(), results.get( 0 )
					.getName() );
		}
	}

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

	public void testDateIndexing() throws Exception {
		{
			List<ArrayBridgeTestEntity> results = findResults( "dates", indexedDate );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from a collection of Date", withoutNull.getName(), results.get( 0 )
					.getName() );
		}
	}

	@SuppressWarnings("unchecked")
	private List<ArrayBridgeTestEntity> findEmbeddedNullResults(String fieldName, Object value) {
		QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder()
				.forEntity( ArrayBridgeTestEntity.class ).get();
		Query query = queryBuilder.keyword().onField( fieldName )
				.ignoreAnalyzer()
				.matching( value ).createQuery();
		return fullTextSession.createFullTextQuery( query, ArrayBridgeTestEntity.class ).list();
	}

	@SuppressWarnings("unchecked")
	private List<ArrayBridgeTestEntity> findResults(String fieldName, Object value) {
		QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder()
				.forEntity( ArrayBridgeTestEntity.class ).get();
		Query query = queryBuilder.keyword().onField( fieldName )
				.matching( value ).createQuery();
		return fullTextSession.createFullTextQuery( query, ArrayBridgeTestEntity.class ).list();
	}

	@SuppressWarnings("unchecked")
	private List<ArrayBridgeTestEntity> findNumericResults(String fieldName, Object number) {
		Query query = NumericFieldUtils.createNumericRangeQuery( fieldName, number, number, true, true );
		return fullTextSession.createFullTextQuery( query, ArrayBridgeTestEntity.class ).list();
	}

	private ArrayBridgeTestEntity persistEntity(Session s, String name) {
		ArrayBridgeTestEntity boy = new ArrayBridgeTestEntity();
		boy.setName( name );
		s.persist( boy );
		return boy;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ArrayBridgeTestEntity.class, };
	}

	private void cleanData() {
		Transaction tx = fullTextSession.beginTransaction();
		@SuppressWarnings("unchecked")
		List<ArrayBridgeTestEntity> locations = fullTextSession.createCriteria( ArrayBridgeTestEntity.class ).list();
		for ( ArrayBridgeTestEntity location : locations ) {
			fullTextSession.delete( location );
		}
		tx.commit();
		fullTextSession.close();
	}

	private boolean indexIsEmpty() {
		int numDocsForeigner = countSizeForType( ArrayBridgeTestEntity.class );
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

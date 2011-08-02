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
import org.apache.lucene.queryParser.ParseException;
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
	private IterableBridgeTestEntity withNull;
	private IterableBridgeTestEntity notIndexable;
	private Date indexedDate;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Session session = openSession();
		fullTextSession = Search.getFullTextSession( session );
		prepareData();
	}

	@Override
	protected void tearDown() throws Exception {
		cleanData();
		assertTrue( indexIsEmpty() );
		super.tearDown();
	}

	private void prepareData() {
		Transaction tx = fullTextSession.beginTransaction();

		// Entity without nulls
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
		
		// Entity with nulls
		withNull = persistEntity( fullTextSession, "Worf" );
		withNull.addNullIndexed( KLINGON );
		withNull.addNullIndexed( ENGLISH );
		withNull.addNullIndexed( null );

		withNull.addNumericNullIndexed( 11 );
		withNull.addNumericNullIndexed( null );

		withNull.addNullNotIndexed( "WorfValue" );
		withNull.addNullNotIndexed( null );

		withNull.addNumericNullNotIndexed( 33L );
		withNull.addNumericNullNotIndexed( null );

		withNull.addDate( null );

		// Null collections
		notIndexable = persistEntity( fullTextSession, "Mime" );
		notIndexable.setDates( null );
		notIndexable.setNumericNullIndexed( null );
		notIndexable.setNumericNullNotIndexed( null );
		notIndexable.setNullIndexed( null );
		notIndexable.setNullNotIndexed( null );
		notIndexable.setDates( null );

		tx.commit();
	}

	public void testNullIndexing() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();
		{
			List<IterableBridgeTestEntity> results = findResults( fullTextSession, "nullIndexed", IterableBridgeTestEntity.NULL_TOKEN );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Unexpected number of results in a collection", 1, results.size() );
			assertEquals( "Wrong result returned looking for a null in a collection", withNull.getName(), results.get( 0 ).getName() );
		}
		tx.commit();
	}

	public void testNumericNullIndexing() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();
		{
			List<IterableBridgeTestEntity> results = findResults( fullTextSession, "numericNullIndexed", IterableBridgeTestEntity.NULL_NUMERIC_TOKEN );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Unexpected number of results in a collection", 1, results.size() );
			assertEquals( "Wrong result returned looking for a null in a collection of numeric", withNull.getName(), results.get( 0 ).getName() );
		}
		tx.commit();
	}

	public void testIndexing() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();
		{
			List<IterableBridgeTestEntity> results = findResults( fullTextSession, "nullIndexed", KLINGON );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed collection", withNull.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<IterableBridgeTestEntity> results = findResults( fullTextSession, "nullIndexed", ITALIAN );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed collection", withoutNull.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<IterableBridgeTestEntity> results = findResults( fullTextSession, "nullIndexed", ENGLISH );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 2, results.size() );
		}
		tx.commit();
	}

	public void testIndexingExcludingNull() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();
		{
			List<IterableBridgeTestEntity> results = findResults( fullTextSession, "nullNotIndexed", "DaltoValue" );

			assertNotNull( "No result found for an indexed array", results );
			assertEquals( "Wrong number of results returned for an indexed array", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed array", withoutNull.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<IterableBridgeTestEntity> results = findResults( fullTextSession, "nullNotIndexed", "WorfValue" );

			assertNotNull( "No result found for an indexed array", results );
			assertEquals( "Wrong number of results returned for an indexed array", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed array", withNull.getName(), results.get( 0 )
					.getName() );
		}
		tx.commit();
	}

	public void testNumericIndexing() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();
		{
			List<IterableBridgeTestEntity> results = findNumericResults( fullTextSession, "numericNullIndexed", 1 );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed collection", withoutNull.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<IterableBridgeTestEntity> results = findNumericResults( fullTextSession, "numericNullIndexed", 11 );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed collection", withNull.getName(), results.get( 0 )
					.getName() );
		}
		tx.commit();
	}

	public void testIndexingNumericExcludingNull() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();
		{
			List<IterableBridgeTestEntity> results = findNumericResults( fullTextSession, "numericNullNotIndexed", 3L );

			assertNotNull( "No result found for an indexed array", results );
			assertEquals( "Wrong number of results returned for an indexed array", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed array", withoutNull.getName(), results.get( 0 )
					.getName() );
		}
		{
			List<IterableBridgeTestEntity> results = findNumericResults( fullTextSession, "numericNullNotIndexed", 33L );

			assertNotNull( "No result found for an indexed array", results );
			assertEquals( "Wrong number of results returned for an indexed array", 1, results.size() );
			assertEquals( "Wrong result returned from an indexed array", withNull.getName(), results.get( 0 )
					.getName() );
		}
		tx.commit();
	}

	public void testDateIndexing() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();
		{
			List<IterableBridgeTestEntity> results = findResults( fullTextSession, "dates", indexedDate );

			assertNotNull( "No result found for an indexed collection", results );
			assertEquals( "Wrong number of results returned for an indexed collection", 1, results.size() );
			assertEquals( "Wrong result returned from a collection of Date", withoutNull.getName(), results.get( 0 )
					.getName() );
		}
		tx.commit();
	}
	
	@SuppressWarnings("unchecked")
	private List<IterableBridgeTestEntity> findResults(Session s, String fieldName, Object value) throws ParseException {
		QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder()
				.forEntity( IterableBridgeTestEntity.class ).get();
		Query query = queryBuilder.keyword().onField( fieldName ).matching( value ).createQuery();
		return fullTextSession.createFullTextQuery( query, IterableBridgeTestEntity.class ).list();
	}

	@SuppressWarnings("unchecked")
	private List<IterableBridgeTestEntity> findNumericResults(Session s, String fieldName, Object number)
			throws ParseException {
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
		IndexReader locationIndexReader = searchFactory.openIndexReader( type );
		try {
			numDocs = locationIndexReader.numDocs();
		}
		finally {
			searchFactory.closeIndexReader( locationIndexReader );
		}
		return numDocs;
	}
}

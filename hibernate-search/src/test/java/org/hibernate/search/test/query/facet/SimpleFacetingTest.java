/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.query.facet;

import java.util.List;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetRequest;
import org.hibernate.search.query.facet.FacetResult;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.SimpleFacetRequest;
import org.hibernate.search.test.SearchTestCase;


/**
 * @author Hardy Ferentschik
 */
public class SimpleFacetingTest extends SearchTestCase {

	private static final String[] colors = { "red", "black", "white", "blue" };
	private static final String[] makes = { "Honda", "Toyota", "BMW", "Mercedes" };
	private static final int[] ccs = { 2407, 2831, 3398 };

	private FullTextSession fullTextSession;
	private Transaction tx;

	private String indexFieldName = "cubicCapacity";
	private String facetName = "ccs";

	public void setUp() throws Exception {
		super.setUp();
		fullTextSession = Search.getFullTextSession( openSession() );
		loadTestData( fullTextSession );
		assertTotalAmountOfCars( fullTextSession, 50 );
		tx = fullTextSession.beginTransaction();
	}

	public void tearDown() throws Exception {
		tx.commit();
		fullTextSession.clear();
		fullTextSession.close();
		super.tearDown();
	}

	public void testSimpleFaceting() throws Exception {
		FacetRequest request = new SimpleFacetRequest( indexFieldName );
		FullTextQuery query = queryHondaWithFacet( facetName, request );

		List<Facet> facetList = assertAndReturnFacetList( query );
		assertEquals( "Wrong number of facets", 4, facetList.size() );
	}

	public void testDefaultSortOrderIsCount() throws Exception {
		FacetRequest request = new SimpleFacetRequest( indexFieldName );
		FullTextQuery query = queryHondaWithFacet( facetName, request );

		List<Facet> facetList = assertAndReturnFacetList( query );
		assertCounts( facetList, new int[] { 5, 4, 4, 0 } );
	}

	public void testCountSortOrderAsc() throws Exception {
		FacetRequest request = new SimpleFacetRequest( indexFieldName, FacetSortOrder.COUNT_ASC );
		FullTextQuery query = queryHondaWithFacet( facetName, request );

		List<Facet> facetList = assertAndReturnFacetList( query );
		assertCounts( facetList, new int[] { 0, 4, 4, 5 } );
	}

	public void testCountSortOrderDesc() throws Exception {
		FacetRequest request = new SimpleFacetRequest( indexFieldName, FacetSortOrder.COUNT_DESC );
		FullTextQuery query = queryHondaWithFacet( facetName, request );

		List<Facet> facetList = assertAndReturnFacetList( query );
		assertCounts( facetList, new int[] { 5, 4, 4, 0 } );
	}

	public void testAlphabeticalSortOrder() throws Exception {
		FacetRequest request = new SimpleFacetRequest( indexFieldName, FacetSortOrder.FIELD_VALUE );
		FullTextQuery query = queryHondaWithFacet( facetName, request );

		List<Facet> facetList = assertAndReturnFacetList( query );
		for ( int i = 1; i < facetList.size() - 1; i++ ) {
			String previousFacetValue = facetList.get( i - 1 ).getValue();
			String currentFacetValue = facetList.get( i ).getValue();
			assertTrue( "Wrong alphabetical sort order", previousFacetValue.compareTo( currentFacetValue ) < 0 );
		}
	}

	public void testZeroCountsExcluded() throws Exception {
		FacetRequest request = new SimpleFacetRequest( indexFieldName, FacetSortOrder.COUNT_DESC, false );
		FullTextQuery query = queryHondaWithFacet( facetName, request );

		List<Facet> facetList = assertAndReturnFacetList( query );
		assertCounts( facetList, new int[] { 5, 4, 4 } );
	}

	public void testNullFieldNameThrowsException() {
		try {
			new SimpleFacetRequest( null );
			fail( "null should not be a valid field name" );
		}
		catch ( IllegalArgumentException e ) {
			// success
		}
	}

	// todo - decide on the final behavior of this. Maybe throw an exception?
	public void testUnknownFieldNameReturnsEmptyResults() {
		FacetRequest request = new SimpleFacetRequest( "foobar" );
		FullTextQuery query = queryHondaWithFacet( "foo", request );

		Map<String, FacetResult> results = query.getFacetResults();
		assertNotNull( results );
		FacetResult facetResult = results.get( "foo" );
		assertNotNull( facetResult );
		assertTrue( "A unknown field name should not create any facets", facetResult.getFacets().size() == 0 );
	}

	public void testEnableDisableFacets() {
		FacetRequest request = new SimpleFacetRequest( indexFieldName );
		FullTextQuery query = queryHondaWithFacet( facetName, request );
		Map<String, FacetResult> results = query.getFacetResults();
		assertNotNull( results );
		assertTrue( "We should have one facet result", results.size() == 1 );

		query.disableQueryFacet( facetName );
		query.list();
		results = query.getFacetResults();
		assertNotNull( results );
		assertTrue( "We should have no facets", results.size() == 0 );
	}

	public void testMultipleFacets() {
		final String descendingOrderedFacet = "desc";
		FacetRequest requestDesc = new SimpleFacetRequest( indexFieldName );

		final String ascendingOrderedFacet = "asc";
		FacetRequest requestAsc = new SimpleFacetRequest( indexFieldName, FacetSortOrder.COUNT_ASC );

		TermQuery term = new TermQuery( new Term( "make", "honda" ) );
		FullTextQuery query = fullTextSession.createFullTextQuery( term, Car.class );

		query.enableQueryFacet( descendingOrderedFacet, requestDesc );
		query.enableQueryFacet( ascendingOrderedFacet, requestAsc );

		Map<String, FacetResult> results = query.getFacetResults();
		assertTrue( "We should have two facet result", results.size() == 2 );
		FacetResult facetResult = results.get( descendingOrderedFacet );
		assertCounts( facetResult.getFacets(), new int[] { 5, 4, 4, 0 } );

		facetResult = results.get( ascendingOrderedFacet );
		assertCounts( facetResult.getFacets(), new int[] { 0, 4, 4, 5 } );

		query.disableQueryFacet( descendingOrderedFacet );
		results = query.getFacetResults();
		assertTrue( "We should have only one result", results.size() == 1 );
		facetResult = results.get( ascendingOrderedFacet );
		assertCounts( facetResult.getFacets(), new int[] { 0, 4, 4, 5 } );

		query.disableQueryFacet( ascendingOrderedFacet );
		results = query.getFacetResults();
		assertTrue( "We should have no facets", results.size() == 0 );
	}


	private void assertCounts(List<Facet> facetList, int[] counts) {
		assertTrue( "Wrong number of facets", facetList.size() == counts.length );
		for ( int i = 0; i < facetList.size(); i++ ) {
			assertEquals( "Wrong facet count for facet " + i, counts[i], facetList.get( i ).getCount() );
		}
	}

	private List<Facet> assertAndReturnFacetList(FullTextQuery query) {
		Map<String, FacetResult> results = query.getFacetResults();
		assertNotNull( results );
		FacetResult facetResult = results.get( facetName );
		assertNotNull( facetResult );
		assertEquals( "Wrong facet field name", indexFieldName, facetResult.getFieldName() );
		List<Facet> facetList = facetResult.getFacets();
		assertNotNull( facetList );
		return facetList;
	}

	private FullTextQuery queryHondaWithFacet(String facetName, FacetRequest request) {
		TermQuery term = new TermQuery( new Term( "make", "honda" ) );
		FullTextQuery query = fullTextSession.createFullTextQuery( term, Car.class );
		query.enableQueryFacet( facetName, request );
		assertEquals( "Wrong number of query matches", 13, query.getResultSize() );
		return query;
	}

	private void assertTotalAmountOfCars(FullTextSession session, int total) {
		Transaction tx = session.beginTransaction();
		WildcardQuery wildcard = new WildcardQuery( new Term( "make", "*" ) );
		FullTextQuery query = session.createFullTextQuery( wildcard, Car.class );
		assertEquals( total, query.list().size() );
		tx.commit();
		session.clear();
	}

	private void loadTestData(Session session) {
		Transaction tx = session.beginTransaction();
		for ( String make : makes ) {
			for ( String color : colors ) {
				for ( int cc : ccs ) {
					Car car = new Car( make, color, cc );
					session.save( car );
				}
			}
		}
		Car car = new Car( "Honda", "yellow", 2407 );
		session.save( car );

		car = new Car( "Ford", "yellow", 2500 );
		session.save( car );
		tx.commit();
		session.clear();
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Car.class
		};
	}
}

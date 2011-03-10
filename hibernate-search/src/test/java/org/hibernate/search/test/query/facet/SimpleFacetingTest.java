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
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetRequest;
import org.hibernate.search.query.facet.FacetResult;
import org.hibernate.search.query.facet.FacetSortOrder;


/**
 * @author Hardy Ferentschik
 */
public class SimpleFacetingTest extends AbstractFacetTest {
	private final String indexFieldName = "cubicCapacity";
	private final String facetName = "ccs";

	public void testSimpleFaceting() throws Exception {
		FacetRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.createFacet();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = getFacetListForFacet( query, facetName );
		assertEquals( "Wrong number of facets", 4, facetList.size() );
	}

	public void testDefaultSortOrderIsCount() throws Exception {
		FacetRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.createFacet();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = getFacetListForFacet( query, facetName );
		assertFacetCounts( facetList, new int[] { 5, 4, 4, 0 } );
	}

	public void testCountSortOrderAsc() throws Exception {
		FacetRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_ASC )
				.createFacet();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = getFacetListForFacet( query, facetName );
		assertFacetCounts( facetList, new int[] { 0, 4, 4, 5 } );
	}

	public void testCountSortOrderDesc() throws Exception {
		FacetRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_DESC )
				.createFacet();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = getFacetListForFacet( query, facetName );
		assertFacetCounts( facetList, new int[] { 5, 4, 4, 0 } );
	}

	public void testAlphabeticalSortOrder() throws Exception {
		FacetRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.orderedBy( FacetSortOrder.FIELD_VALUE )
				.createFacet();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = getFacetListForFacet( query, facetName );
		for ( int i = 1; i < facetList.size() - 1; i++ ) {
			String previousFacetValue = facetList.get( i - 1 ).getValue();
			String currentFacetValue = facetList.get( i ).getValue();
			assertTrue( "Wrong alphabetical sort order", previousFacetValue.compareTo( currentFacetValue ) < 0 );
		}
	}

	public void testZeroCountsExcluded() throws Exception {
		FacetRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_DESC )
				.includeZeroCounts( false )
				.createFacet();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = getFacetListForFacet( query, facetName );
		assertFacetCounts( facetList, new int[] { 5, 4, 4 } );
	}

	public void testMaxFacetCounts() throws Exception {
		FacetRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.maxFacetCount( 1 )
				.createFacet();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = getFacetListForFacet( query, facetName );
		assertEquals( "The number of facets should be restricted", 1, facetList.size() );
	}

	public void testNullFieldNameThrowsException() {
		try {
			queryBuilder( Car.class ).facet()
					.name( facetName )
					.onField( null )
					.discrete()
					.createFacet();
			fail( "null should not be a valid field name" );
		}
		catch ( IllegalArgumentException e ) {
			// success
		}
	}

	public void testNullRequestNameThrowsException() {
		try {
			queryBuilder( Car.class ).facet()
					.name( null )
					.onField( indexFieldName )
					.discrete()
					.createFacet();
			fail( "null should not be a valid request name" );
		}
		catch ( IllegalArgumentException e ) {
			// success
		}
	}

	// todo - decide on the final behavior of this. Maybe throw an exception?
	public void testUnknownFieldNameReturnsEmptyResults() {
		FacetRequest request = queryBuilder( Car.class ).facet()
				.name( "foo" )
				.onField( "foobar" )
				.discrete()
				.createFacet();
		FullTextQuery query = queryHondaWithFacet( request );

		Map<String, FacetResult> results = query.getFacetResults();
		assertNotNull( results );
		FacetResult facetResult = results.get( "foo" );
		assertNotNull( facetResult );
		assertTrue( "A unknown field name should not create any facets", facetResult.getFacets().size() == 0 );
	}

	public void testEnableDisableFacets() {
		FacetRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.createFacet();
		FullTextQuery query = queryHondaWithFacet( request );
		Map<String, FacetResult> results = query.getFacetResults();
		assertNotNull( results );
		assertTrue( "We should have one facet result", results.size() == 1 );

		query.disableFacet( facetName );
		query.list();
		results = query.getFacetResults();
		assertNotNull( results );
		assertTrue( "We should have no facets", results.size() == 0 );
	}

	public void testMultipleFacets() {
		final String descendingOrderedFacet = "desc";
		FacetRequest requestDesc = queryBuilder( Car.class ).facet()
				.name( descendingOrderedFacet )
				.onField( indexFieldName )
				.discrete()
				.createFacet();

		final String ascendingOrderedFacet = "asc";
		FacetRequest requestAsc = queryBuilder( Car.class ).facet()
				.name( ascendingOrderedFacet )
				.onField( indexFieldName )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_ASC )
				.createFacet();
		TermQuery term = new TermQuery( new Term( "make", "honda" ) );
		FullTextQuery query = fullTextSession.createFullTextQuery( term, Car.class );

		query.enableFacet( requestDesc );
		query.enableFacet( requestAsc );

		Map<String, FacetResult> results = query.getFacetResults();
		assertTrue( "We should have two facet result", results.size() == 2 );
		FacetResult facetResult = results.get( descendingOrderedFacet );
		assertFacetCounts( facetResult.getFacets(), new int[] { 5, 4, 4, 0 } );

		facetResult = results.get( ascendingOrderedFacet );
		assertFacetCounts( facetResult.getFacets(), new int[] { 0, 4, 4, 5 } );

		query.disableFacet( descendingOrderedFacet );
		results = query.getFacetResults();
		assertTrue( "We should have only one result", results.size() == 1 );
		facetResult = results.get( ascendingOrderedFacet );
		assertFacetCounts( facetResult.getFacets(), new int[] { 0, 4, 4, 5 } );

		query.disableFacet( ascendingOrderedFacet );
		results = query.getFacetResults();
		assertTrue( "We should have no facets", results.size() == 0 );
	}

	private FullTextQuery queryHondaWithFacet(FacetRequest request) {
		Query luceneQuery = queryBuilder( Car.class ).keyword().onField( "make" ).matching( "Honda" ).createQuery();
		FullTextQuery query = fullTextSession.createFullTextQuery( luceneQuery, Car.class );
		query.enableFacet( request );
		assertEquals( "Wrong number of query matches", 13, query.getResultSize() );
		return query;
	}

	public void loadTestData(Session session) {
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

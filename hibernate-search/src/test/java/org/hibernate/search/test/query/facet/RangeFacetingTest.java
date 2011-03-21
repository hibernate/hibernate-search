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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.SearchException;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;

/**
 * @author Hardy Ferentschik
 */
public class RangeFacetingTest extends AbstractFacetTest {
	private static final DateFormat formatter = new SimpleDateFormat( "yyyy" );
	private final String indexFieldName = "price";
	private final String priceRange = "priceRange";

	public void testRangeQueryForInteger() {
		FacetingRequest rangeRequest = queryBuilder( Cd.class ).facet()
				.name( priceRange )
				.onField( indexFieldName )
				.range()
				.from( 0 ).to( 1000 )
				.from( 1001 ).to( 1500 )
				.from( 1501 ).to( 3000 )
				.createFacetingRequest();
		FullTextQuery query = createMatchAllQuery( Cd.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( rangeRequest );

		List<Facet> facets = facetManager.getFacets( priceRange );
		assertFacetCounts( facets, new int[] { 5, 3, 2 } );
	}

	public void testRangeBelow() {
		FacetingRequest rangeRequest = queryBuilder( Cd.class ).facet()
				.name( priceRange )
				.onField( indexFieldName )
				.range()
				.below( 1500 )
				.createFacetingRequest();
		FullTextQuery query = createMatchAllQuery( Cd.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( rangeRequest );

		List<Facet> facets = facetManager.getFacets( priceRange );
		assertFacetCounts( facets, new int[] { 5 } );
	}

	public void testRangeBelowExcludeLimit() {
		FacetingRequest rangeRequest = queryBuilder( Cd.class ).facet()
				.name( priceRange )
				.onField( indexFieldName )
				.range()
				.below( 1500 ).excludeLimit()
				.createFacetingRequest();
		FullTextQuery query = createMatchAllQuery( Cd.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( rangeRequest );

		List<Facet> facets = facetManager.getFacets( priceRange );
		assertFacetCounts( facets, new int[] { 2 } );
	}

	public void testRangeAbove() {
		FacetingRequest rangeRequest = queryBuilder( Cd.class ).facet()
				.name( priceRange )
				.onField( indexFieldName )
				.range()
				.above( 1500 )
				.createFacetingRequest();
		FullTextQuery query = createMatchAllQuery( Cd.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( rangeRequest );

		List<Facet> facets = facetManager.getFacets( priceRange );
		assertFacetCounts( facets, new int[] { 8 } );
	}

	public void testRangeAboveExcludeLimit() {
		FacetingRequest rangeRequest = queryBuilder( Cd.class ).facet()
				.name( priceRange )
				.onField( indexFieldName )
				.range()
				.above( 1500 ).excludeLimit()
				.createFacetingRequest();
		FullTextQuery query = createMatchAllQuery( Cd.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( rangeRequest );

		List<Facet> facets = facetManager.getFacets( priceRange );
		assertFacetCounts( facets, new int[] { 5 } );
	}

	public void testRangeAboveBelow() {
		FacetingRequest rangeRequest = queryBuilder( Cd.class ).facet()
				.name( priceRange )
				.onField( indexFieldName )
				.range()
				.below( 1500 )
				.above( 1500 ).excludeLimit()
				.createFacetingRequest();
		FullTextQuery query = createMatchAllQuery( Cd.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( rangeRequest );

		List<Facet> facets = facetManager.getFacets( priceRange );
		assertFacetCounts( facets, new int[] { 5, 5 } );
	}

	public void testRangeBelowMiddleAbove() {
		FacetingRequest rangeRequest = queryBuilder( Cd.class ).facet()
				.name( priceRange )
				.onField( indexFieldName )
				.range()
				.below( 1000 )
				.from( 1001 ).to( 1500 )
				.above( 1500 ).excludeLimit()
				.createFacetingRequest();
		FullTextQuery query = createMatchAllQuery( Cd.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( rangeRequest );

		List<Facet> facets = facetManager.getFacets( priceRange );
		assertFacetCounts( facets, new int[] { 5, 3, 2 } );
	}

	public void testRangeQueryForDoubleWithZeroCount() {
		FacetingRequest rangeRequest = queryBuilder( Fruit.class ).facet()
				.name( priceRange )
				.onField( indexFieldName )
				.range()
				.from( 0.00 ).to( 1.00 )
				.from( 1.01 ).to( 1.50 )
				.from( 1.51 ).to( 3.00 )
				.from( 4.00 ).to( 5.00 )
				.createFacetingRequest();
		FullTextQuery query = createMatchAllQuery( Fruit.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( rangeRequest );

		List<Facet> facets = facetManager.getFacets( priceRange );
		assertFacetCounts( facets, new int[] { 5, 3, 2, 0 } );
	}

	public void testRangeQueryForDoubleWithoutZeroCount() {
		FacetingRequest rangeRequest = queryBuilder( Fruit.class ).facet()
				.name( priceRange )
				.onField( indexFieldName )
				.range()
				.from( 0.00 ).to( 1.00 )
				.from( 1.01 ).to( 1.50 )
				.from( 1.51 ).to( 3.00 )
				.from( 4.00 ).to( 5.00 )
				.includeZeroCounts( false )
				.orderedBy( FacetSortOrder.COUNT_ASC )
				.createFacetingRequest();

		FullTextQuery query = createMatchAllQuery( Fruit.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( rangeRequest );

		List<Facet> facets = query.getFacetManager().getFacets( priceRange );
		assertFacetCounts( facets, new int[] { 2, 3, 5 } );
		assertEquals( "[0.0, 1.0]", facets.get( 0 ).getValue() );
		assertEquals( "[1.01, 1.5]", facets.get( 1 ).getValue() );
		assertEquals( "[1.51, 3.0]", facets.get( 2 ).getValue() );
	}

	public void testStringRangeFaceting() {
		final String facetingName = "albumNameFaceting";
		final String fieldName = "name_un_analyzed";
		FacetingRequest rangeRequest = queryBuilder( Cd.class ).facet()
				.name( facetingName )
				.onField( fieldName )
				.range()
				.below( "S" ).excludeLimit()
				.from( "S" ).to( "U" )
				.above( "U" ).excludeLimit()
				.orderedBy( FacetSortOrder.RANGE_DEFINITION_ODER )
				.createFacetingRequest();
		FullTextQuery query = createMatchAllQuery( Cd.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( rangeRequest );

		List<Facet> facets = facetManager.getFacets( facetingName );
		assertFacetCounts( facets, new int[] { 7, 1, 2 } );

		facetManager.getFacetGroup( facetingName ).selectFacets( facets.get( 0 ) );
		facets = facetManager.getFacets( facetingName );
		assertFacetCounts( facets, new int[] { 7, 0, 0 } );
	}

	public void testDateRangeFaceting() throws Exception {
		final String facetingName = "albumYearFaceting";
		final String fieldName = "releaseYear";
		FacetingRequest rangeRequest = queryBuilder( Cd.class ).facet()
				.name( facetingName )
				.onField( fieldName )
				.range()
				.below( formatter.parse( "1970" ) ).excludeLimit()
				.from( formatter.parse( "1970" ) ).to( formatter.parse( "1979" ) )
				.from( formatter.parse( "1980" ) ).to( formatter.parse( "1989" ) )
				.from( formatter.parse( "1990" ) ).to( formatter.parse( "1999" ) )
				.above( formatter.parse( "2000" ) ).excludeLimit()
				.orderedBy( FacetSortOrder.RANGE_DEFINITION_ODER )
				.createFacetingRequest();
		FullTextQuery query = createMatchAllQuery( Cd.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( rangeRequest );

		List<Facet> facets = facetManager.getFacets( facetingName );
		assertFacetCounts( facets, new int[] { 1, 2, 2, 0, 5 } );

		facetManager.getFacetGroup( facetingName ).selectFacets( facets.get( 4 ) );
		facets = facetManager.getFacets( facetingName );
		assertFacetCounts( facets, new int[] { 0, 0, 0, 0, 5 } );
	}

	public void testRangeQueryWithUnsupportedType() {
		try {
			queryBuilder( Cd.class ).facet()
					.name( priceRange )
					.onField( indexFieldName )
					.range()
					.from( new Object() ).to( new Object() )
					.createFacetingRequest();
			fail( "Unsupported range faceting type" );
		}
		catch ( SearchException e ) {
			// success
		}
	}

	public void testRangeQueryWithNullToAndFrom() {
		try {
			queryBuilder( Cd.class ).facet()
					.name( priceRange )
					.onField( indexFieldName )
					.range()
					.from( null ).to( null )
					.createFacetingRequest();
			fail( "Unsupported range faceting type" );
		}
		catch ( SearchException e ) {
			// success
		}
	}

	public void loadTestData(Session session) {
		Transaction tx = session.beginTransaction();
		for ( int i = 0; i < albums.length; i++ ) {
			Cd cd = new Cd( albums[i], albumPrices[i], releaseDates[i] );
			session.save( cd );
		}
		for ( int i = 0; i < fruits.length; i++ ) {
			Fruit fruit = new Fruit( fruits[i], fruitPrices[i] );
			session.save( fruit );
		}
		tx.commit();
		session.clear();
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Cd.class,
				Fruit.class
		};
	}
}

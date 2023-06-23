/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.facet;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.testsupport.junit.PortedToSearch6;
import org.hibernate.search.util.common.SearchException;

import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Hardy Ferentschik
 */
public class RangeFacetingTest extends AbstractFacetTest {

	private static final String indexFieldName = "price";
	private static final String priceRange = "priceRange";

	@Test
	@Category(PortedToSearch6.class)
	public void testRangeQueryForInteger() {
		FacetingRequest rangeRequest = queryBuilder( Cd.class ).facet()
				.name( priceRange )
				.onField( indexFieldName )
				.range()
				.from( 0 ).to( 1000 )
				.from( 1001 ).to( 1500 )
				.from( 1501 ).to( 3000 )
				.from( 3001 ).to( 8000 )
				.includeZeroCounts( true )
				.createFacetingRequest();
		FullTextQuery query = createMatchAllQuery( Cd.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( rangeRequest );

		List<Facet> facets = facetManager.getFacets( priceRange );
		assertFacetCounts( facets, new int[] { 5, 3, 2, 0 } );
	}

	@Test
	@Category(PortedToSearch6.class)
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

	@Test
	@Category(PortedToSearch6.class)
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

	@Test
	@Category(PortedToSearch6.class)
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

	@Test
	@Category(PortedToSearch6.class)
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

	@Test
	@Category(PortedToSearch6.class)
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

	@Test
	@Category(PortedToSearch6.class)
	public void testRangeBelowMiddleAbove() {
		final String facetingName = "cdPriceFaceting";
		FacetingRequest rangeRequest = queryBuilder( Cd.class ).facet()
				.name( facetingName )
				.onField( indexFieldName )
				.range()
				.below( 1000 )
				.from( 1001 ).to( 1500 )
				.above( 1501 )
				.createFacetingRequest();
		FullTextQuery query = createMatchAllQuery( Cd.class );
		query.getFacetManager().enableFaceting( rangeRequest );

		List<Facet> facets = query.getFacetManager().getFacets( facetingName );
		assertFacetCounts( facets, new int[] { 5, 3, 2 } );
	}

	@Test
	@Category(PortedToSearch6.class)
	public void testRangeWithExcludeLimitsAtEachLevel() {
		final String facetingName = "cdPriceFaceting";
		FacetingRequest rangeRequest = queryBuilder( Cd.class ).facet()
				.name( facetingName )
				.onField( indexFieldName )
				.range()
				.below( 1000 ).excludeLimit()
				.from( 1000 ).to( 1500 ).excludeLimit()
				.from( 1500 ).to( 2000 ).excludeLimit()
				.above( 2000 )
				.orderedBy( FacetSortOrder.RANGE_DEFINITION_ORDER )
				.includeZeroCounts( true )
				.createFacetingRequest();
		FullTextQuery query = createMatchAllQuery( Cd.class );
		query.getFacetManager().enableFaceting( rangeRequest );

		List<Facet> facets = query.getFacetManager().getFacets( facetingName );
		assertFacetCounts( facets, new int[] { 2, 0, 6, 2 } );

		rangeRequest = queryBuilder( Cd.class ).facet()
				.name( facetingName )
				.onField( indexFieldName )
				.range()
				.below( 1000 )
				.from( 1000 ).excludeLimit().to( 1500 )
				.from( 1500 ).excludeLimit().to( 2000 )
				.above( 2000 ).excludeLimit()
				.orderedBy( FacetSortOrder.RANGE_DEFINITION_ORDER )
				.createFacetingRequest();
		query = createMatchAllQuery( Cd.class );
		query.getFacetManager().enableFaceting( rangeRequest );

		facets = query.getFacetManager().getFacets( facetingName );
		assertFacetCounts( facets, new int[] { 2, 3, 4, 1 } );

	}

	@Test
	@Category(PortedToSearch6.class)
	public void testRangeQueryForDoubleWithZeroCount() {
		FacetingRequest rangeRequest = queryBuilder( Fruit.class ).facet()
				.name( priceRange )
				.onField( indexFieldName )
				.range()
				.from( 0.00 ).to( 1.00 )
				.from( 1.01 ).to( 1.50 )
				.from( 1.51 ).to( 3.00 )
				.from( 4.00 ).to( 5.00 )
				.includeZeroCounts( true )
				.createFacetingRequest();
		FullTextQuery query = createMatchAllQuery( Fruit.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( rangeRequest );

		List<Facet> facets = facetManager.getFacets( priceRange );
		assertFacetCounts( facets, new int[] { 5, 3, 2, 0 } );
	}

	@Test
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

	@Test
	public void testRangeQueryRangeDefOrderHigherMaxCount() {
		FacetingRequest rangeRequest = queryBuilder( Fruit.class ).facet()
				.name( priceRange )
				.onField( indexFieldName )
				.range()
				.from( 0.00 ).to( 1.00 )
				.from( 1.01 ).to( 1.50 )
				.from( 1.51 ).to( 3.00 )
				.from( 4.00 ).to( 5.00 )
				.includeZeroCounts( false )
				.orderedBy( FacetSortOrder.RANGE_DEFINITION_ORDER )
				.maxFacetCount( 5 )
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

	@Test
	@Category(PortedToSearch6.class)
	public void testDateRangeFaceting() throws Exception {
		final String facetingName = "albumYearFaceting";
		final String fieldName = "releaseYear";
		final DateFormat formatter = new SimpleDateFormat( "yyyy", Locale.ROOT );
		FacetingRequest rangeRequest = queryBuilder( Cd.class ).facet()
				.name( facetingName )
				.onField( fieldName )
				.range()
				.below( formatter.parse( "1970" ) ).excludeLimit()
				.from( formatter.parse( "1970" ) ).to( formatter.parse( "1979" ) )
				.from( formatter.parse( "1980" ) ).to( formatter.parse( "1989" ) )
				.from( formatter.parse( "1990" ) ).to( formatter.parse( "1999" ) )
				.above( formatter.parse( "2000" ) ).excludeLimit()
				.orderedBy( FacetSortOrder.RANGE_DEFINITION_ORDER )
				.createFacetingRequest();
		FullTextQuery query = createMatchAllQuery( Cd.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( rangeRequest );

		List<Facet> facets = facetManager.getFacets( facetingName );
		assertFacetCounts( facets, new int[] { 1, 2, 2, 5 } );
	}

	@Test
	@Category(PortedToSearch6.class)
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
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000269" ) );
		}
	}

	@Test
	@Category(PortedToSearch6.class)
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
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000270" ) );
		}
	}

	@Test
	@Category(PortedToSearch6.class)
	public void testUnsupportedRangeParameterTypeThrowsException() {
		FacetingRequest rangeRequest = queryBuilder( Fruit.class ).facet()
				.name( priceRange )
				.onField( indexFieldName )
				.range()
				.from( "0.00" ).to( "1.00" )
				.createFacetingRequest();

		FullTextQuery query = createMatchAllQuery( Fruit.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( rangeRequest );

		assertThatThrownBy( () -> {
			query.getFacetManager().getFacets( priceRange );
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid type for DSL arguments: 'java.lang.String'.",
						"Expected 'java.lang.Double' or a subtype" );
	}

	@Override
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
		for ( Integer horsePower : horsePowers ) {
			Truck truck = new Truck( horsePower );
			session.save( truck );
		}
		tx.commit();
		session.clear();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Cd.class,
				Fruit.class,
				Truck.class
		};
	}
}

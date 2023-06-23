/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.facet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.PortedToSearch6;
import org.hibernate.search.util.common.SearchException;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * @author Hardy Ferentschik
 */
public class SimpleFacetingTest extends AbstractFacetTest {
	private final String facetName = "ccs";

	@Test
	@Category(PortedToSearch6.class)
	public void testSimpleDiscretFaceting() throws Exception {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.createFacetingRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertEquals( "Wrong number of facets", 3, facetList.size() );
	}

	@Test
	@Category(PortedToSearch6.class)
	public void testDefaultSortOrderIsCount() throws Exception {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.createFacetingRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertFacetCounts( facetList, new int[] { 5, 4, 4 } );
	}

	@Test
	@Category(PortedToSearch6.class)
	public void testCountSortOrderAsc() throws Exception {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_ASC )
				.createFacetingRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertFacetCounts( facetList, new int[] { 4, 4, 5 } );
	}

	@Test
	@Category(PortedToSearch6.class)
	public void testCountSortOrderDesc() throws Exception {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_DESC )
				.createFacetingRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertFacetCounts( facetList, new int[] { 5, 4, 4 } );
	}

	@Test
	@Category(PortedToSearch6.class)
	public void testAlphabeticalSortOrder() throws Exception {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.orderedBy( FacetSortOrder.FIELD_VALUE )
				.createFacetingRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		for ( int i = 1; i < facetList.size() - 1; i++ ) {
			String previousFacetValue = facetList.get( i - 1 ).getValue();
			String currentFacetValue = facetList.get( i ).getValue();
			assertTrue( "Wrong alphabetical sort order", previousFacetValue.compareTo( currentFacetValue ) < 0 );
		}
	}

	@Test
	@Category(PortedToSearch6.class)
	public void testZeroCountsExcluded() throws Exception {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_DESC )
				.includeZeroCounts( false )
				.createFacetingRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertFacetCounts( facetList, new int[] { 5, 4, 4 } );
	}

	@Test
	@Category(PortedToSearch6.class)
	public void testZeroCountsIncluded() throws Exception {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_DESC )
				.includeZeroCounts( true )
				.createFacetingRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertFacetCounts( facetList, new int[] { 5, 4, 4, 0 } );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-776")
	@Category(PortedToSearch6.class)
	public void testMaxFacetCounts() throws Exception {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_DESC )
				.maxFacetCount( 1 )
				.createFacetingRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertEquals( "The number of facets should be restricted", 1, facetList.size() );
		assertFacetCounts( facetList, new int[] { 5 } );
	}

	@Test
	@Category(PortedToSearch6.class)
	public void testNullFieldNameThrowsException() {
		try {
			queryBuilder( Car.class ).facet()
					.name( facetName )
					.onField( null )
					.discrete()
					.createFacetingRequest();
			fail( "null should not be a valid field name" );
		}
		catch (IllegalArgumentException e) {
			// success
		}
	}

	@Test
	public void testNullRequestNameThrowsException() {
		try {
			queryBuilder( Car.class ).facet()
					.name( null )
					.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
					.discrete()
					.createFacetingRequest();
			fail( "null should not be a valid request name" );
		}
		catch (IllegalArgumentException e) {
			// success
		}
	}

	@Test
	public void testRangeDefinitionSortOrderThrowsExceptionForDiscreteFaceting() {
		try {
			queryBuilder( Car.class ).facet()
					.name( facetName )
					.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
					.discrete()
					.orderedBy( FacetSortOrder.RANGE_DEFINITION_ORDER )
					.createFacetingRequest();
			fail( "RANGE_DEFINITION_ODER not allowed on discrete faceting" );
		}
		catch (SearchException e) {
			// success
		}
	}

	@Test
	public void testEnableDisableFacets() {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.createFacetingRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		assertTrue( "We should have facet results", query.getFacetManager().getFacets( facetName ).size() > 0 );

		query.getFacetManager().disableFaceting( facetName );
		query.list();

		assertTrue( "We should have no facets", query.getFacetManager().getFacets( facetName ).size() == 0 );
	}

	@Test
	@Category(PortedToSearch6.class)
	public void testMultipleFacets() {
		final String descendingOrderedFacet = "desc";
		FacetingRequest requestDesc = queryBuilder( Car.class ).facet()
				.name( descendingOrderedFacet )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.includeZeroCounts( true )
				.createFacetingRequest();

		final String ascendingOrderedFacet = "asc";
		FacetingRequest requestAsc = queryBuilder( Car.class ).facet()
				.name( ascendingOrderedFacet )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_ASC )
				.includeZeroCounts( true )
				.createFacetingRequest();
		TermQuery term = new TermQuery( new Term( "make", "Honda" ) );
		FullTextQuery query = fullTextSession.createFullTextQuery( term, Car.class );
		FacetManager facetManager = query.getFacetManager();

		facetManager.enableFaceting( requestDesc );
		facetManager.enableFaceting( requestAsc );

		assertFacetCounts( facetManager.getFacets( descendingOrderedFacet ), new int[] { 5, 4, 4, 0 } );
		assertFacetCounts( facetManager.getFacets( ascendingOrderedFacet ), new int[] { 0, 4, 4, 5 } );

		facetManager.disableFaceting( descendingOrderedFacet );
		assertTrue(
				"descendingOrderedFacet should be disabled", query.getFacetManager().getFacets(
						descendingOrderedFacet
				).isEmpty()
		);
		assertFacetCounts( facetManager.getFacets( ascendingOrderedFacet ), new int[] { 0, 4, 4, 5 } );

		facetManager.disableFaceting( ascendingOrderedFacet );
		assertTrue(
				"descendingOrderedFacet should be disabled",
				facetManager.getFacets( descendingOrderedFacet ).isEmpty()
		);
		assertTrue(
				"ascendingOrderedFacet should be disabled",
				facetManager.getFacets( ascendingOrderedFacet ).isEmpty()
		);
	}

	private FullTextQuery queryHondaWithFacet(FacetingRequest request) {
		Query luceneQuery = queryBuilder( Car.class )
				.keyword()
				.onField( "make" )
				.matching( "Honda" )
				.createQuery();
		FullTextQuery query = fullTextSession.createFullTextQuery( luceneQuery, Car.class );
		query.getFacetManager().enableFaceting( request );
		assertEquals( "Wrong number of query matches", 13, query.getResultSize() );
		return query;
	}

	@Override
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

		Fruit apple = new Fruit( "Apple", 3.15 );
		session.save( apple );

		tx.commit();
		session.clear();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Car.class,
				Fruit.class
		};
	}
}

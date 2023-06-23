/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.facet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.testsupport.junit.PortedToSearch6;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * @author Hardy Ferentschik
 */
@Category(PortedToSearch6.class)
public class NoQueryResultsFacetingTest extends AbstractFacetTest {
	private final String facetName = "ccs";

	@Test
	public void testSimpleDiscretFacetingWithNoResultsQuery() throws Exception {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.createFacetingRequest();
		FullTextQuery query = queryHondaNoResultsWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertEquals( "Wrong number of facets", 0, facetList.size() );
	}

	@Test
	public void testMultipleFacetsWithNoResultsQuery() {
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
		TermQuery term = new TermQuery( new Term( "make", "nonExistentValue" ) );
		FullTextQuery query = fullTextSession.createFullTextQuery( term, Car.class );
		FacetManager facetManager = query.getFacetManager();

		facetManager.enableFaceting( requestDesc );
		facetManager.enableFaceting( requestAsc );

		assertFacetCounts( facetManager.getFacets( descendingOrderedFacet ), new int[] { 0, 0, 0, 0 } );
		assertFacetCounts( facetManager.getFacets( ascendingOrderedFacet ), new int[] { 0, 0, 0, 0 } );

		facetManager.disableFaceting( descendingOrderedFacet );
		assertTrue(
				"descendingOrderedFacet should be disabled", query.getFacetManager().getFacets(
						descendingOrderedFacet
				).isEmpty()
		);
		assertFacetCounts( facetManager.getFacets( ascendingOrderedFacet ), new int[] { 0, 0, 0, 0 } );

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

	private FullTextQuery queryHondaNoResultsWithFacet(FacetingRequest request) {
		Query luceneQuery = queryBuilder( Car.class )
				.keyword()
				.onField( "make" )
				.matching( "nonExistentValue" )
				.createQuery();
		FullTextQuery query = fullTextSession.createFullTextQuery( luceneQuery, Car.class );
		query.getFacetManager().enableFaceting( request );
		assertEquals( "Wrong number of query matches", 0, query.getResultSize() );
		return query;
	}

	@Test
	public void testSimpleDiscretFacetingQuery() throws Exception {
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
	public void testMultipleFacetsQuery() {
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

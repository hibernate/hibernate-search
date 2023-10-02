/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.facet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.Tags;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * @author Hardy Ferentschik
 */
class SimpleFacetingTest extends AbstractFacetTest {
	private final String facetName = "ccs";

	@Test
	@Tag(Tags.PORTED_TO_SEARCH_6)
	void testSimpleDiscretFaceting() throws Exception {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.createFacetingRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertThat( facetList ).as( "Wrong number of facets" ).hasSize( 3 );
	}

	@Test
	@Tag(Tags.PORTED_TO_SEARCH_6)
	void testDefaultSortOrderIsCount() {
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
	@Tag(Tags.PORTED_TO_SEARCH_6)
	void testCountSortOrderAsc() {
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
	@Tag(Tags.PORTED_TO_SEARCH_6)
	void testCountSortOrderDesc() {
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
	@Tag(Tags.PORTED_TO_SEARCH_6)
	void testAlphabeticalSortOrder() {
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
			assertThat( previousFacetValue.compareTo( currentFacetValue ) ).as( "Wrong alphabetical sort order" )
					.isLessThan( 0 );
		}
	}

	@Test
	@Tag(Tags.PORTED_TO_SEARCH_6)
	void testZeroCountsExcluded() {
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
	@Tag(Tags.PORTED_TO_SEARCH_6)
	void testZeroCountsIncluded() {
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
	@Tag(Tags.PORTED_TO_SEARCH_6)
	void testMaxFacetCounts() {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_DESC )
				.maxFacetCount( 1 )
				.createFacetingRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertThat( facetList ).as( "The number of facets should be restricted" ).hasSize( 1 );
		assertFacetCounts( facetList, new int[] { 5 } );
	}

	@Test
	@Tag(Tags.PORTED_TO_SEARCH_6)
	void testNullFieldNameThrowsException() {
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
	void testNullRequestNameThrowsException() {
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
	void testRangeDefinitionSortOrderThrowsExceptionForDiscreteFaceting() {
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
	void testEnableDisableFacets() {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.createFacetingRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		assertThat( query.getFacetManager().getFacets( facetName ) ).as( "We should have facet results" ).isNotEmpty();

		query.getFacetManager().disableFaceting( facetName );
		query.list();

		assertThat( query.getFacetManager().getFacets( facetName ) ).as( "We should have no facets" ).isEmpty();
	}

	@Test
	@Tag(Tags.PORTED_TO_SEARCH_6)
	void testMultipleFacets() {
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
		assertThat(
				query.getFacetManager().getFacets( descendingOrderedFacet )
		).as( "descendingOrderedFacet should be disabled" ).isEmpty();
		assertFacetCounts( facetManager.getFacets( ascendingOrderedFacet ), new int[] { 0, 4, 4, 5 } );

		facetManager.disableFaceting( ascendingOrderedFacet );
		assertThat(
				facetManager.getFacets( descendingOrderedFacet )
		).as( "descendingOrderedFacet should be disabled" ).isEmpty();
		assertThat(
				facetManager.getFacets( ascendingOrderedFacet )
		).as( "ascendingOrderedFacet should be disabled" ).isEmpty();
	}

	private FullTextQuery queryHondaWithFacet(FacetingRequest request) {
		Query luceneQuery = queryBuilder( Car.class )
				.keyword()
				.onField( "make" )
				.matching( "Honda" )
				.createQuery();
		FullTextQuery query = fullTextSession.createFullTextQuery( luceneQuery, Car.class );
		query.getFacetManager().enableFaceting( request );
		assertThat( query.getResultSize() ).as( "Wrong number of query matches" ).isEqualTo( 13 );
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

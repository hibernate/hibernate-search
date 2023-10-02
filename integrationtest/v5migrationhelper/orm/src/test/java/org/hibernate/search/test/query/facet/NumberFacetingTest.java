/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.facet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.testsupport.junit.Tags;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

/**
 * @author Hardy Ferentschik
 */
@Tag(Tags.PORTED_TO_SEARCH_6)
class NumberFacetingTest extends AbstractFacetTest {

	@Test
	void testSimpleFaceting() {
		String facetName = "ccs";
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.createFacetingRequest();
		FullTextQuery query = matchAll( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertThat( facetList ).as( "Wrong number of facets" ).hasSize( 4 );

		assertFacet( facetList.get( 0 ), "2407", 17 );
		assertFacet( facetList.get( 1 ), "2831", 16 );
		assertFacet( facetList.get( 2 ), "3398", 16 );
		assertFacet( facetList.get( 3 ), "2500", 1 );
	}

	private void assertFacet(Facet facet, String expectedCubicCapacity, int expectedCount) {
		assertThat( facet.getValue() ).as( "Wrong facet value" ).isEqualTo( expectedCubicCapacity );
		assertThat( facet.getCount() ).as( "Wrong facet count" ).isEqualTo( expectedCount );
	}

	private FullTextQuery matchAll(FacetingRequest request) {
		Query luceneQuery = new MatchAllDocsQuery();
		FullTextQuery query = fullTextSession.createFullTextQuery( luceneQuery, Car.class );
		query.getFacetManager().enableFaceting( request );
		assertThat( query.getResultSize() ).as( "Wrong number of indexed cars" ).isEqualTo( 50 );
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
		tx.commit();
		session.clear();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Car.class
		};
	}
}

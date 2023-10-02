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
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.jupiter.api.Test;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

/**
 * @author Hardy Ferentschik
 */
class StringFacetingTest extends AbstractFacetTest {

	@Test
	@TestForIssue(jiraKey = "HSEARCH-809")
	void testStringFaceting() {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( "manufacturer" )
				.onField( "make" )
				.discrete()
				.includeZeroCounts( false )
				.createFacetingRequest();
		FullTextQuery query = matchAll( request );

		List<Facet> facetList = query.getFacetManager().getFacets( "manufacturer" );
		assertThat( facetList ).as( "Wrong number of facets" ).hasSize( 5 );

		assertFacet( facetList.get( 0 ), "Honda", 13 );
		assertFacet( facetList.get( 1 ), "BMW", 12 );
		assertFacet( facetList.get( 2 ), "Mercedes", 12 );
		assertFacet( facetList.get( 3 ), "Toyota", 12 );
		assertFacet( facetList.get( 4 ), "Ford", 1 );
	}

	private void assertFacet(Facet facet, String expectedMake, int expectedCount) {
		assertThat( facet.getValue() ).as( "Wrong facet value" ).isEqualTo( expectedMake );
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

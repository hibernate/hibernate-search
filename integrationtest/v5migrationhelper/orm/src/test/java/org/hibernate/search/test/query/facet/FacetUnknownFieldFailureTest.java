/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.facet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.testsupport.junit.Tags;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

@Tag(Tags.PORTED_TO_SEARCH_6)
class FacetUnknownFieldFailureTest extends AbstractFacetTest {

	@Test
	void testUnknownFieldNameThrowsException() {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( "foo" ) // faceting name is irrelevant
				.onField( "foobar" ) // foobar is not a valid field name
				.discrete()
				.createFacetingRequest();
		assertThatThrownBy( () -> queryHondaWithFacet( request ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field 'foobar'" );
	}

	@Test
	void testKnownFieldNameNotConfiguredForFacetingThrowsException() {
		FacetingRequest request = queryBuilder( Fruit.class ).facet()
				.name( "foo" ) // faceting name is irrelevant
				.onField( "name" ) // name is a valid property of apple, but not configured for faceting
				.discrete()
				.createFacetingRequest();

		assertThatThrownBy( () -> {
			FullTextQuery query = fullTextSession.createFullTextQuery( new MatchAllDocsQuery(), Fruit.class );
			query.getFacetManager().enableFaceting( request );
			assertThat( query.getResultSize() ).as( "Wrong number of query matches" ).isEqualTo( 1 );

			query.getFacetManager().getFacets( "foo" );
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Cannot use 'aggregation:terms' on field 'name'" );
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

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

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetFilter;
import org.hibernate.search.query.facet.FacetRequest;

/**
 * @author Hardy Ferentschik
 */
public class FacetFilteringTest extends AbstractFacetTest {
	public void testDiscreteFacetDrillDown() throws Exception {
		final String indexFieldName = "cubicCapacity";
		final String facetName = "ccs";
		Query luceneQuery = queryBuilder( Car.class ).keyword().onField( "make" ).matching( "Honda" ).createQuery();
		FacetRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.createFacet();

		FullTextQuery query = fullTextSession.createFullTextQuery( luceneQuery, Car.class );
		query.enableFacet( request );
		assertEquals( "Wrong number of query matches", 13, query.getResultSize() );

		List<Facet> facetList = getFacetListForFacet( query, facetName );
		assertFacetCounts( facetList, new int[] { 5, 4, 4, 0 } );

		Filter facetFilter = facetList.get( 0 ).getFacetFilter();
		query.setFilter( facetFilter );

		assertEquals( "Wrong number of query matches", 5, query.getResultSize() );
		List<Facet> newFacetList = getFacetListForFacet( query, facetName );
		assertFacetCounts( newFacetList, new int[] { 5, 0, 0, 0 } );

		facetFilter = facetList.get( 1 ).getFacetFilter();
		query.setFilter( facetFilter );

		assertEquals( "Wrong number of query matches", 4, query.getResultSize() );
		newFacetList = getFacetListForFacet( query, facetName );
		assertFacetCounts( newFacetList, new int[] { 4, 0, 0, 0 } );
	}

	public void testMultipleFacetDrillDown() throws Exception {
		final String ccsFacetName = "ccs";
		final String ccsFacetFieldName = "cubicCapacity";
		FacetRequest ccsFacetRequest = queryBuilder( Car.class ).facet()
				.name( ccsFacetName )
				.onField( ccsFacetFieldName )
				.discrete()
				.createFacet();

		final String colorFacetName = "color";
		final String colorFacetFieldName = "color";
		FacetRequest colorFacetRequest = queryBuilder( Car.class ).facet()
				.name( colorFacetName )
				.onField( colorFacetFieldName )
				.discrete()
				.createFacet();

		FullTextQuery query = createMatchAllQuery( Car.class );
		query.enableFacet( colorFacetRequest );
		query.enableFacet( ccsFacetRequest );
		assertEquals( "Wrong number of query matches", 50, query.getResultSize() );

		List<Facet> colorFacetList = getFacetListForFacet( query, colorFacetName );
		assertFacetCounts( colorFacetList, new int[] { 12, 12, 12, 12, 2 } );

		List<Facet> ccsFacetList = getFacetListForFacet( query, ccsFacetName );
		assertFacetCounts( ccsFacetList, new int[] { 17, 16, 16, 1 } );

		FacetFilter facetFilter = new FacetFilter();
		query.setFilter( facetFilter );

		facetFilter.addFacet( colorFacetList.get( 0 ) );
		colorFacetList = getFacetListForFacet( query, colorFacetName );
		assertFacetCounts( colorFacetList, new int[] { 12, 0, 0, 0, 0 } );

		ccsFacetList = getFacetListForFacet( query, ccsFacetName );
		assertFacetCounts( ccsFacetList, new int[] { 4, 4, 4, 0 } );

		facetFilter.addFacet( ccsFacetList.get( 0 ) );
		// needs to set the filter explicitly atm, because I need the query state to reset
		query.setFilter( facetFilter );
		colorFacetList = getFacetListForFacet( query, colorFacetName );
		assertFacetCounts( colorFacetList, new int[] { 4, 0, 0, 0, 0 } );

		ccsFacetList = getFacetListForFacet( query, ccsFacetName );
		assertFacetCounts( ccsFacetList, new int[] { 4, 0, 0, 0 } );
	}

	public void testRangeFacetDrillDown() {
		final String indexFieldName = "price";
		final String priceRange = "priceRange";
		FacetRequest rangeRequest = queryBuilder( Fruit.class ).facet()
				.name( priceRange )
				.onField( indexFieldName )
				.range()
				.from( 0.00 ).to( 1.00 )
				.from( 1.01 ).to( 1.50 )
				.from( 1.51 ).to( 3.00 )
				.from( 4.00 ).to( 5.00 )
				.createFacet();
		FullTextQuery query = createMatchAllQuery( Fruit.class );
		query.enableFacet( rangeRequest );

		assertEquals( "Wrong number of query matches", 10, query.getResultSize() );

		List<Facet> facets = getFacetListForFacet( query, priceRange );
		assertFacetCounts( facets, new int[] { 5, 3, 2, 0 } );

		Filter facetFilter = facets.get( 2 ).getFacetFilter();
		query.setFilter( facetFilter );

		assertEquals( "Wrong number of query matches", 2, query.getResultSize() );
		List<Facet> newFacetList = getFacetListForFacet( query, priceRange );
		assertFacetCounts( newFacetList, new int[] { 2, 0, 0, 0 } );
	}

	public void loadTestData(Session session) {
		Transaction tx = session.beginTransaction();
		for ( int i = 0; i < fruits.length; i++ ) {
			Fruit fruit = new Fruit( fruits[i], fruitPrices[i] );
			session.save( fruit );
		}

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
				Car.class,
				Fruit.class
		};
	}
}

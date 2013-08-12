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

import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetingRequest;

/**
 * @author Hardy Ferentschik
 */
public class FacetFilteringTest extends AbstractFacetTest {
	public void testDiscreteFacetDrillDown() throws Exception {
		final String indexFieldName = "cubicCapacity";
		final String facetName = "ccs";
		Query luceneQuery = queryBuilder( Car.class ).keyword().onField( "make" ).matching( "Honda" ).createQuery();
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.createFacetingRequest();

		FullTextQuery query = fullTextSession.createFullTextQuery( luceneQuery, Car.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( request );
		query.setFirstResult( 0 ).setMaxResults( 1 );
		assertEquals( "Wrong number of query matches", 13, query.getResultSize() );

		List<Facet> facetList = facetManager.getFacets( facetName );
		assertFacetCounts( facetList, new int[] { 5, 4, 4, 0 } );

		facetManager.getFacetGroup( facetName ).selectFacets( facetList.get( 0 ) );
		query.list();
		assertEquals( "Wrong number of query matches", 5, query.getResultSize() );
		List<Facet> newFacetList = facetManager.getFacets( facetName );
		assertFacetCounts( newFacetList, new int[] { 5, 0, 0, 0 } );

		facetManager.getFacetGroup( facetName ).selectFacets( facetList.get( 1 ) );
		query.setMaxResults( Integer.MAX_VALUE );
		assertEquals( "Wrong number of query matches", 9, query.list().size() );
		newFacetList = facetManager.getFacets( facetName );
		assertFacetCounts( newFacetList, new int[] { 5, 4, 0, 0 } );
	}

	public void testMultipleFacetDrillDown() throws Exception {
		final String ccsFacetName = "ccs";
		final String ccsFacetFieldName = "cubicCapacity";
		FacetingRequest ccsFacetRequest = queryBuilder( Car.class ).facet()
				.name( ccsFacetName )
				.onField( ccsFacetFieldName )
				.discrete()
				.createFacetingRequest();

		final String colorFacetName = "color";
		final String colorFacetFieldName = "color";
		FacetingRequest colorFacetRequest = queryBuilder( Car.class ).facet()
				.name( colorFacetName )
				.onField( colorFacetFieldName )
				.discrete()
				.createFacetingRequest();

		FullTextQuery query = createMatchAllQuery( Car.class );
		FacetManager facetManager = query.getFacetManager();
		facetManager.enableFaceting( colorFacetRequest );
		facetManager.enableFaceting( ccsFacetRequest );
		assertEquals( "Wrong number of query matches", 50, query.getResultSize() );
		assertFacetCounts( facetManager.getFacets( colorFacetName ), new int[] { 12, 12, 12, 12, 2 } );
		assertFacetCounts( facetManager.getFacets( ccsFacetName ), new int[] { 17, 16, 16, 1 } );

		Facet colorFacet = facetManager.getFacets( colorFacetName ).get( 0 );
		facetManager.getFacetGroup( colorFacetName ).selectFacets( colorFacet );
		assertFacetCounts( facetManager.getFacets( colorFacetName ), new int[] { 12, 0, 0, 0, 0 } );
		assertFacetCounts( facetManager.getFacets( ccsFacetName ), new int[] { 4, 4, 4, 0 } );

		Facet ccsFacet = facetManager.getFacets( ccsFacetName ).get( 0 );
		facetManager.getFacetGroup( colorFacetName ).selectFacets( colorFacet );
		facetManager.getFacetGroup( ccsFacetName ).selectFacets( ccsFacet );
		assertFacetCounts( facetManager.getFacets( colorFacetName ), new int[] { 4, 0, 0, 0, 0 } );
		assertFacetCounts( facetManager.getFacets( ccsFacetName ), new int[] { 4, 0, 0, 0 } );

		assertEquals(
				"Facets should not take count in equality",
				colorFacet,
				facetManager.getFacets( colorFacetName ).get( 0 )
		);
		assertTrue(
				"We should be able to find facets amongst the selected ones",
				facetManager.getFacetGroup( colorFacetName ).getSelectedFacets().contains(
						facetManager.getFacets( colorFacetName ).get( 0 )
				)
		);

		facetManager.getFacetGroup( colorFacetName ).clearSelectedFacets();
		facetManager.getFacetGroup( ccsFacetName ).clearSelectedFacets();
		assertFacetCounts( facetManager.getFacets( colorFacetName ), new int[] { 12, 12, 12, 12, 2 } );
		assertFacetCounts( facetManager.getFacets( ccsFacetName ), new int[] { 17, 16, 16, 1 } );
	}

	public void testRangeFacetDrillDown() {
		final String indexFieldName = "price";
		final String priceRange = "priceRange";
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

		assertEquals( "Wrong number of query matches", 10, query.getResultSize() );

		List<Facet> facets = facetManager.getFacets( priceRange );
		assertFacetCounts( facets, new int[] { 5, 3, 2, 0 } );

		facetManager.getFacetGroup( priceRange ).selectFacets( facets.get( 2 ) );

		assertEquals( "Wrong number of query matches", 2, query.list().size() );
		List<Facet> newFacetList = facetManager.getFacets( priceRange );
		assertFacetCounts( newFacetList, new int[] { 2, 0, 0, 0 } );
	}

	@Override
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

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Car.class,
				Fruit.class
		};
	}
}

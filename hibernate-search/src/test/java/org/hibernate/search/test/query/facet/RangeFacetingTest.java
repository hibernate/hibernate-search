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
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetRequest;
import org.hibernate.search.query.facet.FacetResult;
import org.hibernate.search.query.facet.FacetSortOrder;

/**
 * @author Hardy Ferentschik
 */
public class RangeFacetingTest extends AbstractFacetTest {
	private final String indexFieldName = "price";
	private final String priceRange = "priceRange";

	public void testRangeQueryForInteger() {
		FacetRequest rangeRequest = queryBuilder( Cd.class ).facet()
				.name( priceRange )
				.onField( indexFieldName )
				.range()
				.from( 0 ).to( 1000 )
				.from( 1001 ).to( 1500 )
				.from( 1501 ).to( 3000 )
				.createFacet();
		FullTextQuery query = createMatchAllQuery( Cd.class );
		query.enableFacet( rangeRequest );

		Map<String, FacetResult> results = query.getFacetResults();
		assertTrue( "We should have three facet result", results.size() == 1 );
		List<Facet> facets = results.get( priceRange ).getFacets();
		assertFacetCounts( facets, new int[] { 5, 3, 2 } );
	}

	public void testRangeQueryForDoubleWithZeroCount() {
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

		Map<String, FacetResult> results = query.getFacetResults();
		List<Facet> facets = results.get( priceRange ).getFacets();
		assertFacetCounts( facets, new int[] { 5, 3, 2, 0 } );
	}

	public void testRangeQueryForDoubleWithoutZeroCount() {
		FacetRequest rangeRequest = queryBuilder( Fruit.class ).facet()
				.name( priceRange )
				.onField( indexFieldName )
				.range()
				.from( 0.00 ).to( 1.00 )
				.from( 1.01 ).to( 1.50 )
				.from( 1.51 ).to( 3.00 )
				.from( 4.00 ).to( 5.00 )
				.createFacet();
		rangeRequest.setSort( FacetSortOrder.COUNT_ASC );
		rangeRequest.setIncludeZeroCounts( false );

		FullTextQuery query = createMatchAllQuery( Fruit.class );
		query.enableFacet( rangeRequest );

		Map<String, FacetResult> results = query.getFacetResults();
		List<Facet> facets = results.get( priceRange ).getFacets();
		assertFacetCounts( facets, new int[] { 2, 3, 5 } );
		assertEquals( "[0.0, 1.0]", facets.get( 0 ).getValue() );
		assertEquals( "[1.01, 1.5]", facets.get( 1 ).getValue() );
		assertEquals( "[1.51, 3.0]", facets.get( 2 ).getValue() );
	}

	public void loadTestData(Session session) {
		Transaction tx = session.beginTransaction();
		for ( int i = 0; i < albums.length; i++ ) {
			Cd cd = new Cd( albums[i], albumPrices[i] );
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

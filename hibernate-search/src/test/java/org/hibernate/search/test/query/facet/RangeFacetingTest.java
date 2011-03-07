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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.impl.RangeFacetRequest;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetRange;
import org.hibernate.search.query.facet.FacetRequest;
import org.hibernate.search.query.facet.FacetResult;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.testing.junit.FailureExpected;


/**
 * @author Hardy Ferentschik
 */
public class RangeFacetingTest extends SearchTestCase {

	private static final String[] albums = {
			"A boy named Johnny",
			"A boy named Sue",
			"A thing called love",
			"Adventures od Johnny Cash",
			"American Outlaw",
			"At Folsom Prison",
			"Any old wind that blows",
			"Unearthed",
			"The man comes around",
			"The man in black"
	};
	private static final int[] albumPrices = { 499, 999, 1500, 1500, 1500, 1600, 1700, 1800, 2000, 2500 };

	private static final String[] fruits = {
			"apple",
			"pear",
			"banana",
			"kiwi",
			"orange",
			"papaya",
			"grape",
			"mango",
			"mandarin",
			"pineapple"
	};
	private static final double[] fruitPrices = { 0.50, 0.99, 1.50, 1.50, 1.50, 1.60, 1.70, 1.80, 2.00, 2.50 };

	private FullTextSession fullTextSession;
	private Transaction tx;

	private String indexFieldName = "price";

	public void setUp() throws Exception {
		super.setUp();
		fullTextSession = Search.getFullTextSession( openSession() );
		loadTestData( fullTextSession );
		tx = fullTextSession.beginTransaction();
	}

	public void tearDown() throws Exception {
		tx.commit();
		fullTextSession.clear();
		fullTextSession.close();
		super.tearDown();
	}

	@FailureExpected( jiraKey = "HSEARCH-667")
	public void testFacetDSL() {
		QueryBuilder builder = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( Cd.class ).get();
		FacetRequest request = builder.facet()
				.name( "priceRange" )
				.onField( indexFieldName )
				.range()
				.from( 0 )
				.to( 1000 )
				.from( 1001 )
				.to( 1500 )
				.from( 1501 )
				.to( 3000 ).excludeLimit()
				.createFacet();
	}

	public void testRangeQueryForInteger() {
		final String priceRange = "priceRange";
		final List<FacetRange<Integer>> ranges = new ArrayList<FacetRange<Integer>>();
		ranges.add( new FacetRange<Integer>( 0, 1000 ) );
		ranges.add( new FacetRange<Integer>( 1001, 1500 ) );
		ranges.add( new FacetRange<Integer>( 1501, 3000 ) );
		RangeFacetRequest<Integer> rangeRequest = new RangeFacetRequest<Integer>( priceRange, indexFieldName, ranges );

		FullTextQuery query = createMatchAllQuery( Cd.class );
		query.enableFacet( rangeRequest );

		Map<String, FacetResult> results = query.getFacetResults();
		assertTrue( "We should have three facet result", results.size() == 1 );
		List<Facet> facets = results.get( priceRange ).getFacets();
		assertEquals( "There should be three price ranges", 3, facets.size() );
		assertTrue( facets.get( 0 ).getCount() == 5 );
		assertTrue( facets.get( 1 ).getCount() == 3 );
		assertTrue( facets.get( 2 ).getCount() == 2 );
	}

	public void testRangeQueryForDoubleWithZeroCount() {
		final String priceRange = "priceRange";
		final List<FacetRange<Double>> ranges = new ArrayList<FacetRange<Double>>();
		ranges.add( new FacetRange<Double>( 0.0, 1.00 ) );
		ranges.add( new FacetRange<Double>( 1.01, 1.50 ) );
		ranges.add( new FacetRange<Double>( 1.51, 3.00 ) );
		ranges.add( new FacetRange<Double>( 4.00, 5.00 ) );
		RangeFacetRequest<Double> rangeRequest = new RangeFacetRequest<Double>( priceRange, indexFieldName, ranges );

		FullTextQuery query = createMatchAllQuery( Fruit.class );
		query.enableFacet( rangeRequest );

		Map<String, FacetResult> results = query.getFacetResults();
		List<Facet> facets = results.get( priceRange ).getFacets();
		assertEquals( "There should be three price ranges", 4, facets.size() );
		assertTrue( facets.get( 0 ).getCount() == 5 );
		assertTrue( facets.get( 1 ).getCount() == 3 );
		assertTrue( facets.get( 2 ).getCount() == 2 );
		assertTrue( facets.get( 3 ).getCount() == 0 );
	}

	public void testRangeQueryForDoubleWithoutZeroCount() {
		final String priceRange = "priceRange";
		final List<FacetRange<Double>> ranges = new ArrayList<FacetRange<Double>>();
		ranges.add( new FacetRange<Double>( 0.0, 1.00 ) );
		ranges.add( new FacetRange<Double>( 1.01, 1.50 ) );
		ranges.add( new FacetRange<Double>( 1.51, 3.00 ) );
		ranges.add( new FacetRange<Double>( 4.00, 5.00 ) );
		RangeFacetRequest<Double> rangeRequest = new RangeFacetRequest<Double>( priceRange, indexFieldName, ranges );
		rangeRequest.setSort( FacetSortOrder.COUNT_ASC );
		rangeRequest.setIncludeZeroCounts( false );

		FullTextQuery query = createMatchAllQuery( Fruit.class );
		query.enableFacet( rangeRequest );

		Map<String, FacetResult> results = query.getFacetResults();
		List<Facet> facets = results.get( priceRange ).getFacets();
		assertEquals( "There should be three price ranges", 3, facets.size() );
		assertEquals( "[0.0, 1.0]", facets.get( 0 ).getValue() );
		assertTrue( facets.get( 0 ).getCount() == 2 );

		assertEquals( "[1.01, 1.5]", facets.get( 1 ).getValue() );
		assertTrue( facets.get( 1 ).getCount() == 3 );

		assertEquals( "[1.51, 3.0]", facets.get( 2 ).getValue() );
		assertTrue( facets.get( 2 ).getCount() == 5 );
	}

	private FullTextQuery createMatchAllQuery(Class<?> clazz) {
		QueryBuilder builder = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( clazz ).get();
		Query luceneQuery = builder.all().createQuery();
		return fullTextSession.createFullTextQuery( luceneQuery, clazz );
	}

	private void loadTestData(Session session) {
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

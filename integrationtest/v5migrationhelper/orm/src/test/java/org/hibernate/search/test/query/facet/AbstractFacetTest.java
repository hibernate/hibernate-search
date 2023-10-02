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
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.test.SearchTestBase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.apache.lucene.search.Query;

/**
 * @author Hardy Ferentschik
 */
public abstract class AbstractFacetTest extends SearchTestBase {
	public static final String[] colors = { "red", "black", "white", "blue" };

	public static final String[] makes = { "Honda", "Toyota", "BMW", "Mercedes" };

	public static final int[] ccs = { 3398, 2407, 2831 };

	public static final String[] albums = {
			"A boy named Johnny",
			"A boy named Sue",
			"A thing called love",
			"Adventures od Johnny Cash",
			"American Outlaw",
			"At Folsom Prison",
			"Any old wind that blows",
			"Unearthed",
			"The man comes around",
			"Water from the Wells of Home"
	};
	public static final int[] albumPrices = { 499, 999, 1500, 1500, 1500, 1600, 1700, 1800, 2000, 2500 };

	public static final String[] releaseDates = {
			"2001",
			"2002",
			"1972",
			"1982",
			"2010",
			"1968",
			"1973",
			"2003",
			"2002",
			"1988"
	};

	public static final String[] fruits = {
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
	public static final double[] fruitPrices = { 0.50, 0.99, 1.50, 1.50, 1.50, 1.60, 1.70, 1.80, 2.00, 2.50 };

	public static final Integer[] horsePowers = { 200, 400, 600, 1300, 730 };

	protected FullTextSession fullTextSession;
	protected Transaction tx;

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		fullTextSession = Search.getFullTextSession( openSession() );
		loadTestData( fullTextSession );
		tx = fullTextSession.beginTransaction();
	}

	@Override
	@AfterEach
	public void tearDown() throws Exception {
		tx.commit();
		fullTextSession.clear();
		fullTextSession.close();
		super.tearDown();
	}

	public FullTextQuery createMatchAllQuery(Class<?> clazz) {
		QueryBuilder builder = queryBuilder( clazz );
		Query luceneQuery = builder.all().createQuery();
		return fullTextSession.createFullTextQuery( luceneQuery, clazz );
	}

	public QueryBuilder queryBuilder(Class<?> clazz) {
		return fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( clazz ).get();
	}

	public void assertFacetCounts(List<Facet> facetList, int[] counts) {
		assertThat( facetList ).as( "Wrong number of facets" ).hasSize( counts.length );
		for ( int i = 0; i < facetList.size(); i++ ) {
			assertThat( facetList.get( i ).getCount() ).as( "Wrong facet count for facet " + i ).isEqualTo( counts[i] );
		}
	}

	public void assertFacetValues(List<Facet> facetList, Object[] values) {
		assertThat( facetList ).as( "Wrong number of facets" ).hasSize( values.length );
		for ( int i = 0; i < facetList.size(); i++ ) {
			assertThat( facetList.get( i ).getValue() ).as( "Wrong facet value for facet " + i ).isEqualTo( values[i] );
		}
	}

	public abstract void loadTestData(Session session);
}

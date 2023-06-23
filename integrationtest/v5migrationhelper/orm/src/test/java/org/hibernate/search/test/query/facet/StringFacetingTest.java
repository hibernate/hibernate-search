/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.facet;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.Test;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

/**
 * @author Hardy Ferentschik
 */
public class StringFacetingTest extends AbstractFacetTest {

	@Test
	@TestForIssue(jiraKey = "HSEARCH-809")
	public void testStringFaceting() throws Exception {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( "manufacturer" )
				.onField( "make" )
				.discrete()
				.includeZeroCounts( false )
				.createFacetingRequest();
		FullTextQuery query = matchAll( request );

		List<Facet> facetList = query.getFacetManager().getFacets( "manufacturer" );
		assertEquals( "Wrong number of facets", 5, facetList.size() );

		assertFacet( facetList.get( 0 ), "Honda", 13 );
		assertFacet( facetList.get( 1 ), "BMW", 12 );
		assertFacet( facetList.get( 2 ), "Mercedes", 12 );
		assertFacet( facetList.get( 3 ), "Toyota", 12 );
		assertFacet( facetList.get( 4 ), "Ford", 1 );
	}

	private void assertFacet(Facet facet, String expectedMake, int expectedCount) {
		assertEquals( "Wrong facet value", expectedMake, facet.getValue() );
		assertEquals( "Wrong facet count", expectedCount, facet.getCount() );
	}

	private FullTextQuery matchAll(FacetingRequest request) {
		Query luceneQuery = new MatchAllDocsQuery();
		FullTextQuery query = fullTextSession.createFullTextQuery( luceneQuery, Car.class );
		query.getFacetManager().enableFaceting( request );
		assertEquals( "Wrong number of indexed cars", 50, query.getResultSize() );
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

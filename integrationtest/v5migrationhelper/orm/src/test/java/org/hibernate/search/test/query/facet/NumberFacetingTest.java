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
import org.hibernate.search.testsupport.junit.PortedToSearch6;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

/**
 * @author Hardy Ferentschik
 */
@Category(PortedToSearch6.class)
public class NumberFacetingTest extends AbstractFacetTest {

	@Test
	public void testSimpleFaceting() throws Exception {
		String facetName = "ccs";
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( Car.CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING )
				.discrete()
				.createFacetingRequest();
		FullTextQuery query = matchAll( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertEquals( "Wrong number of facets", 4, facetList.size() );

		assertFacet( facetList.get( 0 ), "2407", 17 );
		assertFacet( facetList.get( 1 ), "2831", 16 );
		assertFacet( facetList.get( 2 ), "3398", 16 );
		assertFacet( facetList.get( 3 ), "2500", 1 );
	}

	private void assertFacet(Facet facet, String expectedCubicCapacity, int expectedCount) {
		assertEquals( "Wrong facet value", expectedCubicCapacity, facet.getValue() );
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

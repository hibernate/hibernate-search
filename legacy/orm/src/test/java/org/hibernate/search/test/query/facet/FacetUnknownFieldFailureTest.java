/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.facet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.facet.FacetingRequest;
import org.junit.Test;

public class FacetUnknownFieldFailureTest extends AbstractFacetTest {

	@Test
	public void testUnknownFieldNameThrowsException() {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( "foo" ) // faceting name is irrelevant
				.onField( "foobar" ) // foobar is not a valid field name
				.discrete()
				.createFacetingRequest();
		try {
			FullTextQuery query = queryHondaWithFacet( request );
			query.getFacetManager().getFacets( "foo" );
			fail( "The specified field name did not exist. Faceting request should fail" );
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000268" ) );
		}
	}

	@Test
	public void testKnownFieldNameNotConfiguredForFacetingThrowsException() {
		FacetingRequest request = queryBuilder( Fruit.class ).facet()
				.name( "foo" ) // faceting name is irrelevant
				.onField( "name" ) // name is a valid property of apple, but not configured for faceting
				.discrete()
				.createFacetingRequest();

		try {
			FullTextQuery query = fullTextSession.createFullTextQuery( new MatchAllDocsQuery(), Fruit.class );
			query.getFacetManager().enableFaceting( request );
			assertEquals( "Wrong number of query matches", 1, query.getResultSize() );

			query.getFacetManager().getFacets( "foo" );
			fail( "The specified field name did not exist. Faceting request should fail" );
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000268" ) );
		}
	}

	private FullTextQuery queryHondaWithFacet(FacetingRequest request) {
		Query luceneQuery = queryBuilder( Car.class )
				.keyword()
				.onField( "make" )
				.matching( "Honda" )
				.createQuery();
		FullTextQuery query = fullTextSession.createFullTextQuery( luceneQuery, Car.class );
		query.getFacetManager().enableFaceting( request );
		assertEquals( "Wrong number of query matches", 13, query.getResultSize() );
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

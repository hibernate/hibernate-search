/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.facet;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.testsupport.junit.PortedToSearch6;
import org.hibernate.search.util.common.SearchException;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

@Category(PortedToSearch6.class)
public class FacetUnknownFieldFailureTest extends AbstractFacetTest {

	@Test
	public void testUnknownFieldNameThrowsException() {
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
	public void testKnownFieldNameNotConfiguredForFacetingThrowsException() {
		FacetingRequest request = queryBuilder( Fruit.class ).facet()
				.name( "foo" ) // faceting name is irrelevant
				.onField( "name" ) // name is a valid property of apple, but not configured for faceting
				.discrete()
				.createFacetingRequest();

		assertThatThrownBy( () -> {
			FullTextQuery query = fullTextSession.createFullTextQuery( new MatchAllDocsQuery(), Fruit.class );
			query.getFacetManager().enableFaceting( request );
			assertEquals( "Wrong number of query matches", 1, query.getResultSize() );

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

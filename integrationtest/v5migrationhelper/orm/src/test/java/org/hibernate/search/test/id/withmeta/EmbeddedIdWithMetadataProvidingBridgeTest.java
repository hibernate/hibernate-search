/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.id.withmeta;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.id.EmbeddedIdTest;
import org.junit.Test;

/**
 * This test class is a copy of {@link EmbeddedIdTest} with the difference that the fields created by
 * the bridges won't contain `.`.
 * <p>
 * The reason behind it is that Elasticsearch does not allow fields name to contain a `.`.
 *
 * @author Davide D'Alto
 */
public class EmbeddedIdWithMetadataProvidingBridgeTest extends SearchTestBase {

	@Test
	@SuppressWarnings("unchecked")
	public void testFieldBridge() throws Exception {
		PersonPK emmanuelPk = new PersonPK();
		emmanuelPk.setFirstName( "Emmanuel" );
		emmanuelPk.setLastName( "Bernard" );
		Person emmanuel = new Person();
		emmanuel.setFavoriteColor( "Blue" );
		emmanuel.setId( emmanuelPk );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.save( emmanuel );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();
		List<Person> results = Search.getFullTextSession( s ).createFullTextQuery(
				new TermQuery( new Term( "id_lastName", "Bernard" ) )
		).list();
		assertEquals( 1, results.size() );
		emmanuel = (Person) results.get( 0 );
		emmanuel.setFavoriteColor( "Red" );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();
		results = Search.getFullTextSession( s ).createFullTextQuery(
				new TermQuery( new Term( "id_lastName", "Bernard" ) )
		).list();
		assertEquals( 1, results.size() );
		emmanuel = (Person) results.get( 0 );
		assertEquals( "Red", emmanuel.getFavoriteColor() );
		s.delete( results.get( 0 ) );
		tx.commit();
		s.close();
	}

	/**
	 * HSEARCH-306, HSEARCH-248
	 *
	 * @throws Exception throws exception in case the test fails.
	 */
	@Test
	public void testSafeFromTupleId() throws Exception {
		PersonPK emmanuelPk = new PersonPK();
		emmanuelPk.setFirstName( "Emmanuel" );
		emmanuelPk.setLastName( "Bernard" );
		Person emmanuel = new Person();
		emmanuel.setFavoriteColor( "Blue" );
		emmanuel.setId( emmanuelPk );

		PersonPK johnPk = new PersonPK();
		johnPk.setFirstName( "John" );
		johnPk.setLastName( "Doe" );
		Person john = new Person();
		john.setFavoriteColor( "Blue" );
		john.setId( johnPk );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.save( emmanuel );
		s.save( john );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();

		// we need a query which has at least the size of two.
		List results = Search.getFullTextSession( s ).createFullTextQuery(
				new TermQuery( new Term( "favoriteColor", "blue" ) )
		).list();
		assertEquals( 2, results.size() );
		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Person.class
		};
	}
}

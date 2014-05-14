/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine.worker;

import org.hibernate.Session;

import org.hibernate.search.test.SearchTestBase;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class ConcurrencyTest extends SearchTestBase {

	@Test
	public void testMultipleEntitiesInSameIndex() throws Exception {
		Session s = openSession( );
		s.getTransaction().begin();
		Drink d = new Drink();
		d.setName( "Water" );
		Food f = new Food();
		f.setName( "Bread" );
		s.persist( d );
		s.persist( f );
		s.getTransaction().commit();
		s.close();

		s = openSession( );
		s.getTransaction().begin();
		d = (Drink) s.get( Drink.class, d.getId() );
		d.setName( "Coke" );
		f = (Food) s.get( Food.class, f.getId() );
		f.setName( "Cake" );
		try {
			s.getTransaction().commit();
		}
		catch (Exception e) {
			//Check for error logs from JDBCTransaction
		}
		s.close();

		s = openSession( );
		s.getTransaction().begin();
		d = (Drink) s.get( Drink.class, d.getId() );
		s.delete( d );
		f = (Food) s.get( Food.class, f.getId() );
		s.delete( f );
		s.getTransaction().commit();
		s.close();

	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Drink.class,
				Food.class
		};
	}
}

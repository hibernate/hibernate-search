//$Id$
package org.hibernate.search.test.worker;

import org.hibernate.search.test.SearchTestCase;
import org.hibernate.Session;

/**
 * @author Emmanuel Bernard
 */
public class ConcurrencyTest extends SearchTestCase {

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

	protected Class[] getMappings() {
		return new Class[] {
				Drink.class,
				Food.class
		};
	}
}

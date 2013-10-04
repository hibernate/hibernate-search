/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.engine.worker;

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

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Drink.class,
				Food.class
		};
	}
}

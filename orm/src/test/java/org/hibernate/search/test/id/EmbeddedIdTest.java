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
package org.hibernate.search.test.id;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Emmanuel Bernard
 */
public class EmbeddedIdTest extends SearchTestCase {

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
		List results = Search.getFullTextSession( s ).createFullTextQuery(
				new TermQuery( new Term( "id.lastName", "Bernard" ) )
		).list();
		assertEquals( 1, results.size() );
		emmanuel = (Person) results.get( 0 );
		emmanuel.setFavoriteColor( "Red" );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();
		results = Search.getFullTextSession( s ).createFullTextQuery(
				new TermQuery( new Term( "id.lastName", "Bernard" ) )
		).list();
		assertEquals( 1, results.size() );
		emmanuel = (Person) results.get( 0 );
		assertEquals( "Red", emmanuel.getFavoriteColor() );
		s.delete( results.get( 0 ) );
		tx.commit();
		s.close();
	}

	/**
	 * HSEARCH-HSEARCH-306, HSEARCH-248
	 *
	 * @throws Exception throws exception in case the test fails.
	 */
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
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Person.class
		};
	}
}

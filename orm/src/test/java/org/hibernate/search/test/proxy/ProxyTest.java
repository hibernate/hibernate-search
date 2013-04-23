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
package org.hibernate.search.test.proxy;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.test.SearchTestCase;


/**
 * @author Hardy Ferentschik
 */
public class ProxyTest extends SearchTestCase {

	public void testProxy() throws Exception {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		Book book = new Book(
				1,
				"La chute de la petite reine a travers les yeux de Festina",
				"La chute de la petite reine a travers les yeux de Festina, blahblah"
		);
		Author author = new Author();
		author.setBook( book );
		author.setName( "John Doe" );
		Set<IAuthor> authors = new HashSet<IAuthor>();
		authors.add( author );
		book.setAuthors( authors );
		session.save( book );
		tx.commit();
		session.clear();

		tx = session.beginTransaction();
		IAuthor loadedAuthor = (IAuthor) session.get( Author.class, author.getId() );
		//author = (Author) loadedBook.getAuthors().iterator().next();
		session.delete( loadedAuthor );

		tx.commit();
		session.close();
	}

	public void testDeleteProxy() throws Exception {
		createTestData();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		IComment c = (IComment) s.get( Comment.class, 2 );
		s.delete( c );
		tx.commit();
		s.close();
	}

	public void createTestData() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		IProfile p = new Profile();
		p.setId( 1 );
		s.save( p );

		IComment c1 = new Comment();
		c1.setId( 2 );
		c1.setProfile( (IProfile) s.get( Profile.class, 1 ) );
		c1.setContent( "c1" );
		c1.setRootComment( null );
		s.save( c1 );

		IComment c2 = new Comment();
		c2.setId( 3 );
		c2.setProfile( (IProfile) s.get( Profile.class, 1 ) );
		c2.setContent( "c2" );
		c2.setRootComment( c1 );
		s.save( c2 );

		IComment c3 = new Comment();
		c3.setId( 4 );
		c3.setProfile( (IProfile) s.get( Profile.class, 1 ) );
		c3.setContent( "c3" );
		c3.setRootComment( c1 );
		s.save( c3 );

		tx.commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				Author.class,
				Comment.class,
				Profile.class
		};
	}
}

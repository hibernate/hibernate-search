/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.proxy;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.test.SearchTestBase;

import org.junit.Test;

/**
 * @author Hardy Ferentschik
 */
public class ProxyTest extends SearchTestBase {

	@Test
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

	@Test
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
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				Author.class,
				Comment.class,
				Profile.class
		};
	}
}

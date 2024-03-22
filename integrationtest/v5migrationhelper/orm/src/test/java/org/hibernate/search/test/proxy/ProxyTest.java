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

import org.junit.jupiter.api.Test;

/**
 * @author Hardy Ferentschik
 */
class ProxyTest extends SearchTestBase {

	@Test
	void testProxy() {
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

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				Author.class
		};
	}
}

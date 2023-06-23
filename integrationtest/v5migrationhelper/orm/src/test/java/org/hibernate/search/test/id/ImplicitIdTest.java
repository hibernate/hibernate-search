/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.id;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;

import org.junit.Test;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

/**
 * @author Hardy Ferentschik
 */
public class ImplicitIdTest extends SearchTestBase {

	/**
	 * Tests that @DocumentId is optional. See HSEARCH-104.
	 *
	 * @throws Exception in case the test fails.
	 */
	@Test
	public void testImplicitDocumentId() throws Exception {
		Animal dog = new Animal();
		dog.setName( "Dog" );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.save( dog );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();
		List results = Search.getFullTextSession( s ).createFullTextQuery(
				new TermQuery( new Term( "name", "dog" ) )
		).list();
		assertEquals( 1, results.size() );
		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Animal.class
		};
	}
}

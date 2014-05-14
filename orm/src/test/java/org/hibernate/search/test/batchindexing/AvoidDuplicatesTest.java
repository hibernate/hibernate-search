/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import org.apache.lucene.search.MatchAllDocsQuery;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AvoidDuplicatesTest extends SearchTestBase {

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		Nation italy = new Nation( "Italy", "IT" );
		session.persist( italy );

		AncientBook aeneid = new AncientBook();
		aeneid.setTitle( "Aeneid" );
		aeneid.getAlternativeTitles().add( "Aeneis" );
		aeneid.getAlternativeTitles().add( "Eneide" );
		aeneid.setFirstPublishedIn( italy );
		session.persist( aeneid );

		AncientBook commedia = new AncientBook();
		commedia.setTitle( "Commedia" );
		commedia.getAlternativeTitles().add( "La Commedia" );
		commedia.getAlternativeTitles().add( "La Divina Commedia" );
		commedia.setFirstPublishedIn( italy );
		session.persist( commedia );

		transaction.commit();
		session.close();
	}

	@Test
	public void testReindexedOnce() throws InterruptedException {
		Assert.assertEquals( 2, countBooksInIndex() );
		Session session = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		MassIndexer massIndexer = fullTextSession.createIndexer();
		massIndexer.startAndWait();
		session.close();
		Assert.assertEquals( 2, countBooksInIndex() );
	}

	private int countBooksInIndex() {
		Session session = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		fullTextSession.beginTransaction();
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() );
		int size = fullTextQuery.list().size();
		fullTextSession.getTransaction().commit();
		fullTextSession.close();
		return size;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				AncientBook.class,
				Book.class,
				Nation.class
		};
	}

}

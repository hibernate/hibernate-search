/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.id;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.Search;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * @author Hardy Ferentschik
 */
public class ExplicitIdTest extends SearchTestBase {

	/**
	 * Tests that @DocumentId can be specified on a field other than the @Id annotated one. See HSEARCH-574.
	 *
	 * @throws Exception in case the test fails.
	 */
	@Test
	public void testExplicitDocumentIdSingleResult() throws Exception {
		Article hello = new Article();
		hello.setDocumentId( 1 );
		hello.setText( "Hello World" );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.save( hello );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();
		List results = Search.getFullTextSession( s ).createFullTextQuery(
				new TermQuery( new Term( "text", "world" ) )
		).list();
		assertEquals( 1, results.size() );
		tx.commit();
		s.close();
	}

	/**
	 * Tests that @DocumentId can be specified on a field other than the @Id annotated one. See HSEARCH-574.
	 *
	 * @throws Exception in case the test fails.
	 */
	@Test
	public void testExplicitDocumentIdMultipleResults() throws Exception {
		Article hello = new Article();
		hello.setDocumentId( 1 );
		hello.setText( "Hello World" );

		Article goodbye = new Article();
		goodbye.setDocumentId( 2 );
		goodbye.setText( "Goodbye World" );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.save( hello );
		s.save( goodbye );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();
		List results = Search.getFullTextSession( s ).createFullTextQuery(
				new TermQuery( new Term( "text", "world" ) )
		).list();
		assertEquals( 2, results.size() );
		tx.commit();
		s.close();
	}

	/**
	 * Tests that the document id must be unique
	 *
	 * @throws Exception in case the test fails.
	 */
	@Test
	public void testDocumentIdMustBeUnique() throws Exception {
		Article hello = new Article();
		hello.setDocumentId( 1 );
		hello.setText( "Hello World" );

		Article goodbye = new Article();
		goodbye.setDocumentId( 1 );
		goodbye.setText( "Goodbye World" );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.save( hello );
		s.save( goodbye );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();
		try {
			Search.getFullTextSession( s ).createFullTextQuery(
					new TermQuery( new Term( "text", "world" ) )
			).list();
			fail( "Test should fail, because document id is not unique." );
		}
		catch (SearchException e) {
			assertEquals(
					"Loading entity of type org.hibernate.search.test.id.Article using 'documentId' as document id and '1' as value was not unique",
					e.getMessage()
			);
		}
		tx.commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Article.class
		};
	}
}

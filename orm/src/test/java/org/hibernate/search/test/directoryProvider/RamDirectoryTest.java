/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.directoryProvider;

import java.util.List;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Session;

import org.hibernate.search.Search;
import org.hibernate.search.test.AlternateDocument;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
public class RamDirectoryTest extends SearchTestBase {

	@Test
	public void testMultipleEntitiesPerIndex() throws Exception {
		Session s = getSessionFactory().openSession();
		s.getTransaction().begin();
		Document document =
				new Document( "Hibernate in Action", "Object/relational mapping with Hibernate", "blah blah blah" );
		s.persist( document );
		s.flush();
		s.persist(
				new AlternateDocument(
						document.getId(),
						"Hibernate in Action",
						"Object/relational mapping with Hibernate",
						"blah blah blah"
				)
		);
		s.getTransaction().commit();
		s.close();

		assertEquals( 2, getDocumentNbr() );

		s = getSessionFactory().openSession();
		s.getTransaction().begin();
		TermQuery q = new TermQuery( new Term( "alt_title", "hibernate" ) );
		List hibernateDocuments = Search.getFullTextSession( s ).createFullTextQuery( q, Document.class ).list();
		assertEquals(
				"does not properly filter", 0,
				hibernateDocuments.size()
		);
		assertEquals(
				"does not properly filter", 1,
				Search.getFullTextSession( s )
						.createFullTextQuery( q, Document.class, AlternateDocument.class )
						.list().size()
		);
		s.delete( s.get( AlternateDocument.class, document.getId() ) );
		s.getTransaction().commit();
		s.close();

		assertEquals( 1, getDocumentNbr() );

		s = getSessionFactory().openSession();
		s.getTransaction().begin();
		s.delete( s.createCriteria( Document.class ).uniqueResult() );
		s.getTransaction().commit();
		s.close();
	}

	private int getDocumentNbr() throws Exception {
		IndexReader reader = DirectoryReader.open( getDirectory( Document.class ) );
		try {
			return reader.numDocs();
		}
		finally {
			reader.close();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Document.class,
				AlternateDocument.class
		};
	}
}

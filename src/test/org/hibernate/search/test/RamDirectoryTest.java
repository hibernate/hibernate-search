//$Id$
package org.hibernate.search.test;

import org.hibernate.Session;
import org.hibernate.search.Search;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

/**
 * @author Emmanuel Bernard
 */
public class RamDirectoryTest extends SearchTestCase {

	public void testMultipleEntitiesPerIndex() throws Exception {


		Session s = getSessions().openSession();
		s.getTransaction().begin();
		Document document =
				new Document( "Hibernate in Action", "Object/relational mapping with Hibernate", "blah blah blah" );
		s.persist(document);
		s.flush();
		s.persist(
				new AlternateDocument( document.getId(), "Hibernate in Action", "Object/relational mapping with Hibernate", "blah blah blah" )
		);
		s.getTransaction().commit();
		s.close();

		assertEquals( 2, getDocumentNbr() );

		s = getSessions().openSession();
		s.getTransaction().begin();
		TermQuery q = new TermQuery(new Term("alt_title", "hibernate"));
		assertEquals( "does not properly filter", 0,
				Search.getFullTextSession( s ).createFullTextQuery( q, Document.class ).list().size() );
		assertEquals( "does not properly filter", 1,
				Search.getFullTextSession( s ).createFullTextQuery( q, Document.class, AlternateDocument.class ).list().size() );
		s.delete( s.get( AlternateDocument.class, document.getId() ) );
		s.getTransaction().commit();
		s.close();

		assertEquals( 1, getDocumentNbr() );

		s = getSessions().openSession();
		s.getTransaction().begin();
		s.delete( s.createCriteria( Document.class ).uniqueResult() );
		s.getTransaction().commit();
		s.close();
	}

	private int getDocumentNbr() throws Exception {
		IndexReader reader = IndexReader.open( getDirectory( Document.class ), false );
		try {
			return reader.numDocs();
		}
		finally {
			reader.close();
		}
	}

	protected Class[] getMappings() {
		return new Class[]{
				Document.class,
				AlternateDocument.class
		};
	}

}

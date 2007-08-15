//$Id$
package org.hibernate.search.test;

import org.hibernate.Session;
import org.apache.lucene.index.IndexReader;

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
		IndexReader reader = IndexReader.open( getDirectory( Document.class ) );
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

//$Id$
package org.hibernate.search.test.indexingStrategy;

import org.apache.lucene.index.IndexReader;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.test.AlternateDocument;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.Environment;

/**
 * @author Emmanuel Bernard
 */
public class ManualIndexingStrategyTest extends SearchTestCase {

	public void testMultipleEntitiesPerIndex() throws Exception {

		Session s = getSessions().openSession();
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

		assertEquals( 0, getDocumentNbr() );

		s = getSessions().openSession();
		s.getTransaction().begin();
		s.delete( s.get( AlternateDocument.class, document.getId() ) );
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
		return new Class[] {
				Document.class,
				AlternateDocument.class
		};
	}


	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.INDEXING_STRATEGY, "manual" );
	}
}

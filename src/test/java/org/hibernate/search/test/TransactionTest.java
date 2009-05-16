//$Id$
package org.hibernate.search.test;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;

import org.hibernate.Session;

/**
 * @author Emmanuel Bernard
 */
public class TransactionTest extends SearchTestCase {

	public void testTransactionCommit() throws Exception {
		Session s = getSessions().openSession();
		s.getTransaction().begin();
		s.persist(
				new Document( "Hibernate in Action", "Object/relational mapping with Hibernate", "blah blah blah" )
		);
		s.persist(
				new Document( "Lucene in Action", "FullText search engine", "blah blah blah" )
		);
		s.persist(
				new Document( "Hibernate Search in Action", "ORM and FullText search engine", "blah blah blah" )
		);
		s.getTransaction().commit();
		s.close();

		assertEquals( "transaction.commit() should index", 3, getDocumentNumber() );

		s = getSessions().openSession();
		s.getTransaction().begin();
		s.persist(
				new Document(
						"Java Persistence with Hibernate", "Object/relational mapping with Hibernate", "blah blah blah"
				)
		);
		s.flush();
		s.getTransaction().rollback();
		s.close();

		assertEquals( "rollback() should not index", 3, getDocumentNumber() );
		
		s = getSessions().openSession();
		s.connection().setAutoCommit( true );  // www.hibernate.org/403.html
		s.persist(
				new Document(
						"Java Persistence with Hibernate", "Object/relational mapping with Hibernate", "blah blah blah"
				)
		);
		s.flush();	
		s.close();

		assertEquals( "no transaction should index", 4, getDocumentNumber() );

	}

	private int getDocumentNumber() throws IOException {
		IndexReader reader = IndexReader.open( getDirectory( Document.class ), false );
		try {
			return reader.numDocs();
		}
		finally {
			reader.close();
		}
	}

	protected Class[] getMappings() {
		return new Class[] { Document.class };
	}
}

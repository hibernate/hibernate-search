/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.SearchTestBase;

import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class TransactionTest extends SearchTestBase {

	@Test
	public void testTransactionCommit() throws Exception {
		Session s = getSessionFactory().openSession();
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

		s = getSessionFactory().openSession();
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

		s = getSessionFactory().openSession();
		s.doWork( new Work() {
			@Override
			public void execute(Connection connection) throws SQLException {
				connection.setAutoCommit( true ); // www.hibernate.org/403.html
			}
		} );
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
		return getNumberOfDocumentsInIndex( "Documents" );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Document.class };
	}

	@Override
	public void configure(Map<String, Object> cfg) {
		cfg.put( "hibernate.allow_update_outside_transaction", "true" );
	}

}

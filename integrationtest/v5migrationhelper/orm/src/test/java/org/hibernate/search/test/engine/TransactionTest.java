/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.SearchTestBase;

import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
class TransactionTest extends SearchTestBase {

	@Test
	void testTransactionCommit() throws Exception {
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

		assertThat( getDocumentNumber() ).as( "transaction.commit() should index" ).isEqualTo( 3 );

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

		assertThat( getDocumentNumber() ).as( "rollback() should not index" ).isEqualTo( 3 );

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

		assertThat( getDocumentNumber() ).as( "no transaction should index" ).isEqualTo( 4 );

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

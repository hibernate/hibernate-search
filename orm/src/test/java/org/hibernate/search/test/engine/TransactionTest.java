/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.engine;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.lucene.index.IndexReader;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Emmanuel Bernard
 */
public class TransactionTest extends SearchTestCase {

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
		IndexReader reader = IndexReader.open( getDirectory( Document.class ), false );
		try {
			return reader.numDocs();
		}
		finally {
			reader.close();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Document.class };
	}
}

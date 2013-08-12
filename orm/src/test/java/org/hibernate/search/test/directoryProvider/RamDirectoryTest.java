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
package org.hibernate.search.test.directoryProvider;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Session;
import org.hibernate.search.Search;
import org.hibernate.search.test.AlternateDocument;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Emmanuel Bernard
 */
public class RamDirectoryTest extends SearchTestCase {

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
		assertEquals(
				"does not properly filter", 0,
				Search.getFullTextSession( s ).createFullTextQuery( q, Document.class ).list().size()
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
		IndexReader reader = IndexReader.open( getDirectory( Document.class ), true );
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

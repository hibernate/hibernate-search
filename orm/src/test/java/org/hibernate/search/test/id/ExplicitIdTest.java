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
package org.hibernate.search.test.id;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.Search;
import org.hibernate.search.SearchException;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Hardy Ferentschik
 */
public class ExplicitIdTest extends SearchTestCase {

	/**
	 * Tests that @DocumentId can be specified on a field other than the @Id annotated one. See HSEARCH-574.
	 *
	 * @throws Exception in case the test fails.
	 */
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

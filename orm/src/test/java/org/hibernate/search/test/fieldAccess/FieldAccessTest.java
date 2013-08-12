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
package org.hibernate.search.test.fieldAccess;

import java.util.List;

import org.apache.lucene.queryParser.QueryParser;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;

/**
 * @author Emmanuel Bernard
 */
public class FieldAccessTest extends SearchTestCase {

	public void testFields() throws Exception {
		Document doc = new Document( "Hibernate in Action", "Object/relational mapping with Hibernate",
				"blah blah blah" );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( doc );
		tx.commit();

		s.clear();

		FullTextSession session = Search.getFullTextSession( s );
		tx = session.beginTransaction();
		QueryParser p = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
		List result = session.createFullTextQuery( p.parse( "Abstract:Hibernate" ) ).list();
		assertEquals( "Query by field", 1, result.size() );
		s.delete( result.get( 0 ) );
		tx.commit();
		s.close();

	}

	public void testFieldBoost() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( new Document( "Hibernate in Action", "Object and Relational", "blah blah blah" )		);
		s.persist(
				new Document( "Object and Relational", "Hibernate in Action", "blah blah blah" )
		);
		tx.commit();

		s.clear();

		FullTextSession session = Search.getFullTextSession( s );
		tx = session.beginTransaction();
		QueryParser p = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
		List result = session.createFullTextQuery( p.parse( "title:Action OR Abstract:Action" ) ).list();
		assertEquals( "Query by field", 2, result.size() );
		assertEquals( "@Boost fails", "Hibernate in Action", ( (Document) result.get( 0 ) ).getTitle() );
		s.delete( result.get( 0 ) );
		tx.commit();
		s.close();

	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Document.class };
	}
}

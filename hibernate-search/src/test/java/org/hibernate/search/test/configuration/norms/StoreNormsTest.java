/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.configuration.norms;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.queryParser.QueryParser;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.test.SearchTestCase;

/**
 * Test storing and omitting index time norms
 *
 * @author Hardy Ferentschik
 */
public class StoreNormsTest extends SearchTestCase {

	public void testStoreAndOmitNorms() throws Exception {
		Session session = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		Transaction tx = fullTextSession.beginTransaction();
		Test test = new Test();
		test.setWithNormsImplicit( "hello" );
		test.setWithNormsExplicit( "world" );
		test.setWithoutNorms( "how are you?" );
		fullTextSession.save( test );
		tx.commit();

// querying works
//		QueryParser parser = new QueryParser(
//				getTargetLuceneVersion(),
//				"withNormsImplicit",
//				fullTextSession.getSearchFactory().getAnalyzer( Test.class )
//		);
//		org.apache.lucene.search.Query luceneQuery = parser.parse( "withNormsImplicit:hello" );
//		FullTextQuery query = fullTextSession.createFullTextQuery( luceneQuery, Test.class );
//		assertEquals( 1, query.getResultSize() );

		// get a index reader to the underlying index to verify using the Lucene API
		SearchFactoryImplementor searchFactory = getSearchFactoryImpl();
		IndexManager indexManager = searchFactory.getAllIndexesManager().getIndexManager( "test" );
		org.apache.lucene.index.IndexReader indexReader = indexManager.getIndexReaderManager().openIndexReader();
		try {
			Document document = indexReader.document( 0 );
			Fieldable implicitNormField = document.getFieldable( "withNormsImplicit" );
			assertTrue( "norms should be stored for this field", implicitNormField.getOmitNorms() );
		}
		finally {
			indexManager.getIndexReaderManager().closeIndexReader( indexReader );
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Test.class };
	}
}

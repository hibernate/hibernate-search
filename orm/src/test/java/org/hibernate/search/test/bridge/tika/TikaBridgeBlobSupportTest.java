/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.bridge.tika;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Blob;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;

/**
 * @author Hardy Ferentschik
 */
public class TikaBridgeBlobSupportTest extends SearchTestCase {
	private static final String TEST_DOCUMENT_PDF = "/org/hibernate/search/test/bridge/tika/test-document-1.pdf";
	private static final String PATH_TO_TEST_DOCUMENT_PDF;

	static {
		try {
			File pdfFile = new File( TikaBridgeBlobSupportTest.class.getResource( TEST_DOCUMENT_PDF ).toURI() );
			PATH_TO_TEST_DOCUMENT_PDF = pdfFile.getAbsolutePath();
		}
		catch (URISyntaxException e) {
			throw new RuntimeException( "Unable to determine file path for test document" );
		}
	}

	public void testDefaultTikaBridgeWithBlobData() throws Exception {
		Session session = openSession();

		persistBook( session );
		// we have to index manually. Using the Blob (streaming approach) the indexing would try to re-read the
		// input stream of the blob after it was persisted into the database
		indexBook( session );
		searchBook( session );

		session.close();
	}

	@SuppressWarnings("unchecked")
	private void searchBook(Session session) throws ParseException {
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		Transaction tx = session.beginTransaction();
		QueryParser parser = new QueryParser(
				TestConstants.getTargetLuceneVersion(),
				"content",
				TestConstants.standardAnalyzer
		);
		Query query = parser.parse( "foo" );


		List<Book> result = fullTextSession.createFullTextQuery( query ).list();
		assertEquals( "there should be no match", 0, result.size() );

		query = parser.parse( "Lucene" );

		result = fullTextSession.createFullTextQuery( query ).list();
		assertEquals( "there should be match", 1, result.size() );

		tx.commit();
	}

	private void persistBook(Session session) throws IOException {
		Transaction tx = session.beginTransaction();

		Book book = new Book();
		Blob data = getBlobData( PATH_TO_TEST_DOCUMENT_PDF, session );
		book.setContent( data );

		session.save( book );
		session.flush();
		tx.commit();
		session.clear();
	}

	void indexBook(Session session) {
		FullTextSession fullTextSession = org.hibernate.search.Search.getFullTextSession( session );
		fullTextSession.setFlushMode( FlushMode.MANUAL );
		fullTextSession.setCacheMode( CacheMode.IGNORE );

		Transaction transaction = fullTextSession.beginTransaction();

		int BATCH_SIZE = 10;
		ScrollableResults results = fullTextSession.createCriteria( Book.class )
				.setFetchSize( BATCH_SIZE )
				.scroll( ScrollMode.FORWARD_ONLY );
		int index = 0;
		while ( results.next() ) {
			index++;
			fullTextSession.index( results.get( 0 ) );
			if ( index % BATCH_SIZE == 0 ) {
				fullTextSession.flushToIndexes();
				fullTextSession.clear();
			}
		}
		fullTextSession.flush();
		transaction.commit();
		fullTextSession.clear();
	}


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Book.class
		};
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.INDEXING_STRATEGY, "manual" );
	}

	private Blob getBlobData(String fileName, Session session) throws IOException {
		File file = new File( fileName );
		FileInputStream in = FileUtils.openInputStream( file );
		return session.getLobHelper().createBlob( in, file.length() );
	}
}

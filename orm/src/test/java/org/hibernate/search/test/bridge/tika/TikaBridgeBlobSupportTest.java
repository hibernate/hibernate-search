/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.tika;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Blob;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Hardy Ferentschik
 */
public class TikaBridgeBlobSupportTest extends SearchTestBase {
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

	@Test
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

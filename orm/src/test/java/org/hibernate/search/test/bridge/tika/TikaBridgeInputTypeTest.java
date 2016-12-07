/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.tika;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.sql.Blob;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.util.impl.ClasspathResourceAsFile;
import org.hibernate.search.testsupport.TestConstants;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Hardy Ferentschik
 */
public class TikaBridgeInputTypeTest extends SearchTestBase {

	private static final String TEST_DOCUMENT_PDF_1 = "/org/hibernate/search/test/bridge/tika/test-document-1.pdf";
	private static final String TEST_DOCUMENT_PDF_2 = "/org/hibernate/search/test/bridge/builtin/test-document-2.pdf";

	@Rule
	public ClasspathResourceAsFile testDocumentPdf1 = new ClasspathResourceAsFile( getClass(), TEST_DOCUMENT_PDF_1 );

	@Rule
	public ClasspathResourceAsFile testDocumentPdf2 = new ClasspathResourceAsFile( getClass(), TEST_DOCUMENT_PDF_2 );

	@Test
	public void testDefaultTikaBridgeWithListOfString() throws Exception {
		try ( Session session = openSession() ) {
			String content1 = testDocumentPdf1.get().getAbsolutePath();
			String content2 = testDocumentPdf2.get().getAbsolutePath();

			persistBook( session, new Book( content1, content2 ) );

			indexBook( session );

			List<Book> resultWithLucene = search( session, "contentAsListOfString", "Lucene" );
			assertEquals( "there should be a match", 1, resultWithLucene.size() );

			List<Book> resultWithTika = search( session, "contentAsListOfString", "Tika" );
			assertEquals( "there should be a match", 1, resultWithTika.size() );
		}
	}

	private List<Book> search(Session session, String field, String keyword) throws ParseException {
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		Transaction transaction = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( field, TestConstants.standardAnalyzer );
		Query query = parser.parse( keyword );
		@SuppressWarnings("unchecked")
		List<Book> result = fullTextSession.createFullTextQuery( query ).list();
		transaction.commit();
		fullTextSession.clear();
		return result;
	}

	@Test
	public void testDefaultTikaBridgeWithBlob() throws Exception {
		try ( Session session = openSession() ) {
			Blob content = dataAsBlob( testDocumentPdf1.get(), session );

			persistBook( session, new Book( content ) );
			persistBook( session, new Book() );

			// we have to index manually. Using the Blob (streaming approach) the indexing would try to re-read the
			// input stream of the blob after it was persisted into the database
			indexBook( session );
			searchBook( session, "contentAsBlob" );
		}
	}

	@Test
	public void testDefaultTikaBridgeWithByteArray() throws Exception {
		try ( Session session = openSession() ) {
			byte[] content = dataAsBytes( testDocumentPdf1.get() );

			persistBook( session, new Book( content ) );
			persistBook( session, new Book() );

			indexBook( session );
			searchBook( session, "contentAsBytes" );
		}
	}

	@Test
	public void testDefaultTikaBridgeWithURI() throws Exception {
		try ( Session session = openSession() ) {
			URI content = testDocumentPdf1.get().toURI();

			persistBook( session, new Book( content ) );
			persistBook( session, new Book() );

			indexBook( session );
			searchBook( session, "contentAsURI" );
		}
	}

	@SuppressWarnings("unchecked")
	private void searchBook(Session session, String field) throws ParseException {
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		Transaction transaction = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser(
				field,
				TestConstants.standardAnalyzer
		);
		Query query = parser.parse( "foo" );

		List<Book> result = fullTextSession.createFullTextQuery( query ).list();
		assertEquals( "there should be no match", 0, result.size() );

		query = parser.parse( "Lucene" );

		result = fullTextSession.createFullTextQuery( query ).list();
		assertEquals( "there should be match", 1, result.size() );

		query = parser.parse( "<NULL>" );

		result = fullTextSession.createFullTextQuery( query ).list();
		assertEquals( "there should be match", 1, result.size() );

		result = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() ).list();
		assertEquals( "there should be match", 2, result.size() );
		transaction.commit();
		fullTextSession.clear();
	}

	private void persistBook(Session session, Book book) throws IOException {
		Transaction tx = session.beginTransaction();
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
	public Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				Book.class
		};
	}

	@Override
	public void configure(Map<String, Object> cfg) {
		super.configure( cfg );
		cfg.put( Environment.INDEXING_STRATEGY, "manual" );
	}

	private Blob dataAsBlob(File file, Session session) throws IOException {
		FileInputStream in = FileUtils.openInputStream( file );
		return session.getLobHelper().createBlob( in, file.length() );
	}

	private byte[] dataAsBytes(File file) throws IOException {
		return Files.readAllBytes( file.toPath() );
	}
}

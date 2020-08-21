/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.builtin;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;

import javax.sql.rowset.serial.SerialBlob;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.test.util.impl.ClasspathResourceAsFile;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Hardy Ferentschik
 */
public class TikaBridgeInputTypeTest {

	private static final String TEST_DOCUMENT_PDF_1 = "/org/hibernate/search/test/bridge/builtin/test-document-3.pdf";
	private static final String TEST_DOCUMENT_PDF_2 = "/org/hibernate/search/test/bridge/builtin/test-document-2.pdf";

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( Book.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Rule
	public final ClasspathResourceAsFile testDocumentPdf1 = new ClasspathResourceAsFile( getClass(), TEST_DOCUMENT_PDF_1 );

	@Rule
	public final ClasspathResourceAsFile testDocumentPdf2 = new ClasspathResourceAsFile( getClass(), TEST_DOCUMENT_PDF_2 );

	@Test
	public void testDefaultTikaBridgeWithListOfString() throws Exception {
		String content1 = testDocumentPdf1.get().getAbsolutePath();
		String content2 = testDocumentPdf2.get().getAbsolutePath();

		helper.add( new Book( 1, content1, content2 ) );

		List<EntityInfo> resultWithLucene = search( "contentAsListOfString", "Lucene" );
		assertEquals( "there should be a match", 1, resultWithLucene.size() );

		List<EntityInfo> resultWithTika = search( "contentAsListOfString", "Tika" );
		assertEquals( "there should be a match", 1, resultWithTika.size() );
	}

	@Test
	public void testDefaultTikaBridgeWithBlob() throws Exception {
		Blob content = dataAsBlob( testDocumentPdf1.get() );

		helper.add(
				new Book( 1, content ),
				new Book( 2 )
		);

		assertSearchMatches( "contentAsBlob" );
	}

	@Test
	public void testDefaultTikaBridgeWithByteArray() throws Exception {
		byte[] content = dataAsBytes( testDocumentPdf1.get() );

		helper.add(
				new Book( 1, content ),
				new Book( 2 )
		);

		assertSearchMatches( "contentAsBytes" );
	}

	@Test
	public void testDefaultTikaBridgeWithURI() throws Exception {
		URI content = testDocumentPdf1.get().toURI();

		helper.add(
				new Book( 1, content ),
				new Book( 2 )
		);

		assertSearchMatches( "contentAsURI" );
	}

	private List<EntityInfo> search(String field, String keyword) throws ParseException {
		ExtendedSearchIntegrator integrator = sfHolder.getSearchFactory();
		QueryParser parser = new QueryParser( field, TestConstants.standardAnalyzer );
		Query query = parser.parse( keyword );
		List<EntityInfo> result = integrator.createHSQuery( query, Book.class ).queryEntityInfos();
		return result;
	}

	private void assertSearchMatches(String field) throws ParseException {
		helper.assertThat( field, "foo" )
				.from( Book.class )
				.hasResultSize( 0 );

		helper.assertThat( field, "lucene" )
				.from( Book.class )
				.hasResultSize( 1 );

		helper.assertThat( field, "null" )
				.from( Book.class )
				.hasResultSize( 1 );

		helper.assertThat()
				.from( Book.class )
				.hasResultSize( 2 );
	}

	private Blob dataAsBlob(File file) throws IOException {
		byte[] byteArray = dataAsBytes( file );
		try {
			return new SerialBlob( byteArray );
		}
		catch (SQLException e) {
			throw new AssertionFailure( "Unexpected error creating a blob", e );
		}
	}

	private byte[] dataAsBytes(File file) throws IOException {
		return Files.readAllBytes( file.toPath() );
	}
}

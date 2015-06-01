/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.builtin;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TikaMetadataProcessor;
import org.hibernate.search.bridge.TikaParseContextProvider;
import org.hibernate.search.bridge.builtin.TikaBridge;
import org.hibernate.search.engine.impl.LuceneOptionsImpl;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
public class TikaBridgeTest {

	private static final String TEST_DOCUMENT_PDF = "/org/hibernate/search/test/bridge/builtin/test-document-1.pdf";
	private static final String PATH_TO_TEST_DOCUMENT_PDF;

	static {
		try {
			File pdfFile = new File( TikaBridgeTest.class.getResource( TEST_DOCUMENT_PDF ).toURI() );
			PATH_TO_TEST_DOCUMENT_PDF = pdfFile.getAbsolutePath();
		}
		catch (URISyntaxException e) {
			throw new RuntimeException( "Unable to determine file path for test document" );
		}
	}

	private final String testFieldName = "content";
	private TikaBridge bridgeUnderTest;
	private Document testDocument;
	private LuceneOptions options;

	@Before
	public void setUp() {
		bridgeUnderTest = new TikaBridge();
		testDocument = new Document();
		DocumentFieldMetadata fieldMetadata =
				new DocumentFieldMetadata.Builder( null, Store.YES, Field.Index.ANALYZED, Field.TermVector.NO )
						.boost( 0F )
						.build();
		options = new LuceneOptionsImpl( fieldMetadata, 1f, 1f );

		CustomTikaMetadataProcessor.invocationCount = 0;
		CustomTikaParseContextProvider.invocationCount = 0;
	}

	@Test
	public void testPdfToString() throws Exception {
		URI pdfUri = TikaBridgeTest.class.getResource( TEST_DOCUMENT_PDF ).toURI();
		bridgeUnderTest.set( testFieldName, pdfUri, testDocument, options );
		assertEquals(
				"Wrong extracted text",
				"Hibernate Search pdf test document",
				testDocument.get( testFieldName ).trim()
		);
	}

	@Test
	public void testUnknownTikaMetadataProcessor() throws Exception {
		try {
			bridgeUnderTest.setMetadataProcessorClass( this.getClass() );
			fail();
		}
		catch (SearchException e) {
			assertEquals(
					"Wrong error message",
					"Wrong configuration of Tika parse context provider: class org.hibernate.search.test.bridge.builtin.TikaBridgeTest does not implement interface org.hibernate.search.bridge.TikaMetadataProcessor",
					e.getMessage()
			);
		}
	}

	@Test
	public void testPrepareMetadata() {
		bridgeUnderTest.setMetadataProcessorClass( CustomTikaMetadataProcessor.class );
		bridgeUnderTest.set( testFieldName, PATH_TO_TEST_DOCUMENT_PDF, testDocument, options );
		assertEquals(
				"The set method of the custom metadata processor should have been called",
				1,
				CustomTikaMetadataProcessor.invocationCount
		);
	}

	@Test
	public void testIndexingMetadata() {
		bridgeUnderTest.setMetadataProcessorClass( CustomTikaMetadataProcessor.class );
		bridgeUnderTest.set( testFieldName, PATH_TO_TEST_DOCUMENT_PDF, testDocument, options );

		assertEquals(
				"The content type should have been indexed",
				"application/pdf",
				testDocument.get( "type" )
		);
	}

	@Test
	public void testUnknownTikaParseContextProvider() throws Exception {
		try {
			bridgeUnderTest.setParseContextProviderClass( this.getClass() );
			fail();
		}
		catch (SearchException e) {
			assertEquals(
					"Wrong error message",
					"Wrong configuration of Tika metadata processor: class org.hibernate.search.test.bridge.builtin.TikaBridgeTest does not implement interface org.hibernate.search.bridge.TikaParseContextProvider",
					e.getMessage()
			);
		}
	}

	@Test
	public void testCustomTikaParseContextProvider() throws Exception {
		bridgeUnderTest.setParseContextProviderClass( CustomTikaParseContextProvider.class );
		bridgeUnderTest.set( testFieldName, PATH_TO_TEST_DOCUMENT_PDF, testDocument, options );

		assertEquals(
				"The getParseContext method of the custom parse context provider should have been called",
				1,
				CustomTikaParseContextProvider.invocationCount
		);

	}

	@Test
	public void testInvalidPath() throws Exception {
		try {
			bridgeUnderTest.set( testFieldName, "/foo", testDocument, options );
		}
		catch (SearchException e) {
			assertTrue( "Wrong error type", e.getMessage().startsWith( "HSEARCH000152" ) );
		}
	}

	public static class CustomTikaMetadataProcessor implements TikaMetadataProcessor {
		public static int invocationCount = 0;

		@Override
		public Metadata prepareMetadata() {
			Metadata meta = new Metadata();
			meta.add( Metadata.RESOURCE_NAME_KEY, PATH_TO_TEST_DOCUMENT_PDF );
			return meta;
		}

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions, Metadata metadata) {
			invocationCount++;

			assertEquals(
					"Metadata.RESOURCE_NAME_KEY should be set in the metadata",
					PATH_TO_TEST_DOCUMENT_PDF,
					metadata.get( Metadata.RESOURCE_NAME_KEY )
			);

			// indexing the discovered content type
			luceneOptions.addFieldToDocument( "type", metadata.get( Metadata.CONTENT_TYPE ), document );
		}
	}

	public static class CustomTikaParseContextProvider implements TikaParseContextProvider {
		public static int invocationCount = 0;

		@Override
		public ParseContext getParseContext(String name, Object value) {
			invocationCount++;
			return new ParseContext();
		}
	}
}

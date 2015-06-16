/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.sql.Blob;
import java.sql.SQLException;

import org.apache.lucene.document.Document;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.WriteOutContentHandler;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TikaMetadataProcessor;
import org.hibernate.search.bridge.TikaParseContextProvider;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import static org.apache.tika.io.IOUtils.closeQuietly;

/**
 * Bridge implementation which uses Apache Tika to extract data from provided input.
 *
 * @author Hardy Ferentschik
 */
public class TikaBridge implements FieldBridge {
	private static final Log log = LoggerFactory.make();

	// Expensive, so only do it once. The Parser is threadsafe.
	private final Parser parser = new AutoDetectParser();

	private TikaMetadataProcessor metadataProcessor;
	private TikaParseContextProvider parseContextProvider;

	public TikaBridge() {
		setMetadataProcessorClass( null );
		setParseContextProviderClass( null );
	}

	public void setParseContextProviderClass(Class<?> parseContextProviderClass) {
		if ( parseContextProviderClass == null ) {
			parseContextProvider = new NoopParseContextProvider();
		}
		else {
			parseContextProvider = ClassLoaderHelper.instanceFromClass(
					TikaParseContextProvider.class,
					parseContextProviderClass,
					"Tika metadata processor"
			);
		}
	}

	public void setMetadataProcessorClass(Class<?> metadataProcessorClass) {
		if ( metadataProcessorClass == null ) {
			metadataProcessor = new NoopTikaMetadataProcessor();
		}
		else {
			metadataProcessor = ClassLoaderHelper.instanceFromClass(
					TikaMetadataProcessor.class,
					metadataProcessorClass,
					"Tika parse context provider"
			);
		}
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		final Metadata metadata;
		final String fieldValue;

		if ( value != null ) {
			metadata = metadataProcessor.prepareMetadata();
			fieldValue = getFieldValue( name, value, metadata );
		}
		else if ( luceneOptions.indexNullAs() != null ) {
			metadata = metadataProcessor.prepareMetadata();
			fieldValue = luceneOptions.indexNullAs();
		}
		else {
			return;
		}

		luceneOptions.addFieldToDocument( name, fieldValue, document );

		// allow for optional indexing of metadata by the user
		metadataProcessor.set( name, value, document, luceneOptions, metadata );
	}

	/**
	 * Opens an input stream for the given blob, byte array, file or URI and returns its contents.
	 */
	private String getFieldValue(String name, Object value, Metadata metadata) {
		InputStream in = getInputStreamForData( value );
		try {
			ParseContext parseContext = parseContextProvider.getParseContext( name, value );

			StringWriter writer = new StringWriter();
			WriteOutContentHandler contentHandler = new WriteOutContentHandler( writer );

			parser.parse( in, contentHandler, metadata, parseContext );

			return writer.toString();
		}
		catch (Exception e) {
			throw log.unableToParseDocument( e );
		}
		finally {
			closeQuietly( in );
		}
	}

	private InputStream getInputStreamForData(Object object) {
		if ( object instanceof Blob ) {
			try {
				return ( (Blob) object ).getBinaryStream();
			}
			catch (SQLException e) {
				throw log.unableToGetInputStreamFromBlob( e );
			}
		}
		else if ( object instanceof byte[] ) {
			byte[] data = (byte[]) object;
			return new ByteArrayInputStream( data );
		}
		else if ( object instanceof String ) {
			String path = (String) object;
			File file = new File( path );
			return openInputStream( file );
		}
		else if ( object instanceof URI ) {
			URI uri = (URI) object;
			File file = new File( uri );
			return openInputStream( file );
		}
		else {
			throw log.unsupportedTikaBridgeType( object != null ? object.getClass() : null );
		}
	}

	private FileInputStream openInputStream(File file) {
		if ( file.exists() ) {
			if ( file.isDirectory() ) {
				throw log.fileIsADirectory( file.toString() );
			}
			if ( !file.canRead() ) {
				throw log.fileIsNotReadable( file.toString() );
			}
		}
		else {
			throw log.fileDoesNotExist( file.toString() );
		}
		try {
			return new FileInputStream( file );
		}
		catch (FileNotFoundException e) {
			throw log.fileDoesNotExist( file.toString() );
		}
	}

	private static class NoopTikaMetadataProcessor implements TikaMetadataProcessor {
		@Override
		public Metadata prepareMetadata() {
			return new Metadata();
		}

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions, Metadata metadata) {
		}
	}

	private static class NoopParseContextProvider implements TikaParseContextProvider {
		@Override
		public ParseContext getParseContext(String name, Object value) {
			return new ParseContext();
		}
	}
}

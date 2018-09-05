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

import org.hibernate.search.bridge.AppliedOnTypeAwareBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.MetadataProvidingTikaMetadataProcessor;
import org.hibernate.search.bridge.TikaMetadataProcessor;
import org.hibernate.search.bridge.TikaParseContextProvider;
import org.hibernate.search.bridge.TikaParserProvider;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

import static org.apache.tika.io.IOUtils.closeQuietly;

/**
 * Bridge implementation which uses Apache Tika to extract data from provided input.
 *
 * @author Hardy Ferentschik
 */
public class TikaBridge implements MetadataProvidingFieldBridge, AppliedOnTypeAwareBridge {
	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private TikaParserProvider parserProvider;
	private Parser parser;

	private TikaMetadataProcessor metadataProcessor;
	private TikaParseContextProvider parseContextProvider;

	public TikaBridge() {
		setParserProviderClass( null );
		setMetadataProcessorClass( null );
		setParseContextProviderClass( null );
	}

	@Override
	public void setAppliedOnType(Class<?> returnType) {
		// Use this hook to initialize the bridge after the setters have been called.
		// This is expensive, so we should only do it once.
		// The Parser is threadsafe.
		parser = parserProvider.createParser();
		parserProvider = null;
	}

	@Override
	public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {

		if ( metadataProcessor instanceof MetadataProvidingTikaMetadataProcessor ) {
			( (MetadataProvidingTikaMetadataProcessor) metadataProcessor )
					.configureFieldMetadata( name, builder );
		}
	}

	public void setParserProviderClass(Class<?> parserProviderClass) {
		if ( parserProviderClass == null ) {
			parserProvider = new AutoDetectParserProvider();
		}
		else {
			parserProvider = ClassLoaderHelper.instanceFromClass(
					TikaParserProvider.class,
					parserProviderClass,
					"Tika parser provider"
			);
		}
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

	private static class AutoDetectParserProvider implements TikaParserProvider {
		@Override
		public Parser createParser() {
			return new AutoDetectParser();
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

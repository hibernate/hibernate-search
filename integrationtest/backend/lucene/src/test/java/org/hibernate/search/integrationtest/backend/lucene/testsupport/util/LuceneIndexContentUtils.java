/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;

import org.junit.rules.TemporaryFolder;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.assertj.core.api.iterable.ThrowingExtractor;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.util.common.AssertionFailure;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class LuceneIndexContentUtils {

	private LuceneIndexContentUtils() {
	}

	public static <T> T doOnIndexCopy(SearchSetupHelper setupHelper, TemporaryFolder temporaryFolder,
		String indexName, ThrowingExtractor<DirectoryReader, T, IOException> action) throws IOException {
		Path indexCopyPath = temporaryFolder.getRoot().toPath().resolve( indexName + "_copy" );

		T result;
		LuceneTckBackendAccessor accessor = (LuceneTckBackendAccessor) setupHelper.getBackendAccessor();
		try {
			// Copy the index to be able to open a directory despite the lock
			accessor.copyIndexContent( indexCopyPath, indexName );

			try ( Directory directory = FSDirectory.open( indexCopyPath );
				DirectoryReader reader = DirectoryReader.open( directory ) ) {
				result = action.apply( reader );
			}
		}
		finally {
			try {
				deleteRecursively( indexCopyPath );
			}
			catch (RuntimeException | IOException e) {
				System.out.println( "Could not delete '" + indexCopyPath + "': " + e );
			}
		}

		return result;
	}

	public static void deleteRecursively(Path path) throws IOException {
		Files.walkFileTree( path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete( file );
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.deleteIfExists( dir );
				return FileVisitResult.CONTINUE;
			}
		} );
	}

	public static JSONObject loadIndexData(String values) {
		Path path = Paths.get( "src", "test", "resources", values );
		if ( !Files.exists( path ) ) {
			throw new AssertionFailure( String.format( "can not find resorce file %s.", values ) );
		}

		try {
			path = path.toAbsolutePath();
			String content = new String( Files.readAllBytes( path ), "UTF8" );
			JSONObject result = new JSONObject( content );
			return result;
		}
		catch (IOException | JSONException ex) {
			throw new AssertionFailure( "load json index data", ex );
		}
	}

	public static void initIndexFromJson(JSONObject jdocuments, SimpleIndexMapping mapping,
		IndexIndexingPlan<? extends DocumentElement> plan) {

		try {

			for ( int i = 0; i < jdocuments.length(); i++ ) {
				String id = jdocuments.names().getString( i );
				JSONObject jdocument = jdocuments.getJSONObject( id );

				plan.add( referenceProvider( id ), document -> {
					try {
						initIndexDocumentFromJson( "", id, document, jdocument, mapping, plan );
					}
					catch (JSONException ex) {
						throw new AssertionFailure( "init document from json data", ex );
					}
				} );
			}
		}
		catch (JSONException ex) {
			throw new AssertionFailure( "init index from json data", ex );
		}
	}

	private static void initIndexDocumentFromJson(String prefix, String id, DocumentElement document,
		JSONObject jdocument, SimpleIndexMapping mapping,
		IndexIndexingPlan<? extends DocumentElement> plan) throws JSONException {

		Map<String, DocumentElement> refmap = new HashMap<>();

		for ( int i = 0; i < jdocument.length(); i++ ) {
			String name = jdocument.names().getString( i );
			String path = prefix + name;

			Class<?> type = mapping.getType( path );
			if ( type == null ) {
				continue;
			}

			if ( IndexObjectFieldReference.class.equals( type ) ) {
				ObjectFieldStorage storage = mapping.getObjectStorage( path );

				JSONObject jsubdata = jdocument.getJSONObject( name );
				IndexObjectFieldReference reference = mapping.getObjectReference( path );
				for ( int j = 0; j < jsubdata.length(); j++ ) {
					String subid = jsubdata.names().getString( j );
					JSONObject jsub = jsubdata.getJSONObject( subid );

					DocumentElement subdocument;

					if ( storage == ObjectFieldStorage.NESTED ) {
						subdocument = document.addObject( reference );
					}
					else {
						subdocument = refmap.get( path );
						if ( subdocument == null ) {
							subdocument = document.addObject( reference );
							refmap.put( path, subdocument );
						}
					}

					initIndexDocumentFromJson( path + ".",
						subid, subdocument, jsub, mapping, plan );
				}
			}
			else {
				Object values = jdocument.get( name );
				IndexFieldReference reference = mapping.getFieldReference( path );
				if ( values instanceof JSONArray ) {
					JSONArray array = (JSONArray) values;
					for ( int j = 0; j < array.length(); j++ ) {
						initIndexValueFromJson( reference, type,
							j, array, document );
					}
				}
				else {
					initIndexValueFromJson( reference,
						type, name, jdocument, document );
				}
			}
		}
	}

	private static <T> void initIndexValueFromJson(IndexFieldReference reference,
		Class<T> type, String name, JSONObject jdocument, DocumentElement document)
		throws JSONException {

		if ( String.class.isAssignableFrom( type ) ) {
			String value = jdocument.getString( name );
			document.addValue( reference, value );
		}
		else if ( Double.class.isAssignableFrom( type ) ) {
			double value = jdocument.getDouble( name );
			document.addValue( reference, value );
		}
		else if ( Float.class.isAssignableFrom( type ) ) {
			double value = jdocument.getDouble( name );
			document.addValue( reference, new Double( value ).floatValue() );
		}
		else if ( Long.class.isAssignableFrom( type ) ) {
			long value = jdocument.getLong( name );
			document.addValue( reference, value );
		}
		else if ( Integer.class.isAssignableFrom( type ) ) {
			int value = jdocument.getInt( name );
			document.addValue( reference, value );
		}
		else if ( Boolean.class.isAssignableFrom( type ) ) {
			boolean value = jdocument.getBoolean( name );
			document.addValue( reference, value );
		}
	}

	private static <T> void initIndexValueFromJson(IndexFieldReference reference,
		Class<T> type, int index, JSONArray jdocument, DocumentElement document)
		throws JSONException {

		if ( String.class.isAssignableFrom( type ) ) {
			String value = jdocument.getString( index );
			document.addValue( reference, value );
		}
		else if ( Double.class.isAssignableFrom( type ) ) {
			double value = jdocument.getDouble( index );
			document.addValue( reference, value );
		}
		else if ( Float.class.isAssignableFrom( type ) ) {
			double value = jdocument.getDouble( index );
			document.addValue( reference, new Double( value ).floatValue() );
		}
		else if ( Long.class.isAssignableFrom( type ) ) {
			long value = jdocument.getLong( index );
			document.addValue( reference, value );
		}
		else if ( Integer.class.isAssignableFrom( type ) ) {
			int value = jdocument.getInt( index );
			document.addValue( reference, value );
		}
		else if ( Boolean.class.isAssignableFrom( type ) ) {
			boolean value = jdocument.getBoolean( index );
			document.addValue( reference, value );
		}
	}

}

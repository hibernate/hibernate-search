/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
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

public final class JSONTestModelLoader {

	private JSONTestModelLoader() {
	}

	public static JSONObject loadIndexData(String resource) {
		ClassLoader cl = JSONTestModelLoader.class.getClassLoader();
		try ( InputStream in = cl.getResourceAsStream( resource ) ) {
			if ( in == null ) {
				throw new AssertionFailure( "can not find resorce file " + resource );
			}

			BufferedReader reader = new BufferedReader( new InputStreamReader( in, "UTF8" ) );
			StringBuilder out = new StringBuilder();
			String line;
			while ( (line = reader.readLine()) != null ) {
				out.append( line ).append( "\n" );
			}
			String content = out.toString();
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

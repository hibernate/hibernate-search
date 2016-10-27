/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl.gson;

import java.io.IOException;

import org.hibernate.search.elasticsearch.schema.impl.model.IndexType;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * @author Yoann Rodiere
 */
@SuppressWarnings("deprecation")
public class ES2IndexTypeJsonAdapter extends TypeAdapter<IndexType> {

	private static final String ANALYZED_STRING = "analyzed";
	private static final String NOT_ANALYZED_STRING = "not_analyzed";
	private static final String NO_STRING = "no";

	@Override
	public void write(JsonWriter out, IndexType value) throws IOException {
		switch ( value ) {
			case ANALYZED:
				out.value( ANALYZED_STRING );
				break;
			case NOT_ANALYZED:
				out.value( NOT_ANALYZED_STRING );
				break;
			case NO:
				out.value( NO_STRING );
				break;
			default:
				throw new IllegalStateException( "Invalid value for IndexType in ES2: " + value );
		}
	}

	@Override
	public IndexType read(JsonReader in) throws IOException {
		String value = in.nextString();
		switch ( value ) {
			case ANALYZED_STRING:
				return IndexType.ANALYZED;
			case NOT_ANALYZED_STRING:
				return IndexType.NOT_ANALYZED;
			case NO_STRING:
				return IndexType.NO;
			default:
				throw new IllegalStateException( "Invalid value for IndexType in ES2: " + value );
		}
	}

}

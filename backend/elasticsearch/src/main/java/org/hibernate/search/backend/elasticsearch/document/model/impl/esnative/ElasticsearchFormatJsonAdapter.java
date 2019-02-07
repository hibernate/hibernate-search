/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl.esnative;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchFormatJsonAdapter extends TypeAdapter<List<String>> {

	private static final String FORMAT_SEPARATOR_REGEX = "\\|\\|";
	private static final String FORMAT_SEPARATOR = "||";

	@Override
	public void write(JsonWriter out, List<String> value) throws IOException {
		if ( value == null ) {
			out.nullValue();
			return;
		}

		String joinedFormats = value.stream().collect( Collectors.joining( FORMAT_SEPARATOR ) );
		out.value( joinedFormats );
	}

	@Override
	public List<String> read(JsonReader in) throws IOException {
		if ( in.peek() == JsonToken.NULL ) {
			in.nextNull();
			return null;
		}

		String joinedFormats = in.nextString();
		return Arrays.asList( joinedFormats.split( FORMAT_SEPARATOR_REGEX ) );
	}

}

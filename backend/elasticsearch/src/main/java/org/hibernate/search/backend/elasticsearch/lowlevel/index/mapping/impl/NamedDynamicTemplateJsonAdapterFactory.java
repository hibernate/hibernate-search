/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class NamedDynamicTemplateJsonAdapterFactory implements TypeAdapterFactory {

	private static final TypeToken<NamedDynamicTemplate> NAMED_DYNAMIC_TEMPLATE_TYPE_TOKEN =
			TypeToken.get( NamedDynamicTemplate.class );
	private static final TypeToken<DynamicTemplate> DYNAMIC_TEMPLATE_TYPE_TOKEN = TypeToken.get( DynamicTemplate.class );

	@Override
	@SuppressWarnings("unchecked")
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
		if ( !NAMED_DYNAMIC_TEMPLATE_TYPE_TOKEN.equals( typeToken ) ) {
			return null;
		}

		TypeAdapter<DynamicTemplate> templateAdapter = gson.getAdapter( DYNAMIC_TEMPLATE_TYPE_TOKEN );

		return (TypeAdapter<T>) new Adapter( templateAdapter );
	}

	private static class Adapter extends TypeAdapter<NamedDynamicTemplate> {
		private final TypeAdapter<DynamicTemplate> templateAdapter;

		public Adapter(TypeAdapter<DynamicTemplate> templateAdapter) {
			this.templateAdapter = templateAdapter;
		}

		@Override
		public void write(JsonWriter out, NamedDynamicTemplate value) throws IOException {
			if ( value == null ) {
				out.nullValue();
				return;
			}

			out.beginObject();
			out.name( value.name );
			templateAdapter.write( out, value.template );
			out.endObject();
		}

		@Override
		public NamedDynamicTemplate read(JsonReader in) throws IOException {
			if ( in.peek() == JsonToken.NULL ) {
				in.nextNull();
				return null;
			}

			in.beginObject();
			String name = in.nextName();
			DynamicTemplate template = templateAdapter.read( in );
			in.endObject();

			return new NamedDynamicTemplate( name, template );
		}
	}
}

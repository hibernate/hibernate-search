/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.gson.impl;

import java.io.IOException;
import java.util.Map;

import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public abstract class AbstractExtraPropertiesJsonAdapter<T> extends TypeAdapter<T> {

	interface FieldAdapter<T> {
		void read(JsonReader in, T instance) throws IOException;

		void write(JsonWriter out, T instance) throws IOException;

		boolean serialized();
	}

	interface ExtraPropertyAdapter<T> {
		void readOne(JsonReader in, String name, T instance) throws IOException;

		void writeAll(JsonWriter out, T instance) throws IOException;
	}

	private final Map<String, ? extends FieldAdapter<? super T>> fieldAdapters;
	private final ExtraPropertyAdapter<? super T> extraPropertyAdapter;

	public AbstractExtraPropertiesJsonAdapter(Map<String, ? extends FieldAdapter<? super T>> fieldAdapters,
			ExtraPropertyAdapter<? super T> extraPropertyAdapter) {
		super();
		this.fieldAdapters = fieldAdapters;
		this.extraPropertyAdapter = extraPropertyAdapter;
	}

	@Override
	public T read(JsonReader in) throws IOException {
		if ( in.peek() == JsonToken.NULL ) {
			in.nextNull();
			return null;
		}

		T instance = createInstance();
		try {
			in.beginObject();
			while ( in.hasNext() ) {
				String name = in.nextName();
				FieldAdapter<? super T> fieldAdapter = fieldAdapters.get( name );
				if ( fieldAdapter == null ) {
					extraPropertyAdapter.readOne( in, name, instance );
				}
				else {
					fieldAdapter.read( in, instance );
				}
			}
			in.endObject();
		}
		catch (IllegalStateException e) {
			throw new JsonSyntaxException( e );
		}

		return instance;
	}

	@Override
	public void write(JsonWriter out, T instance) throws IOException {
		if ( instance == null ) {
			out.nullValue();
		}
		out.beginObject();
		for ( Map.Entry<String, ? extends FieldAdapter<? super T>> entry : fieldAdapters.entrySet() ) {
			FieldAdapter<? super T> fieldAdapter = entry.getValue();
			if ( fieldAdapter.serialized() ) {
				out.name( entry.getKey() );
				entry.getValue().write( out, instance );
			}
		}
		extraPropertyAdapter.writeAll( out, instance );
		out.endObject();
	}

	protected abstract T createInstance();

}

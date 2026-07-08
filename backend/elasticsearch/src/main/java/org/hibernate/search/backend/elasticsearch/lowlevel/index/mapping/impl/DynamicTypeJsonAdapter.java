/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import java.io.IOException;

import org.hibernate.search.util.common.AssertionFailure;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class DynamicTypeJsonAdapter extends TypeAdapter<DynamicType> {

	@Override
	public void write(JsonWriter out, DynamicType value) throws IOException {
		if ( value == null ) {
			out.nullValue();
			return;
		}
		switch ( value ) {
			case TRUE -> out.value( "true" );
			case FALSE -> out.value( "false" );
			case STRICT -> out.value( "strict" );
			default -> throw new AssertionFailure( "Unknown DynamicType: " + value );
		}
	}

	@Override
	public DynamicType read(JsonReader in) throws IOException {
		if ( in.peek() == JsonToken.NULL ) {
			in.nextNull();
			return null;
		}
		String value = in.nextString();
		return switch ( value ) {
			case "true" -> DynamicType.TRUE;
			case "false" -> DynamicType.FALSE;
			case "strict" -> DynamicType.STRICT;
			default -> throw new AssertionFailure( "Unknown dynamic type value: " + value );
		};
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import java.io.IOException;

import org.hibernate.search.util.common.AssertionFailure;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class RoutingTypeJsonAdapter extends TypeAdapter<RoutingType> {

	@Override
	public void write(JsonWriter out, RoutingType value) throws IOException {
		if ( value == null ) {
			out.nullValue();
			return;
		}

		out.beginObject();
		switch ( value ) {
			case REQUIRED:
				out.name( "required" );
				out.value( true );
				break;
			default:
				throw new AssertionFailure( "Unexpected value for attribute of type " + RoutingType.class + ": " + value );
		}
		out.endObject();
	}

	@Override
	public RoutingType read(JsonReader in) throws IOException {
		if ( in.peek() == JsonToken.NULL ) {
			in.nextNull();
			return null;
		}

		RoutingType value = null;
		in.beginObject();
		while ( in.hasNext() ) {
			String name = in.nextName();
			switch ( name ) {
				case "required":
					if ( in.nextBoolean() ) {
						value = RoutingType.REQUIRED;
					}
					else {
						value = RoutingType.OPTIONAL;
					}
					break;
				default:
					throw new AssertionFailure(
							"Unexpected property for attribute of type " + RoutingType.class + ": " + name );
			}
		}
		in.endObject();

		return value;
	}

}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl.gson;

import java.io.IOException;

import org.hibernate.search.elasticsearch.schema.impl.model.NormsType;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * @author Yoann Rodiere
 */
public class ES5NormsTypeJsonAdapter extends TypeAdapter<NormsType> {

	@Override
	public void write(JsonWriter out, NormsType value) throws IOException {
		switch ( value ) {
			case TRUE:
				out.value( true );
				break;
			case FALSE:
				out.value( false );
				break;
			default:
				throw new IllegalStateException( "Invalid value for NormsType in ES5: " + value );
		}
	}

	@Override
	public NormsType read(JsonReader in) throws IOException {
		if ( in.nextBoolean() ) {
			return NormsType.TRUE;
		}
		else {
			return NormsType.FALSE;
		}
	}

}

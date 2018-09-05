/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl.gson;

import java.io.IOException;

import org.hibernate.search.elasticsearch.schema.impl.model.FieldDataType;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * @author Yoann Rodiere
 */
public class ES2FieldDataTypeJsonAdapter extends TypeAdapter<FieldDataType> {

	@Override
	public void write(JsonWriter out, FieldDataType value) throws IOException {
		// Ignore the value: fielddata is not supported on ES2
		boolean previousSerializeNulls = out.getSerializeNulls();
		out.setSerializeNulls( false );
		out.nullValue();
		out.setSerializeNulls( previousSerializeNulls );
	}

	@Override
	public FieldDataType read(JsonReader in) throws IOException {
		// Ignore: fielddata is not supported on ES2
		in.skipValue();
		return null;
	}

}

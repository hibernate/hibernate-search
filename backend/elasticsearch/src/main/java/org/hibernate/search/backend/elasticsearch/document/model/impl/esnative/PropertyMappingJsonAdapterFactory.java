/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl.esnative;

import java.util.Map;

import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

public class PropertyMappingJsonAdapterFactory extends AbstractTypeMappingJsonAdapterFactory {

	private static final TypeToken<Map<String, PropertyMapping>> FIELD_MAP_TYPE_TOKEN =
			new TypeToken<Map<String, PropertyMapping>>() {
			};

	@Override
	protected <T> void addFields(Builder<T> builder) {
		super.addFields( builder );
		builder.add( "type", DataType.class );
		builder.add( "boost", Float.class );
		builder.add( "index", IndexType.class );
		builder.add( "norms", NormsType.class );
		builder.add( "docValues", Boolean.class );
		builder.add( "store", Boolean.class );
		builder.add( "nullValue", JsonPrimitive.class );
		builder.add( "fields", FIELD_MAP_TYPE_TOKEN );
		builder.add( "analyzer", String.class );
		builder.add( "fieldData", FieldDataType.class );
		builder.add( "normalizer", String.class );
		builder.add( "format", new ElasticsearchFormatJsonAdapter() );
	}
}

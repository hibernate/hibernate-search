/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl.esnative;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.gson.impl.AbstractConfiguredExtraPropertiesJsonAdapterFactory;

import com.google.gson.reflect.TypeToken;

public class AbstractTypeMappingJsonAdapterFactory extends AbstractConfiguredExtraPropertiesJsonAdapterFactory {

	private static final TypeToken<Map<String, PropertyMapping>> PROPERTY_MAP_TYPE_TOKEN =
			new TypeToken<Map<String, PropertyMapping>>() {
			};

	@Override
	protected <T> void addFields(Builder<T> builder) {
		builder.add( "properties", PROPERTY_MAP_TYPE_TOKEN );
		builder.add( "dynamic", DynamicType.class );
	}
}

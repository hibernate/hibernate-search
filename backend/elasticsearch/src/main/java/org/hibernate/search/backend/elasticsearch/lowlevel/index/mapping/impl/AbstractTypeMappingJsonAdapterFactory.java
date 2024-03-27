/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.gson.impl.AbstractConfiguredExtraPropertiesJsonAdapterFactory;

import com.google.gson.reflect.TypeToken;

class AbstractTypeMappingJsonAdapterFactory extends AbstractConfiguredExtraPropertiesJsonAdapterFactory {

	private static final TypeToken<Map<String, PropertyMapping>> PROPERTY_MAP_TYPE_TOKEN =
			new TypeToken<Map<String, PropertyMapping>>() {
			};

	@Override
	protected <T> void addFields(Builder<T> builder) {
		builder.add( "properties", PROPERTY_MAP_TYPE_TOKEN );
		builder.add( "dynamic", DynamicType.class );
	}
}

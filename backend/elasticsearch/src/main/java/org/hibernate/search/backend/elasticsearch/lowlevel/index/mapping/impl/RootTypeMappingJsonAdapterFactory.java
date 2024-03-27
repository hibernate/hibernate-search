/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import java.util.List;

import com.google.gson.reflect.TypeToken;

public class RootTypeMappingJsonAdapterFactory extends AbstractTypeMappingJsonAdapterFactory {

	private static final TypeToken<List<NamedDynamicTemplate>> DYNAMIC_TEMPLATES_TYPE_TOKEN =
			new TypeToken<List<NamedDynamicTemplate>>() {};

	@Override
	protected <T> void addFields(Builder<T> builder) {
		super.addFields( builder );
		builder.add( "routing", RoutingType.class );
		builder.add( "dynamicTemplates", DYNAMIC_TEMPLATES_TYPE_TOKEN );
	}
}

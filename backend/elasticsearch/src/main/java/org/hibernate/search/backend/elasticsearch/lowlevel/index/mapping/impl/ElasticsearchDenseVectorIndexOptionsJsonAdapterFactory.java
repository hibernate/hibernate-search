/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.AbstractConfiguredExtraPropertiesJsonAdapterFactory;

public class ElasticsearchDenseVectorIndexOptionsJsonAdapterFactory
		extends
		AbstractConfiguredExtraPropertiesJsonAdapterFactory {

	@Override
	protected <T> void addFields(Builder<T> builder) {
		builder.add( "type", String.class );
		builder.add( "m", Integer.class );
		builder.add( "efConstruction", Integer.class );
	}
}

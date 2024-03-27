/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.AbstractConfiguredExtraPropertiesJsonAdapterFactory;

public class OpenSearchVectorTypeMethodJsonAdapterFactory
		extends
		AbstractConfiguredExtraPropertiesJsonAdapterFactory {

	@Override
	protected <T> void addFields(Builder<T> builder) {
		builder.add( "name", String.class );
		builder.add( "spaceType", String.class );
		builder.add( "engine", String.class );
		builder.add( "parameters", OpenSearchVectorTypeMethod.Parameters.class );
	}

	public static class ParametersJsonAdapterFactory
			extends
			AbstractConfiguredExtraPropertiesJsonAdapterFactory {
		@Override
		protected <T> void addFields(Builder<T> builder) {
			builder.add( "m", Integer.class );
			builder.add( "efConstruction", Integer.class );
		}
	}
}

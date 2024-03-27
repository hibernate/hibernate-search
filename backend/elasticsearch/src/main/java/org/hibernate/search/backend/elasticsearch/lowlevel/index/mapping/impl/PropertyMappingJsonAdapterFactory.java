/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import com.google.gson.JsonElement;

public class PropertyMappingJsonAdapterFactory extends AbstractTypeMappingJsonAdapterFactory {

	@Override
	protected <T> void addFields(Builder<T> builder) {
		super.addFields( builder );
		builder.add( "type", String.class );
		builder.add( "index", Boolean.class );
		builder.add( "norms", Boolean.class );
		builder.add( "docValues", Boolean.class );
		builder.add( "nullValue", JsonElement.class );
		builder.add( "analyzer", String.class );
		builder.add( "searchAnalyzer", String.class );
		builder.add( "normalizer", String.class );
		builder.add( "format", new FormatJsonAdapter() );
		builder.add( "scalingFactor", Double.class );
		builder.add( "termVector", String.class );
		builder.add( "elementType", String.class );
		builder.add( "dims", Integer.class );
		builder.add( "similarity", String.class );
		builder.add( "indexOptions", ElasticsearchDenseVectorIndexOptions.class );
		builder.add( "dimension", Integer.class );
		builder.add( "method", OpenSearchVectorTypeMethod.class );
		builder.add( "dataType", String.class );
	}
}

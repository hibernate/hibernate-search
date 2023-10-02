/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl;

import java.util.List;

import com.google.gson.reflect.TypeToken;

public class NormalizerDefinitionJsonAdapterFactory extends AnalysisDefinitionJsonAdapterFactory {

	private static final TypeToken<List<String>> STRING_LIST_TYPE_TOKEN =
			new TypeToken<List<String>>() {
			};

	@Override
	protected <T> void addFields(Builder<T> builder) {
		super.addFields( builder );
		builder.add( "tokenFilters", STRING_LIST_TYPE_TOKEN );
		builder.add( "charFilters", STRING_LIST_TYPE_TOKEN );
	}

}

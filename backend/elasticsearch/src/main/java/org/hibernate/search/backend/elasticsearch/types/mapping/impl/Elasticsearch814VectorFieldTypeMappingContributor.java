/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.mapping.impl;

public class Elasticsearch814VectorFieldTypeMappingContributor extends Elasticsearch812VectorFieldTypeMappingContributor {

	@Override
	protected boolean indexOptionAddCondition(Context context) {
		// we want to always add index options and in particular include `hnsw` type.
		return context.searchable();
	}
}

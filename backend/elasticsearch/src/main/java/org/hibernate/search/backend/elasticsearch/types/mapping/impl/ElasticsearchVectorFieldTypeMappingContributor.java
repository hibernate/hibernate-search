/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.mapping.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.backend.types.VectorSimilarity;

public interface ElasticsearchVectorFieldTypeMappingContributor {

	void contribute(PropertyMapping mapping, Context context);

	<F> void contribute(ElasticsearchIndexValueFieldType.Builder<F> builder, Context context);

	interface Context {
		String type();

		VectorSimilarity vectorSimilarity();

		int dimension();

		Integer efConstruction();

		Integer m();

		boolean searchable();
	}
}

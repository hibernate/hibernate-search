/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.mapping.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchKnnPredicate;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;
import org.hibernate.search.engine.search.predicate.spi.KnnPredicateBuilder;

public class OpenSearch214VectorFieldTypeMappingContributor extends AbstractOpenSearch2VectorFieldTypeMappingContributor {

	@Override
	protected <F> SearchQueryElementFactory<? extends KnnPredicateBuilder,
			ElasticsearchSearchIndexScope<?>,
			ElasticsearchSearchIndexValueFieldContext<F>> getKnnPredicateFactory(
					ElasticsearchIndexValueFieldType.Builder<F> builder) {
		return new ElasticsearchKnnPredicate.OpenSearch214Factory<>( builder.codec() );
	}
}

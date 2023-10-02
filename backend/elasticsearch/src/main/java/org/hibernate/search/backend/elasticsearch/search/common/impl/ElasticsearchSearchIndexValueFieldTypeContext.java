/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.common.impl;

import java.util.Optional;

import org.hibernate.search.engine.search.common.spi.SearchIndexValueFieldTypeContext;

import com.google.gson.JsonPrimitive;

public interface ElasticsearchSearchIndexValueFieldTypeContext<F>
		extends
		SearchIndexValueFieldTypeContext<ElasticsearchSearchIndexScope<?>, ElasticsearchSearchIndexValueFieldContext<F>, F> {

	JsonPrimitive elasticsearchTypeAsJson();

	Optional<String> searchAnalyzerName();

	Optional<String> normalizerName();

	boolean hasNormalizerOnAtLeastOneIndex();

}

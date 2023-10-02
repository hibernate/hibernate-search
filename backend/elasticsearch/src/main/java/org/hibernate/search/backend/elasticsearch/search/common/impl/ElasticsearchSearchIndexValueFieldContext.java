/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.common.impl;

import org.hibernate.search.engine.search.common.spi.SearchIndexValueFieldContext;

/**
 * Information about a value (non-object) field targeted by search,
 * be it in a projection, a predicate, a sort, ...
 *
 * @param <F> The indexed field value type.
 */
public interface ElasticsearchSearchIndexValueFieldContext<F>
		extends SearchIndexValueFieldContext<ElasticsearchSearchIndexScope<?>>,
		ElasticsearchSearchIndexNodeContext {

	@Override
	ElasticsearchSearchIndexValueFieldTypeContext<F> type();

}

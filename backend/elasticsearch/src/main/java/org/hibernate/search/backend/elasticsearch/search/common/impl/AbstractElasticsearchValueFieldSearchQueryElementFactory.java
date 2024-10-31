/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.common.impl;

import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;

public abstract class AbstractElasticsearchValueFieldSearchQueryElementFactory<T, F>
		implements
		SearchQueryElementFactory<T, ElasticsearchSearchIndexScope<?>, ElasticsearchSearchIndexValueFieldContext<F>> {

	@Override
	public abstract T create(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<F> field);

	@Override
	public void checkCompatibleWith(SearchQueryElementFactory<?, ?, ?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			throw QueryLog.INSTANCE.differentImplementationClassForQueryElement( getClass(), other.getClass() );
		}
	}
}

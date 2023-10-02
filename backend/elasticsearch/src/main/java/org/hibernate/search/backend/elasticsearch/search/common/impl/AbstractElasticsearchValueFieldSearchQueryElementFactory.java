/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.common.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractElasticsearchValueFieldSearchQueryElementFactory<T, F>
		implements
		SearchQueryElementFactory<T, ElasticsearchSearchIndexScope<?>, ElasticsearchSearchIndexValueFieldContext<F>> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public abstract T create(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<F> field);

	@Override
	public void checkCompatibleWith(SearchQueryElementFactory<?, ?, ?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			throw log.differentImplementationClassForQueryElement( getClass(), other.getClass() );
		}
	}
}

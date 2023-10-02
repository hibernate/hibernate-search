/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.common.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractLuceneValueFieldSearchQueryElementFactory<T, F>
		implements SearchQueryElementFactory<T, LuceneSearchIndexScope<?>, LuceneSearchIndexValueFieldContext<F>> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public abstract T create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field);

	@Override
	public void checkCompatibleWith(SearchQueryElementFactory<?, ?, ?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			throw log.differentImplementationClassForQueryElement( getClass(), other.getClass() );
		}
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;

abstract class AbstractLuceneProjection<P> implements LuceneSearchProjection<P> {

	private final Set<String> indexNames;

	AbstractLuceneProjection(AbstractBuilder<?> builder) {
		this( builder.scope );
	}

	AbstractLuceneProjection(LuceneSearchIndexScope<?> scope) {
		this.indexNames = scope.hibernateSearchIndexNames();
	}

	@Override
	public final Set<String> indexNames() {
		return indexNames;
	}

	abstract static class AbstractBuilder<P> implements SearchProjectionBuilder<P> {
		protected final LuceneSearchIndexScope<?> scope;

		AbstractBuilder(LuceneSearchIndexScope<?> scope) {
			this.scope = scope;
		}
	}

}

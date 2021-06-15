/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionIndexScope;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;

/**
 * A DSL context used when building a {@link SearchProjection} object,
 * either when calling {@link MappedIndexScope#sort()} from a search scope
 * or when calling {@link SearchQueryOptionsStep#sort(Function)} to build the sort using a lambda
 * (in which case the lambda may retrieve the resulting {@link SearchProjection} object and cache it).
 */
public final class SearchProjectionDslContextImpl<SC extends SearchProjectionIndexScope<?>>
		implements SearchProjectionDslContext<SC> {

	public static <SC extends SearchProjectionIndexScope<?>> SearchProjectionDslContext<SC> root(SC scope) {
		return new SearchProjectionDslContextImpl<>( scope );
	}

	private final SC scope;

	private SearchProjectionDslContextImpl(SC scope) {
		this.scope = scope;
	}

	@Override
	public SC scope() {
		return scope;
	}

}

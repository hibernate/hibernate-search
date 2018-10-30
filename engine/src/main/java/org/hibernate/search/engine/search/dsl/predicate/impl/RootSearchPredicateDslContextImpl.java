/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractObjectCreatingSearchPredicateContributor;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * A DSL context used when building a {@link SearchPredicate} object,
 * either when calling {@link IndexSearchTarget#predicate()} from a search target
 * or when calling {@link SearchQueryResultContext#predicate(Consumer)} to build the predicate using a lambda
 * (in which case the lambda may retrieve the resulting {@link SearchPredicate} object and cache it).
 */
public final class RootSearchPredicateDslContextImpl<B>
		extends AbstractObjectCreatingSearchPredicateContributor<B>
		implements SearchPredicateDslContext<B> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private SearchPredicateContributor<? extends B> singlePredicateContributor;

	public RootSearchPredicateDslContextImpl(SearchPredicateFactory<?, B> factory) {
		super( factory );
	}

	@Override
	public void addChild(SearchPredicateContributor<? extends B> child) {
		if ( isContributed() ) {
			throw log.cannotAddPredicateToUsedContext();
		}
		if ( this.singlePredicateContributor != null ) {
			throw log.cannotAddMultiplePredicatesToQueryRoot();
		}
		this.singlePredicateContributor = child;
	}

	public B getResultingBuilder() {
		return contribute();
	}

	@Override
	protected B doContribute() {
		return singlePredicateContributor.contribute();
	}
}

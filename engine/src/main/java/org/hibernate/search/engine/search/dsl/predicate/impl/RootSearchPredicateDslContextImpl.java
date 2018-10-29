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
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * A DSL context used when building a {@link SearchPredicate} object,
 * either when calling {@link IndexSearchTarget#predicate()} from a search target
 * or when calling {@link SearchQueryResultContext#predicate(Consumer)} to build the predicate using a lambda
 * (in which case the lambda may retrieve the resulting {@link SearchPredicate} object and cache it).
 */
public final class RootSearchPredicateDslContextImpl<B>
		implements SearchPredicateDslContext<SearchPredicate, B> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchPredicateFactory<?, B> factory;

	private SearchPredicateContributor<? extends B> singlePredicateContributor;
	private boolean usedContributor = false;
	private SearchPredicate predicateResult;

	public RootSearchPredicateDslContextImpl(SearchPredicateFactory<?, B> factory) {
		this.factory = factory;
	}

	@Override
	public void addChild(SearchPredicateContributor<? extends B> child) {
		if ( usedContributor ) {
			throw log.cannotAddPredicateToUsedContext();
		}
		if ( this.singlePredicateContributor != null ) {
			throw log.cannotAddMultiplePredicatesToQueryRoot();
		}
		this.singlePredicateContributor = child;
	}

	@Override
	public SearchPredicate getNextContext() {
		if ( predicateResult == null ) {
			if ( usedContributor ) {
				// HSEARCH-3207: we must never call a contribution twice. Contributions may have side-effects.
				throw new AssertionFailure(
						"A predicate object was requested after the corresponding information was contributed to the DSL." +
						" There is a bug in Hibernate Search, please report it."
				);
			}
			predicateResult = factory.toSearchPredicate( getResultingBuilder() );
		}
		return predicateResult;
	}

	public B getResultingBuilder() {
		if ( predicateResult != null ) {
			/*
			 * If the SearchPredicate object was already created,
			 * we can't use the builder collected by the aggregator anymore: it might be single-use.
			 * We just ask the factory to convert the SearchPredicate object back to a builder.
			 * If the builders can be used multiple times, the factory can optimize this.
			 */
			return factory.toImplementation( predicateResult );
		}
		else {
			if ( usedContributor ) {
				// HSEARCH-3207: we must never call a contribution twice. Contributions may have side-effects.
				throw new AssertionFailure(
						"A predicate contributor was called twice. There is a bug in Hibernate Search, please report it."
				);
			}
			usedContributor = true;
			/*
			 * Optimization: we know the user will not be able to request a SearchPredicate object anymore,
			 * so we don't need to build a SearchPredicate object in this case,
			 * we can just use the builder collected by the aggregator directly.
			 */
			return singlePredicateContributor.contribute();
		}
	}
}

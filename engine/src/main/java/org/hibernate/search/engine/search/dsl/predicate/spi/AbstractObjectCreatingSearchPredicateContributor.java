/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.spi;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.util.AssertionFailure;

/**
 * An abstract base for {@link SearchPredicateContributor} implementation that can also be requested
 * to create a {@link SearchPredicate} object, independently from being asked to {@link #contribute()} a builder.
 * <p>
 * This is especially useful when implementing DSL contexts that implement
 * {@link org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext}.
 *
 * @param <B> The implementation type of builders, see {@link SearchPredicateContributor}.
 */
public abstract class AbstractObjectCreatingSearchPredicateContributor<B> implements SearchPredicateContributor<B> {

	protected final SearchPredicateFactory<?, B> factory;

	private boolean contributed = false;
	private SearchPredicate predicateResult;

	public AbstractObjectCreatingSearchPredicateContributor(SearchPredicateFactory<?, B> factory) {
		this.factory = factory;
	}

	@Override
	public final B contribute() {
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
			if ( contributed ) {
				// HSEARCH-3207: we must never call a contribution twice. Contributions may have side-effects.
				throw new AssertionFailure(
						"A predicate contributor was called twice. There is a bug in Hibernate Search, please report it."
				);
			}
			contributed = true;
			/*
			 * Optimization: we know the user will not be able to request a SearchPredicate object anymore,
			 * so we don't need to build a SearchPredicate object in this case,
			 * we can just use the builder collected by the aggregator directly.
			 */
			return doContribute();
		}
	}

	public SearchPredicate toPredicate() {
		if ( predicateResult == null ) {
			if ( contributed ) {
				// HSEARCH-3207: we must never call a contribution twice. Contributions may have side-effects.
				throw new AssertionFailure(
						"A predicate object was requested after the corresponding information was contributed to the DSL." +
						" There is a bug in Hibernate Search, please report it."
				);
			}
			contributed = true;
			predicateResult = factory.toSearchPredicate( doContribute() );
		}
		return predicateResult;
	}

	protected abstract B doContribute();

	protected boolean isContributed() {
		return contributed;
	}
}

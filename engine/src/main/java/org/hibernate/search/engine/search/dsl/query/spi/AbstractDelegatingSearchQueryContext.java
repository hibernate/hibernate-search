/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.spi;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContextExtension;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;

public abstract class AbstractDelegatingSearchQueryContext<
		S extends SearchQueryContext<S, Q, SC>,
		Q,
		PC extends SearchPredicateFactoryContext,
		SC extends SearchSortContainerContext
		>
		implements SearchQueryContextImplementor<S, Q, PC, SC> {

	private final SearchQueryContextImplementor<?, Q, ?, ?> delegate;

	public AbstractDelegatingSearchQueryContext(SearchQueryContextImplementor<?, Q, ?, ?> delegate) {
		this.delegate = delegate;
	}

	@Override
	public S predicate(SearchPredicate predicate) {
		delegate.predicate( predicate );
		return thisAsS();
	}

	@Override
	public S predicate(Function<? super PC, SearchPredicateTerminalContext> predicateContributor) {
		delegate.predicate( f -> predicateContributor.apply( extendPredicateContext( f ) ) );
		return thisAsS();
	}

	@Override
	public <T> T extension(SearchQueryContextExtension<T, Q> extension) {
		return delegate.extension( extension );
	}

	@Override
	public S routing(String routingKey) {
		delegate.routing( routingKey );
		return thisAsS();
	}

	@Override
	public S routing(Collection<String> routingKeys) {
		delegate.routing( routingKeys );
		return thisAsS();
	}

	@Override
	public S sort(SearchSort sort) {
		delegate.sort( sort );
		return thisAsS();
	}

	@Override
	public S sort(Consumer<? super SC> sortContributor) {
		delegate.sort( f -> sortContributor.accept( extendSortContext( f ) ) );
		return thisAsS();
	}

	@Override
	public Q toQuery() {
		return delegate.toQuery();
	}

	protected abstract S thisAsS();

	protected abstract PC extendPredicateContext(SearchPredicateFactoryContext predicateFactoryContext);

	protected abstract SC extendSortContext(SearchSortContainerContext sortContainerContext);
}

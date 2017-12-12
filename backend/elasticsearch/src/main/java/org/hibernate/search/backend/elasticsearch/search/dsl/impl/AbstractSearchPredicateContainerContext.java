/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.impl;

import java.util.Optional;
import java.util.function.Consumer;

import org.hibernate.search.backend.elasticsearch.ElasticsearchSearchContainerContextExtension;
import org.hibernate.search.backend.elasticsearch.search.ElasticsearchSearchPredicateContainerContext;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.BooleanJunctionPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContextExtension;


/**
 * @author Yoann Rodiere
 */
abstract class AbstractSearchPredicateContainerContext<N> implements ElasticsearchSearchPredicateContainerContext<N> {

	private final SearchTargetContext targetContext;

	public AbstractSearchPredicateContainerContext(SearchTargetContext targetContext) {
		this.targetContext = targetContext;
	}

	@Override
	public BooleanJunctionPredicateContext<N> bool() {
		BooleanJunctionPredicateContextImpl<N> child = new BooleanJunctionPredicateContextImpl<>( targetContext, this::getNext );
		add( child );
		return child;
	}

	@Override
	public MatchPredicateContext<N> match() {
		MatchPredicateContextImpl<N> child = new MatchPredicateContextImpl<>( targetContext, this::getNext );
		add( child );
		return child;
	}

	@Override
	public RangePredicateContext<N> range() {
		RangePredicateContextImpl<N> child = new RangePredicateContextImpl<>( targetContext, this::getNext );
		add( child );
		return child;
	}

	@Override
	public N predicate(SearchPredicate predicate) {
		add( ElasticsearchSearchPredicate.cast( predicate ) );
		return getNext();
	}

	@Override
	public N fromJsonString(String jsonString) {
		add( new UserProvidedJsonPredicateContributor( jsonString ) );
		return getNext();
	}

	protected abstract void add(ElasticsearchSearchPredicateContributor child);

	protected abstract N getNext();

	@Override
	public <T> T withExtension(SearchPredicateContainerContextExtension<N, T> extension) {
		return extension.extendOrFail( this );
	}

	@Override
	public <T> N withExtensionOptional(
			SearchPredicateContainerContextExtension<N, T> extension, Consumer<T> clauseContributor) {
		extension.extendOptional( this ).ifPresent( clauseContributor );
		return getNext();
	}

	@Override
	public <T> N withExtensionOptional(
			SearchPredicateContainerContextExtension<N, T> extension,
			Consumer<T> clauseContributor,
			Consumer<SearchPredicateContainerContext<N>> fallbackClauseContributor) {
		Optional<T> optional = extension.extendOptional( this );
		if ( optional.isPresent() ) {
			clauseContributor.accept( optional.get() );
		}
		else {
			fallbackClauseContributor.accept( this );
		}
		return getNext();
	}

	private <T extends SearchPredicateContainerContext<N>> boolean supportsExtension(SearchPredicateContainerContextExtension<N, T> extension) {
		return extension == ElasticsearchSearchContainerContextExtension.get();
	}

}

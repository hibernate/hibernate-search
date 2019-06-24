/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.function.Consumer;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.dsl.predicate.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.dsl.predicate.ExistsPredicateFieldStep;
import org.hibernate.search.engine.search.dsl.predicate.MatchAllPredicateOptionsStep;
import org.hibernate.search.engine.search.dsl.predicate.MatchIdPredicateMatchingStep;
import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateFieldStep;
import org.hibernate.search.engine.search.dsl.predicate.NestedPredicateFieldStep;
import org.hibernate.search.engine.search.dsl.predicate.PhrasePredicateFieldStep;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateFieldStep;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContextExtension;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContextExtensionStep;
import org.hibernate.search.engine.search.dsl.predicate.PredicateFinalStep;
import org.hibernate.search.engine.search.dsl.predicate.SimpleQueryStringPredicateFieldStep;
import org.hibernate.search.engine.search.dsl.predicate.SpatialPredicateInitialStep;
import org.hibernate.search.engine.search.dsl.predicate.WildcardPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


public class DefaultSearchPredicateFactoryContext<B> implements SearchPredicateFactoryContext {

	private final SearchPredicateBuilderFactory<?, B> factory;

	public DefaultSearchPredicateFactoryContext(SearchPredicateBuilderFactory<?, B> factory) {
		this.factory = factory;
	}

	@Override
	public MatchAllPredicateOptionsStep matchAll() {
		return new MatchAllPredicateOptionsStepImpl<>( factory, this );
	}

	@Override
	public MatchIdPredicateMatchingStep id() {
		return new MatchIdPredicateMatchingStepImpl<>( factory );
	}

	@Override
	public BooleanPredicateClausesStep bool() {
		return new BooleanPredicateClausesStepImpl<>( factory, this );
	}

	@Override
	public PredicateFinalStep bool(Consumer<? super BooleanPredicateClausesStep> clauseContributor) {
		BooleanPredicateClausesStep next = bool();
		clauseContributor.accept( next );
		return next;
	}

	@Override
	public MatchPredicateFieldStep match() {
		return new MatchPredicateFieldStepImpl<>( factory );
	}

	@Override
	public RangePredicateFieldStep range() {
		return new RangePredicateFieldStepImpl<>( factory );
	}

	@Override
	public PhrasePredicateFieldStep phrase() {
		return new PhrasePredicateFieldStepImpl<>( factory );
	}

	@Override
	public WildcardPredicateFieldStep wildcard() {
		return new WildcardPredicateFieldStepImpl<>( factory );
	}

	@Override
	public NestedPredicateFieldStep nested() {
		return new NestedPredicateFieldStepImpl<>( factory, this );
	}

	@Override
	public SimpleQueryStringPredicateFieldStep simpleQueryString() {
		return new SimpleQueryStringPredicateFieldStepImpl<>( factory );
	}

	@Override
	public ExistsPredicateFieldStep exists() {
		return new ExistsPredicateFieldStepImpl<>( factory );
	}

	@Override
	public SpatialPredicateInitialStep spatial() {
		return new SpatialPredicateInitialStepImpl<>( factory );
	}

	@Override
	public <T> T extension(SearchPredicateFactoryContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, factory )
		);
	}

	@Override
	public SearchPredicateFactoryContextExtensionStep extension() {
		return new SearchPredicateFactoryContextExtensionStepImpl<>( this, factory );
	}

}

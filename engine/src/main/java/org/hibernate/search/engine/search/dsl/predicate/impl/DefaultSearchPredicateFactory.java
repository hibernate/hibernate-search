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
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryExtension;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryExtensionIfSupportedStep;
import org.hibernate.search.engine.search.dsl.predicate.PredicateFinalStep;
import org.hibernate.search.engine.search.dsl.predicate.SimpleQueryStringPredicateFieldStep;
import org.hibernate.search.engine.search.dsl.predicate.SpatialPredicateInitialStep;
import org.hibernate.search.engine.search.dsl.predicate.WildcardPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


public class DefaultSearchPredicateFactory<B> implements SearchPredicateFactory {

	private final SearchPredicateBuilderFactory<?, B> builderFactory;

	public DefaultSearchPredicateFactory(SearchPredicateBuilderFactory<?, B> builderFactory) {
		this.builderFactory = builderFactory;
	}

	@Override
	public MatchAllPredicateOptionsStep matchAll() {
		return new MatchAllPredicateOptionsStepImpl<>( builderFactory, this );
	}

	@Override
	public MatchIdPredicateMatchingStep id() {
		return new MatchIdPredicateMatchingStepImpl<>( builderFactory );
	}

	@Override
	public BooleanPredicateClausesStep bool() {
		return new BooleanPredicateClausesStepImpl<>( builderFactory, this );
	}

	@Override
	public PredicateFinalStep bool(Consumer<? super BooleanPredicateClausesStep> clauseContributor) {
		BooleanPredicateClausesStep next = bool();
		clauseContributor.accept( next );
		return next;
	}

	@Override
	public MatchPredicateFieldStep match() {
		return new MatchPredicateFieldStepImpl<>( builderFactory );
	}

	@Override
	public RangePredicateFieldStep range() {
		return new RangePredicateFieldStepImpl<>( builderFactory );
	}

	@Override
	public PhrasePredicateFieldStep phrase() {
		return new PhrasePredicateFieldStepImpl<>( builderFactory );
	}

	@Override
	public WildcardPredicateFieldStep wildcard() {
		return new WildcardPredicateFieldStepImpl<>( builderFactory );
	}

	@Override
	public NestedPredicateFieldStep nested() {
		return new NestedPredicateFieldStepImpl<>( builderFactory, this );
	}

	@Override
	public SimpleQueryStringPredicateFieldStep simpleQueryString() {
		return new SimpleQueryStringPredicateFieldStepImpl<>( builderFactory );
	}

	@Override
	public ExistsPredicateFieldStep exists() {
		return new ExistsPredicateFieldStepImpl<>( builderFactory );
	}

	@Override
	public SpatialPredicateInitialStep spatial() {
		return new SpatialPredicateInitialStepImpl<>( builderFactory );
	}

	@Override
	public <T> T extension(SearchPredicateFactoryExtension<T> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, builderFactory )
		);
	}

	@Override
	public SearchPredicateFactoryExtensionIfSupportedStep extension() {
		return new SearchPredicateFactoryExtensionStep<>( this, builderFactory );
	}

}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.function.Consumer;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.ExistsPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchAllPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchIdPredicateMatchingStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.NestedPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.PhrasePredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtension;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtensionIfSupportedStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryStringPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.SpatialPredicateInitialStep;
import org.hibernate.search.engine.search.predicate.dsl.WildcardPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;


public class DefaultSearchPredicateFactory implements SearchPredicateFactory {

	private final SearchPredicateDslContext<?> dslContext;

	public DefaultSearchPredicateFactory(SearchPredicateDslContext<?> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public MatchAllPredicateOptionsStep<?> matchAll() {
		return new MatchAllPredicateOptionsStepImpl( dslContext, this );
	}

	@Override
	public MatchIdPredicateMatchingStep<?> id() {
		return new MatchIdPredicateMatchingStepImpl( dslContext );
	}

	@Override
	public BooleanPredicateClausesStep<?> bool() {
		return new BooleanPredicateClausesStepImpl( dslContext, this );
	}

	@Override
	public PredicateFinalStep bool(Consumer<? super BooleanPredicateClausesStep<?>> clauseContributor) {
		BooleanPredicateClausesStep<?> next = bool();
		clauseContributor.accept( next );
		return next;
	}

	@Override
	public MatchPredicateFieldStep<?> match() {
		return new MatchPredicateFieldStepImpl( dslContext );
	}

	@Override
	public RangePredicateFieldStep<?> range() {
		return new RangePredicateFieldStepImpl( dslContext );
	}

	@Override
	public PhrasePredicateFieldStep<?> phrase() {
		return new PhrasePredicateFieldStepImpl( dslContext );
	}

	@Override
	public WildcardPredicateFieldStep<?> wildcard() {
		return new WildcardPredicateFieldStepImpl( dslContext );
	}

	@Override
	public NestedPredicateFieldStep<?> nested() {
		return new NestedPredicateFieldStepImpl( dslContext, this );
	}

	@Override
	public SimpleQueryStringPredicateFieldStep<?> simpleQueryString() {
		return new SimpleQueryStringPredicateFieldStepImpl( dslContext );
	}

	@Override
	public ExistsPredicateFieldStep<?> exists() {
		return new ExistsPredicateFieldStepImpl( dslContext );
	}

	@Override
	public SpatialPredicateInitialStep spatial() {
		return new SpatialPredicateInitialStepImpl( dslContext );
	}

	@Override
	public <T> T extension(SearchPredicateFactoryExtension<T> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, dslContext )
		);
	}

	@Override
	public SearchPredicateFactoryExtensionIfSupportedStep extension() {
		return new SearchPredicateFactoryExtensionStep( this, dslContext );
	}

}

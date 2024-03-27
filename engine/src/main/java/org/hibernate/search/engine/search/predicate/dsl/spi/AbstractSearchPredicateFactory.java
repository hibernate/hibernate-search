/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.spi;

import static org.hibernate.search.engine.search.predicate.dsl.impl.AbstractSimpleBooleanPredicateClausesStep.SimpleBooleanPredicateOperator.AND;
import static org.hibernate.search.engine.search.predicate.dsl.impl.AbstractSimpleBooleanPredicateClausesStep.SimpleBooleanPredicateOperator.OR;

import java.util.function.Consumer;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.ExistsPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.ExtendedSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchAllPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchIdPredicateMatchingStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchNonePredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.NamedPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.NestedPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.NotPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.PhrasePredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.QueryStringPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.RegexpPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtension;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtensionIfSupportedStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryStringPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.SpatialPredicateInitialStep;
import org.hibernate.search.engine.search.predicate.dsl.TermsPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.WildcardPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.impl.BooleanPredicateClausesStepImpl;
import org.hibernate.search.engine.search.predicate.dsl.impl.ExistsPredicateFieldStepImpl;
import org.hibernate.search.engine.search.predicate.dsl.impl.MatchAllPredicateOptionsStepImpl;
import org.hibernate.search.engine.search.predicate.dsl.impl.MatchIdPredicateMatchingStepImpl;
import org.hibernate.search.engine.search.predicate.dsl.impl.MatchNonePredicateFinalStepImpl;
import org.hibernate.search.engine.search.predicate.dsl.impl.MatchPredicateFieldStepImpl;
import org.hibernate.search.engine.search.predicate.dsl.impl.NamedPredicateOptionsStepImpl;
import org.hibernate.search.engine.search.predicate.dsl.impl.NestedPredicateClausesStepImpl;
import org.hibernate.search.engine.search.predicate.dsl.impl.NotPredicateFinalStepImpl;
import org.hibernate.search.engine.search.predicate.dsl.impl.PhrasePredicateFieldStepImpl;
import org.hibernate.search.engine.search.predicate.dsl.impl.QueryStringPredicateFieldStepImpl;
import org.hibernate.search.engine.search.predicate.dsl.impl.RangePredicateFieldStepImpl;
import org.hibernate.search.engine.search.predicate.dsl.impl.RegexpPredicateFieldStepImpl;
import org.hibernate.search.engine.search.predicate.dsl.impl.SearchPredicateFactoryExtensionStep;
import org.hibernate.search.engine.search.predicate.dsl.impl.SimpleBooleanPredicateClausesStepImpl;
import org.hibernate.search.engine.search.predicate.dsl.impl.SimpleQueryStringPredicateFieldStepImpl;
import org.hibernate.search.engine.search.predicate.dsl.impl.SpatialPredicateInitialStepImpl;
import org.hibernate.search.engine.search.predicate.dsl.impl.TermsPredicateFieldStepImpl;
import org.hibernate.search.engine.search.predicate.dsl.impl.WildcardPredicateFieldStepImpl;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateIndexScope;
import org.hibernate.search.util.common.impl.Contracts;

public abstract class AbstractSearchPredicateFactory<
		S extends ExtendedSearchPredicateFactory<S>,
		SC extends SearchPredicateIndexScope<?>>
		implements ExtendedSearchPredicateFactory<S> {

	protected final SearchPredicateDslContext<SC> dslContext;

	public AbstractSearchPredicateFactory(SearchPredicateDslContext<SC> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public MatchAllPredicateOptionsStep<?> matchAll() {
		return new MatchAllPredicateOptionsStepImpl( dslContext, this );
	}

	@Override
	public MatchNonePredicateFinalStep matchNone() {
		return new MatchNonePredicateFinalStepImpl( dslContext );
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
	public SimpleBooleanPredicateClausesStep<?> and() {
		return new SimpleBooleanPredicateClausesStepImpl( AND, dslContext, this );
	}

	@Override
	public SimpleBooleanPredicateOptionsStep<?> and(
			SearchPredicate firstSearchPredicate,
			SearchPredicate... otherSearchPredicates) {
		return new SimpleBooleanPredicateClausesStepImpl( AND, dslContext, this, firstSearchPredicate, otherSearchPredicates );
	}

	@Override
	public SimpleBooleanPredicateOptionsStep<?> and(PredicateFinalStep firstSearchPredicate,
			PredicateFinalStep... otherSearchPredicate) {
		return new SimpleBooleanPredicateClausesStepImpl( AND, dslContext, this, firstSearchPredicate, otherSearchPredicate );
	}

	@Override
	public SimpleBooleanPredicateClausesStep<?> or() {
		return new SimpleBooleanPredicateClausesStepImpl( OR, dslContext, this );
	}

	@Override
	public SimpleBooleanPredicateOptionsStep<?> or(SearchPredicate firstSearchPredicate,
			SearchPredicate... otherSearchPredicate) {
		return new SimpleBooleanPredicateClausesStepImpl( OR, dslContext, this, firstSearchPredicate, otherSearchPredicate );
	}

	@Override
	public SimpleBooleanPredicateOptionsStep<?> or(PredicateFinalStep firstSearchPredicate,
			PredicateFinalStep... otherSearchPredicate) {
		return new SimpleBooleanPredicateClausesStepImpl( OR, dslContext, this, firstSearchPredicate, otherSearchPredicate );
	}

	@Override
	public NotPredicateFinalStep not(SearchPredicate searchPredicate) {
		return new NotPredicateFinalStepImpl( dslContext, searchPredicate );
	}

	@Override
	public NotPredicateFinalStep not(PredicateFinalStep searchPredicate) {
		return new NotPredicateFinalStepImpl( dslContext, searchPredicate );
	}

	@Override
	@SuppressWarnings("deprecation") // javac warns about this method being deprecated, but we have to implement it
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
	public RegexpPredicateFieldStep<?> regexp() {
		return new RegexpPredicateFieldStepImpl( dslContext );
	}

	@Override
	public TermsPredicateFieldStep<?> terms() {
		return new TermsPredicateFieldStepImpl( dslContext );
	}

	@Override
	@Deprecated
	public org.hibernate.search.engine.search.predicate.dsl.NestedPredicateFieldStep<?> nested() {
		return new org.hibernate.search.engine.search.predicate.dsl.impl.NestedPredicateFieldStepImpl( dslContext, this );
	}

	@Override
	public NestedPredicateClausesStep<?> nested(String objectFieldPath) {
		return new NestedPredicateClausesStepImpl( dslContext, objectFieldPath, this );
	}

	@Override
	public SimpleQueryStringPredicateFieldStep<?> simpleQueryString() {
		return new SimpleQueryStringPredicateFieldStepImpl( dslContext );
	}

	@Override
	public QueryStringPredicateFieldStep<?> queryString() {
		return new QueryStringPredicateFieldStepImpl( dslContext );
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
	public NamedPredicateOptionsStep named(String path) {
		Contracts.assertNotNull( path, "path" );
		String fieldPath;
		String predicateName;
		int dotIndex = path.lastIndexOf( FieldPaths.PATH_SEPARATOR );
		if ( dotIndex >= 0 ) {
			fieldPath = path.substring( 0, dotIndex );
			predicateName = path.substring( dotIndex + 1 );
		}
		else {
			fieldPath = null;
			predicateName = path;
		}
		return new NamedPredicateOptionsStepImpl( this, dslContext, fieldPath, predicateName );
	}

	@Override
	public KnnPredicateFieldStep knn(int k) {
		Contracts.assertStrictlyPositive( k, "k" );
		return new KnnPredicateFieldStepImpl( this, dslContext, k );
	}

	@Override
	public <T> T extension(SearchPredicateFactoryExtension<T> extension) {
		return DslExtensionState.returnIfSupported( extension, extension.extendOptional( this ) );
	}

	@Override
	public SearchPredicateFactoryExtensionIfSupportedStep extension() {
		return new SearchPredicateFactoryExtensionStep( this );
	}

	@Override
	public final String toAbsolutePath(String relativeFieldPath) {
		return dslContext.scope().toAbsolutePath( relativeFieldPath );
	}
}

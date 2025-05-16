/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.spi;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.common.NonStaticMetamodelScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.ExistsPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchAllPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchIdPredicateMatchingStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchNonePredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.NamedPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.NestedPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.NestedPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.NotPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.PhrasePredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.PrefixPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.QueryStringPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.RegexpPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtension;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtensionIfSupportedStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryStringPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.SpatialPredicateInitialStep;
import org.hibernate.search.engine.search.predicate.dsl.TermsPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.WildcardPredicateFieldStep;

@SuppressWarnings({ "deprecation", "removal" })
public record SearchPredicateFactoryDelegate(TypedSearchPredicateFactory<NonStaticMetamodelScope> delegate)
		implements SearchPredicateFactory {

	@Override
	public MatchAllPredicateOptionsStep<NonStaticMetamodelScope, ?> matchAll() {
		return delegate.matchAll();
	}

	@Override
	public MatchNonePredicateFinalStep matchNone() {
		return delegate.matchNone();
	}

	@Override
	public MatchIdPredicateMatchingStep<?> id() {
		return delegate.id();
	}

	@Override
	public BooleanPredicateClausesStep<NonStaticMetamodelScope, ?> bool() {
		return delegate.bool();
	}

	@Override
	public PredicateFinalStep bool(
			Consumer<? super BooleanPredicateClausesStep<NonStaticMetamodelScope, ?>> clauseContributor) {
		return delegate.bool( clauseContributor );
	}

	@Override
	public SimpleBooleanPredicateClausesStep<NonStaticMetamodelScope, ?> and() {
		return delegate.and();
	}

	@Override
	public SimpleBooleanPredicateOptionsStep<?> and(SearchPredicate firstSearchPredicate,
			SearchPredicate... otherSearchPredicates) {
		return delegate.and( firstSearchPredicate, otherSearchPredicates );
	}

	@Override
	public SimpleBooleanPredicateOptionsStep<?> and(PredicateFinalStep firstSearchPredicate,
			PredicateFinalStep... otherSearchPredicates) {
		return delegate.and( firstSearchPredicate, otherSearchPredicates );
	}

	@Override
	public SimpleBooleanPredicateClausesStep<NonStaticMetamodelScope, ?> or() {
		return delegate.or();
	}

	@Override
	public SimpleBooleanPredicateOptionsStep<?> or(SearchPredicate firstSearchPredicate,
			SearchPredicate... otherSearchPredicates) {
		return delegate.or( firstSearchPredicate, otherSearchPredicates );
	}

	@Override
	public SimpleBooleanPredicateOptionsStep<?> or(PredicateFinalStep firstSearchPredicate,
			PredicateFinalStep... otherSearchPredicates) {
		return delegate.or( firstSearchPredicate, otherSearchPredicates );
	}

	@Override
	public NotPredicateFinalStep not(SearchPredicate searchPredicate) {
		return delegate.not( searchPredicate );
	}

	@Override
	public NotPredicateFinalStep not(PredicateFinalStep searchPredicate) {
		return delegate.not( searchPredicate );
	}

	@Override
	public MatchPredicateFieldStep<NonStaticMetamodelScope, ?> match() {
		return delegate.match();
	}

	@Override
	public RangePredicateFieldStep<NonStaticMetamodelScope, ?> range() {
		return delegate.range();
	}

	@Override
	public PhrasePredicateFieldStep<NonStaticMetamodelScope, ?> phrase() {
		return delegate.phrase();
	}

	@Override
	public WildcardPredicateFieldStep<NonStaticMetamodelScope, ?> wildcard() {
		return delegate.wildcard();
	}

	@Override
	public PrefixPredicateFieldStep<NonStaticMetamodelScope, ?> prefix() {
		return delegate.prefix();
	}

	@Override
	public RegexpPredicateFieldStep<NonStaticMetamodelScope, ?> regexp() {
		return delegate.regexp();
	}

	@Override
	public TermsPredicateFieldStep<NonStaticMetamodelScope, ?> terms() {
		return delegate.terms();
	}

	@Override
	public NestedPredicateFieldStep<NonStaticMetamodelScope, ?> nested() {
		return delegate.nested();
	}

	@Override
	public NestedPredicateClausesStep<NonStaticMetamodelScope, ?> nested(String objectFieldPath) {
		return delegate.nested( objectFieldPath );
	}

	@Override
	public SimpleQueryStringPredicateFieldStep<NonStaticMetamodelScope, ?> simpleQueryString() {
		return delegate.simpleQueryString();
	}

	@Override
	public QueryStringPredicateFieldStep<NonStaticMetamodelScope, ?> queryString() {
		return delegate.queryString();
	}

	@Override
	public ExistsPredicateFieldStep<NonStaticMetamodelScope, ?> exists() {
		return delegate.exists();
	}

	@Override
	public SpatialPredicateInitialStep<NonStaticMetamodelScope> spatial() {
		return delegate.spatial();
	}

	@Override
	public NamedPredicateOptionsStep named(String path) {
		return delegate.named( path );
	}

	@Override
	public KnnPredicateFieldStep<NonStaticMetamodelScope> knn(int k) {
		return delegate.knn( k );
	}

	@Override
	public PredicateFinalStep withParameters(Function<? super NamedValues, ? extends PredicateFinalStep> predicateCreator) {
		return delegate.withParameters( predicateCreator );
	}

	@Override
	public <T> T extension(SearchPredicateFactoryExtension<NonStaticMetamodelScope, T> extension) {
		return delegate.extension( extension );
	}

	@Override
	public SearchPredicateFactoryExtensionIfSupportedStep<NonStaticMetamodelScope> extension() {
		return delegate.extension();
	}

	@Override
	public SearchPredicateFactory withRoot(String objectFieldPath) {
		return new SearchPredicateFactoryDelegate( delegate.withRoot( objectFieldPath ) );
	}

	@Override
	public String toAbsolutePath(String relativeFieldPath) {
		return delegate.toAbsolutePath( relativeFieldPath );
	}
}

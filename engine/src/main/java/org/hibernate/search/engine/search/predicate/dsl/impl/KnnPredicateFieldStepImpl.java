/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateVectorGenericStep;
import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateVectorStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.KnnPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.reference.predicate.KnnPredicateFieldReference;

public class KnnPredicateFieldStepImpl<SR>
		extends AbstractPredicateFinalStep
		implements KnnPredicateFieldStep<SR>, KnnPredicateVectorStep<SR>, KnnPredicateOptionsStep<SR> {

	private final SearchPredicateFactory<SR> factory;
	private final int k;
	private BooleanPredicateBuilder booleanBuilder;
	protected KnnPredicateBuilder builder;

	public KnnPredicateFieldStepImpl(SearchPredicateFactory<SR> factory, SearchPredicateDslContext<?> dslContext, int k) {
		super( dslContext );
		this.factory = factory;
		this.k = k;
	}

	@Override
	public KnnPredicateVectorStep<SR> field(String fieldPath) {
		this.builder = dslContext.scope().fieldQueryElement( fieldPath, PredicateTypeKeys.KNN );
		this.builder.k( k );
		return this;
	}

	@Override
	public <T> KnnPredicateVectorGenericStep<SR, T> field(KnnPredicateFieldReference<SR, T> field) {
		this.field( field.absolutePath() );
		return new KnnPredicateVectorGenericStepImpl<>();
	}

	@Override
	public KnnPredicateOptionsStep<SR> filter(SearchPredicate searchPredicate) {
		this.booleanPredicateBuilder().must( searchPredicate );
		return this;
	}

	@Override
	public KnnPredicateOptionsStep<SR> filter(
			Function<? super SearchPredicateFactory<SR>, ? extends PredicateFinalStep> clauseContributor) {
		this.booleanPredicateBuilder().must( clauseContributor.apply( factory ).toPredicate() );
		return this;
	}

	@Override
	public KnnPredicateOptionsStep<SR> matching(byte... vector) {
		this.builder.vector( vector );
		return this;
	}

	@Override
	public KnnPredicateOptionsStep<SR> matching(float... vector) {
		this.builder.vector( vector );
		return this;
	}

	@Override
	public KnnPredicateOptionsStep<SR> requiredMinimumSimilarity(float similarity) {
		this.builder.requiredMinimumSimilarity( similarity );
		return this;
	}

	@Override
	public KnnPredicateOptionsStep<SR> requiredMinimumScore(float score) {
		this.builder.requiredMinimumScore( score );
		return this;
	}

	@Override
	public KnnPredicateOptionsStep<SR> boost(float boost) {
		this.builder.boost( boost );
		return this;
	}

	@Override
	public KnnPredicateOptionsStep<SR> constantScore() {
		this.builder.constantScore();
		return this;
	}

	@Override
	protected SearchPredicate build() {
		if ( this.booleanBuilder != null ) {
			builder.filter( booleanBuilder.build() );
		}
		return builder.build();
	}

	private BooleanPredicateBuilder booleanPredicateBuilder() {
		if ( this.booleanBuilder == null ) {
			this.booleanBuilder = dslContext.scope().predicateBuilders().bool();
		}
		return this.booleanBuilder;
	}

	private class KnnPredicateVectorGenericStepImpl<T> implements KnnPredicateVectorGenericStep<SR, T> {

		@Override
		public KnnPredicateOptionsStep<SR> matching(T vector) {
			KnnPredicateFieldStepImpl.this.builder.vector( vector );
			return KnnPredicateFieldStepImpl.this;
		}
	}

}

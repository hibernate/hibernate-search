/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.spi;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateVectorStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.KnnPredicateBuilder;

/**
 * An abstract base for knn predicate steps implementations ({@link KnnPredicateOptionsStep}/{@link KnnPredicateVectorStep}/{@link KnnPredicateFieldStep}).
 */
public abstract class AbstractKnnPredicateFieldStep<T extends KnnPredicateOptionsStep<?>, B extends KnnPredicateBuilder>
		extends AbstractPredicateFinalStep
		implements KnnPredicateFieldStep<T>, KnnPredicateVectorStep<T>,
		KnnPredicateOptionsStep<T> {

	private final SearchPredicateFactory factory;
	private final int k;
	private BooleanPredicateBuilder booleanBuilder;
	protected B builder;

	public AbstractKnnPredicateFieldStep(SearchPredicateFactory factory, SearchPredicateDslContext<?> dslContext, int k) {
		super( dslContext );
		this.factory = factory;
		this.k = k;
	}

	@Override
	public KnnPredicateVectorStep<T> field(String fieldPath) {
		this.builder = createBuilder( fieldPath );
		this.builder.k( k );
		return this;
	}

	protected abstract B createBuilder(String fieldPath);

	@Override
	protected SearchPredicate build() {
		if ( this.booleanBuilder != null ) {
			builder.filter( booleanBuilder.build() );
		}
		return builder.build();
	}

	@Override
	public T filter(SearchPredicate searchPredicate) {
		this.booleanPredicateBuilder().must( searchPredicate );
		return thisAsT();
	}

	@Override
	public T filter(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor) {
		this.booleanPredicateBuilder().must( clauseContributor.apply( factory ).toPredicate() );
		return thisAsT();
	}

	@Override
	public T matching(byte... vector) {
		this.builder.vector( vector );
		return thisAsT();
	}

	@Override
	public T matching(float... vector) {
		this.builder.vector( vector );
		return thisAsT();
	}

	@Override
	public T boost(float boost) {
		this.builder.boost( boost );
		return thisAsT();
	}

	@Override
	public T constantScore() {
		this.builder.constantScore();
		return thisAsT();
	}

	protected abstract T thisAsT();

	private BooleanPredicateBuilder booleanPredicateBuilder() {
		if ( this.booleanBuilder == null ) {
			this.booleanBuilder = dslContext.scope().predicateBuilders().bool();
		}
		return this.booleanBuilder;
	}

}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;

import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;


public abstract class AbstractLuceneSearchPredicateBuilder implements SearchPredicateBuilder<LuceneSearchPredicateBuilder>,
		LuceneSearchPredicateBuilder {

	private Float boost;
	private boolean constantScore;

	@Override
	public void boost(float boost) {
		this.boost = boost;
	}

	@Override
	public void constantScore() {
		this.constantScore = true;
	}

	@Override
	public LuceneSearchPredicateBuilder toImplementation() {
		return this;
	}

	@Override
	public Query build(LuceneSearchPredicateContext context) {
		Query query = doBuild( context );

		// the boost should be applied on top of the constant score,
		// otherwise the boost will simply be ignored
		if ( constantScore ) {
			query = new ConstantScoreQuery( query );
		}
		if ( boost != null ) {
			query = new BoostQuery( query, boost );
		}

		return query;
	}

	protected abstract Query doBuild(LuceneSearchPredicateContext context);

	protected Query applyImplicitNestedSteps(List<String> furtherImplicitNestedSteps, LuceneSearchPredicateContext context,
			Function<LuceneSearchPredicateContext, Query> baseBuild) {
		if ( furtherImplicitNestedSteps.isEmpty() ) {
			return baseBuild.apply( context );
		}

		if ( furtherImplicitNestedSteps.size() == 1 ) {
			String lastStep = furtherImplicitNestedSteps.get( 0 );
			// baseBuild.apply( context ) must be called only at the very end of the recursion.
			// Note that there is no reason here to update the context, because we already reached the target field.
			return LuceneNestedPredicateBuilder.doBuild( context, lastStep, baseBuild.apply( context ) );
		}

		String step = furtherImplicitNestedSteps.remove( 0 );
		LuceneSearchPredicateContext childContext = new LuceneSearchPredicateContext( step );
		return LuceneNestedPredicateBuilder.doBuild( context, step, applyImplicitNestedSteps( furtherImplicitNestedSteps, childContext, baseBuild ) );
	}
}

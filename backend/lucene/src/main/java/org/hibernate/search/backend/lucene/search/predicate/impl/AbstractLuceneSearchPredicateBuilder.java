/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;

import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;



public abstract class AbstractLuceneSearchPredicateBuilder implements SearchPredicateBuilder<LuceneSearchPredicateBuilder>,
		LuceneSearchPredicateBuilder {

	private Float boost;
	private boolean withConstantScore;

	@Override
	public void boost(float boost) {
		this.boost = boost;
	}

	@Override
	public void withConstantScore() {
		this.withConstantScore = true;
	}

	@Override
	public LuceneSearchPredicateBuilder toImplementation() {
		return this;
	}

	@Override
	public final Query build(LuceneSearchPredicateContext context) {
		Query query = doBuild( context );

		// the boost should be applied on top of the constant score,
		// otherwise the boost will simply be ignored
		if ( withConstantScore ) {
			query = new ConstantScoreQuery( query );
		}
		if ( boost != null ) {
			query = new BoostQuery( query, boost );
		}

		return query;
	}

	protected abstract Query doBuild(LuceneSearchPredicateContext context);
}

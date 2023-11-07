/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;

import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;

public abstract class AbstractLuceneSearchPredicate implements LuceneSearchPredicate {

	protected final Set<String> indexNames;
	// NOTE: below modifiers (boost, constant score) are used to implement hasNoModifiers() that other predicates
	// rely on and might build on to include additional predicate-specific modifiers LuceneBooleanPredicate in particular.
	// IMPORTANT: Review where current modifiers are used and how the new modifier affects that logic, when adding a new modifier.
	private final Float boost;
	private final boolean constantScore;

	protected AbstractLuceneSearchPredicate(AbstractBuilder builder) {
		indexNames = builder.scope.hibernateSearchIndexNames();
		boost = builder.boost;
		constantScore = builder.constantScore;
	}

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	@Override
	public Query toQuery(PredicateRequestContext context) {
		Query query = doToQuery( context );

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

	protected abstract Query doToQuery(PredicateRequestContext context);

	protected boolean hasNoModifiers() {
		return !constantScore && boost == null;
	}

	public abstract static class AbstractBuilder implements SearchPredicateBuilder {
		protected final LuceneSearchIndexScope<?> scope;

		// NOTE: below modifiers (boost, constant score) are used to implement hasNoModifiers() that other predicates
		// rely on and might build on to include additional predicate-specific modifiers ElasticsearchBooleanPredicate in particular.
		// IMPORTANT: Review where current modifiers are used and how the new modifier affects that logic, when adding a new modifier.
		private Float boost;
		private boolean constantScore;

		protected AbstractBuilder(LuceneSearchIndexScope<?> scope) {
			this.scope = scope;
		}

		@Override
		public void boost(float boost) {
			this.boost = boost;
		}

		@Override
		public void constantScore() {
			this.constantScore = true;
		}

		protected boolean hasNoModifiers() {
			return !constantScore && boost == null;
		}
	}
}

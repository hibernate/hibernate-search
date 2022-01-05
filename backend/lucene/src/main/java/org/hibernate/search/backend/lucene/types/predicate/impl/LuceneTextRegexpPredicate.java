/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.RegexpPredicateBuilder;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;

public class LuceneTextRegexpPredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private LuceneTextRegexpPredicate(Builder<?> builder) {
		super( builder );
	}

	public static class Factory<F>
			extends AbstractLuceneValueFieldSearchQueryElementFactory<RegexpPredicateBuilder, F> {
		@Override
		public Builder<F> create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			return new Builder<>( scope, field );
		}
	}

	private static class Builder<F> extends AbstractBuilder<F> implements RegexpPredicateBuilder {

		private String pattern;

		private Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			super( scope, field );
		}

		@Override
		public void pattern(String regexpPattern) {
			this.pattern = regexpPattern;
		}

		@Override
		public SearchPredicate build() {
			return new LuceneTextRegexpPredicate( this );
		}

		@Override
		protected Query buildQuery() {
			// set no optional flag as default
			return new RegexpQuery( new Term( absoluteFieldPath, pattern ), 0 );
		}
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;

import org.apache.lucene.search.Query;

public class LuceneExistsPredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private LuceneExistsPredicate(Builder builder) {
		super( builder );
	}

	public static class Builder<F> extends AbstractBuilder<F> implements ExistsPredicateBuilder {
		private final LuceneFieldCodec<F> codec;

		Builder(LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field,
				LuceneFieldCodec<F> codec) {
			super( searchContext, field );
			this.codec = codec;
			// Score is always constant for this query
			constantScore();
		}

		@Override
		public SearchPredicate build() {
			return new LuceneExistsPredicate( this );
		}

		@Override
		protected Query buildQuery() {
			return codec.createExistsQuery( absoluteFieldPath );
		}
	}
}

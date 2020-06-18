/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneStandardMatchPredicate;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.engine.search.predicate.SearchPredicate;

import org.apache.lucene.search.Query;

class LuceneNumericMatchPredicate extends AbstractLuceneStandardMatchPredicate {

	private LuceneNumericMatchPredicate(Builder builder) {
		super( builder );
	}

	static class Builder<F, E extends Number> extends AbstractBuilder<F, E, AbstractLuceneNumericFieldCodec<F, E>> {
		Builder(LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field,
				AbstractLuceneNumericFieldCodec<F, E> codec) {
			super( searchContext, field, codec );
		}

		@Override
		public SearchPredicate build() {
			return new LuceneNumericMatchPredicate( this );
		}

		@Override
		protected Query buildQuery() {
			return codec.getDomain().createExactQuery( absoluteFieldPath, value );
		}
	}
}

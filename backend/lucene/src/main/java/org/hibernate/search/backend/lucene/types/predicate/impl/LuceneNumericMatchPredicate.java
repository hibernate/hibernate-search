/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Query;

class LuceneNumericMatchPredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private LuceneNumericMatchPredicate(Builder builder) {
		super( builder );
	}

	static class Builder<F, E extends Number> extends AbstractBuilder<F> implements MatchPredicateBuilder {
		private final AbstractLuceneNumericFieldCodec<F, E> codec;

		private E value;

		Builder(LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field,
				AbstractLuceneNumericFieldCodec<F, E> codec) {
			super( searchContext, field );
			this.codec = codec;
		}

		@Override
		public void value(Object value, ValueConvert convert) {
			this.value = convertAndEncode( codec, value, convert );
		}

		@Override
		public void fuzzy(int maxEditDistance, int exactPrefixLength) {
			throw log.textPredicatesNotSupportedByFieldType( field.eventContext() );
		}

		@Override
		public void analyzer(String analyzerName) {
			throw log.textPredicatesNotSupportedByFieldType( field.eventContext() );
		}

		@Override
		public void skipAnalysis() {
			throw log.textPredicatesNotSupportedByFieldType( field.eventContext() );
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

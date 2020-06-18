/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStandardFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Query;

public abstract class AbstractLuceneStandardRangePredicate extends AbstractLuceneSingleFieldPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Query query;

	protected AbstractLuceneStandardRangePredicate(AbstractBuilder<?, ?, ?> builder) {
		super( builder );
		query = builder.buildQuery();
	}

	@Override
	protected final Query doToQuery(PredicateRequestContext context) {
		return query;
	}

	/**
	 * @param <F> The field type exposed to the mapper.
	 * @param <E> The encoded type.
	 * @param <C> The codec type.
	 * @see LuceneStandardFieldCodec
	 */
	public abstract static class AbstractBuilder<F, E, C extends LuceneStandardFieldCodec<F, E>>
			extends AbstractLuceneSingleFieldPredicate.AbstractBuilder
			implements RangePredicateBuilder {
		private final LuceneSearchFieldContext<F> field;
		protected final C codec;

		protected Range<E> range;

		protected AbstractBuilder(LuceneSearchContext searchContext,
				LuceneSearchFieldContext<F> field, C codec) {
			super( searchContext, field );
			this.field = field;
			this.codec = codec;
		}

		@Override
		public void range(Range<?> range, ValueConvert convertLowerBound, ValueConvert convertUpperBound) {
			this.range = Range.between(
					convertAndEncode( range.lowerBoundValue(), convertLowerBound ),
					range.lowerBoundInclusion(),
					convertAndEncode( range.upperBoundValue(), convertUpperBound ),
					range.upperBoundInclusion()
			);
		}

		protected abstract Query buildQuery();

		private E convertAndEncode(Optional<?> valueOptional, ValueConvert convert) {
			if ( !valueOptional.isPresent() ) {
				return null;
			}
			Object value = valueOptional.get();
			DslConverter<?, ? extends F> toFieldValueConverter = field.type().dslConverter( convert );
			try {
				F converted = toFieldValueConverter.convertUnknown(
						value, searchContext.toDocumentFieldValueConvertContext()
				);
				return codec.encode( converted );
			}
			catch (RuntimeException e) {
				throw log.cannotConvertDslParameter( e.getMessage(), e, field.eventContext() );
			}
		}
	}
}

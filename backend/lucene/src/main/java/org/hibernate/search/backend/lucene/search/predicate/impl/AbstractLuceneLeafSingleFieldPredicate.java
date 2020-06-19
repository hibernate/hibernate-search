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
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Query;

public abstract class AbstractLuceneLeafSingleFieldPredicate extends AbstractLuceneSingleFieldPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Query query;

	protected AbstractLuceneLeafSingleFieldPredicate(AbstractBuilder<?> builder) {
		super( builder );
		query = builder.buildQuery();
	}

	@Override
	protected final Query doToQuery(PredicateRequestContext context) {
		return query;
	}

	public abstract static class AbstractBuilder<F>
			extends AbstractLuceneSingleFieldPredicate.AbstractBuilder {
		protected final LuceneSearchFieldContext<F> field;

		protected AbstractBuilder(LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field) {
			super( searchContext, field );
			this.field = field;
		}

		protected abstract Query buildQuery();

		protected <E> E convertAndEncode(LuceneStandardFieldCodec<F, E> codec, Object value, ValueConvert convert) {
			DslConverter<?, ? extends F> toFieldValueConverter = field.type().dslConverter( convert );
			try {
				F converted = toFieldValueConverter.convertUnknown( value,
						searchContext.toDocumentFieldValueConvertContext() );
				return codec.encode( converted );
			}
			catch (RuntimeException e) {
				throw log.cannotConvertDslParameter( e.getMessage(), e, field.eventContext() );
			}
		}

		protected <E> Range<E> convertAndEncode(LuceneStandardFieldCodec<F, E> codec, Range<?> range,
				ValueConvert convertLowerBound,
				ValueConvert convertUpperBound) {
			return Range.between(
					convertAndEncode( codec, range.lowerBoundValue(), convertLowerBound ),
					range.lowerBoundInclusion(),
					convertAndEncode( codec, range.upperBoundValue(), convertUpperBound ),
					range.upperBoundInclusion()
			);
		}

		private <E> E convertAndEncode(LuceneStandardFieldCodec<F, E> codec, Optional<?> valueOptional,
				ValueConvert convert) {
			if ( !valueOptional.isPresent() ) {
				return null;
			}
			else {
				return convertAndEncode( codec, valueOptional.get(), convert );
			}
		}
	}
}

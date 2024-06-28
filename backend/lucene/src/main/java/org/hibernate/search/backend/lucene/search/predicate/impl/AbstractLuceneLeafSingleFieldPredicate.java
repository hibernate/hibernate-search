/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStandardFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Query;

public abstract class AbstractLuceneLeafSingleFieldPredicate extends AbstractLuceneSingleFieldPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final AbstractBuilder<?> builder;

	protected AbstractLuceneLeafSingleFieldPredicate(AbstractBuilder<?> builder) {
		super( builder );
		this.builder = builder;
	}

	@Override
	protected final Query doToQuery(PredicateRequestContext context) {
		return builder.buildQuery( context );
	}

	public abstract static class AbstractBuilder<F>
			extends AbstractLuceneSingleFieldPredicate.AbstractBuilder {
		protected final LuceneSearchIndexValueFieldContext<F> field;

		protected AbstractBuilder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			super( scope, field );
			this.field = field;
		}

		protected abstract Query buildQuery(PredicateRequestContext context);

		protected <E> E convertAndEncode(LuceneStandardFieldCodec<F, E> codec, Object value, ValueModel valueModel) {
			DslConverter<?, ? extends F> toFieldValueConverter = field.type().dslConverter( valueModel );
			try {
				F converted = toFieldValueConverter.unknownTypeToDocumentValue( value,
						scope.toDocumentValueConvertContext() );
				return codec.encode( converted );
			}
			catch (RuntimeException e) {
				throw log.cannotConvertDslParameter( e.getMessage(), e, field.eventContext() );
			}
		}

		protected <E> Range<E> convertAndEncode(LuceneStandardFieldCodec<F, E> codec, Range<?> range,
				ValueModel lowerBoundModel,
				ValueModel upperBoundModel) {
			return Range.between(
					convertAndEncode( codec, range.lowerBoundValue(), lowerBoundModel ),
					range.lowerBoundInclusion(),
					convertAndEncode( codec, range.upperBoundValue(), upperBoundModel ),
					range.upperBoundInclusion()
			);
		}

		private <E> E convertAndEncode(LuceneStandardFieldCodec<F, E> codec, Optional<?> valueOptional,
				ValueModel valueModel) {
			if ( valueOptional.isEmpty() ) {
				return null;
			}
			else {
				return convertAndEncode( codec, valueOptional.get(), valueModel );
			}
		}
	}
}

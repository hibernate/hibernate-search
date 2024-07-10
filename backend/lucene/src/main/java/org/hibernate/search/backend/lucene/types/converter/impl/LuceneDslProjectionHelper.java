/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.converter.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class LuceneDslProjectionHelper {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private LuceneDslProjectionHelper() {
	}

	@SuppressWarnings("unchecked")
	public static <E, F> E convertAndEncode(LuceneSearchIndexScope<?> scope,
			LuceneFieldCodec<F, E> codec,
			LuceneSearchIndexValueFieldContext<F> field,
			Object value, ValueModel valueModel) {
		if ( ValueModel.RAW.equals( valueModel ) ) {
			DslConverter<?, E> dslConverter = (DslConverter<?, E>) field.type().rawDslConverter();
			try {
				return dslConverter.unknownTypeToDocumentValue( value, scope.toDocumentValueConvertContext() );
			}
			catch (RuntimeException e) {
				throw log.cannotConvertDslParameter( e.getMessage(), e, field.eventContext() );
			}
		}
		else {
			DslConverter<?, ? extends F> toFieldValueConverter = field.type().dslConverter( valueModel );
			try {
				F converted = toFieldValueConverter.unknownTypeToDocumentValue( value, scope.toDocumentValueConvertContext() );
				return codec.encode( converted );
			}
			catch (RuntimeException e) {
				throw log.cannotConvertDslParameter( e.getMessage(), e, field.eventContext() );
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static <F, E, T> Function<T, E> encoder(LuceneSearchIndexScope<?> scope,
			LuceneFieldCodec<F, E> codec,
			LuceneSearchIndexValueFieldContext<F> field,
			Class<T> expectedType, ValueModel valueModel) {
		if ( ValueModel.RAW.equals( valueModel ) ) {
			DslConverter<? super T, E> dslConverter = (DslConverter<? super T, E>) field.type().rawDslConverter()
					.withInputType( expectedType, field );
			try {
				return value -> dslConverter.toDocumentValue( value, scope.toDocumentValueConvertContext() );
			}
			catch (RuntimeException e) {
				throw log.cannotConvertDslParameter( e.getMessage(), e, field.eventContext() );
			}
		}
		else {
			DslConverter<? super T, ? extends F> toFieldValueConverter = field.type().dslConverter( valueModel )
					.withInputType( expectedType, field );
			try {
				return value -> codec.encode(
						toFieldValueConverter.toDocumentValue( value, scope.toDocumentValueConvertContext() ) );
			}
			catch (RuntimeException e) {
				throw log.cannotConvertDslParameter( e.getMessage(), e, field.eventContext() );
			}
		}
	}

}

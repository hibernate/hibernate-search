/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.converter.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;

public final class ElasticsearchDslProjectionHelper {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private ElasticsearchDslProjectionHelper() {
	}

	@SuppressWarnings("unchecked")
	public static <F> JsonElement convertAndEncode(ElasticsearchSearchIndexScope<?> scope,
			ElasticsearchFieldCodec<F> codec,
			ElasticsearchSearchIndexValueFieldContext<F> field,
			Object value, ValueModel valueModel,
			BiFunction<ElasticsearchFieldCodec<F>, F, JsonElement> encodeFunction) {
		if ( ValueModel.RAW.equals( valueModel ) ) {
			DslConverter<?, JsonElement> dslConverter = (DslConverter<?, JsonElement>) field.type().rawDslConverter();
			try {
				return dslConverter.unknownTypeToDocumentValue( value, scope.toDocumentValueConvertContext() );
			}
			catch (RuntimeException e) {
				throw log.cannotConvertDslParameter( e.getMessage(), e,
						EventContexts.fromIndexFieldAbsolutePath( field.absolutePath() ) );
			}
		}
		else {
			DslConverter<?, ? extends F> toFieldValueConverter = field.type().dslConverter( valueModel );
			try {
				F converted = toFieldValueConverter.unknownTypeToDocumentValue( value, scope.toDocumentValueConvertContext() );
				return encodeFunction.apply( codec, converted );
			}
			catch (RuntimeException e) {
				throw log.cannotConvertDslParameter( e.getMessage(), e,
						EventContexts.fromIndexFieldAbsolutePath( field.absolutePath() ) );
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static <F, T> Function<T, JsonElement> encoder(
			ElasticsearchSearchIndexScope<?> scope,
			ElasticsearchFieldCodec<F> codec,
			ElasticsearchSearchIndexValueFieldContext<F> field,
			Class<T> expectedType, ValueModel valueModel) {
		try {
			if ( ValueModel.RAW.equals( valueModel ) ) {
				DslConverter<? super T, JsonElement> dslConverter =
						( (DslConverter<?, JsonElement>) field.type().rawDslConverter() )
								.withInputType( expectedType, field );
				return v -> dslConverter.toDocumentValue( v, scope.toDocumentValueConvertContext() );
			}
			else {
				DslConverter<? super T, F> toFieldValueConverter = field.type().dslConverter( valueModel )
						.withInputType( expectedType, field );

				return v -> codec.encodeForAggregation( scope.searchSyntax(),
						toFieldValueConverter.toDocumentValue( v, scope.toDocumentValueConvertContext() ) );
			}
		}
		catch (RuntimeException e) {
			throw log.cannotConvertDslParameter( e.getMessage(), e,
					EventContexts.fromIndexFieldAbsolutePath( field.absolutePath() ) );
		}
	}

}

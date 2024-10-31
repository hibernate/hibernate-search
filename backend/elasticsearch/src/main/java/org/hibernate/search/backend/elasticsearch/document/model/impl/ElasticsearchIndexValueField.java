/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.logging.impl.IndexingLog;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchEncodingContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.backend.document.model.spi.AbstractIndexValueField;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.JsonElement;

public final class ElasticsearchIndexValueField<F>
		extends AbstractIndexValueField<
				ElasticsearchIndexValueField<F>,
				ElasticsearchSearchIndexScope<?>,
				ElasticsearchIndexValueFieldType<F>,
				ElasticsearchIndexCompositeNode,
				F>
		implements ElasticsearchIndexField, ElasticsearchSearchIndexValueFieldContext<F>,
		ElasticsearchSearchEncodingContext<F> {

	public ElasticsearchIndexValueField(ElasticsearchIndexCompositeNode parent, String relativeFieldName,
			ElasticsearchIndexValueFieldType<F> type, TreeNodeInclusion inclusion, boolean multiValued) {
		super( parent, relativeFieldName, type, inclusion, multiValued );
	}

	@Override
	protected ElasticsearchIndexValueField<F> self() {
		return this;
	}

	@Override
	public ElasticsearchIndexObjectField toObjectField() {
		return SearchIndexSchemaElementContextHelper.throwingToObjectField( this );
	}

	@SuppressWarnings("unchecked")
	public <T> ElasticsearchIndexValueField<? super T> withValueType(Class<T> expectedSubType, EventContext eventContext) {
		if ( !type.valueClass().isAssignableFrom( expectedSubType ) ) {
			throw IndexingLog.INSTANCE.invalidFieldValueType( type.valueClass(), expectedSubType,
					eventContext.append( EventContexts.fromIndexFieldAbsolutePath( absolutePath ) ) );
		}
		return (ElasticsearchIndexValueField<? super T>) this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Function<T, JsonElement> encoder(ElasticsearchSearchIndexScope<?> scope,
			ElasticsearchSearchIndexValueFieldContext<F> field, Class<T> expectedType,
			ValueModel valueModel) {
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

				return v -> field.type().codec().encodeForAggregation( scope.searchSyntax(),
						toFieldValueConverter.toDocumentValue( v, scope.toDocumentValueConvertContext() ) );
			}
		}
		catch (RuntimeException e) {
			throw IndexingLog.INSTANCE.cannotConvertDslParameter( e.getMessage(), e,
					EventContexts.fromIndexFieldAbsolutePath( field.absolutePath() ) );
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public JsonElement convertAndEncode(ElasticsearchSearchIndexScope<?> scope,
			ElasticsearchSearchIndexValueFieldContext<F> field,
			Object value, ValueModel valueModel,
			BiFunction<ElasticsearchFieldCodec<F>, F, JsonElement> encodeFunction) {
		if ( ValueModel.RAW.equals( valueModel ) ) {
			DslConverter<?, JsonElement> dslConverter = (DslConverter<?, JsonElement>) field.type().rawDslConverter();
			try {
				return dslConverter.unknownTypeToDocumentValue( value, scope.toDocumentValueConvertContext() );
			}
			catch (RuntimeException e) {
				throw IndexingLog.INSTANCE.cannotConvertDslParameter( e.getMessage(), e,
						EventContexts.fromIndexFieldAbsolutePath( field.absolutePath() ) );
			}
		}
		else {
			DslConverter<?, ? extends F> toFieldValueConverter = field.type().dslConverter( valueModel );
			try {
				F converted = toFieldValueConverter.unknownTypeToDocumentValue( value, scope.toDocumentValueConvertContext() );
				return encodeFunction.apply( field.type().codec(), converted );
			}
			catch (RuntimeException e) {
				throw IndexingLog.INSTANCE.cannotConvertDslParameter( e.getMessage(), e,
						EventContexts.fromIndexFieldAbsolutePath( field.absolutePath() ) );
			}
		}
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchSearchEncodingContext<?> other) {
		return other instanceof ElasticsearchIndexValueField
				? ( (ElasticsearchIndexValueField<?>) other ).type().codec().isCompatibleWith( type.codec() )
				: false;
	}

	@Override
	public ElasticsearchSearchEncodingContext<F> encodingContext() {
		return this;
	}
}

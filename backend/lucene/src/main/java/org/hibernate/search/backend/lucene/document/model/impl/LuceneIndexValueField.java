/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchEncodingContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexValueFieldType;
import org.hibernate.search.engine.backend.document.model.spi.AbstractIndexValueField;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public final class LuceneIndexValueField<F>
		extends AbstractIndexValueField<
				LuceneIndexValueField<F>,
				LuceneSearchIndexScope<?>,
				LuceneIndexValueFieldType<F>,
				LuceneIndexCompositeNode,
				F>
		implements LuceneIndexField, LuceneSearchIndexValueFieldContext<F>, LuceneSearchEncodingContext<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final boolean dynamic;

	public LuceneIndexValueField(LuceneIndexCompositeNode parent, String relativeFieldName,
			LuceneIndexValueFieldType<F> type, TreeNodeInclusion inclusion, boolean multiValued,
			boolean dynamic) {
		super( parent, relativeFieldName, type, inclusion, multiValued );
		this.dynamic = dynamic;
	}

	@Override
	protected LuceneIndexValueField<F> self() {
		return this;
	}

	@Override
	public LuceneIndexObjectField toObjectField() {
		return SearchIndexSchemaElementContextHelper.throwingToObjectField( this );
	}

	@Override
	public boolean dynamic() {
		return dynamic;
	}

	@SuppressWarnings("unchecked")
	public <T> LuceneIndexValueField<? super T> withValueType(Class<T> expectedSubType, EventContext eventContext) {
		if ( !type.valueClass().isAssignableFrom( expectedSubType ) ) {
			throw log.invalidFieldValueType( type.valueClass(), expectedSubType,
					eventContext.append( EventContexts.fromIndexFieldAbsolutePath( absolutePath ) ) );
		}
		return (LuceneIndexValueField<? super T>) this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E, T> Function<T, E> encoder(LuceneSearchIndexScope<?> scope,
			LuceneSearchIndexValueFieldContext<F> field, LuceneFieldCodec<F, E> codec, Class<T> expectedType,
			ValueModel valueModel) {
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

	@Override
	@SuppressWarnings("unchecked")
	public <E> E convertAndEncode(LuceneSearchIndexScope<?> scope,
			LuceneSearchIndexValueFieldContext<F> field,
			LuceneFieldCodec<F, E> codec,
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

	@Override
	public boolean isCompatibleWith(LuceneSearchEncodingContext<?> other) {
		return other instanceof LuceneIndexValueField<?>
				? ( (LuceneIndexValueField<?>) other ).type().codec().isCompatibleWith( type().codec() )
				: false;
	}

	@Override
	public LuceneSearchEncodingContext<F> encodingContext() {
		return this;
	}
}

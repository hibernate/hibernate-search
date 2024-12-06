/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.function.Function;

import org.hibernate.search.backend.lucene.logging.impl.QueryLog;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.StoredFieldsValuesDelegate;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.TopDocsDataCollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSetIterator;

/**
 * A projection on the values of an index field.
 *
 * @param <F> The type of individual field values obtained from the backend (before conversion).
 * @param <V> The type of individual field values after conversion.
 * @param <P> The type of the final projection result representing accumulated values of type {@code V}.
 */
public class LuceneFieldProjection<F, V, P, T> extends AbstractLuceneProjection<P> {

	private final String absoluteFieldPath;
	private final String nestedDocumentPath;
	private final String requiredContextAbsoluteFieldPath;

	private final Function<IndexableField, T> decodeFunction;
	private final ProjectionConverter<T, ? extends V> converter;
	private final ProjectionCollector.Provider<V, P> collectorProvider;

	private LuceneFieldProjection(Builder<F, V, T> builder, ProjectionCollector.Provider<V, P> collectorProvider) {
		this( builder.scope, builder.field, builder.decodeFunction, builder.converter, collectorProvider );
	}

	LuceneFieldProjection(LuceneSearchIndexScope<?> scope,
			LuceneSearchIndexValueFieldContext<?> field,
			Function<IndexableField, T> decodeFunction,
			ProjectionConverter<T, ? extends V> converter,
			ProjectionCollector.Provider<V, P> collectorProvider) {
		super( scope );
		this.absoluteFieldPath = field.absolutePath();
		this.nestedDocumentPath = field.nestedDocumentPath();
		this.requiredContextAbsoluteFieldPath = collectorProvider.isSingleValued()
				? field.closestMultiValuedParentAbsolutePath()
				: null;
		this.decodeFunction = decodeFunction;
		this.converter = converter;
		this.collectorProvider = collectorProvider;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "absoluteFieldPath=" + absoluteFieldPath
				+ ", collectorProvider=" + collectorProvider
				+ "]";
	}

	@Override
	public ValueFieldExtractor<?> request(ProjectionRequestContext context) {
		context.checkValidField( absoluteFieldPath );
		if ( !context.projectionCardinalityCorrectlyAddressed( requiredContextAbsoluteFieldPath ) ) {
			throw QueryLog.INSTANCE.invalidSingleValuedProjectionOnValueFieldInMultiValuedObjectField(
					absoluteFieldPath, requiredContextAbsoluteFieldPath );
		}
		context.requireStoredField( absoluteFieldPath, nestedDocumentPath );
		return new ValueFieldExtractor<>( context.absoluteCurrentNestedFieldPath(), collectorProvider.get() );
	}

	/**
	 * @param <A> The type of the temporary storage for accumulated values, before and after being transformed.
	 */
	private class ValueFieldExtractor<A> implements LuceneSearchProjection.Extractor<A, P> {

		private final String contextAbsoluteFieldPath;
		private final ProjectionCollector<T, V, A, P> collector;

		public ValueFieldExtractor(String contextAbsoluteFieldPath, ProjectionCollector<T, V, A, P> collector) {
			this.collector = collector;
			this.contextAbsoluteFieldPath = contextAbsoluteFieldPath;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "["
					+ "absoluteFieldPath=" + absoluteFieldPath
					+ ", collector=" + collector
					+ "]";
		}

		@Override
		public Values<A> values(ProjectionExtractContext context) {
			return new StoredFieldValues( collector, context.collectorExecutionContext() );
		}

		private class StoredFieldValues extends AbstractNestingAwareAccumulatingValues<T, A> {
			private final StoredFieldsValuesDelegate delegate;

			public StoredFieldValues(ProjectionCollector<T, V, A, P> collector,
					TopDocsDataCollectorExecutionContext context) {
				super( contextAbsoluteFieldPath, nestedDocumentPath, collector, context );
				this.delegate = context.storedFieldsValuesDelegate();
			}

			@Override
			protected DocIdSetIterator doContext(LeafReaderContext context) {
				// We don't have a cost-effective way to iterate on children that have values for our stored field.
				return null;
			}

			@Override
			protected A accumulate(A accumulated, int docId) {
				Document document = delegate.get( docId );
				for ( IndexableField field : document.getFields() ) {
					if ( field.name().equals( absoluteFieldPath ) ) {
						T decoded = decodeFunction.apply( field );
						accumulated = collector.accumulate( accumulated, decoded );
					}
				}
				return accumulated;
			}
		}

		@Override
		public P transform(LoadingResult<?> loadingResult, A extractedData, ProjectionTransformContext context) {
			FromDocumentValueConvertContext convertContext = context.fromDocumentValueConvertContext();
			A transformedData = collector.transformAll( extractedData, converter.delegate(), convertContext );
			return collector.finish( transformedData );
		}
	}

	public static class Factory<F, E>
			extends
			AbstractLuceneCodecAwareSearchQueryElementFactory<FieldProjectionBuilder.TypeSelector, F, LuceneFieldCodec<F, E>> {
		public Factory(LuceneFieldCodec<F, E> codec) {
			super( codec );
		}

		@Override
		public TypeSelector<?, ?> create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			// Fail early if the nested structure differs in the case of multi-index search.
			field.nestedPathHierarchy();
			return new TypeSelector<>( codec, scope, field );
		}
	}

	private static class TypeSelector<F, E> implements FieldProjectionBuilder.TypeSelector {
		private final LuceneFieldCodec<F, E> codec;
		private final LuceneSearchIndexScope<?> scope;
		private final LuceneSearchIndexValueFieldContext<F> field;

		private TypeSelector(LuceneFieldCodec<F, E> codec,
				LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			this.codec = codec;
			this.scope = scope;
			this.field = field;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <V> Builder<F, V, ?> type(Class<V> expectedType, ValueModel valueModel) {
			if ( ValueModel.RAW.equals( valueModel ) ) {
				return new Builder<>( scope, field,
						codec::raw,
						// unchecked cast to make eclipse-compiler happy
						// we know that Lucene projection converters work with the encoded type
						( (ProjectionConverter<E, ?>) field.type().rawProjectionConverter() )
								.withConvertedType( expectedType, field )
				);
			}
			else {
				return new Builder<>( scope, field,
						codec::decode,
						field.type().projectionConverter( valueModel ).withConvertedType( expectedType, field )
				);
			}
		}
	}

	private static class Builder<F, V, T> extends AbstractLuceneProjection.AbstractBuilder<V>
			implements FieldProjectionBuilder<V> {

		private final Function<IndexableField, T> decodeFunction;

		private final LuceneSearchIndexValueFieldContext<F> field;

		private final ProjectionConverter<T, ? extends V> converter;

		private Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field,
				Function<IndexableField, T> decodeFunction, ProjectionConverter<T, ? extends V> converter) {
			super( scope );
			this.decodeFunction = decodeFunction;
			this.field = field;
			this.converter = converter;
		}

		@Override
		public <P> SearchProjection<P> build(ProjectionCollector.Provider<V, P> collectorProvider) {
			if ( collectorProvider.isSingleValued() && field.multiValued() ) {
				throw QueryLog.INSTANCE.invalidSingleValuedProjectionOnMultiValuedField( field.absolutePath(),
						field.eventContext() );
			}
			return new LuceneFieldProjection<>( this, collectorProvider );
		}
	}
}

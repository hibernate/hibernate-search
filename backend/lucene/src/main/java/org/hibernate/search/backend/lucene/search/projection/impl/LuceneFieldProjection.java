/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.StoredFieldsValuesDelegate;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReaderContext;

/**
 * A projection on the values of an index field.
 *
 * @param <F> The type of individual field values obtained from the backend (before conversion).
 * @param <V> The type of individual field values after conversion.
 * @param <A> The type of the temporary storage for accumulated values, before and after being transformed.
 * @param <P> The type of the final projection result representing accumulated values of type {@code V}.
 */
public class LuceneFieldProjection<F, V, A, P> extends AbstractLuceneProjection<P>
		implements LuceneSearchProjection.Extractor<A, P> {

	private final String absoluteFieldPath;
	private final String nestedDocumentPath;

	private final Function<IndexableField, F> decodeFunction;
	private final ProjectionConverter<F, ? extends V> converter;
	private final ProjectionAccumulator<F, V, A, P> accumulator;

	private LuceneFieldProjection(Builder<F, V> builder, ProjectionAccumulator<F, V, A, P> accumulator) {
		this( builder.scope, builder.field, builder.codec::decode, builder.converter, accumulator );
	}

	LuceneFieldProjection(LuceneSearchIndexScope<?> scope,
			LuceneSearchIndexValueFieldContext<?> field,
			Function<IndexableField, F> decodeFunction, ProjectionConverter<F, ? extends V> converter,
			ProjectionAccumulator<F, V, A, P> accumulator) {
		super( scope );
		this.absoluteFieldPath = field.absolutePath();
		this.nestedDocumentPath = field.nestedDocumentPath();
		this.decodeFunction = decodeFunction;
		this.converter = converter;
		this.accumulator = accumulator;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "absoluteFieldPath=" + absoluteFieldPath
				+ ", accumulator=" + accumulator
				+ "]";
	}

	@Override
	public Extractor<A, P> request(ProjectionRequestContext context) {
		context.requireStoredField( absoluteFieldPath, nestedDocumentPath );
		return this;
	}

	@Override
	public Values<A> values(ProjectionExtractContext context) {
		return new StoredFieldValues( context.collectorExecutionContext().storedFieldsValuesDelegate() );
	}

	private class StoredFieldValues implements Values<A> {
		private final StoredFieldsValuesDelegate delegate;

		private StoredFieldValues(StoredFieldsValuesDelegate delegate) {
			this.delegate = delegate;
		}

		@Override
		public void context(LeafReaderContext context) {
			// Nothing to do
		}

		@Override
		public A get(int doc) {
			Document document = delegate.get( doc );
			A accumulated = accumulator.createInitial();
			for ( IndexableField field : document.getFields() ) {
				if ( field.name().equals( absoluteFieldPath ) ) {
					F decoded = decodeFunction.apply( field );
					accumulated = accumulator.accumulate( accumulated, decoded );
				}
			}
			return accumulated;
		}
	}

	@Override
	public P transform(LoadingResult<?, ?> loadingResult, A extractedData,
			ProjectionTransformContext context) {
		FromDocumentValueConvertContext convertContext = context.fromDocumentValueConvertContext();
		A transformedData = accumulator.transformAll( extractedData, converter, convertContext );
		return accumulator.finish( transformedData );
	}

	public static class Factory<F>
			extends
			AbstractLuceneCodecAwareSearchQueryElementFactory<FieldProjectionBuilder.TypeSelector, F, LuceneFieldCodec<F>> {
		public Factory(LuceneFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public TypeSelector<?> create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			// Fail early if the nested structure differs in the case of multi-index search.
			field.nestedPathHierarchy();
			return new TypeSelector<>( codec, scope, field );
		}
	}

	private static class TypeSelector<F> implements FieldProjectionBuilder.TypeSelector {
		private final LuceneFieldCodec<F> codec;
		private final LuceneSearchIndexScope<?> scope;
		private final LuceneSearchIndexValueFieldContext<F> field;

		private TypeSelector(LuceneFieldCodec<F> codec,
				LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			this.codec = codec;
			this.scope = scope;
			this.field = field;
		}

		@Override
		public <V> Builder<F, V> type(Class<V> expectedType, ValueConvert convert) {
			return new Builder<>( codec, scope, field,
					field.type().projectionConverter( convert ).withConvertedType( expectedType, field ) );
		}
	}

	private static class Builder<F, V> extends AbstractLuceneProjection.AbstractBuilder<V>
			implements FieldProjectionBuilder<V> {

		private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

		private final LuceneFieldCodec<F> codec;

		private final LuceneSearchIndexValueFieldContext<F> field;

		private final ProjectionConverter<F, ? extends V> converter;

		private Builder(LuceneFieldCodec<F> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field, ProjectionConverter<F, ? extends V> converter) {
			super( scope );
			this.codec = codec;
			this.field = field;
			this.converter = converter;
		}

		@Override
		public <P> SearchProjection<P> build(ProjectionAccumulator.Provider<V, P> accumulatorProvider) {
			if ( accumulatorProvider.isSingleValued() && field.multiValuedInRoot() ) {
				throw log.invalidSingleValuedProjectionOnMultiValuedField( field.absolutePath(), field.eventContext() );
			}
			return new LuceneFieldProjection<>( this, accumulatorProvider.get() );
		}
	}
}

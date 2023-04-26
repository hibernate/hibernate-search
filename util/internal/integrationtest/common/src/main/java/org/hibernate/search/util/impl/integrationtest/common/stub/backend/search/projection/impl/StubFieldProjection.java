/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexNodeContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexValueFieldContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.AbstractStubSearchQueryElementFactory;

public class StubFieldProjection<F, V, A, P> implements StubSearchProjection<P> {
	private final Class<F> fieldType;
	private final ProjectionConverter<F, ? extends V> converter;
	private final ProjectionAccumulator<F, V, A, P> accumulator;
	private final boolean singleValued;

	public StubFieldProjection(Class<F> fieldType, ProjectionConverter<F,? extends V> converter,
			ProjectionAccumulator<F, V, A, P> accumulator, boolean singleValued) {
		this.fieldType = fieldType;
		this.converter = converter;
		this.accumulator = accumulator;
		this.singleValued = singleValued;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "fieldType=" + fieldType
				+ ", converter=" + converter
				+ ", accumulator=" + accumulator
				+ "]";
	}

	@Override
	public A extract(ProjectionHitMapper<?> projectionHitMapper, Iterator<?> projectionFromIndex,
			StubSearchProjectionContext context) {
		List<?> fieldValues;
		if ( singleValued ) {
			Object singleValue = projectionFromIndex.next();
			fieldValues = singleValue == null ? Collections.emptyList() : Arrays.asList( singleValue );
		}
		else {
			fieldValues = (List<?>) projectionFromIndex.next();
		}
		A accumulated = accumulator.createInitial();
		for ( Object fieldValue : fieldValues ) {
			accumulated = accumulator.accumulate( accumulated, fieldType.cast( fieldValue ) );
		}
		return accumulated;
	}

	@Override
	@SuppressWarnings("unchecked")
	public P transform(LoadingResult<?> loadingResult, Object extractedData,
			StubSearchProjectionContext context) {
		A accumulated = (A) extractedData;
		A transformedData = accumulator.transformAll( accumulated, converter, context.fromDocumentValueConvertContext() );
		return accumulator.finish( transformedData );
	}

	public static class Factory extends AbstractStubSearchQueryElementFactory<FieldProjectionBuilder.TypeSelector> {
		@Override
		public FieldProjectionBuilder.TypeSelector create(StubSearchIndexScope scope,
				StubSearchIndexNodeContext node) {
			return new TypeSelector<>( node.toValueField() );
		}
	}

	public static class TypeSelector<F> implements FieldProjectionBuilder.TypeSelector {
		private final StubSearchIndexValueFieldContext<F> field;

		public TypeSelector(StubSearchIndexValueFieldContext<F> field) {
			this.field = field;
		}

		@Override
		public <V> Builder<F, V> type(Class<V> expectedType, ValueConvert convert) {
			ProjectionConverter<F, ? extends V> converter = field.type().projectionConverter( convert )
					.withConvertedType( expectedType, field );
			return new Builder<>( field.type().valueClass(), converter );
		}
	}

	static class Builder<F, V> implements FieldProjectionBuilder<V> {
		private final Class<F> valueClass;
		private final ProjectionConverter<F, ? extends V> converter;

		Builder(Class<F> valueClass, ProjectionConverter<F, ? extends V> converter) {
			this.valueClass = valueClass;
			this.converter = converter;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <P> SearchProjection<P> build(ProjectionAccumulator.Provider<V, P> accumulatorProvider) {
			return new StubFieldProjection<>( valueClass, converter,
					accumulatorProvider.get(), accumulatorProvider.isSingleValued() );
		}
	}
}

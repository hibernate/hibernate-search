/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.search.projection.spi.SingleValuedProjectionAccumulator;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexSchemaElementContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchValueFieldContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchQueryElementFactory;

public class StubFieldSearchProjection<F, V> implements StubSearchProjection<V> {
	private final Class<F> valueClass;
	private final ProjectionConverter<F, ? extends V> converter;

	private StubFieldSearchProjection(Class<F> valueClass, ProjectionConverter<F, ? extends V> converter) {
		this.valueClass = valueClass;
		this.converter = converter;
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, Object projectionFromIndex,
			StubSearchProjectionContext context) {
		return converter.convert( valueClass.cast( projectionFromIndex ),
				context.fromDocumentFieldValueConvertContext() );
	}

	@Override
	public V transform(LoadingResult<?, ?> loadingResult, Object extractedData,
			StubSearchProjectionContext context) {
		return converter.valueType().cast( extractedData );
	}

	public static class Factory implements StubSearchQueryElementFactory<FieldProjectionBuilder.TypeSelector> {
		@Override
		public FieldProjectionBuilder.TypeSelector create(StubSearchIndexScope scope,
				StubSearchIndexSchemaElementContext element) {
			return new TypeSelector<>( element.toValueField() );
		}
	}

	public static class TypeSelector<F> implements FieldProjectionBuilder.TypeSelector {
		private final StubSearchValueFieldContext<F> field;

		public TypeSelector(StubSearchValueFieldContext<F> field) {
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
		public SearchProjection<V> build() {
			return new StubFieldSearchProjection<>( valueClass, converter );
		}

		@Override
		@SuppressWarnings("unchecked")
		public <P> SearchProjection<P> build(ProjectionAccumulator.Provider<V, P> accumulatorProvider) {
			if ( accumulatorProvider == SingleValuedProjectionAccumulator.provider() ) {
				return (SearchProjection<P>) build();
			}
			else {
				throw new AssertionFailure( "Multi-valued projections are not supported in the stub backend." );
			}
		}
	}
}

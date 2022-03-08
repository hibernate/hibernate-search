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
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexNodeContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexValueFieldContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.AbstractStubSearchQueryElementFactory;

public class StubFieldProjection<F, V> implements StubSearchProjection<V> {
	private final Class<F> valueClass;
	private final ProjectionConverter<F, ? extends V> converter;

	private StubFieldProjection(Class<F> valueClass, ProjectionConverter<F, ? extends V> converter) {
		this.valueClass = valueClass;
		this.converter = converter;
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, Object projectionFromIndex,
			StubSearchProjectionContext context) {
		return converter.fromDocumentValue( valueClass.cast( projectionFromIndex ),
				context.fromDocumentValueConvertContext() );
	}

	@Override
	public V transform(LoadingResult<?, ?> loadingResult, Object extractedData,
			StubSearchProjectionContext context) {
		return converter.valueType().cast( extractedData );
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
		public SearchProjection<V> build() {
			return new StubFieldProjection<>( valueClass, converter );
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

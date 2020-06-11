/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.projection.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchFieldProjectionBuilder;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchStandardFieldProjectionBuilderFactory<F>
		implements ElasticsearchFieldProjectionBuilderFactory<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final boolean projectable;

	private final ElasticsearchFieldCodec<F> codec;

	public ElasticsearchStandardFieldProjectionBuilderFactory(boolean projectable,
			ElasticsearchFieldCodec<F> codec) {
		this.projectable = projectable;
		this.codec = codec;
	}

	@Override
	public boolean isProjectable() {
		return projectable;
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldProjectionBuilderFactory<?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		ElasticsearchStandardFieldProjectionBuilderFactory<?> castedOther =
				(ElasticsearchStandardFieldProjectionBuilderFactory<?>) other;
		return projectable == castedOther.projectable && codec.isCompatibleWith( castedOther.codec );
	}

	@Override
	@SuppressWarnings("unchecked") // We check the cast is legal by asking the converter
	public <T> FieldProjectionBuilder<T> createFieldValueProjectionBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<F> field, Class<T> expectedType, ValueConvert convert) {
		checkProjectable( field );

		ProjectionConverter<? super F, ?> requestConverter = field.type().projectionConverter( convert );
		if ( !requestConverter.isConvertedTypeAssignableTo( expectedType ) ) {
			throw log.invalidProjectionInvalidType( field.absolutePath(), expectedType, field.eventContext() );
		}

		return (FieldProjectionBuilder<T>) new ElasticsearchFieldProjectionBuilder<>( searchContext, field,
				requestConverter, codec );
	}

	@Override
	public DistanceToFieldProjectionBuilder createDistanceProjectionBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<F> field, GeoPoint center) {
		throw log.distanceOperationsNotSupportedByFieldType( field.eventContext() );
	}

	protected void checkProjectable(ElasticsearchSearchFieldContext<?> field) {
		if ( !projectable ) {
			throw log.nonProjectableField( field.absolutePath(), field.eventContext() );
		}
	}
}

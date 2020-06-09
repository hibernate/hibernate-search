/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchFieldProjectionBuilder;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchStandardFieldProjectionBuilderFactory<F> implements ElasticsearchFieldProjectionBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final boolean projectable;

	private final ProjectionConverter<? super F, ?> converter;
	private final ProjectionConverter<? super F, F> rawConverter;

	private final ElasticsearchFieldCodec<F> codec;

	public ElasticsearchStandardFieldProjectionBuilderFactory(boolean projectable,
			ProjectionConverter<? super F, ?> converter, ProjectionConverter<? super F, F> rawConverter,
			ElasticsearchFieldCodec<F> codec) {
		this.projectable = projectable;
		this.converter = converter;
		this.rawConverter = rawConverter;
		this.codec = codec;
	}

	@Override
	public boolean isProjectable() {
		return projectable;
	}

	@Override
	@SuppressWarnings("unchecked") // We check the cast is legal by asking the converter
	public <T> FieldProjectionBuilder<T> createFieldValueProjectionBuilder(Set<String> indexNames, String absoluteFieldPath,
			boolean multiValuedFieldInRoot, Class<T> expectedType, ValueConvert convert) {
		checkProjectable( absoluteFieldPath, projectable );

		ProjectionConverter<? super F, ?> requestConverter = getConverter( convert );
		if ( !requestConverter.isConvertedTypeAssignableTo( expectedType ) ) {
			throw log.invalidProjectionInvalidType( absoluteFieldPath, expectedType,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}

		// FIXME HSEARCH-3945 ideally this should be done at bootstrap
		String[] absoluteFieldPathComponents = FieldPaths.split( absoluteFieldPath );

		return (FieldProjectionBuilder<T>) new ElasticsearchFieldProjectionBuilder<>(
				indexNames, absoluteFieldPath, absoluteFieldPathComponents, multiValuedFieldInRoot,
				requestConverter, codec );
	}

	@Override
	public DistanceToFieldProjectionBuilder createDistanceProjectionBuilder(Set<String> indexNames, String absoluteFieldPath, String nestedPath,
			GeoPoint center) {
		throw log.distanceOperationsNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public boolean hasCompatibleCodec(ElasticsearchFieldProjectionBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		ElasticsearchStandardFieldProjectionBuilderFactory<?> castedOther =
				(ElasticsearchStandardFieldProjectionBuilderFactory<?>) other;
		return projectable == castedOther.projectable && codec.isCompatibleWith( castedOther.codec );
	}

	@Override
	public boolean hasCompatibleConverter(ElasticsearchFieldProjectionBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		ElasticsearchStandardFieldProjectionBuilderFactory<?> castedOther =
				(ElasticsearchStandardFieldProjectionBuilderFactory<?>) other;
		return converter.isCompatibleWith( castedOther.converter );
	}

	protected static void checkProjectable(String absoluteFieldPath, boolean projectable) {
		if ( !projectable ) {
				throw log.nonProjectableField( absoluteFieldPath,
						EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}
	}

	protected ProjectionConverter<? super F, ?> getConverter(ValueConvert convert) {
		switch ( convert ) {
			case NO:
				return rawConverter;
			case YES:
			default:
				return converter;
		}
	}
}

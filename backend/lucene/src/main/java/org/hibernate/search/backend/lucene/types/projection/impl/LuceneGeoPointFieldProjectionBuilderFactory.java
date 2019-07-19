/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneDistanceToFieldProjectionBuilder;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneFieldProjectionBuilder;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneGeoPointFieldProjectionBuilderFactory implements LuceneFieldProjectionBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final boolean projectable;

	private final FromDocumentFieldValueConverter<? super GeoPoint, ?> converter;
	private final FromDocumentFieldValueConverter<? super GeoPoint, GeoPoint> rawConverter;
	private final LuceneFieldCodec<GeoPoint> codec;

	public LuceneGeoPointFieldProjectionBuilderFactory(boolean projectable,
			LuceneFieldCodec<GeoPoint> codec,
			FromDocumentFieldValueConverter<? super GeoPoint, ?> converter, FromDocumentFieldValueConverter<? super GeoPoint, GeoPoint> rawConverter) {
		this.projectable = projectable;
		this.converter = converter;
		this.rawConverter = rawConverter;
		this.codec = codec;
	}

	@Override
	@SuppressWarnings("unchecked") // We check the cast is legal by asking the converter
	public <T> FieldProjectionBuilder<T> createFieldValueProjectionBuilder(Set<String> indexNames, String absoluteFieldPath,
			Class<T> expectedType, ValueConvert convert) {
		checkProjectable( absoluteFieldPath, projectable );

		FromDocumentFieldValueConverter<? super GeoPoint, ?> requestConverter = getConverter( convert );
		if ( !requestConverter.isConvertedTypeAssignableTo( expectedType ) ) {
			throw log.invalidProjectionInvalidType( absoluteFieldPath, expectedType,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}

		return (FieldProjectionBuilder<T>) new LuceneFieldProjectionBuilder<>( indexNames, absoluteFieldPath, requestConverter, codec );
	}

	@Override
	public DistanceToFieldProjectionBuilder createDistanceProjectionBuilder(Set<String> indexNames, String absoluteFieldPath,
			GeoPoint center) {
		checkProjectable( absoluteFieldPath, projectable );

		return new LuceneDistanceToFieldProjectionBuilder( indexNames, absoluteFieldPath, center );
	}

	@Override
	public boolean hasCompatibleCodec(LuceneFieldProjectionBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		LuceneGeoPointFieldProjectionBuilderFactory castedOther =
				(LuceneGeoPointFieldProjectionBuilderFactory) other;
		return projectable == castedOther.projectable && codec.isCompatibleWith( castedOther.codec );
	}

	@Override
	public boolean hasCompatibleConverter(LuceneFieldProjectionBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		LuceneGeoPointFieldProjectionBuilderFactory castedOther =
				(LuceneGeoPointFieldProjectionBuilderFactory) other;
		return converter.isCompatibleWith( castedOther.converter );
	}

	private static void checkProjectable(String absoluteFieldPath, boolean projectable) {
		if ( !projectable ) {
			throw log.nonProjectableField( absoluteFieldPath,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}
	}

	private FromDocumentFieldValueConverter<? super GeoPoint, ?> getConverter(ValueConvert convert) {
		switch ( convert ) {
			case NO:
				return rawConverter;
			case YES:
			default:
				return converter;
		}
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.projection.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneFieldProjectionBuilder;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @param <F> The field type exposed to the mapper.
 * @see LuceneFieldCodec
 */
public class LuceneStandardFieldProjectionBuilderFactory<F> implements LuceneFieldProjectionBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final boolean projectable;

	private final FromDocumentFieldValueConverter<? super F, ?> converter;
	private final LuceneFieldCodec<F> codec;

	public LuceneStandardFieldProjectionBuilderFactory(boolean projectable,
			FromDocumentFieldValueConverter<? super F, ?> converter,
			LuceneFieldCodec<F> codec) {
		this.projectable = projectable;
		this.converter = converter;
		this.codec = codec;
	}

	@Override
	@SuppressWarnings("unchecked") // We check the cast is legal by asking the converter
	public <T> FieldProjectionBuilder<T> createFieldValueProjectionBuilder(String absoluteFieldPath,
			Class<T> expectedType) {
		checkProjectable( absoluteFieldPath, projectable );

		if ( !converter.isConvertedTypeAssignableTo( expectedType ) ) {
			throw log.invalidProjectionInvalidType( absoluteFieldPath, expectedType,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}

		return (FieldProjectionBuilder<T>) new LuceneFieldProjectionBuilder<>( absoluteFieldPath, converter, codec );
	}

	@Override
	public DistanceToFieldProjectionBuilder createDistanceProjectionBuilder(String absoluteFieldPath,
			GeoPoint center) {
		throw log.distanceOperationsNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public boolean isDslCompatibleWith(LuceneFieldProjectionBuilderFactory obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj.getClass() != LuceneStandardFieldProjectionBuilderFactory.class ) {
			return false;
		}

		LuceneStandardFieldProjectionBuilderFactory<?> other = (LuceneStandardFieldProjectionBuilderFactory<?>) obj;

		return projectable == other.projectable &&
				codec.isCompatibleWith( other.codec ) &&
				converter.isCompatibleWith( other.converter );
	}

	private static void checkProjectable(String absoluteFieldPath, boolean projectable) {
		if ( !projectable ) {
			throw log.nonProjectableField( absoluteFieldPath,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}
	}
}

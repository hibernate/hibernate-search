/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.projection.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.projection.impl.FieldSearchProjectionBuilderImpl;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.backend.lucene.types.converter.impl.LuceneFieldConverter;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldSearchProjectionBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.common.LoggerFactory;

public class StandardFieldProjectionBuilderFactory<T> implements LuceneFieldProjectionBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final boolean projectable;

	private final LuceneFieldCodec<T> codec;

	private final LuceneFieldConverter<T, ?> converter;

	public StandardFieldProjectionBuilderFactory(boolean projectable, LuceneFieldCodec<T> codec,
			LuceneFieldConverter<T, ?> converter) {
		this.projectable = projectable;
		this.codec = codec;
		this.converter = converter;
	}

	@Override
	public <U> FieldSearchProjectionBuilder<U> createFieldValueProjectionBuilder(String absoluteFieldPath,
			Class<U> expectedType) {
		checkProjectable( absoluteFieldPath, projectable );

		if ( !converter.isProjectionCompatibleWith( expectedType ) ) {
			throw log.invalidProjectionInvalidType( absoluteFieldPath, expectedType,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}

		return new FieldSearchProjectionBuilderImpl<>( absoluteFieldPath, codec, converter );
	}

	@Override
	public DistanceToFieldSearchProjectionBuilder createDistanceProjectionBuilder(String absoluteFieldPath,
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
		if ( obj.getClass() != StandardFieldProjectionBuilderFactory.class ) {
			return false;
		}

		StandardFieldProjectionBuilderFactory<?> other = (StandardFieldProjectionBuilderFactory<?>) obj;

		return projectable == other.projectable &&
				codec.isCompatibleWith( other.codec ) &&
				converter.isConvertIndexToProjectionCompatibleWith( other.converter );
	}

	private static void checkProjectable(String absoluteFieldPath, boolean projectable) {
		if ( !projectable ) {
			throw log.nonProjectableField( absoluteFieldPath,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}
	}
}

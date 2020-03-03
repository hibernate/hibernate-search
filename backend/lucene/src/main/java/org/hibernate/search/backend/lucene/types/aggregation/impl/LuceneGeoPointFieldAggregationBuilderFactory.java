/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneGeoPointFieldAggregationBuilderFactory
		implements LuceneFieldAggregationBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final boolean aggregable;

	private final DslConverter<?, ? extends GeoPoint> toFieldValueConverter;
	private final ProjectionConverter<? super GeoPoint, ?> fromFieldValueConverter;
	private final LuceneFieldCodec<GeoPoint> codec;

	public LuceneGeoPointFieldAggregationBuilderFactory(boolean aggregable,
			DslConverter<?, ? extends GeoPoint> toFieldValueConverter,
			ProjectionConverter<? super GeoPoint, ?> fromFieldValueConverter,
			LuceneFieldCodec<GeoPoint> codec) {
		this.aggregable = aggregable;
		this.toFieldValueConverter = toFieldValueConverter;
		this.fromFieldValueConverter = fromFieldValueConverter;
		this.codec = codec;
	}

	@Override
	public <K> TermsAggregationBuilder<K> createTermsAggregationBuilder(LuceneSearchContext searchContext,
			String nestedDocumentPath, String absoluteFieldPath, Class<K> expectedType, ValueConvert convert) {
		throw log.directValueLookupNotSupportedByGeoPoint(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public <K> RangeAggregationBuilder<K> createRangeAggregationBuilder(LuceneSearchContext searchContext,
			String nestedDocumentPath, String absoluteFieldPath, Class<K> expectedType, ValueConvert convert) {
		throw log.rangesNotSupportedByGeoPoint(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public boolean hasCompatibleCodec(LuceneFieldAggregationBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		LuceneGeoPointFieldAggregationBuilderFactory castedOther =
				(LuceneGeoPointFieldAggregationBuilderFactory) other;
		return aggregable == castedOther.aggregable && codec.isCompatibleWith( castedOther.codec );
	}

	@Override
	public boolean hasCompatibleConverter(LuceneFieldAggregationBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		LuceneGeoPointFieldAggregationBuilderFactory castedOther =
				(LuceneGeoPointFieldAggregationBuilderFactory) other;
		return toFieldValueConverter.isCompatibleWith( castedOther.toFieldValueConverter )
				&& fromFieldValueConverter.isCompatibleWith( castedOther.fromFieldValueConverter );
	}
}

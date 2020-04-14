/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.aggregation.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchGeoPointFieldAggregationBuilderFactory
		implements ElasticsearchFieldAggregationBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final boolean aggregable;

	private final DslConverter<?, ? extends GeoPoint> toFieldValueConverter;
	private final ProjectionConverter<? super GeoPoint, ?> fromFieldValueConverter;
	private final ElasticsearchFieldCodec<GeoPoint> codec;

	public ElasticsearchGeoPointFieldAggregationBuilderFactory(boolean aggregable,
			DslConverter<?, ? extends GeoPoint> toFieldValueConverter,
			ProjectionConverter<? super GeoPoint, ?> fromFieldValueConverter,
			ElasticsearchFieldCodec<GeoPoint> codec) {
		this.aggregable = aggregable;
		this.toFieldValueConverter = toFieldValueConverter;
		this.fromFieldValueConverter = fromFieldValueConverter;
		this.codec = codec;
	}

	@Override
	public <K> TermsAggregationBuilder<K> createTermsAggregationBuilder(ElasticsearchSearchContext searchContext,
			String absoluteFieldPath, List<String> nestedPathHierarchy, Class<K> expectedType, ValueConvert convert) {
		throw log.directValueLookupNotSupportedByGeoPoint(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public <K> RangeAggregationBuilder<K> createRangeAggregationBuilder(ElasticsearchSearchContext searchContext,
			String absoluteFieldPath, List<String> nestedPathHierarchy, Class<K> expectedType, ValueConvert convert) {
		throw log.rangesNotSupportedByGeoPoint(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public boolean hasCompatibleCodec(ElasticsearchFieldAggregationBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		ElasticsearchGeoPointFieldAggregationBuilderFactory castedOther =
				(ElasticsearchGeoPointFieldAggregationBuilderFactory) other;
		return aggregable == castedOther.aggregable && codec.isCompatibleWith( castedOther.codec );
	}

	@Override
	public boolean hasCompatibleConverter(ElasticsearchFieldAggregationBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		ElasticsearchGeoPointFieldAggregationBuilderFactory castedOther =
				(ElasticsearchGeoPointFieldAggregationBuilderFactory) other;
		return toFieldValueConverter.isCompatibleWith( castedOther.toFieldValueConverter )
				&& fromFieldValueConverter.isCompatibleWith( castedOther.fromFieldValueConverter );
	}
}

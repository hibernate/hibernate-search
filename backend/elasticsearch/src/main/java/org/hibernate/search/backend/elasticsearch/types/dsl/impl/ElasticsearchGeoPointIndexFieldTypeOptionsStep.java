/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchGeoPointFieldAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchGeoPointFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchGeoPointFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchGeoPointFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchGeoPointFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.spatial.GeoPoint;


class ElasticsearchGeoPointIndexFieldTypeOptionsStep
		extends AbstractElasticsearchSimpleStandardFieldTypeOptionsStep<ElasticsearchGeoPointIndexFieldTypeOptionsStep, GeoPoint> {

	ElasticsearchGeoPointIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, GeoPoint.class, DataTypes.GEO_POINT );
	}

	@Override
	protected ElasticsearchIndexFieldType<GeoPoint> toIndexFieldType(PropertyMapping mapping) {
		ElasticsearchGeoPointFieldCodec codec = ElasticsearchGeoPointFieldCodec.INSTANCE;

		// We need doc values for the projection script when not sorting on the same field
		mapping.setDocValues( resolvedSortable || resolvedProjectable );

		ToDocumentFieldValueConverter<?, ? extends GeoPoint> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super GeoPoint, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();
		FromDocumentFieldValueConverter<? super GeoPoint, GeoPoint> rawIndexToProjectionConverter =
				createFromDocumentRawConverter();

		return new ElasticsearchIndexFieldType<>(
				codec,
				new ElasticsearchGeoPointFieldPredicateBuilderFactory( resolvedSearchable ),
				new ElasticsearchGeoPointFieldSortBuilderFactory( resolvedSortable ),
				new ElasticsearchGeoPointFieldProjectionBuilderFactory(
						resolvedProjectable, indexToProjectionConverter, rawIndexToProjectionConverter, codec
				),
				new ElasticsearchGeoPointFieldAggregationBuilderFactory(
						resolvedAggregable,
						dslToIndexConverter,
						indexToProjectionConverter,
						codec
				),
				mapping
		);
	}

	@Override
	protected ElasticsearchGeoPointIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}

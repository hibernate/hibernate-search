/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchGeoPointFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchGeoPointFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchGeoPointFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchGeoPointFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.spatial.GeoPoint;


class ElasticsearchGeoPointIndexFieldTypeContext
		extends AbstractElasticsearchScalarFieldTypeContext<ElasticsearchGeoPointIndexFieldTypeContext, GeoPoint> {

	ElasticsearchGeoPointIndexFieldTypeContext(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, GeoPoint.class, DataType.GEO_POINT );
	}

	@Override
	protected ElasticsearchIndexFieldType<GeoPoint> toIndexFieldType(PropertyMapping mapping) {
		FromDocumentFieldValueConverter<? super GeoPoint, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();
		ElasticsearchGeoPointFieldCodec codec = ElasticsearchGeoPointFieldCodec.INSTANCE;

		return new ElasticsearchIndexFieldType<>(
				codec,
				new ElasticsearchGeoPointFieldPredicateBuilderFactory( resolvedSearchable ),
				new ElasticsearchGeoPointFieldSortBuilderFactory( resolvedSortable ),
				new ElasticsearchGeoPointFieldProjectionBuilderFactory( resolvedProjectable, indexToProjectionConverter, createFromDocumentRawConverter(), codec ),
				mapping
		);
	}

	@Override
	protected ElasticsearchGeoPointIndexFieldTypeContext thisAsS() {
		return this;
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.sort.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilder;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchGeoPointFieldCodec;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

public class ElasticsearchGeoPointFieldSortBuilderFactory
		extends AbstractElasticsearchFieldSortBuilderFactory<GeoPoint> {

	public ElasticsearchGeoPointFieldSortBuilderFactory(boolean sortable) {
		super( sortable, ElasticsearchGeoPointFieldCodec.INSTANCE );
	}

	@Override
	public FieldSortBuilder<ElasticsearchSearchSortBuilder> createFieldSortBuilder(
			ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<GeoPoint> field) {
		throw log.traditionalSortNotSupportedByGeoPoint( field.eventContext() );
	}

	@Override
	public DistanceSortBuilder<ElasticsearchSearchSortBuilder> createDistanceSortBuilder(
			ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<GeoPoint> field,
			GeoPoint center) {
		checkSortable( field );
		return new ElasticsearchDistanceSortBuilder( searchContext, field, center );
	}

}

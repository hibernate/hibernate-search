/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.GeoPointFieldCodec;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

import com.google.gson.JsonObject;

public class DistanceSortBuilderImpl extends AbstractSearchSortBuilder
		implements DistanceSortBuilder<ElasticsearchSearchSortBuilder> {

	private static final JsonObjectAccessor GEO_DISTANCE = JsonAccessor.root().property( "_geo_distance" ).asObject();

	private final String absoluteFieldPath;
	private final GeoPoint location;

	public DistanceSortBuilderImpl(String absoluteFieldPath, GeoPoint location) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.location = location;
	}

	@Override
	protected void doBuildAndAddTo(ElasticsearchSearchSortCollector collector) {
		getInnerObject().add( absoluteFieldPath, GeoPointFieldCodec.INSTANCE.encode( location ) );

		JsonObject outerObject = new JsonObject();
		GEO_DISTANCE.add( outerObject, getInnerObject() );
		collector.collectDistanceSort( outerObject, absoluteFieldPath, location );
	}
}

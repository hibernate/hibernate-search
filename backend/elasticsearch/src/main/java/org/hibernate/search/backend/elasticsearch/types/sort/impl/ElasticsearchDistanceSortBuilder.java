/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.sort.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilder;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortCollector;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchGeoPointFieldCodec;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

public class ElasticsearchDistanceSortBuilder extends AbstractElasticsearchDocumentValueSortBuilder
		implements DistanceSortBuilder<ElasticsearchSearchSortBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonObjectAccessor GEO_DISTANCE_ACCESSOR = JsonAccessor.root().property( "_geo_distance" ).asObject();

	private final GeoPoint location;

	public ElasticsearchDistanceSortBuilder(ElasticsearchSearchContext searchContext, String absoluteFieldPath,
			List<String> nestedPathHierarchy, GeoPoint location) {
		super( absoluteFieldPath, nestedPathHierarchy, searchContext.searchSyntax() );
		this.location = location;
	}

	@Override
	public void mode(SortMode mode) {
		switch ( mode ) {
			case MIN:
			case MAX:
			case AVG:
			case MEDIAN:
				super.mode( mode );
				break;
			case SUM:
			default:
				throw log.cannotComputeSumForDistanceSort( getEventContext() );
		}
	}

	@Override
	protected void doBuildAndAddTo(ElasticsearchSearchSortCollector collector, JsonObject innerObject) {
		innerObject.add( absoluteFieldPath, ElasticsearchGeoPointFieldCodec.INSTANCE.encode( location ) );

		JsonObject outerObject = new JsonObject();
		GEO_DISTANCE_ACCESSOR.add( outerObject, innerObject );
		collector.collectDistanceSort( outerObject, absoluteFieldPath, location );
	}
}

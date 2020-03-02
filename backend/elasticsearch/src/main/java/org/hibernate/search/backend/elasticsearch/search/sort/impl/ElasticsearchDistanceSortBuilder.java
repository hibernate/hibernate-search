/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchGeoPointFieldCodec;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

import com.google.gson.JsonObject;
import org.hibernate.search.engine.search.sort.dsl.SortMultiValue;

public class ElasticsearchDistanceSortBuilder extends AbstractElasticsearchSearchNestedSortBuilder
		implements DistanceSortBuilder<ElasticsearchSearchSortBuilder> {

	private static final JsonObjectAccessor GEO_DISTANCE_ACCESSOR = JsonAccessor.root().property( "_geo_distance" ).asObject();

	private final String absoluteFieldPath;
	private final GeoPoint location;

	public ElasticsearchDistanceSortBuilder(ElasticsearchSearchContext searchContext, String absoluteFieldPath, List<String> nestedPathHierarchy, GeoPoint location) {
		super( nestedPathHierarchy, searchContext.getSearchSyntax() );
		this.absoluteFieldPath = absoluteFieldPath;
		this.location = location;
	}

	@Override
	protected void doBuildAndAddTo(ElasticsearchSearchSortCollector collector, JsonObject innerObject) {
		innerObject.add( absoluteFieldPath, ElasticsearchGeoPointFieldCodec.INSTANCE.encode( location ) );

		JsonObject outerObject = new JsonObject();
		GEO_DISTANCE_ACCESSOR.add( outerObject, innerObject );
		collector.collectDistanceSort( outerObject, absoluteFieldPath, location );
	}

	@Override
	public void multi(SortMultiValue multi) {
		throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
	}
}

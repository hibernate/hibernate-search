/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateCollector;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionExecutionContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionExecutionContext.DistanceSortKey;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortCollector;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.common.CollectionHelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ElasticsearchSearchQueryElementCollector
		implements ElasticsearchSearchPredicateCollector, ElasticsearchSearchSortCollector {

	private JsonObject jsonPredicate;
	private JsonArray jsonSort;

	private Map<DistanceSortKey, Integer> distanceSorts;

	@Override
	public void collectPredicate(JsonObject jsonQuery) {
		this.jsonPredicate = jsonQuery;
	}

	@Override
	public void collectSort(JsonElement sort) {
		if ( jsonSort == null ) {
			jsonSort = new JsonArray();
		}
		this.jsonSort.add( sort );
	}

	@Override
	public void collectDistanceSort(JsonElement sort, String absoluteFieldPath, GeoPoint center) {
		collectSort( sort );

		int index = jsonSort.size() - 1;
		if ( distanceSorts == null ) {
			distanceSorts = CollectionHelper.newHashMap( 3 );
		}

		distanceSorts.put( new DistanceSortKey( absoluteFieldPath, center ), index );
	}

	@Override
	public void collectSort(JsonArray sorts) {
		if ( jsonSort == null ) {
			jsonSort = new JsonArray();
		}
		this.jsonSort.addAll( sorts );
	}

	public JsonObject toJsonPredicate() {
		return jsonPredicate;
	}

	public JsonArray toJsonSort() {
		return jsonSort;
	}

	public SearchProjectionExecutionContext toSearchProjectionExecutionContext(SessionContextImplementor sessionContext) {
		return new SearchProjectionExecutionContext( sessionContext, distanceSorts );
	}
}

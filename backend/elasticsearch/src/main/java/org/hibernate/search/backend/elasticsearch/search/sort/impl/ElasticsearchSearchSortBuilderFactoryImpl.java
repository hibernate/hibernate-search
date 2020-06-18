/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexesContext;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.spi.CompositeSortBuilder;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

import com.google.gson.JsonObject;

public class ElasticsearchSearchSortBuilderFactoryImpl implements ElasticsearchSearchSortBuilderFactory {

	private final ElasticsearchSearchContext searchContext;
	private final ElasticsearchSearchIndexesContext indexes;

	public ElasticsearchSearchSortBuilderFactoryImpl(ElasticsearchSearchContext searchContext) {
		this.searchContext = searchContext;
		this.indexes = searchContext.indexes();
	}

	@Override
	public void contribute(ElasticsearchSearchSortCollector collector, SearchSort sort) {
		ElasticsearchSearchSort elasticsearchSort = ElasticsearchSearchSort.from( searchContext, sort );
		elasticsearchSort.toJsonSorts( collector );
	}

	@Override
	public ScoreSortBuilder score() {
		return new ElasticsearchScoreSort.Builder( searchContext );
	}

	@Override
	public FieldSortBuilder field(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createFieldSortBuilder( searchContext );
	}

	@Override
	public DistanceSortBuilder distance(String absoluteFieldPath, GeoPoint location) {
		return indexes.field( absoluteFieldPath ).createDistanceSortBuilder( searchContext, location );
	}

	@Override
	public SearchSort indexOrder() {
		return new ElasticsearchIndexOrderSort( searchContext );
	}

	@Override
	public CompositeSortBuilder composite() {
		return new ElasticsearchCompositeSort.Builder( searchContext );
	}

	@Override
	public ElasticsearchSearchSort fromJson(JsonObject jsonObject) {
		return new ElasticsearchUserProvidedJsonSort( searchContext, jsonObject );
	}

	@Override
	public ElasticsearchSearchSort fromJson(String jsonString) {
		return fromJson( searchContext.userFacingGson().fromJson( jsonString, JsonObject.class ) );
	}
}

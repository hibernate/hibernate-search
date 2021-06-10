/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.spi.CompositeSortBuilder;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SortTypeKeys;

import com.google.gson.JsonObject;

public class ElasticsearchSearchSortBuilderFactoryImpl implements ElasticsearchSearchSortBuilderFactory {

	private final ElasticsearchSearchIndexScope scope;

	public ElasticsearchSearchSortBuilderFactoryImpl(ElasticsearchSearchIndexScope scope) {
		this.scope = scope;
	}

	@Override
	public void contribute(ElasticsearchSearchSortCollector collector, SearchSort sort) {
		ElasticsearchSearchSort elasticsearchSort = ElasticsearchSearchSort.from( scope, sort );
		elasticsearchSort.toJsonSorts( collector );
	}

	@Override
	public ScoreSortBuilder score() {
		return new ElasticsearchScoreSort.Builder( scope );
	}

	@Override
	public FieldSortBuilder field(String absoluteFieldPath) {
		return scope.field( absoluteFieldPath ).queryElement( SortTypeKeys.FIELD, scope );
	}

	@Override
	public DistanceSortBuilder distance(String absoluteFieldPath) {
		return scope.field( absoluteFieldPath ).queryElement( SortTypeKeys.DISTANCE, scope );
	}

	@Override
	public SearchSort indexOrder() {
		return new ElasticsearchIndexOrderSort( scope );
	}

	@Override
	public CompositeSortBuilder composite() {
		return new ElasticsearchCompositeSort.Builder( scope );
	}

	@Override
	public ElasticsearchSearchSort fromJson(JsonObject jsonObject) {
		return new ElasticsearchUserProvidedJsonSort( scope, jsonObject );
	}

	@Override
	public ElasticsearchSearchSort fromJson(String jsonString) {
		return fromJson( scope.userFacingGson().fromJson( jsonString, JsonObject.class ) );
	}
}

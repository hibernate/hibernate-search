/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexesContext;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

public class ElasticsearchSearchSortBuilderFactoryImpl implements ElasticsearchSearchSortBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchSearchContext searchContext;
	private final ElasticsearchSearchIndexesContext indexes;

	public ElasticsearchSearchSortBuilderFactoryImpl(ElasticsearchSearchContext searchContext) {
		this.searchContext = searchContext;
		this.indexes = searchContext.indexes();
	}

	@Override
	public SearchSort toSearchSort(List<ElasticsearchSearchSortBuilder> builders) {
		return new ElasticsearchSearchSort( builders, indexes.hibernateSearchIndexNames() );
	}

	@Override
	public ElasticsearchSearchSortBuilder toImplementation(SearchSort sort) {
		if ( !( sort instanceof ElasticsearchSearchSort ) ) {
			throw log.cannotMixElasticsearchSearchSortWithOtherSorts( sort );
		}
		ElasticsearchSearchSort casted = (ElasticsearchSearchSort) sort;
		if ( !indexes.hibernateSearchIndexNames().equals( casted.getIndexNames() ) ) {
			throw log.sortDefinedOnDifferentIndexes( sort, casted.getIndexNames(), indexes.hibernateSearchIndexNames() );
		}
		return casted;
	}

	@Override
	public void contribute(ElasticsearchSearchSortCollector collector, ElasticsearchSearchSortBuilder builder) {
		builder.buildAndAddTo( collector );
	}

	@Override
	public ScoreSortBuilder<ElasticsearchSearchSortBuilder> score() {
		return new ElasticsearchScoreSortBuilder();
	}

	@Override
	public FieldSortBuilder<ElasticsearchSearchSortBuilder> field(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createFieldSortBuilder( searchContext );
	}

	@Override
	public DistanceSortBuilder<ElasticsearchSearchSortBuilder> distance(String absoluteFieldPath, GeoPoint location) {
		return indexes.field( absoluteFieldPath ).createDistanceSortBuilder( searchContext, location );
	}

	@Override
	public ElasticsearchSearchSortBuilder indexOrder() {
		return ElasticsearchIndexOrderSortBuilder.INSTANCE;
	}

	@Override
	public ElasticsearchSearchSortBuilder fromJson(JsonObject jsonObject) {
		return new ElasticsearchUserProvidedJsonSortBuilder( jsonObject );
	}

	@Override
	public ElasticsearchSearchSortBuilder fromJson(String jsonString) {
		return fromJson( searchContext.userFacingGson().fromJson( jsonString, JsonObject.class ) );
	}
}

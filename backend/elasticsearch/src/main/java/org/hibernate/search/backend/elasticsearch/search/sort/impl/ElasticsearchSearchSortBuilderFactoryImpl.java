/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchScopedIndexFieldComponent;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchScopeModel;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.IndexSchemaFieldNodeComponentRetrievalStrategy;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchFieldSortBuilderFactory;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

public class ElasticsearchSearchSortBuilderFactoryImpl implements ElasticsearchSearchSortBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final SortBuilderFactoryRetrievalStrategy SORT_BUILDER_FACTORY_RETRIEVAL_STRATEGY =
			new SortBuilderFactoryRetrievalStrategy();

	private final ElasticsearchSearchContext searchContext;

	private final ElasticsearchScopeModel scopeModel;

	public ElasticsearchSearchSortBuilderFactoryImpl(ElasticsearchSearchContext searchContext,
			ElasticsearchScopeModel scopeModel) {
		this.searchContext = searchContext;
		this.scopeModel = scopeModel;
	}

	@Override
	public SearchSort toSearchSort(List<ElasticsearchSearchSortBuilder> builders) {
		return new ElasticsearchSearchSort( builders, scopeModel.getHibernateSearchIndexNames() );
	}

	@Override
	public ElasticsearchSearchSortBuilder toImplementation(SearchSort sort) {
		if ( !( sort instanceof ElasticsearchSearchSort ) ) {
			throw log.cannotMixElasticsearchSearchSortWithOtherSorts( sort );
		}
		ElasticsearchSearchSort casted = (ElasticsearchSearchSort) sort;
		if ( !scopeModel.getHibernateSearchIndexNames().equals( casted.getIndexNames() ) ) {
			throw log.sortDefinedOnDifferentIndexes( sort, casted.getIndexNames(), scopeModel.getHibernateSearchIndexNames() );
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
		ElasticsearchScopedIndexFieldComponent<ElasticsearchFieldSortBuilderFactory> fieldComponent = scopeModel
				.getSchemaNodeComponent( absoluteFieldPath, SORT_BUILDER_FACTORY_RETRIEVAL_STRATEGY );
		return fieldComponent.getComponent().createFieldSortBuilder( searchContext, absoluteFieldPath, scopeModel.getNestedPathHierarchy( absoluteFieldPath ),
				fieldComponent.getConverterCompatibilityChecker() );
	}

	@Override
	public DistanceSortBuilder<ElasticsearchSearchSortBuilder> distance(String absoluteFieldPath, GeoPoint location) {
		return scopeModel
				.getSchemaNodeComponent( absoluteFieldPath, SORT_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.getComponent().createDistanceSortBuilder( searchContext, absoluteFieldPath, scopeModel.getNestedPathHierarchy( absoluteFieldPath ), location );
	}

	@Override
	public ElasticsearchSearchSortBuilder indexOrder() {
		return ElasticsearchIndexOrderSortBuilder.INSTANCE;
	}

	@Override
	public ElasticsearchSearchSortBuilder fromJson(String jsonString) {
		return new ElasticsearchUserProvidedJsonSortBuilder(
				searchContext.getUserFacingGson().fromJson( jsonString, JsonObject.class )
		);
	}

	private static class SortBuilderFactoryRetrievalStrategy
			implements IndexSchemaFieldNodeComponentRetrievalStrategy<ElasticsearchFieldSortBuilderFactory> {

		@Override
		public ElasticsearchFieldSortBuilderFactory extractComponent(ElasticsearchIndexSchemaFieldNode<?> schemaNode) {
			return schemaNode.getSortBuilderFactory();
		}

		@Override
		public boolean hasCompatibleCodec(ElasticsearchFieldSortBuilderFactory component1, ElasticsearchFieldSortBuilderFactory component2) {
			return component1.hasCompatibleCodec( component2 );
		}

		@Override
		public boolean hasCompatibleConverter(ElasticsearchFieldSortBuilderFactory component1, ElasticsearchFieldSortBuilderFactory component2) {
			return component1.hasCompatibleConverter( component2 );
		}

		@Override
		public boolean hasCompatibleAnalyzer(ElasticsearchFieldSortBuilderFactory component1, ElasticsearchFieldSortBuilderFactory component2) {
			// analyzers are not involved in a sort clause
			return true;
		}

		@Override
		public SearchException createCompatibilityException(String absoluteFieldPath,
				ElasticsearchFieldSortBuilderFactory component1, ElasticsearchFieldSortBuilderFactory component2,
				EventContext context) {
			return log.conflictingFieldTypesForSort( absoluteFieldPath, component1, component2, context );
		}
	}
}

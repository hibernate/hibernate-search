/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchScopeModel;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchScopedIndexFieldComponent;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.IndexSchemaFieldNodeComponentRetrievalStrategy;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchFieldAggregationBuilderFactory;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.JsonObject;

public class ElasticsearchSearchAggregationBuilderFactory
		implements SearchAggregationBuilderFactory<ElasticsearchSearchAggregationCollector> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final AggregationBuilderFactoryRetrievalStrategy AGGREGATION_BUILDER_FACTORY_RETRIEVAL_STRATEGY =
			new AggregationBuilderFactoryRetrievalStrategy();

	private final ElasticsearchSearchContext searchContext;
	private final ElasticsearchScopeModel scopeModel;

	public ElasticsearchSearchAggregationBuilderFactory(ElasticsearchSearchContext searchContext,
			ElasticsearchScopeModel scopeModel) {
		this.searchContext = searchContext;
		this.scopeModel = scopeModel;
	}

	@Override
	public <A> void contribute(ElasticsearchSearchAggregationCollector collector,
			AggregationKey<A> key, SearchAggregation<A> aggregation) {
		if ( !( aggregation instanceof ElasticsearchSearchAggregation ) ) {
			throw log.cannotMixElasticsearchSearchQueryWithOtherAggregations( aggregation );
		}

		ElasticsearchSearchAggregation<A> casted = (ElasticsearchSearchAggregation<A>) aggregation;
		if ( !scopeModel.hibernateSearchIndexNames().equals( casted.getIndexNames() ) ) {
			throw log.aggregationDefinedOnDifferentIndexes(
					aggregation, casted.getIndexNames(), scopeModel.hibernateSearchIndexNames()
			);
		}

		collector.collectAggregation( key, casted );
	}

	@Override
	public <T> TermsAggregationBuilder<T> createTermsAggregationBuilder(String absoluteFieldPath, Class<T> expectedType,
			ValueConvert convert) {
		ElasticsearchScopedIndexFieldComponent<ElasticsearchFieldAggregationBuilderFactory> fieldComponent =
				scopeModel.schemaNodeComponent( absoluteFieldPath, AGGREGATION_BUILDER_FACTORY_RETRIEVAL_STRATEGY );
		checkConverterCompatibility( fieldComponent, convert );
		return fieldComponent.getComponent().createTermsAggregationBuilder(
				searchContext, absoluteFieldPath,
				scopeModel.nestedPathHierarchyForField( absoluteFieldPath ),
				expectedType, convert
		);
	}

	@Override
	public <T> RangeAggregationBuilder<T> createRangeAggregationBuilder(String absoluteFieldPath, Class<T> expectedType,
			ValueConvert convert) {
		ElasticsearchScopedIndexFieldComponent<ElasticsearchFieldAggregationBuilderFactory> fieldComponent =
				scopeModel.schemaNodeComponent( absoluteFieldPath, AGGREGATION_BUILDER_FACTORY_RETRIEVAL_STRATEGY );
		checkConverterCompatibility( fieldComponent, convert );
		return fieldComponent.getComponent().createRangeAggregationBuilder(
				searchContext, absoluteFieldPath,
				scopeModel.nestedPathHierarchyForField( absoluteFieldPath ),
				expectedType, convert
		);
	}

	public SearchAggregationBuilder<JsonObject> fromJson(JsonObject jsonObject) {
		return new ElasticsearchUserProvidedJsonAggregation.Builder( searchContext, jsonObject );
	}

	public SearchAggregationBuilder<JsonObject> fromJson(String jsonString) {
		return fromJson( searchContext.userFacingGson().fromJson( jsonString, JsonObject.class ) );
	}

	private void checkConverterCompatibility(
			ElasticsearchScopedIndexFieldComponent<ElasticsearchFieldAggregationBuilderFactory> fieldComponent,
			ValueConvert convert) {
		switch ( convert ) {
			case NO:
				break;
			case YES:
			default:
				fieldComponent.getConverterCompatibilityChecker().failIfNotCompatible();
				break;
		}
	}

	private static class AggregationBuilderFactoryRetrievalStrategy
			implements IndexSchemaFieldNodeComponentRetrievalStrategy<ElasticsearchFieldAggregationBuilderFactory> {

		@Override
		public ElasticsearchFieldAggregationBuilderFactory extractComponent(ElasticsearchIndexSchemaFieldNode<?> schemaNode) {
			return schemaNode.type().aggregationBuilderFactory();
		}

		@Override
		public boolean hasCompatibleCodec(ElasticsearchFieldAggregationBuilderFactory component1, ElasticsearchFieldAggregationBuilderFactory component2) {
			return component1.hasCompatibleCodec( component2 );
		}

		@Override
		public boolean hasCompatibleConverter(ElasticsearchFieldAggregationBuilderFactory component1, ElasticsearchFieldAggregationBuilderFactory component2) {
			return component1.hasCompatibleConverter( component2 );
		}

		@Override
		public boolean hasCompatibleAnalyzer(ElasticsearchFieldAggregationBuilderFactory component1, ElasticsearchFieldAggregationBuilderFactory component2) {
			// analyzers are not involved in a aggregation clause
			return true;
		}

		@Override
		public SearchException createCompatibilityException(String absoluteFieldPath,
				ElasticsearchFieldAggregationBuilderFactory component1,
				ElasticsearchFieldAggregationBuilderFactory component2,
				EventContext context) {
			return log.conflictingFieldTypesForSearch( absoluteFieldPath, component1, component2, context );
		}
	}
}

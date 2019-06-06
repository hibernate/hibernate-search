/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchScopedIndexFieldComponent;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchScopeModel;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.IndexSchemaFieldNodeComponentRetrievalStrategy;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchFieldPredicateBuilderFactory;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchIdPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

public class ElasticsearchSearchPredicateBuilderFactoryImpl implements ElasticsearchSearchPredicateBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	static final PredicateBuilderFactoryRetrievalStrategy PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY =
			new PredicateBuilderFactoryRetrievalStrategy();

	private final ElasticsearchSearchContext searchContext;

	private final ElasticsearchScopeModel scopeModel;

	public ElasticsearchSearchPredicateBuilderFactoryImpl(ElasticsearchSearchContext searchContext,
			ElasticsearchScopeModel scopeModel) {
		this.searchContext = searchContext;
		this.scopeModel = scopeModel;
	}

	@Override
	public SearchPredicate toSearchPredicate(ElasticsearchSearchPredicateBuilder builder) {
		return new ElasticsearchSearchPredicate( builder );
	}

	@Override
	public ElasticsearchSearchPredicateBuilder toImplementation(SearchPredicate predicate) {
		if ( !( predicate instanceof ElasticsearchSearchPredicate ) ) {
			throw log.cannotMixElasticsearchSearchQueryWithOtherPredicates( predicate );
		}
		return (ElasticsearchSearchPredicate) predicate;
	}

	@Override
	public void contribute(ElasticsearchSearchPredicateCollector collector,
			ElasticsearchSearchPredicateBuilder builder) {
		collector.collectPredicate( builder.build( collector.getRootPredicateContext() ) );
	}

	@Override
	public MatchAllPredicateBuilder<ElasticsearchSearchPredicateBuilder> matchAll() {
		return new ElasticsearchMatchAllPredicateBuilder();
	}

	@Override
	public MatchIdPredicateBuilder<ElasticsearchSearchPredicateBuilder> id() {
		return new ElasticsearchMatchIdPredicateBuilder( searchContext, scopeModel.getIdDslConverter() );
	}

	@Override
	public BooleanJunctionPredicateBuilder<ElasticsearchSearchPredicateBuilder> bool() {
		return new ElasticsearchBooleanJunctionPredicateBuilder();
	}

	@Override
	public MatchPredicateBuilder<ElasticsearchSearchPredicateBuilder> match(String absoluteFieldPath) {
		ElasticsearchScopedIndexFieldComponent<ElasticsearchFieldPredicateBuilderFactory> fieldComponent = scopeModel.getSchemaNodeComponent(
				absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY );
		return fieldComponent.getComponent().createMatchPredicateBuilder( searchContext, absoluteFieldPath, fieldComponent.getConverterCompatibilityChecker(),
				fieldComponent.getAnalyzerCompatibilityChecker() );
	}

	@Override
	public RangePredicateBuilder<ElasticsearchSearchPredicateBuilder> range(String absoluteFieldPath) {
		ElasticsearchScopedIndexFieldComponent<ElasticsearchFieldPredicateBuilderFactory> fieldComponent = scopeModel.getSchemaNodeComponent(
				absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY );
		return fieldComponent.getComponent().createRangePredicateBuilder( searchContext, absoluteFieldPath, fieldComponent.getConverterCompatibilityChecker() );
	}

	@Override
	public PhrasePredicateBuilder<ElasticsearchSearchPredicateBuilder> phrase(String absoluteFieldPath) {
		ElasticsearchScopedIndexFieldComponent<ElasticsearchFieldPredicateBuilderFactory> fieldComponent = scopeModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY );
		return fieldComponent.getComponent().createPhrasePredicateBuilder( absoluteFieldPath, fieldComponent.getAnalyzerCompatibilityChecker() );
	}

	@Override
	public WildcardPredicateBuilder<ElasticsearchSearchPredicateBuilder> wildcard(String absoluteFieldPath) {
		return scopeModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.getComponent().createWildcardPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public SimpleQueryStringPredicateBuilder<ElasticsearchSearchPredicateBuilder> simpleQueryString() {
		return new ElasticsearchSimpleQueryStringPredicateBuilder( scopeModel );
	}

	@Override
	public ExistsPredicateBuilder<ElasticsearchSearchPredicateBuilder> exists(String absoluteFieldPath) {
		// Make sure to fail for fields with different type or for unknown fields
		// We may be able to relax this constraint, but that would require more extensive testing
		scopeModel.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY );
		return new ElasticsearchExistsPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public SpatialWithinCirclePredicateBuilder<ElasticsearchSearchPredicateBuilder> spatialWithinCircle(
			String absoluteFieldPath) {
		return scopeModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.getComponent().createSpatialWithinCirclePredicateBuilder( absoluteFieldPath );
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder<ElasticsearchSearchPredicateBuilder> spatialWithinPolygon(
			String absoluteFieldPath) {
		return scopeModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.getComponent().createSpatialWithinPolygonPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder<ElasticsearchSearchPredicateBuilder> spatialWithinBoundingBox(
			String absoluteFieldPath) {
		return scopeModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.getComponent().createSpatialWithinBoundingBoxPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public NestedPredicateBuilder<ElasticsearchSearchPredicateBuilder> nested(String absoluteFieldPath) {
		scopeModel.checkNestedField( absoluteFieldPath );
		return new ElasticsearchNestedPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public ElasticsearchSearchPredicateBuilder fromJson(String jsonString) {
		return new ElasticsearchUserProvidedJsonPredicateContributor(
				searchContext.getUserFacingGson().fromJson( jsonString, JsonObject.class )
		);
	}

	private static class PredicateBuilderFactoryRetrievalStrategy
			implements IndexSchemaFieldNodeComponentRetrievalStrategy<ElasticsearchFieldPredicateBuilderFactory> {

		@Override
		public ElasticsearchFieldPredicateBuilderFactory extractComponent(ElasticsearchIndexSchemaFieldNode<?> schemaNode) {
			return schemaNode.getPredicateBuilderFactory();
		}

		@Override
		public boolean hasCompatibleCodec(ElasticsearchFieldPredicateBuilderFactory component1, ElasticsearchFieldPredicateBuilderFactory component2) {
			return component1.hasCompatibleCodec( component2 );
		}

		@Override
		public boolean hasCompatibleConverter(ElasticsearchFieldPredicateBuilderFactory component1, ElasticsearchFieldPredicateBuilderFactory component2) {
			return component1.hasCompatibleConverter( component2 );
		}

		@Override
		public boolean hasCompatibleAnalyzer(ElasticsearchFieldPredicateBuilderFactory component1, ElasticsearchFieldPredicateBuilderFactory component2) {
			return component1.hasCompatibleAnalyzer( component2 );
		}

		@Override
		public SearchException createCompatibilityException(String absoluteFieldPath,
				ElasticsearchFieldPredicateBuilderFactory component1, ElasticsearchFieldPredicateBuilderFactory component2,
				EventContext context) {
			return log.conflictingFieldTypesForPredicate( absoluteFieldPath, component1, component2, context );
		}
	}
}

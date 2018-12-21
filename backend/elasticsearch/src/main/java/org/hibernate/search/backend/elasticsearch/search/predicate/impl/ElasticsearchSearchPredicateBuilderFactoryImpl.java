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
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchTargetModel;
import org.hibernate.search.backend.elasticsearch.search.impl.IndexSchemaFieldNodeComponentRetrievalStrategy;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchFieldPredicateBuilderFactory;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchIdPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.util.EventContext;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
// TODO have one version of the factory per Elasticsearch dialect, if necessary
public class ElasticsearchSearchPredicateBuilderFactoryImpl implements ElasticsearchSearchPredicateBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final PredicateBuilderFactoryRetrievalStrategy PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY =
			new PredicateBuilderFactoryRetrievalStrategy();

	private final ElasticsearchSearchContext searchContext;

	private final ElasticsearchSearchTargetModel searchTargetModel;

	public ElasticsearchSearchPredicateBuilderFactoryImpl(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchTargetModel searchTargetModel) {
		this.searchContext = searchContext;
		this.searchTargetModel = searchTargetModel;
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
		return new ElasticsearchMatchIdPredicateBuilder( searchContext, searchTargetModel.getIdDslConverter() );
	}

	@Override
	public BooleanJunctionPredicateBuilder<ElasticsearchSearchPredicateBuilder> bool() {
		return new ElasticsearchBooleanJunctionPredicateBuilder();
	}

	@Override
	public MatchPredicateBuilder<ElasticsearchSearchPredicateBuilder> match(String absoluteFieldPath) {
		return searchTargetModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.createMatchPredicateBuilder( searchContext, absoluteFieldPath );
	}

	@Override
	public RangePredicateBuilder<ElasticsearchSearchPredicateBuilder> range(String absoluteFieldPath) {
		return searchTargetModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.createRangePredicateBuilder( searchContext, absoluteFieldPath );
	}

	@Override
	public SpatialWithinCirclePredicateBuilder<ElasticsearchSearchPredicateBuilder> spatialWithinCircle(
			String absoluteFieldPath) {
		return searchTargetModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.createSpatialWithinCirclePredicateBuilder( absoluteFieldPath );
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder<ElasticsearchSearchPredicateBuilder> spatialWithinPolygon(
			String absoluteFieldPath) {
		return searchTargetModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.createSpatialWithinPolygonPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder<ElasticsearchSearchPredicateBuilder> spatialWithinBoundingBox(
			String absoluteFieldPath) {
		return searchTargetModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.createSpatialWithinBoundingBoxPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public NestedPredicateBuilder<ElasticsearchSearchPredicateBuilder> nested(String absoluteFieldPath) {
		searchTargetModel.checkNestedField( absoluteFieldPath );
		return new ElasticsearchNestedPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public ElasticsearchSearchPredicateBuilder fromJsonString(String jsonString) {
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
		public boolean areCompatible(ElasticsearchFieldPredicateBuilderFactory component1,
				ElasticsearchFieldPredicateBuilderFactory component2) {
			return component1.isDslCompatibleWith( component2 );
		}

		@Override
		public SearchException createCompatibilityException(String absoluteFieldPath,
				ElasticsearchFieldPredicateBuilderFactory component1, ElasticsearchFieldPredicateBuilderFactory component2,
				EventContext context) {
			return log.conflictingFieldTypesForPredicate( absoluteFieldPath, component1, component2, context );
		}
	}
}

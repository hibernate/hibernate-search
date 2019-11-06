/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.scope.model.impl.IndexSchemaFieldNodeComponentRetrievalStrategy;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopedIndexFieldComponent;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopedIndexObjectComponent;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopeModel;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneExistsCompositePredicateBuilder;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneFieldPredicateBuilderFactory;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;
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


public class LuceneSearchPredicateBuilderFactoryImpl implements LuceneSearchPredicateBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	static final PredicateBuilderFactoryRetrievalStrategy PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY =
			new PredicateBuilderFactoryRetrievalStrategy();

	private final LuceneSearchContext searchContext;
	private final LuceneScopeModel scopeModel;

	public LuceneSearchPredicateBuilderFactoryImpl(LuceneSearchContext searchContext,
			LuceneScopeModel scopeModel) {
		this.searchContext = searchContext;
		this.scopeModel = scopeModel;
	}

	@Override
	public SearchPredicate toSearchPredicate(LuceneSearchPredicateBuilder builder) {
		return new LuceneSearchPredicate( scopeModel.getIndexNames(), builder );
	}

	@Override
	public LuceneSearchPredicateBuilder toImplementation(SearchPredicate predicate) {
		if ( !( predicate instanceof LuceneSearchPredicate ) ) {
			throw log.cannotMixLuceneSearchQueryWithOtherPredicates( predicate );
		}
		LuceneSearchPredicate casted = (LuceneSearchPredicate) predicate;
		if ( !scopeModel.getIndexNames().equals( casted.getIndexNames() ) ) {
			throw log.predicateDefinedOnDifferentIndexes( predicate, casted.getIndexNames(), scopeModel.getIndexNames() );
		}
		return casted;
	}

	@Override
	public void contribute(LuceneSearchPredicateCollector collector,
			LuceneSearchPredicateBuilder builder) {
		collector.collectPredicate( builder.build( LuceneSearchPredicateContext.root() ) );
	}

	@Override
	public MatchAllPredicateBuilder<LuceneSearchPredicateBuilder> matchAll() {
		return new LuceneMatchAllPredicateBuilder();
	}

	@Override
	public MatchIdPredicateBuilder<LuceneSearchPredicateBuilder> id() {
		return new LuceneMatchIdPredicateBuilder( searchContext, scopeModel.getIdDslConverter() );
	}

	@Override
	public BooleanPredicateBuilder<LuceneSearchPredicateBuilder> bool() {
		return new LuceneBooleanPredicateBuilder();
	}

	@Override
	public MatchPredicateBuilder<LuceneSearchPredicateBuilder> match(String absoluteFieldPath) {
		LuceneScopedIndexFieldComponent<LuceneFieldPredicateBuilderFactory> fieldComponent = scopeModel.getSchemaNodeComponent(
				absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY );
		return fieldComponent.getComponent().createMatchPredicateBuilder( searchContext, absoluteFieldPath, fieldComponent.getConverterCompatibilityChecker(),
				fieldComponent.getAnalyzerCompatibilityChecker() );
	}

	@Override
	public RangePredicateBuilder<LuceneSearchPredicateBuilder> range(String absoluteFieldPath) {
		LuceneScopedIndexFieldComponent<LuceneFieldPredicateBuilderFactory> fieldComponent = scopeModel.getSchemaNodeComponent(
				absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY );
		return fieldComponent.getComponent().createRangePredicateBuilder( searchContext, absoluteFieldPath, fieldComponent.getConverterCompatibilityChecker() );
	}

	@Override
	public PhrasePredicateBuilder<LuceneSearchPredicateBuilder> phrase(String absoluteFieldPath) {
		LuceneScopedIndexFieldComponent<LuceneFieldPredicateBuilderFactory> fieldComponent = scopeModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY );
		return fieldComponent.getComponent().createPhrasePredicateBuilder( searchContext, absoluteFieldPath, fieldComponent.getAnalyzerCompatibilityChecker() );
	}

	@Override
	public WildcardPredicateBuilder<LuceneSearchPredicateBuilder> wildcard(String absoluteFieldPath) {
		return scopeModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.getComponent().createWildcardPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public SimpleQueryStringPredicateBuilder<LuceneSearchPredicateBuilder> simpleQueryString() {
		return new LuceneSimpleQueryStringPredicateBuilder( searchContext, scopeModel );
	}

	@Override
	public ExistsPredicateBuilder<LuceneSearchPredicateBuilder> exists(String absoluteFieldPath) {
		// trying object node first
		LuceneScopedIndexObjectComponent<LuceneFieldPredicateBuilderFactory> schemaObjectNodeComponent = scopeModel.getSchemaObjectNodeComponent(
				absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY );
		if ( schemaObjectNodeComponent != null ) {
			LuceneExistsCompositePredicateBuilder objectPredicateBuilder = new LuceneExistsCompositePredicateBuilder();
			for ( Map.Entry<String, LuceneFieldPredicateBuilderFactory> fieldComponentEntry : schemaObjectNodeComponent.getFieldComponents().entrySet() ) {
				ExistsPredicateBuilder<LuceneSearchPredicateBuilder> existsPredicateBuilder = fieldComponentEntry.getValue().createExistsPredicateBuilder(
						fieldComponentEntry.getKey() );
				objectPredicateBuilder.addChild( existsPredicateBuilder );
			}
			return objectPredicateBuilder;
		}

		return scopeModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.getComponent().createExistsPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public SpatialWithinCirclePredicateBuilder<LuceneSearchPredicateBuilder> spatialWithinCircle(String absoluteFieldPath) {
		return scopeModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.getComponent().createSpatialWithinCirclePredicateBuilder( absoluteFieldPath );
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder<LuceneSearchPredicateBuilder> spatialWithinPolygon(String absoluteFieldPath) {
		return scopeModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.getComponent().createSpatialWithinPolygonPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder<LuceneSearchPredicateBuilder> spatialWithinBoundingBox(
			String absoluteFieldPath) {
		return scopeModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.getComponent().createSpatialWithinBoundingBoxPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public NestedPredicateBuilder<LuceneSearchPredicateBuilder> nested(String absoluteFieldPath) {
		scopeModel.checkNestedField( absoluteFieldPath );
		return new LuceneNestedPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public LuceneSearchPredicateBuilder fromLuceneQuery(Query query) {
		return new LuceneUserProvidedLuceneQueryPredicateBuilder( query );
	}

	private static class PredicateBuilderFactoryRetrievalStrategy
			implements IndexSchemaFieldNodeComponentRetrievalStrategy<LuceneFieldPredicateBuilderFactory> {

		@Override
		public LuceneFieldPredicateBuilderFactory extractComponent(LuceneIndexSchemaFieldNode<?> schemaNode) {
			return schemaNode.getPredicateBuilderFactory();
		}

		@Override
		public boolean hasCompatibleCodec(LuceneFieldPredicateBuilderFactory component1, LuceneFieldPredicateBuilderFactory component2) {
			return component1.hasCompatibleCodec( component2 );
		}

		@Override
		public boolean hasCompatibleConverter(LuceneFieldPredicateBuilderFactory component1, LuceneFieldPredicateBuilderFactory component2) {
			return component1.hasCompatibleConverter( component2 );
		}

		@Override
		public boolean hasCompatibleAnalyzer(LuceneFieldPredicateBuilderFactory component1, LuceneFieldPredicateBuilderFactory component2) {
			return component1.hasCompatibleAnalyzer( component2 );
		}

		@Override
		public SearchException createCompatibilityException(String absoluteFieldPath,
				LuceneFieldPredicateBuilderFactory component1, LuceneFieldPredicateBuilderFactory component2,
				EventContext context) {
			return log.conflictingFieldTypesForPredicate( absoluteFieldPath, component1, component2, context );
		}
	}
}

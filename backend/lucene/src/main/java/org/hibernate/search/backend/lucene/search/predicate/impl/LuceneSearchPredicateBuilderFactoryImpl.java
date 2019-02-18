/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.IndexSchemaFieldNodeComponentRetrievalStrategy;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchTargetModel;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneFieldPredicateBuilderFactory;
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
import org.hibernate.search.util.reporting.EventContext;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Guillaume Smet
 */
public class LuceneSearchPredicateBuilderFactoryImpl implements LuceneSearchPredicateBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final PredicateBuilderFactoryRetrievalStrategy PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY =
			new PredicateBuilderFactoryRetrievalStrategy();

	private final LuceneSearchContext searchContext;
	private final LuceneSearchTargetModel searchTargetModel;

	public LuceneSearchPredicateBuilderFactoryImpl(LuceneSearchContext searchContext,
			LuceneSearchTargetModel searchTargetModel) {
		this.searchContext = searchContext;
		this.searchTargetModel = searchTargetModel;
	}

	@Override
	public SearchPredicate toSearchPredicate(LuceneSearchPredicateBuilder builder) {
		return new LuceneSearchPredicate( builder );
	}

	@Override
	public LuceneSearchPredicateBuilder toImplementation(SearchPredicate predicate) {
		if ( !( predicate instanceof LuceneSearchPredicate ) ) {
			throw log.cannotMixLuceneSearchQueryWithOtherPredicates( predicate );
		}
		return (LuceneSearchPredicate) predicate;
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
		return new LuceneMatchIdPredicateBuilder( searchContext, searchTargetModel.getIdDslConverter() );
	}

	@Override
	public BooleanJunctionPredicateBuilder<LuceneSearchPredicateBuilder> bool() {
		return new LuceneBooleanJunctionPredicateBuilder();
	}

	@Override
	public MatchPredicateBuilder<LuceneSearchPredicateBuilder> match(String absoluteFieldPath) {
		return searchTargetModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.createMatchPredicateBuilder( searchContext, absoluteFieldPath );
	}

	@Override
	public RangePredicateBuilder<LuceneSearchPredicateBuilder> range(String absoluteFieldPath) {
		return searchTargetModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.createRangePredicateBuilder( searchContext, absoluteFieldPath );
	}

	@Override
	public SpatialWithinCirclePredicateBuilder<LuceneSearchPredicateBuilder> spatialWithinCircle(String absoluteFieldPath) {
		return searchTargetModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.createSpatialWithinCirclePredicateBuilder( absoluteFieldPath );
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder<LuceneSearchPredicateBuilder> spatialWithinPolygon(String absoluteFieldPath) {
		return searchTargetModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.createSpatialWithinPolygonPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder<LuceneSearchPredicateBuilder> spatialWithinBoundingBox(
			String absoluteFieldPath) {
		return searchTargetModel
				.getSchemaNodeComponent( absoluteFieldPath, PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.createSpatialWithinBoundingBoxPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public NestedPredicateBuilder<LuceneSearchPredicateBuilder> nested(String absoluteFieldPath) {
		searchTargetModel.checkNestedField( absoluteFieldPath );
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
		public boolean areCompatible(LuceneFieldPredicateBuilderFactory component1,
				LuceneFieldPredicateBuilderFactory component2) {
			return component1.isDslCompatibleWith( component2 );
		}

		@Override
		public SearchException createCompatibilityException(String absoluteFieldPath,
				LuceneFieldPredicateBuilderFactory component1, LuceneFieldPredicateBuilderFactory component2,
				EventContext context) {
			return log.conflictingFieldTypesForPredicate( absoluteFieldPath, component1, component2, context );
		}
	}
}

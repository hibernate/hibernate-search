/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchScopeModel;
import org.hibernate.search.backend.elasticsearch.search.impl.IndexSchemaFieldNodeComponentRetrievalStrategy;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchFieldProjectionBuilderFactory;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.predicate.DslConverter;
import org.hibernate.search.engine.search.projection.ProjectionConverter;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ObjectProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ScoreProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.function.TriFunction;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchSearchProjectionBuilderFactory implements SearchProjectionBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ProjectionBuilderFactoryRetrievalStrategy PROJECTION_BUILDER_FACTORY_RETRIEVAL_STRATEGY =
			new ProjectionBuilderFactoryRetrievalStrategy();

	private final SearchProjectionBackendContext searchProjectionBackendContext;

	private final ElasticsearchSearchScopeModel scopeModel;

	public ElasticsearchSearchProjectionBuilderFactory(SearchProjectionBackendContext searchProjectionBackendContext,
			ElasticsearchSearchScopeModel scopeModel) {
		this.searchProjectionBackendContext = searchProjectionBackendContext;
		this.scopeModel = scopeModel;
	}

	@Override
	public DocumentReferenceProjectionBuilder documentReference() {
		return searchProjectionBackendContext.getDocumentReferenceProjectionBuilder();
	}

	@Override
	public <T> FieldProjectionBuilder<T> field(String absoluteFieldPath, Class<T> expectedType, ProjectionConverter projectionConverter) {
		return scopeModel
				.getSchemaNodeComponent( absoluteFieldPath, PROJECTION_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.getComponent().createFieldValueProjectionBuilder( absoluteFieldPath, expectedType, projectionConverter );
	}

	@Override
	public <O> ObjectProjectionBuilder<O> object() {
		return searchProjectionBackendContext.getObjectProjectionBuilder();
	}

	@Override
	public <R> ReferenceProjectionBuilder<R> reference() {
		return searchProjectionBackendContext.getReferenceProjectionBuilder();
	}

	@Override
	public ScoreProjectionBuilder score() {
		return searchProjectionBackendContext.getScoreProjectionBuilder();
	}

	@Override
	public DistanceToFieldProjectionBuilder distance(String absoluteFieldPath, GeoPoint center) {
		return scopeModel
				.getSchemaNodeComponent( absoluteFieldPath, PROJECTION_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.getComponent().createDistanceProjectionBuilder( absoluteFieldPath, center );
	}

	@Override
	public <T> CompositeProjectionBuilder<T> composite(Function<List<?>, T> transformer,
			SearchProjection<?>... projections) {
		List<ElasticsearchSearchProjection<?, ?>> typedProjections = new ArrayList<>( projections.length );
		for ( SearchProjection<?> projection : projections ) {
			typedProjections.add( toImplementation( projection ) );
		}

		return new ElasticsearchCompositeProjectionBuilder<>(
				new ElasticsearchCompositeListProjection<>( transformer, typedProjections )
		);
	}

	@Override
	public <P, T> CompositeProjectionBuilder<T> composite(Function<P, T> transformer,
			SearchProjection<P> projection) {
		return new ElasticsearchCompositeProjectionBuilder<>(
				new ElasticsearchCompositeFunctionProjection<>( transformer, toImplementation( projection ) )
		);
	}

	@Override
	public <P1, P2, T> CompositeProjectionBuilder<T> composite(BiFunction<P1, P2, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2) {
		return new ElasticsearchCompositeProjectionBuilder<>(
				new ElasticsearchCompositeBiFunctionProjection<>( transformer, toImplementation( projection1 ),
						toImplementation( projection2 ) )
		);
	}

	@Override
	public <P1, P2, P3, T> CompositeProjectionBuilder<T> composite(TriFunction<P1, P2, P3, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3) {
		return new ElasticsearchCompositeProjectionBuilder<>(
				new ElasticsearchCompositeTriFunctionProjection<>( transformer, toImplementation( projection1 ),
						toImplementation( projection2 ), toImplementation( projection3 ) )
		);
	}

	public SearchProjectionBuilder<String> source() {
		return searchProjectionBackendContext.getSourceProjectionBuilder();
	}

	public SearchProjectionBuilder<String> explanation() {
		return searchProjectionBackendContext.getExplanationProjectionBuilder();
	}

	@SuppressWarnings("unchecked")
	public <T> ElasticsearchSearchProjection<?, T> toImplementation(SearchProjection<T> projection) {
		if ( !( projection instanceof ElasticsearchSearchProjection ) ) {
			throw log.cannotMixElasticsearchSearchQueryWithOtherProjections( projection );
		}
		return (ElasticsearchSearchProjection<?, T>) projection;
	}

	private static class ProjectionBuilderFactoryRetrievalStrategy
			implements IndexSchemaFieldNodeComponentRetrievalStrategy<ElasticsearchFieldProjectionBuilderFactory> {

		@Override
		public ElasticsearchFieldProjectionBuilderFactory extractComponent(ElasticsearchIndexSchemaFieldNode<?> schemaNode) {
			return schemaNode.getProjectionBuilderFactory();
		}

		@Override
		public boolean areCompatible(ElasticsearchFieldProjectionBuilderFactory component1,
				ElasticsearchFieldProjectionBuilderFactory component2, DslConverter dslConverter) {
			// TODO HSEARCH-3257 handle dslConverter option
			return component1.isDslCompatibleWith( component2 );
		}

		@Override
		public boolean hasCompatibleConverter(ElasticsearchFieldProjectionBuilderFactory component1, ElasticsearchFieldProjectionBuilderFactory component2) {
			return true;
		}

		@Override
		public SearchException createCompatibilityException(String absoluteFieldPath,
				ElasticsearchFieldProjectionBuilderFactory component1,
				ElasticsearchFieldProjectionBuilderFactory component2,
				EventContext context) {
			return log.conflictingFieldTypesForProjection( absoluteFieldPath, component1, component2, context );
		}
	}
}

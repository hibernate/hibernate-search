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
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchScopedIndexFieldComponent;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchScopeModel;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.IndexSchemaFieldNodeComponentRetrievalStrategy;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchFieldProjectionBuilderFactory;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.EntityProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.EntityReferenceProjectionBuilder;
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

	private final ElasticsearchScopeModel scopeModel;

	public ElasticsearchSearchProjectionBuilderFactory(SearchProjectionBackendContext searchProjectionBackendContext,
			ElasticsearchScopeModel scopeModel) {
		this.searchProjectionBackendContext = searchProjectionBackendContext;
		this.scopeModel = scopeModel;
	}

	@Override
	public DocumentReferenceProjectionBuilder documentReference() {
		return searchProjectionBackendContext.createDocumentReferenceProjectionBuilder( scopeModel.getHibernateSearchIndexNames() );
	}

	@Override
	public <T> FieldProjectionBuilder<T> field(String absoluteFieldPath, Class<T> expectedType, ValueConvert convert) {
		ElasticsearchScopedIndexFieldComponent<ElasticsearchFieldProjectionBuilderFactory> fieldComponent =
				scopeModel.getSchemaNodeComponent( absoluteFieldPath, PROJECTION_BUILDER_FACTORY_RETRIEVAL_STRATEGY );
		switch ( convert ) {
			case NO:
				break;
			case YES:
			default:
				fieldComponent.getConverterCompatibilityChecker().failIfNotCompatible();
				break;
		}
		return fieldComponent.getComponent()
				.createFieldValueProjectionBuilder(
						scopeModel.getHibernateSearchIndexNames(), absoluteFieldPath, expectedType, convert
				);
	}

	@Override
	public <E> EntityProjectionBuilder<E> entity() {
		return searchProjectionBackendContext.createEntityProjectionBuilder( scopeModel.getHibernateSearchIndexNames() );
	}

	@Override
	public <R> EntityReferenceProjectionBuilder<R> entityReference() {
		return searchProjectionBackendContext.createReferenceProjectionBuilder( scopeModel.getHibernateSearchIndexNames() );
	}

	@Override
	public ScoreProjectionBuilder score() {
		return searchProjectionBackendContext.createScoreProjectionBuilder( scopeModel.getHibernateSearchIndexNames() );
	}

	@Override
	public DistanceToFieldProjectionBuilder distance(String absoluteFieldPath, GeoPoint center) {
		return scopeModel
				.getSchemaNodeComponent( absoluteFieldPath, PROJECTION_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.getComponent().createDistanceProjectionBuilder( scopeModel.getHibernateSearchIndexNames(), absoluteFieldPath, scopeModel.getNestedDocumentPaths( absoluteFieldPath ), center );
	}

	@Override
	public <P> CompositeProjectionBuilder<P> composite(Function<List<?>, P> transformer,
			SearchProjection<?>... projections) {
		List<ElasticsearchSearchProjection<?, ?>> typedProjections = new ArrayList<>( projections.length );
		for ( SearchProjection<?> projection : projections ) {
			typedProjections.add( toImplementation( projection ) );
		}

		return new ElasticsearchCompositeProjectionBuilder<>(
				new ElasticsearchCompositeListProjection<>( scopeModel.getHibernateSearchIndexNames(), transformer, typedProjections )
		);
	}

	@Override
	public <P1, P> CompositeProjectionBuilder<P> composite(Function<P1, P> transformer,
			SearchProjection<P1> projection) {
		return new ElasticsearchCompositeProjectionBuilder<>(
				new ElasticsearchCompositeFunctionProjection<>( scopeModel.getHibernateSearchIndexNames(), transformer, toImplementation( projection ) )
		);
	}

	@Override
	public <P1, P2, P> CompositeProjectionBuilder<P> composite(BiFunction<P1, P2, P> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2) {
		return new ElasticsearchCompositeProjectionBuilder<>(
				new ElasticsearchCompositeBiFunctionProjection<>( scopeModel.getHibernateSearchIndexNames(), transformer, toImplementation( projection1 ),
						toImplementation( projection2 ) )
		);
	}

	@Override
	public <P1, P2, P3, P> CompositeProjectionBuilder<P> composite(TriFunction<P1, P2, P3, P> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3) {
		return new ElasticsearchCompositeProjectionBuilder<>(
				new ElasticsearchCompositeTriFunctionProjection<>( scopeModel.getHibernateSearchIndexNames(), transformer, toImplementation( projection1 ),
						toImplementation( projection2 ), toImplementation( projection3 ) )
		);
	}

	public SearchProjectionBuilder<String> source() {
		return searchProjectionBackendContext.createSourceProjectionBuilder( scopeModel.getHibernateSearchIndexNames() );
	}

	public SearchProjectionBuilder<String> explanation() {
		return searchProjectionBackendContext.createExplanationProjectionBuilder( scopeModel.getHibernateSearchIndexNames() );
	}

	@SuppressWarnings("unchecked")
	public <T> ElasticsearchSearchProjection<?, T> toImplementation(SearchProjection<T> projection) {
		if ( !( projection instanceof ElasticsearchSearchProjection ) ) {
			throw log.cannotMixElasticsearchSearchQueryWithOtherProjections( projection );
		}
		ElasticsearchSearchProjection<?, T> casted = (ElasticsearchSearchProjection<?, T>) projection;
		if ( !scopeModel.getHibernateSearchIndexNames().equals( casted.getIndexNames() ) ) {
			throw log.projectionDefinedOnDifferentIndexes( projection, casted.getIndexNames(), scopeModel.getHibernateSearchIndexNames() );
		}
		return casted;
	}

	private static class ProjectionBuilderFactoryRetrievalStrategy
			implements IndexSchemaFieldNodeComponentRetrievalStrategy<ElasticsearchFieldProjectionBuilderFactory> {

		@Override
		public ElasticsearchFieldProjectionBuilderFactory extractComponent(ElasticsearchIndexSchemaFieldNode<?> schemaNode) {
			return schemaNode.getProjectionBuilderFactory();
		}

		@Override
		public boolean hasCompatibleCodec(ElasticsearchFieldProjectionBuilderFactory component1, ElasticsearchFieldProjectionBuilderFactory component2) {
			return component1.hasCompatibleCodec( component2 );
		}

		@Override
		public boolean hasCompatibleConverter(ElasticsearchFieldProjectionBuilderFactory component1, ElasticsearchFieldProjectionBuilderFactory component2) {
			return component1.hasCompatibleConverter( component2 );
		}

		@Override
		public boolean hasCompatibleAnalyzer(ElasticsearchFieldProjectionBuilderFactory component1, ElasticsearchFieldProjectionBuilderFactory component2) {
			// analyzers are not involved in a projection clause
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
